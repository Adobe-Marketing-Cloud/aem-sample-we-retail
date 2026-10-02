/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.celadon.aem;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure (Sling-free, JCR-free) readers for the structural parts of a classic AEM commerce product or
 * variant node that the legacy importer needs besides its scalar attributes.
 *
 * <p>Classic nodes reference their image either through an {@code image} child
 * ({@code image/fileReference}) or through an {@code assets} list whose children each carry a
 * {@code fileReference}. A master product declares its configurable axes in
 * {@code cq:productVariantAxes}; its {@code variant} children are only selectable along those axes, so
 * a product that declares none is a simple product in the classic storefront.
 */
public final class LegacyProductStructure {

    private LegacyProductStructure() {
    }

    /**
     * Source image references of a product or variant node: the {@code image} child's
     * {@code fileReference} when present, otherwise the {@code fileReference} of every {@code assets}
     * child in source order. Blank references are skipped.
     */
    public static List<String> imageReferences(Map<String, Object> node) {
        String image = fileReference(node.get("image"));
        if (!image.isEmpty()) {
            return List.of(image);
        }
        List<String> references = new ArrayList<>();
        if (node.get("assets") instanceof Map<?, ?> assets) {
            for (Object asset : assets.values()) {
                String reference = fileReference(asset);
                if (!reference.isEmpty()) {
                    references.add(reference);
                }
            }
        }
        return references;
    }

    /**
     * The axes a master product declares in {@code cq:productVariantAxes}, in declaration order. JCR
     * multi-value properties render as a JSON array and single values as a bare string; both shapes are
     * accepted, and blank or duplicate entries are dropped.
     */
    public static List<String> declaredVariantAxes(Map<String, Object> product) {
        Object raw = product.get("cq:productVariantAxes");
        Set<String> axes = new LinkedHashSet<>();
        if (raw instanceof Iterable<?> values) {
            for (Object value : values) {
                addAxis(axes, value);
            }
        } else if (raw instanceof Object[] values) {
            for (Object value : values) {
                addAxis(axes, value);
            }
        } else {
            addAxis(axes, raw);
        }
        return List.copyOf(axes);
    }

    /**
     * The manifest's variant-scoped {@code SELECT}/{@code MULTISELECT} entries that are among the
     * product's declared axes, in declaration order. These are the only attributes that become
     * configurable options of the product, and the classic storefront lists its variant selectors in
     * that order.
     */
    public static List<AttributeEntry> configurableAxisEntries(AttributeManifest manifest, List<String> declaredAxes) {
        List<AttributeEntry> entries = new ArrayList<>();
        for (String axis : declaredAxes) {
            for (AttributeEntry entry : manifest.entries()) {
                if (entry.code().equals(axis) && entry.scope().appliesToVariant()
                        && (entry.type() == NormalizedType.SELECT || entry.type() == NormalizedType.MULTISELECT)) {
                    entries.add(entry);
                    break;
                }
            }
        }
        return entries;
    }

    private static void addAxis(Set<String> axes, Object value) {
        if (value == null) {
            return;
        }
        String axis = value.toString().trim();
        if (!axis.isEmpty()) {
            axes.add(axis);
        }
    }

    private static String fileReference(Object node) {
        if (node instanceof Map<?, ?> map && map.get("fileReference") != null) {
            return map.get("fileReference").toString().trim();
        }
        return "";
    }
}

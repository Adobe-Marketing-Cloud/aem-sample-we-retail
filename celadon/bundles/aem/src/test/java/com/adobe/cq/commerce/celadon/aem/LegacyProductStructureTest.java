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

import static org.junit.Assert.assertEquals;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LegacyProductStructureTest {

    private static final String SLEEK = "/content/dam/we-retail/en/products/apparel/coats/Sleek.jpg";
    private static final String POLES = "/content/dam/we-retail/en/products/activities/hiking/170227049.jpg";
    private static final String POLES_DETAIL = "/content/dam/we-retail/en/products/activities/hiking/170227049-detail.jpg";

    private static final AttributeManifest MANIFEST = new AttributeManifest("we-retail", List.of(
            AttributeEntry.of("sku", "SKU", NormalizedType.STRING, AttributeScope.BOTH, true, false, 0),
            AttributeEntry.of("brand", "Brand", NormalizedType.SELECT, AttributeScope.PRODUCT, true, true, 10),
            AttributeEntry.of("size", "Size", NormalizedType.SELECT, AttributeScope.BOTH, true, true, 20),
            AttributeEntry.of("price", "Price", NormalizedType.PRICE, AttributeScope.BOTH, true, true, 30),
            AttributeEntry.of("color", "Color", NormalizedType.SELECT, AttributeScope.VARIANT, true, true, 40)
    ));

    /** Builds an ordered map from alternating key/value pairs. */
    private static Map<String, Object> m(Object... kv) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i].toString(), kv[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> imageNode(String fileReference) {
        return m("jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "commerce/components/product/image",
                "fileReference", fileReference);
    }

    private static List<String> axisCodes(List<String> declaredAxes) {
        return LegacyProductStructure.configurableAxisEntries(MANIFEST, declaredAxes).stream()
                .map(AttributeEntry::code)
                .toList();
    }

    @Test
    public void imageReferences_readsTheImageChild() {
        Map<String, Object> product = m("jcr:title", "Sleek Insulated Coat", "image", imageNode(SLEEK));

        assertEquals(List.of(SLEEK), LegacyProductStructure.imageReferences(product));
    }

    @Test
    public void imageReferences_fallsBackToTheAssetsListInSourceOrder() {
        // Shape of the classic we-retail Hiking Poles product: no image child, an assets list instead.
        Map<String, Object> product = m("jcr:title", "Hiking Poles",
                "assets", m("jcr:primaryType", "nt:unstructured",
                        "asset", imageNode(POLES),
                        "asset_0", imageNode(POLES_DETAIL)));

        assertEquals(List.of(POLES, POLES_DETAIL), LegacyProductStructure.imageReferences(product));
    }

    @Test
    public void imageReferences_prefersTheImageChildOverTheAssetsList() {
        Map<String, Object> product = m("image", imageNode(SLEEK), "assets", m("asset", imageNode(POLES)));

        assertEquals(List.of(SLEEK), LegacyProductStructure.imageReferences(product));
    }

    @Test
    public void imageReferences_skipsBlankAndMissingReferences() {
        Map<String, Object> product = m("image", imageNode("  "),
                "assets", m("asset", m("sling:resourceType", "commerce/components/product/image")));

        assertEquals(List.of(), LegacyProductStructure.imageReferences(product));
        assertEquals(List.of(), LegacyProductStructure.imageReferences(m("jcr:title", "No image")));
    }

    @Test
    public void declaredVariantAxes_keepsTheDeclarationOrder() {
        Map<String, Object> product = m("cq:productVariantAxes", List.of("color", "size"));

        assertEquals(List.of("color", "size"), LegacyProductStructure.declaredVariantAxes(product));
    }

    @Test
    public void declaredVariantAxes_acceptsASingleValue() {
        assertEquals(List.of("size"), LegacyProductStructure.declaredVariantAxes(m("cq:productVariantAxes", "size")));
    }

    @Test
    public void declaredVariantAxes_dropsBlankAndDuplicateEntries() {
        Map<String, Object> product = m("cq:productVariantAxes", List.of(" size ", "", "size", "color"));

        assertEquals(List.of("size", "color"), LegacyProductStructure.declaredVariantAxes(product));
    }

    @Test
    public void declaredVariantAxes_isEmptyWhenTheProductDeclaresNone() {
        // Hiking Poles carries a master size and a variant child, but declares no axes.
        Map<String, Object> product = m("jcr:title", "Hiking Poles", "size", "Regular",
                "hiking_poles", m("cq:commerceType", "variant", "size", "Long"));

        assertEquals(List.of(), LegacyProductStructure.declaredVariantAxes(product));
    }

    @Test
    public void configurableAxisEntries_keepsDeclaredVariantSelectsInManifestOrder() {
        assertEquals(List.of("size", "color"), axisCodes(List.of("color", "size")));
        assertEquals(List.of("size"), axisCodes(List.of("size")));
    }

    @Test
    public void configurableAxisEntries_ignoresUndeclaredAndNonVariantSelectAttributes() {
        assertEquals(List.of(), axisCodes(List.of()));
        assertEquals(List.of(), axisCodes(List.of("brand", "price", "sku")));
    }
}

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
package com.adobe.cq.commerce.celadon.aem.attribute.model;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

/**
 * Regenerates the per-catalog product Content Fragment model at
 * {@code /conf/<catalog>/settings/dam/cfm/models/product} from an
 * {@link AttributeManifest}.
 *
 * <p>The generator deletes any existing model and recreates it, so the resulting
 * model always reflects the current manifest. The structure mirrors the canonical
 * AEM Content Fragment Model template shape (cq:Template &rarr; jcr:content
 * (cq:PageContent) &rarr; model (cq:PageContent) &rarr; cq:dialog &rarr; content
 * &rarr; items &rarr; field-per-attribute).</p>
 */
public final class ProductModelGenerator {

    /**
     * Attribute codes that are emitted as scaffold-level fields by
     * {@link #addImageField} / {@link #addConfigurableOptionsField}. Manifest
     * entries with these codes are skipped in the per-attribute loop to avoid
     * "Unable to create node" collisions when both the scaffold and the manifest
     * declare them.
     */
    private static final Set<String> SCAFFOLD_CODES = Set.of("image", "configurableOptions", "additionalCategories");

    public void regenerate(ResourceResolver resolver, String catalog, AttributeManifest manifest) throws PersistenceException {
        String modelPath = "/conf/" + catalog + "/settings/dam/cfm/models/product";
        Resource existing = resolver.getResource(modelPath);
        if (existing != null) {
            resolver.delete(existing);
        }

        String modelsRoot = "/conf/" + catalog + "/settings/dam/cfm/models";
        Resource modelsFolder = ResourceUtil.getOrCreateResource(
                resolver, modelsRoot, "sling:Folder", "sling:Folder", false);

        // Root: cq:Template
        Map<String, Object> modelProps = new LinkedHashMap<>();
        modelProps.put("jcr:primaryType", "cq:Template");
        modelProps.put("allowedPaths", new String[]{"/content/dam(/.*)?"});
        modelProps.put("ranking", 100L);
        Resource model = resolver.create(modelsFolder, "product", modelProps);

        // jcr:content (cq:PageContent)
        Map<String, Object> contentProps = new LinkedHashMap<>();
        contentProps.put("jcr:primaryType", "cq:PageContent");
        contentProps.put("jcr:title", "Product (" + catalog + ")");
        contentProps.put("jcr:description", "Per-catalog product model generated from attribute manifest");
        contentProps.put("status", "enabled");
        contentProps.put("sling:resourceType", "dam/cfm/models/console/components/data/entity/default");
        contentProps.put("sling:resourceSuperType", "dam/cfm/models/console/components/data/entity");
        contentProps.put("cq:templateType", "/libs/settings/dam/cfm/model-types/fragment");
        contentProps.put("cq:scaffolding", modelPath + "/jcr:content/model");
        Resource jcrContent = resolver.create(model, "jcr:content", contentProps);

        // metadata (nt:unstructured)
        resolver.create(jcrContent, "metadata", Map.of("jcr:primaryType", "nt:unstructured"));

        // model (cq:PageContent)
        Map<String, Object> modelNodeProps = new LinkedHashMap<>();
        modelNodeProps.put("jcr:primaryType", "cq:PageContent");
        modelNodeProps.put("sling:resourceType", "wcm/scaffolding/components/scaffolding");
        modelNodeProps.put("dataTypesConfig", "/mnt/overlay/settings/dam/cfm/models/formbuilderconfig/datatypes");
        modelNodeProps.put("cq:targetPath", "/content/entities");
        modelNodeProps.put("maxGeneratedOrder", (long) Math.max(5, manifest.entries().size()));
        Resource modelNode = resolver.create(jcrContent, "model", modelNodeProps);

        // cq:dialog -> content -> items
        // Scaffold-level fields (outside the manifest loop, written by the importer directly):
        // image (asset paths) and configurableOptions (option-definition fragment refs).
        Resource items = buildDialogScaffold(resolver, modelNode, manifest.entries().size() + 3);
        addImageField(resolver, items, 1);
        addConfigurableOptionsField(resolver, items, 2);
        addAdditionalCategoriesField(resolver, items, 3);
        int order = 4;
        for (AttributeEntry entry : manifest.entries()) {
            if (SCAFFOLD_CODES.contains(entry.code())) continue;
            addField(resolver, items, entry, order++);
        }

        // variations -> cq:dialog -> content -> items (only variant-scoped entries)
        Map<String, Object> variationsProps = new LinkedHashMap<>();
        variationsProps.put("jcr:primaryType", "nt:unstructured");
        Resource variations = resolver.create(modelNode, "variations", variationsProps);
        int variantCount = 0;
        for (AttributeEntry entry : manifest.entries()) {
            if (entry.scope().appliesToVariant()) {
                variantCount++;
            }
        }
        // Variants need the image scaffold (per-variant photos) but not configurableOptions
        // (option refs live on the master product only).
        Resource variantItems = buildDialogScaffold(resolver, variations, variantCount + 1);
        addImageField(resolver, variantItems, 1);
        int variantOrder = 2;
        for (AttributeEntry entry : manifest.entries()) {
            if (SCAFFOLD_CODES.contains(entry.code())) continue;
            if (entry.scope().appliesToVariant()) {
                addField(resolver, variantItems, entry, variantOrder++);
            }
        }

        resolver.commit();
    }

    private static Resource buildDialogScaffold(ResourceResolver resolver,
                                                Resource parent,
                                                int fieldCount) throws PersistenceException {
        Map<String, Object> dialogProps = new LinkedHashMap<>();
        dialogProps.put("jcr:primaryType", "nt:unstructured");
        dialogProps.put("sling:resourceType", "cq/gui/components/authoring/dialog");
        Resource dialog = resolver.create(parent, "cq:dialog", dialogProps);

        Map<String, Object> contentProps = new LinkedHashMap<>();
        contentProps.put("jcr:primaryType", "nt:unstructured");
        contentProps.put("sling:resourceType", "granite/ui/components/coral/foundation/fixedcolumns");
        Resource dialogContent = resolver.create(dialog, "content", contentProps);

        Map<String, Object> itemsProps = new LinkedHashMap<>();
        itemsProps.put("jcr:primaryType", "nt:unstructured");
        itemsProps.put("maxGeneratedOrder", (long) Math.max(5, fieldCount));
        return resolver.create(dialogContent, "items", itemsProps);
    }

    /**
     * Adds the scaffold-level {@code image} multifield. The legacy importer
     * writes asset paths to this element directly (outside the manifest loop)
     * because images need the asset-download pipeline, so the field must
     * always be present on the model.
     */
    private static void addImageField(ResourceResolver resolver, Resource items, int order) throws PersistenceException {
        // A content-reference multifield (metaType=reference) so the CF editor shows
        // an asset picker + image thumbnail per entry, instead of a bare path textfield.
        // Shape mirrors AEM's "Content Reference (multiple)" data type.
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("jcr:primaryType", "nt:unstructured");
        props.put("sling:resourceType", "granite/ui/components/coral/foundation/form/multifield");
        props.put("metaType", "reference");
        props.put("valueType", "string[]");
        props.put("name", "image");
        props.put("fieldLabel", "Image");
        props.put("cfm-element", "Image");
        props.put("nameSuffix", "contentReference");
        props.put("filter", "hierarchy");
        props.put("rootPath", "/content/dam/celadon");
        props.put("listOrder", Integer.toString(order));
        props.put("renderReadOnly", "false");
        props.put("showEmptyInReadOnly", "true");
        Resource field = resolver.create(items, "image", props);

        Map<String, Object> nestedProps = new LinkedHashMap<>();
        nestedProps.put("jcr:primaryType", "nt:unstructured");
        nestedProps.put("sling:resourceType", "dam/cfm/models/editor/components/contentreference");
        nestedProps.put("name", "image");
        nestedProps.put("filter", "hierarchy");
        nestedProps.put("rootPath", "/content/dam/celadon");
        nestedProps.put("validation", "cfm.validation.contenttype.image");
        nestedProps.put("showThumbnail", "true");
        nestedProps.put("renderReadOnly", "false");
        Resource nested = resolver.create(field, "field", nestedProps);

        Map<String, Object> graniteData = new LinkedHashMap<>();
        graniteData.put("jcr:primaryType", "nt:unstructured");
        graniteData.put("showThumbnail", "true");
        graniteData.put("thumbnail-validation", "cfm.validation.thumbnail.show");
        resolver.create(nested, "granite:data", graniteData);
    }

    /**
     * Adds the scaffold-level {@code configurableOptions} field, which holds a
     * list of paths to {@code celadon-option-definition} fragments. The importer
     * writes these via {@link AemContentFragmentSupport} so the field has to
     * exist on the model.
     */
    private static void addConfigurableOptionsField(ResourceResolver resolver, Resource items, int order) throws PersistenceException {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("jcr:primaryType", "nt:unstructured");
        props.put("sling:resourceType", "granite/ui/components/coral/foundation/form/multifield");
        props.put("metaType", "text-single");
        props.put("valueType", "string[]");
        props.put("name", "configurableOptions");
        props.put("fieldLabel", "Configurable options");
        props.put("cfm-element", "Configurable options");
        props.put("listOrder", Integer.toString(order));
        props.put("renderReadOnly", "false");
        props.put("showEmptyInReadOnly", "true");
        Resource field = resolver.create(items, "configurableOptions", props);

        Map<String, Object> nestedProps = new LinkedHashMap<>();
        nestedProps.put("jcr:primaryType", "nt:unstructured");
        nestedProps.put("sling:resourceType", "granite/ui/components/coral/foundation/form/textfield");
        nestedProps.put("name", "configurableOptions");
        nestedProps.put("fieldLabel", "Option-definition path");
        nestedProps.put("renderReadOnly", "false");
        resolver.create(field, "field", nestedProps);
    }

    private static void addAdditionalCategoriesField(ResourceResolver resolver, Resource items, int order) throws PersistenceException {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("jcr:primaryType", "nt:unstructured");
        props.put("sling:resourceType", "granite/ui/components/coral/foundation/form/multifield");
        props.put("metaType", "text-single");
        props.put("valueType", "string[]");
        props.put("name", "additionalCategories");
        props.put("fieldLabel", "Additional categories");
        props.put("cfm-element", "Additional categories");
        props.put("listOrder", Integer.toString(order));
        props.put("renderReadOnly", "false");
        props.put("showEmptyInReadOnly", "true");
        Resource field = resolver.create(items, "additionalCategories", props);

        Map<String, Object> nestedProps = new LinkedHashMap<>();
        nestedProps.put("jcr:primaryType", "nt:unstructured");
        nestedProps.put("sling:resourceType", "granite/ui/components/coral/foundation/form/textfield");
        nestedProps.put("name", "additionalCategories");
        nestedProps.put("fieldLabel", "Category path");
        nestedProps.put("renderReadOnly", "false");
        resolver.create(field, "field", nestedProps);
    }

    private static void addField(ResourceResolver resolver, Resource items, AttributeEntry entry, int order) throws PersistenceException {
        String label = entry.label() == null || entry.label().isBlank() ? entry.code() : entry.label();
        WidgetSpec widget = widgetFor(entry.type());

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("jcr:primaryType", "nt:unstructured");
        props.put("sling:resourceType", widget.resourceType);
        props.put("metaType", widget.metaType);
        props.put("valueType", widget.valueType);
        props.put("name", entry.code());
        props.put("fieldLabel", label);
        props.put("cfm-element", label);
        props.put("listOrder", Integer.toString(order));
        props.put("renderReadOnly", "false");
        props.put("showEmptyInReadOnly", "true");
        // A Coral checkbox shows its caption from "text", not "fieldLabel"; without
        // it the CF editor renders the checkbox label as "null".
        if (entry.type() == NormalizedType.BOOLEAN) {
            props.put("text", label);
        }
        // A Coral numberfield defaults to step=1, which rejects decimals ("must be a
        // multiple of 1"). Prices and other floats need a decimal step.
        if (entry.type() == NormalizedType.FLOAT || entry.type() == NormalizedType.PRICE) {
            props.put("step", "any");
        }

        Resource field = resolver.create(items, entry.code(), props);

        // Multifield needs a nested field describing the array element
        if (entry.type() == NormalizedType.MULTISELECT) {
            Map<String, Object> nestedProps = new LinkedHashMap<>();
            nestedProps.put("jcr:primaryType", "nt:unstructured");
            nestedProps.put("sling:resourceType", "granite/ui/components/coral/foundation/form/textfield");
            nestedProps.put("name", entry.code());
            nestedProps.put("fieldLabel", "Value");
            nestedProps.put("renderReadOnly", "false");
            resolver.create(field, "field", nestedProps);
        }

        // Stash scope as an authoring hint so downstream tooling can read it.
        ModifiableValueMap valueMap = field.adaptTo(ModifiableValueMap.class);
        if (valueMap != null) {
            switch (entry.scope()) {
                case VARIANT -> valueMap.put("scope", "variant");
                case BOTH -> valueMap.put("scope", "both");
                case PRODUCT -> valueMap.put("scope", "product");
            }
        }
    }

    private static WidgetSpec widgetFor(NormalizedType type) {
        return switch (type) {
            case STRING, IMAGE_URL, SELECT -> new WidgetSpec(
                    "granite/ui/components/coral/foundation/form/textfield",
                    "text-single",
                    "string");
            case TEXT -> new WidgetSpec(
                    "granite/ui/components/coral/foundation/form/textarea",
                    "text-multi",
                    "string");
            case INT -> new WidgetSpec(
                    "granite/ui/components/coral/foundation/form/numberfield",
                    "number",
                    "long");
            case FLOAT, PRICE -> new WidgetSpec(
                    "granite/ui/components/coral/foundation/form/numberfield",
                    "number",
                    "double");
            case BOOLEAN -> new WidgetSpec(
                    "granite/ui/components/coral/foundation/form/checkbox",
                    "boolean",
                    "boolean");
            case DATE -> new WidgetSpec(
                    "granite/ui/components/coral/foundation/form/datepicker",
                    "date",
                    "string");
            case MULTISELECT -> new WidgetSpec(
                    "granite/ui/components/coral/foundation/form/multifield",
                    "text-single",
                    "string[]");
        };
    }

    private record WidgetSpec(String resourceType, String metaType, String valueType) {}
}

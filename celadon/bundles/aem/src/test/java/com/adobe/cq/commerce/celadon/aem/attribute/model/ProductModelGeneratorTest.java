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
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductModelGeneratorTest {

    private static final String FIELDS_BASE =
            "/conf/venia/settings/dam/cfm/models/product/jcr:content/model/cq:dialog/content/items";
    private static final String VARIANT_FIELDS_BASE =
            "/conf/venia/settings/dam/cfm/models/product/jcr:content/model/variations/cq:dialog/content/items";

    @Rule public final SlingContext context = new SlingContext();

    @Test
    public void replacesExistingModel() throws Exception {
        // pretend an old model exists
        context.create().resource(
                "/conf/venia/settings/dam/cfm/models/product/jcr:content/model/cq:dialog/content/items/legacy_field");
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        assertNull(context.resourceResolver().getResource(FIELDS_BASE + "/legacy_field"));
        assertNotNull(context.resourceResolver().getResource(FIELDS_BASE + "/sku"));
    }

    @Test
    public void generatesFieldPerEntryInOrder() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0),
                AttributeEntry.of("price", "Price", NormalizedType.PRICE,
                        AttributeScope.PRODUCT, true, true, 10),
                AttributeEntry.of("color", "Color", NormalizedType.SELECT,
                        AttributeScope.VARIANT, true, true, 20)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        assertNotNull(context.resourceResolver().getResource(FIELDS_BASE + "/sku"));
        assertNotNull(context.resourceResolver().getResource(FIELDS_BASE + "/price"));
        assertNotNull(context.resourceResolver().getResource(FIELDS_BASE + "/color"));

        // listOrder starts at "4" because slots 1, 2 and 3 are taken by the scaffold-level
        // "image", "configurableOptions" and "additionalCategories" fields.
        ValueMap skuProps = context.resourceResolver().getResource(FIELDS_BASE + "/sku").getValueMap();
        assertEquals("4", skuProps.get("listOrder", String.class));
        ValueMap priceProps = context.resourceResolver().getResource(FIELDS_BASE + "/price").getValueMap();
        assertEquals("5", priceProps.get("listOrder", String.class));
        ValueMap colorProps = context.resourceResolver().getResource(FIELDS_BASE + "/color").getValueMap();
        assertEquals("6", colorProps.get("listOrder", String.class));
    }

    @Test
    public void writesTemplateRootProperties() throws Exception {
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        Resource modelRoot = context.resourceResolver()
                .getResource("/conf/venia/settings/dam/cfm/models/product");
        assertNotNull(modelRoot);
        ValueMap rootProps = modelRoot.getValueMap();
        assertEquals("cq:Template", rootProps.get("jcr:primaryType", String.class));
        String[] allowed = rootProps.get("allowedPaths", String[].class);
        assertNotNull(allowed);
        assertEquals(1, allowed.length);
        assertEquals("/content/dam(/.*)?", allowed[0]);

        Resource jcrContent = context.resourceResolver()
                .getResource("/conf/venia/settings/dam/cfm/models/product/jcr:content");
        ValueMap contentProps = jcrContent.getValueMap();
        assertEquals("cq:PageContent", contentProps.get("jcr:primaryType", String.class));
        assertEquals("enabled", contentProps.get("status", String.class));
        assertEquals("dam/cfm/models/console/components/data/entity/default",
                contentProps.get("sling:resourceType", String.class));
        assertEquals("dam/cfm/models/console/components/data/entity",
                contentProps.get("sling:resourceSuperType", String.class));
        assertEquals("/libs/settings/dam/cfm/model-types/fragment",
                contentProps.get("cq:templateType", String.class));
        assertEquals("/conf/venia/settings/dam/cfm/models/product/jcr:content/model",
                contentProps.get("cq:scaffolding", String.class));

        Resource modelNode = context.resourceResolver()
                .getResource("/conf/venia/settings/dam/cfm/models/product/jcr:content/model");
        ValueMap modelProps = modelNode.getValueMap();
        assertEquals("cq:PageContent", modelProps.get("jcr:primaryType", String.class));
        assertEquals("wcm/scaffolding/components/scaffolding",
                modelProps.get("sling:resourceType", String.class));
        assertEquals("/mnt/overlay/settings/dam/cfm/models/formbuilderconfig/datatypes",
                modelProps.get("dataTypesConfig", String.class));
        assertEquals("/content/entities", modelProps.get("cq:targetPath", String.class));
    }

    @Test
    public void stringFieldUsesTextfieldWidget() throws Exception {
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        ValueMap props = context.resourceResolver().getResource(FIELDS_BASE + "/sku").getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/textfield",
                props.get("sling:resourceType", String.class));
        assertEquals("text-single", props.get("metaType", String.class));
        assertEquals("string", props.get("valueType", String.class));
        assertEquals("SKU", props.get("cfm-element", String.class));
        assertEquals("sku", props.get("name", String.class));
        assertEquals("SKU", props.get("fieldLabel", String.class));
        assertEquals("false", props.get("renderReadOnly", String.class));
        assertEquals("true", props.get("showEmptyInReadOnly", String.class));
    }

    @Test
    public void numericFieldForInt() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("weight", "Weight", NormalizedType.INT,
                        AttributeScope.PRODUCT, false, false, 50)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        ValueMap props = context.resourceResolver().getResource(FIELDS_BASE + "/weight").getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/numberfield",
                props.get("sling:resourceType", String.class));
        assertEquals("number", props.get("metaType", String.class));
        assertEquals("long", props.get("valueType", String.class));
    }

    @Test
    public void numericFieldForPriceUsesDouble() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("price", "Price", NormalizedType.PRICE,
                        AttributeScope.PRODUCT, false, true, 10)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        ValueMap props = context.resourceResolver().getResource(FIELDS_BASE + "/price").getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/numberfield",
                props.get("sling:resourceType", String.class));
        assertEquals("number", props.get("metaType", String.class));
        assertEquals("double", props.get("valueType", String.class));
    }

    @Test
    public void booleanFieldUsesCheckbox() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("inStock", "In stock", NormalizedType.BOOLEAN,
                        AttributeScope.PRODUCT, true, false, 5)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        ValueMap props = context.resourceResolver().getResource(FIELDS_BASE + "/inStock").getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/checkbox",
                props.get("sling:resourceType", String.class));
        assertEquals("boolean", props.get("metaType", String.class));
        assertEquals("boolean", props.get("valueType", String.class));
    }

    @Test
    public void textFieldUsesTextarea() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("desc", "Description", NormalizedType.TEXT,
                        AttributeScope.PRODUCT, false, false, 1)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        ValueMap props = context.resourceResolver().getResource(FIELDS_BASE + "/desc").getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/textarea",
                props.get("sling:resourceType", String.class));
        assertEquals("text-multi", props.get("metaType", String.class));
        assertEquals("string", props.get("valueType", String.class));
    }

    @Test
    public void dateFieldUsesDatepicker() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("created", "Created", NormalizedType.DATE,
                        AttributeScope.PRODUCT, false, false, 1)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        ValueMap props = context.resourceResolver().getResource(FIELDS_BASE + "/created").getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/datepicker",
                props.get("sling:resourceType", String.class));
        assertEquals("date", props.get("metaType", String.class));
        assertEquals("string", props.get("valueType", String.class));
    }

    @Test
    public void multiselectUsesMultifieldWithStringArrayValueType() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("tags", "Tags", NormalizedType.MULTISELECT,
                        AttributeScope.PRODUCT, true, false, 1)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        Resource field = context.resourceResolver().getResource(FIELDS_BASE + "/tags");
        ValueMap props = field.getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/multifield",
                props.get("sling:resourceType", String.class));
        assertEquals("string[]", props.get("valueType", String.class));
        // The nested element descriptor should be present.
        assertNotNull(field.getChild("field"));
    }

    @Test
    public void selectUsesTextfield() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("color", "Color", NormalizedType.SELECT,
                        AttributeScope.VARIANT, true, true, 20)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        ValueMap props = context.resourceResolver().getResource(FIELDS_BASE + "/color").getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/textfield",
                props.get("sling:resourceType", String.class));
        assertEquals("text-single", props.get("metaType", String.class));
        assertEquals("string", props.get("valueType", String.class));
    }

    @Test
    public void writesScaffoldImageField() throws Exception {
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        // The image scaffold field must be present on both the main dialog and the
        // variations dialog so the importer can write asset paths to it.
        Resource image = context.resourceResolver().getResource(FIELDS_BASE + "/image");
        assertNotNull("scaffold image field must be present", image);
        ValueMap props = image.getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/multifield",
                props.get("sling:resourceType", String.class));
        assertEquals("string[]", props.get("valueType", String.class));
        assertEquals("image", props.get("name", String.class));
        assertNotNull(image.getChild("field"));

        Resource variantImage = context.resourceResolver().getResource(VARIANT_FIELDS_BASE + "/image");
        assertNotNull("scaffold image field must be present on variations", variantImage);
    }

    @Test
    public void writesScaffoldConfigurableOptionsField() throws Exception {
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        // configurableOptions holds paths to option-definition fragments on the master only.
        Resource opts = context.resourceResolver().getResource(FIELDS_BASE + "/configurableOptions");
        assertNotNull("scaffold configurableOptions field must be present", opts);
        ValueMap props = opts.getValueMap();
        assertEquals("string[]", props.get("valueType", String.class));
        assertEquals("configurableOptions", props.get("name", String.class));
        assertNotNull(opts.getChild("field"));

        // Variants should NOT carry configurableOptions; only the master does.
        assertNull(context.resourceResolver().getResource(VARIANT_FIELDS_BASE + "/configurableOptions"));
    }

    @Test
    public void writesScaffoldAdditionalCategoriesField() throws Exception {
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        // additionalCategories holds catalog-relative category paths on the master only.
        Resource cats = context.resourceResolver().getResource(FIELDS_BASE + "/additionalCategories");
        assertNotNull("scaffold additionalCategories field must be present", cats);
        ValueMap props = cats.getValueMap();
        assertEquals("granite/ui/components/coral/foundation/form/multifield",
                props.get("sling:resourceType", String.class));
        assertEquals("string[]", props.get("valueType", String.class));
        assertEquals("additionalCategories", props.get("name", String.class));
        assertNotNull(cats.getChild("field"));

        // Variants should NOT carry additionalCategories; membership is master-only.
        assertNull(context.resourceResolver().getResource(VARIANT_FIELDS_BASE + "/additionalCategories"));
    }

    @Test
    public void writesVariationContainerForVariantScopedEntries() throws Exception {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0),
                AttributeEntry.of("color", "Color", NormalizedType.SELECT,
                        AttributeScope.VARIANT, true, true, 20),
                AttributeEntry.of("desc", "Description", NormalizedType.TEXT,
                        AttributeScope.PRODUCT, false, false, 30)));

        new ProductModelGenerator().regenerate(context.resourceResolver(), "venia", m);

        // variants block should list color (VARIANT) and sku (BOTH), not desc (PRODUCT)
        Resource variants = context.resourceResolver().getResource(VARIANT_FIELDS_BASE);
        assertNotNull(variants);
        assertNotNull(variants.getChild("color"));
        assertNotNull(variants.getChild("sku"));
        assertNull(variants.getChild("desc"));
    }
}

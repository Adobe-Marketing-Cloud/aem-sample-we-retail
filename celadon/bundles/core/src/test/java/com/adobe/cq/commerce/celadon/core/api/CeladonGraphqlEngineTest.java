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
package com.adobe.cq.commerce.celadon.core.api;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class CeladonGraphqlEngineTest {
    @Test
    public void shouldInitializeAndSerializeNulls() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Assert.assertNotNull(engine);
        Assert.assertTrue(engine.gson().toJson(TestCatalogFixtures.map("value", null)).contains("\"value\":null"));
    }

    @Test
    public void shouldServeIntrospectionAndSelectedOperations() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> introspection = engine.execute("{ __typename }", null, null);
        Assert.assertNotNull(introspection.get("data"));

        Map<String, Object> multi = engine.execute(
                "query A { storeConfig { root_category_uid } } query B { storeConfig { base_currency_code } }",
                "B",
                null
        );
        Map<String, Object> data = castMap(multi.get("data"));
        Assert.assertEquals("USD", castMap(data.get("storeConfig")).get("base_currency_code"));
    }

    @Test
    public void shouldReturnGraphqlErrorsForUnknownFieldsAndSyntaxErrors() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> unknownField = engine.execute("{ products { items { does_not_exist } } }", null, null);
        Assert.assertTrue(((List<?>) unknownField.get("errors")).size() >= 1);
        Assert.assertNotNull(unknownField.get("data"));

        Map<String, Object> malformed = engine.execute("{ products(", null, null);
        Assert.assertTrue(((List<?>) malformed.get("errors")).size() >= 1);
        Assert.assertNotNull(malformed.get("data"));
    }

    @Test
    public void shouldExcludeUnderscorePrefixedFoldersFromCategories() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList { uid name children { uid name } } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("/", roots.getFirst().get("uid"));
        List<Map<String, Object>> children = castList(roots.getFirst().get("children"));
        for (Map<String, Object> child : children) {
            Assert.assertFalse(
                    "Folders starting with '_' must be excluded from categories: " + child.get("uid"),
                    child.get("uid").toString().contains("_options")
            );
        }
        Assert.assertTrue(
                children.stream().anyMatch(child -> "venia-dresses".equals(child.get("uid")))
        );
    }

    @Test
    public void shouldExposeFolderThumbnailAsCategoryImage() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList(filters:{ category_uid:{ eq:\"venia-dresses\" } }) { uid image } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("venia-dresses", roots.getFirst().get("uid"));
        Assert.assertEquals(
                "/content/dam/celadon/venia/venia-dresses/jcr:content/folderThumbnail",
                roots.getFirst().get("image")
        );
    }

    @Test
    public void shouldExposeManualThumbnailAsCategoryImage() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList(filters:{ category_uid:{ eq:\"venia-tops\" } }) { uid image } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("venia-tops", roots.getFirst().get("uid"));
        Assert.assertEquals(
                "/content/dam/celadon/venia/venia-tops/jcr:content/manualThumbnail.jpg",
                roots.getFirst().get("image")
        );
    }

    @Test
    public void shouldLeaveCategoryImageBlankWhenNoFolderThumbnail() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList { uid image } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("/", roots.getFirst().get("uid"));
        Assert.assertEquals("", roots.getFirst().get("image"));
    }

    @Test
    public void shouldBuildCustomAttributeMetadataFromSchema() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ customAttributeMetadata(attributes:[{attribute_code:\"sku\",entity_type:\"catalog_product\"}]) { items { attribute_code attribute_type input_type } } }",
                null,
                null
        );

        List<Map<String, Object>> items = castList(castMap(castMap(response.get("data")).get("customAttributeMetadata")).get("items"));
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sku", items.getFirst().get("attribute_code"));
    }

    @Test
    public void shouldSelectManifestAttributesOnProductOutput() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engineWithCustomAttributes();

        Map<String, Object> response = engine.execute(
                "{ products(filter:{ sku:{ eq:\"angelina-tank-dress-sku\" } }) { items { sku material activities } } }",
                null,
                null
        );

        Assert.assertNull("custom attribute selection must not raise errors", response.get("errors"));
        List<Map<String, Object>> items = castList(castMap(castMap(response.get("data")).get("products")).get("items"));
        Map<String, Object> product = items.getFirst();
        Assert.assertEquals("angelina-tank-dress-sku", product.get("sku"));
        Assert.assertEquals("Cotton", product.get("material"));
        Assert.assertEquals(List.of("Hiking", "Camping"), product.get("activities"));
    }

    @Test
    public void shouldCoerceNumericAndBooleanManifestAttributes() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engineWithTypedCustomAttributes();

        Map<String, Object> response = engine.execute(
                "{ products(filter:{ sku:{ eq:\"angelina-tank-dress-sku\" } }) { items { sku units_per_pack rating msrp featured tags activities } } }",
                null,
                null
        );

        Assert.assertNull("typed custom attribute selection must not raise errors", response.get("errors"));
        List<Map<String, Object>> items = castList(castMap(castMap(response.get("data")).get("products")).get("items"));
        Map<String, Object> product = items.getFirst();
        Assert.assertEquals(Integer.valueOf(3), product.get("units_per_pack"));
        Assert.assertEquals(Double.valueOf(4.5), product.get("rating"));
        Assert.assertEquals(Double.valueOf(42.0), product.get("msrp"));
        Assert.assertEquals(Boolean.TRUE, product.get("featured"));
        Assert.assertEquals(List.of("Summer", "Floral", "New"), product.get("tags"));
        Assert.assertEquals(List.of("Hiking", "Camping"), product.get("activities"));
    }

    @Test
    public void shouldSelectManifestAttributesOnVariants() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engineWithTypedCustomAttributes();

        Map<String, Object> response = engine.execute(
                "{ products(filter:{ sku:{ eq:\"candace-dress-sku\" } }) { items { sku __typename ... on ConfigurableProduct { material variants { product { sku material } } } } } }",
                null,
                null
        );

        Assert.assertNull("variant custom attribute selection must not raise errors", response.get("errors"));
        List<Map<String, Object>> items = castList(castMap(castMap(response.get("data")).get("products")).get("items"));
        Map<String, Object> product = items.getFirst();
        Assert.assertEquals("ConfigurableProduct", product.get("__typename"));
        Assert.assertEquals("Polyester", product.get("material"));

        List<Map<String, Object>> variants = castList(product.get("variants"));
        Map<String, String> materialBySku = new java.util.HashMap<>();
        for (Map<String, Object> variant : variants) {
            Map<String, Object> variantProduct = castMap(variant.get("product"));
            materialBySku.put(
                    String.valueOf(variantProduct.get("sku")),
                    (String) variantProduct.get("material")
            );
        }
        Assert.assertEquals("per-variation value must win", "Silk", materialBySku.get("candace-dress-lilac-l"));
        Assert.assertEquals("missing per-variation value must fall back to master", "Polyester",
                materialBySku.get("candace-dress-peach-l"));
    }

    @Test
    public void shouldNotOverwriteReservedFieldWithManifestAttribute() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engineWithTypedCustomAttributes();

        Map<String, Object> response = engine.execute(
                "{ products(filter:{ sku:{ eq:\"angelina-tank-dress-sku\" } }) { items { sku name } } }",
                null,
                null
        );

        Assert.assertNull(response.get("errors"));
        List<Map<String, Object>> items = castList(castMap(castMap(response.get("data")).get("products")).get("items"));
        Map<String, Object> product = items.getFirst();
        Assert.assertEquals("angelina-tank-dress-sku", product.get("sku"));
        Assert.assertEquals("manifest 'name' (INT) must not overwrite the reserved base field",
                "Angelina Tank Dress", product.get("name"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}

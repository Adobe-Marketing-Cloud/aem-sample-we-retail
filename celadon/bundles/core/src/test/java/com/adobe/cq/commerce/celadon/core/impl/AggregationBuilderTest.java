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
package com.adobe.cq.commerce.celadon.core.impl;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class AggregationBuilderTest {

    @Test
    public void shouldReturnEmptyForEmptyProductList() throws Exception {
        List<Map<String, Object>> result = AggregationBuilder.build(List.of(), Map.of(), product -> List.of());
        Assert.assertEquals(List.of(), result);
    }

    @Test
    public void shouldBuildPriceCategoryAndConfigurableFacets() throws Exception {
        Map<String, CatalogCategory> categories = new LinkedHashMap<>();
        categories.put("venia-accessories", new CatalogCategory("venia-accessories", "Accessories", ""));
        categories.put("venia-accessories/venia-scarves",
                new CatalogCategory("venia-accessories/venia-scarves", "Scarves", ""));
        categories.put("venia-accessories/venia-jewelry",
                new CatalogCategory("venia-accessories/venia-jewelry", "Jewelry", ""));

        CatalogProduct scarf1 = product("venia-accessories/venia-scarves", "va01", 38.4);
        CatalogProduct scarf2 = product("venia-accessories/venia-scarves", "va02", 110.0);
        CatalogProduct jewel = product("venia-accessories/venia-jewelry", "va10", 250.0);

        Map<String, List<Map<String, Object>>> optionsByProduct = new LinkedHashMap<>();
        optionsByProduct.put(scarf1.fullPath(), List.of(
                configurableOption("fashion_color", "Fashion Color", List.of(value(92, "Peach"), value(98, "Lilac"))),
                configurableOption("fashion_size", "Fashion Size", List.of(value(138, "XS"), value(135, "S")))
        ));
        optionsByProduct.put(scarf2.fullPath(), List.of(
                configurableOption("fashion_color", "Fashion Color", List.of(value(92, "Peach")))
        ));
        // jewel has no configurable options

        List<Map<String, Object>> result = AggregationBuilder.build(
                List.of(scarf1, scarf2, jewel),
                categories,
                product -> optionsByProduct.getOrDefault(product.fullPath(), List.of()));

        Assert.assertFalse(result.isEmpty());
        Map<String, Map<String, Object>> byCode = byAttributeCode(result);
        Assert.assertTrue("expected price facet", byCode.containsKey("price"));
        Assert.assertTrue("expected category_uid facet", byCode.containsKey("category_uid"));
        Assert.assertTrue("expected fashion_color facet", byCode.containsKey("fashion_color"));
        Assert.assertTrue("expected fashion_size facet", byCode.containsKey("fashion_size"));

        // ---- price (100-unit buckets): 38.4 → 0-100, 110.0 → 100-200, 250.0 → 200-300 ----
        List<Map<String, Object>> priceOptions = options(byCode.get("price"));
        Assert.assertEquals(3, priceOptions.size());
        Assert.assertEquals("0_100", priceOptions.get(0).get("value"));
        Assert.assertEquals("0-100", priceOptions.get(0).get("label"));
        Assert.assertEquals(1, priceOptions.get(0).get("count"));
        Assert.assertEquals("100_200", priceOptions.get(1).get("value"));
        Assert.assertEquals(1, priceOptions.get(1).get("count"));
        Assert.assertEquals("200_300", priceOptions.get(2).get("value"));
        Assert.assertEquals(1, priceOptions.get(2).get("count"));

        // ---- category_uid: ancestors counted ----
        Map<String, Integer> categoryCounts = optionCounts(byCode.get("category_uid"));
        Assert.assertEquals(Integer.valueOf(3), categoryCounts.get("venia-accessories"));
        Assert.assertEquals(Integer.valueOf(2), categoryCounts.get("venia-accessories/venia-scarves"));
        Assert.assertEquals(Integer.valueOf(1), categoryCounts.get("venia-accessories/venia-jewelry"));
        Map<String, String> categoryLabels = optionLabels(byCode.get("category_uid"));
        Assert.assertEquals("Accessories", categoryLabels.get("venia-accessories"));
        Assert.assertEquals("Scarves", categoryLabels.get("venia-accessories/venia-scarves"));

        // ---- fashion_color: Peach=2 (scarf1+scarf2), Lilac=1 (scarf1) ----
        Map<String, Integer> colorCounts = optionCounts(byCode.get("fashion_color"));
        Assert.assertEquals(Integer.valueOf(2), colorCounts.get("92"));
        Assert.assertEquals(Integer.valueOf(1), colorCounts.get("98"));
        Assert.assertEquals("Fashion Color", byCode.get("fashion_color").get("label"));

        // ---- fashion_size: only scarf1 contributes, XS=1, S=1 ----
        Map<String, Integer> sizeCounts = optionCounts(byCode.get("fashion_size"));
        Assert.assertEquals(Integer.valueOf(1), sizeCounts.get("138"));
        Assert.assertEquals(Integer.valueOf(1), sizeCounts.get("135"));
    }

    @Test
    public void shouldCountParentProductOncePerVariantValue() throws Exception {
        // A single parent with 4 colors and 4 sizes => each color/size option gets count=1, not 4.
        CatalogProduct parent = product("venia-accessories/venia-scarves", "va01", 38.4);
        List<Map<String, Object>> options = List.of(
                configurableOption("fashion_color", "Fashion Color",
                        List.of(value(92, "Peach"), value(98, "Lilac"), value(101, "Rain"), value(104, "Mint"))),
                configurableOption("fashion_size", "Fashion Size",
                        List.of(value(138, "XS"), value(135, "S"), value(132, "M"), value(129, "L")))
        );

        List<Map<String, Object>> result = AggregationBuilder.build(
                List.of(parent), Map.of(), p -> options);

        Map<String, Map<String, Object>> byCode = byAttributeCode(result);
        Map<String, Integer> colorCounts = optionCounts(byCode.get("fashion_color"));
        for (Map.Entry<String, Integer> entry : colorCounts.entrySet()) {
            Assert.assertEquals("color " + entry.getKey() + " should be 1", Integer.valueOf(1), entry.getValue());
        }
        Assert.assertEquals(4, colorCounts.size());
    }

    @Test
    public void shouldFallBackToPathLeafLabelWhenCategoryUnknown() throws Exception {
        CatalogProduct product = product("orphans/foo", "p1", 10.0);
        List<Map<String, Object>> result = AggregationBuilder.build(
                List.of(product), Map.of(), p -> List.of());

        Map<String, Map<String, Object>> byCode = byAttributeCode(result);
        Map<String, String> labels = optionLabels(byCode.get("category_uid"));
        Assert.assertEquals("orphans", labels.get("orphans"));
        Assert.assertEquals("foo", labels.get("orphans/foo"));
    }

    @Test
    public void shouldUsePriceVariationMinimum() throws Exception {
        Map<String, Object> price = new LinkedHashMap<>();
        price.put("value", 200.0);
        Map<String, Object> variations = new LinkedHashMap<>();
        variations.put("v1", Map.of("value", 75.0));
        variations.put("v2", Map.of("value", 130.0));
        price.put("variations", variations);
        Map<String, Object> elements = new LinkedHashMap<>();
        elements.put("price", price);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("elements", elements);
        CatalogProduct product = new CatalogProduct("cat", "n1", properties, 0);

        List<Map<String, Object>> result = AggregationBuilder.build(
                List.of(product), Map.of(), p -> List.of());
        Map<String, Map<String, Object>> byCode = byAttributeCode(result);
        List<Map<String, Object>> priceOptions = options(byCode.get("price"));
        Assert.assertEquals(1, priceOptions.size());
        Assert.assertEquals("0_100", priceOptions.get(0).get("value"));
    }

    @Test
    public void usesManifestForRangeBucketingOfInt() throws Exception {
        // Two products with `weight` integer values; manifest declares it aggregatable.
        // Existing assertions for category_uid and price still pass.
        AttributeEntry weight = AttributeEntry.of("weight", "Weight", NormalizedType.INT,
                AttributeScope.PRODUCT, true, true, 100);
        AttributeManifest manifest = new AttributeManifest("venia", List.of(weight));

        CatalogProduct a = product("venia-accessories/venia-scarves", "va01", 38.4);
        CatalogProduct b = product("venia-accessories/venia-scarves", "va02", 110.0);
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        values.put(a.fullPath(), Map.of("weight", 100L));
        values.put(b.fullPath(), Map.of("weight", 200L));

        List<Map<String, Object>> result = AggregationBuilder.build(
                List.of(a, b),
                Map.of(),
                p -> List.of(),
                manifest,
                (p, code) -> values.getOrDefault(p.fullPath(), Map.of()).get(code));

        Map<String, Map<String, Object>> byCode = byAttributeCode(result);
        Assert.assertTrue("expected weight facet", byCode.containsKey("weight"));
        Assert.assertTrue("expected price facet", byCode.containsKey("price"));
        Assert.assertTrue("expected category_uid facet", byCode.containsKey("category_uid"));
        List<Map<String, Object>> weightOptions = options(byCode.get("weight"));
        Assert.assertEquals(1, weightOptions.size());
        Assert.assertEquals("100_200", weightOptions.get(0).get("value"));
        Assert.assertEquals(2, weightOptions.get(0).get("count"));
    }

    // ---- helpers ----

    private static CatalogProduct product(String categoryPath, String nodeName, double price) {
        Map<String, Object> priceElement = new LinkedHashMap<>();
        priceElement.put("value", price);
        Map<String, Object> elements = new LinkedHashMap<>();
        elements.put("price", priceElement);
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("elements", elements);
        return new CatalogProduct(categoryPath, nodeName, properties, 0);
    }

    private static Map<String, Object> configurableOption(String attributeCode, String label, List<Map<String, Object>> values) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("attribute_code", attributeCode);
        option.put("label", label);
        option.put("values", values);
        return option;
    }

    private static Map<String, Object> value(int valueIndex, String label) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value_index", valueIndex);
        map.put("label", label);
        return map;
    }

    private static Map<String, Map<String, Object>> byAttributeCode(List<Map<String, Object>> facets) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> facet : facets) {
            map.put(String.valueOf(facet.get("attribute_code")), facet);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> options(Map<String, Object> facet) {
        return (List<Map<String, Object>>) facet.get("options");
    }

    private static Map<String, Integer> optionCounts(Map<String, Object> facet) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> option : options(facet)) {
            result.put(String.valueOf(option.get("value")), (Integer) option.get("count"));
        }
        return result;
    }

    private static Map<String, String> optionLabels(Map<String, Object> facet) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map<String, Object> option : options(facet)) {
            result.put(String.valueOf(option.get("value")), String.valueOf(option.get("label")));
        }
        return result;
    }
}

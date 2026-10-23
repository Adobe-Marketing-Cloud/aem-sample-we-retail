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
package com.adobe.cq.commerce.celadon.aem.attribute.source;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MagentoIntrospectorTest {

    @Test
    public void parsesCustomAttributeMetadataResponse() {
        String json = """
            {"data":{"customAttributeMetadata":{"items":[
                {"attribute_code":"fashion_color","attribute_type":"Int","input_type":"select"},
                {"attribute_code":"fashion_size","attribute_type":"Int","input_type":"select"},
                {"attribute_code":"weight","attribute_type":"Float","input_type":"text"}
            ]}}}""";
        AttributeManifest m = MagentoIntrospector.parse("venia", json,
                new DiscoveryHints(List.of(), List.of()));
        // 3 custom + 5 universals (sku, name, description, price, image)
        assertEquals(8, m.entries().size());
        assertEquals(NormalizedType.SELECT, m.entryFor("fashion_color").orElseThrow().type());
        assertEquals(NormalizedType.SELECT, m.entryFor("fashion_size").orElseThrow().type());
        assertEquals(NormalizedType.FLOAT, m.entryFor("weight").orElseThrow().type());
    }

    @Test
    public void appliesDenyList() {
        String json = """
            {"data":{"customAttributeMetadata":{"items":[
                {"attribute_code":"sku","attribute_type":"String","input_type":"text"},
                {"attribute_code":"internal_score","attribute_type":"Int","input_type":"text"}
            ]}}}""";
        AttributeManifest m = MagentoIntrospector.parse("venia", json,
                new DiscoveryHints(List.of(), List.of("internal_*")));
        // 5 universals only — sku is a universal (custom sku shadowed), internal_score denied
        assertEquals(5, m.entries().size());
        assertTrue(m.entryFor("sku").isPresent());
        assertFalse(m.entryFor("internal_score").isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void allowListWithoutSkuTrips() {
        String json = """
            {"data":{"customAttributeMetadata":{"items":[
                {"attribute_code":"name","attribute_type":"String","input_type":"text"}
            ]}}}""";
        MagentoIntrospector.parse("venia", json,
                new DiscoveryHints(List.of("name"), List.of()));
    }

    @Test
    public void emptyItemsYieldsOnlyUniversals() {
        String json = """
            {"data":{"customAttributeMetadata":{"items":[]}}}""";
        AttributeManifest m = MagentoIntrospector.parse("venia", json,
                new DiscoveryHints(List.of(), List.of()));
        // Only universal attributes are present (sku, name, description, price, image)
        assertEquals(5, m.entries().size());
    }

    @Test
    public void seedsUniversalAttributes() {
        String json = """
            {"data":{"customAttributeMetadata":{"items":[]}}}""";
        AttributeManifest m = MagentoIntrospector.parse("venia", json,
                new DiscoveryHints(List.of(), List.of()));
        assertTrue(m.entryFor("sku").isPresent());
        assertTrue(m.entryFor("name").isPresent());
        assertTrue(m.entryFor("price").isPresent());
        assertEquals(NormalizedType.STRING, m.entryFor("sku").orElseThrow().type());
        assertEquals(NormalizedType.PRICE, m.entryFor("price").orElseThrow().type());
        assertEquals(NormalizedType.IMAGE_URL, m.entryFor("image").orElseThrow().type());
    }

    @Test
    public void capturesAttributeOptions() {
        String json = """
            {"data":{"customAttributeMetadata":{"items":[
                {"attribute_code":"fashion_color","attribute_type":"Int","input_type":"select",
                 "attribute_options":[{"value":"92","label":"Peach"},{"value":"98","label":"Lilac"}]}
            ]}}}""";
        var result = MagentoIntrospector.parseWithOptions("venia", json,
                new DiscoveryHints(List.of(), List.of()));
        var options = result.options().get("fashion_color");
        assertEquals(2, options.size());
        assertEquals("Peach", options.get(0).label());
        assertEquals("92", options.get(0).value());
    }

    @Test
    public void customAttributesDoNotShadowUniversals() {
        String json = """
            {"data":{"customAttributeMetadata":{"items":[
                {"attribute_code":"name","attribute_type":"String","input_type":"textarea"}
            ]}}}""";
        AttributeManifest m = MagentoIntrospector.parse("venia", json,
                new DiscoveryHints(List.of(), List.of()));
        // universal "name" wins; custom entry is dropped
        assertEquals(1, m.entries().stream().filter(e -> e.code().equals("name")).count());
        assertEquals(NormalizedType.STRING, m.entryFor("name").orElseThrow().type());
    }
}

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
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacyDiscoveryTest {

    @Test
    public void singleProductOnlySingleStringAttribute() {
        String tree = """
            {"products":{
                "shirt":{
                    "cq:commerceType":"product",
                    "sku":"SH-1","jcr:title":"Shirt","price":29.99,"material":"Cotton"
                }
            }}""";
        AttributeManifest m = LegacyDiscovery.parse("venia", tree,
                new DiscoveryHints(List.of(), List.of()));
        assertTrue(m.entryFor("material").isPresent());
        assertEquals(NormalizedType.STRING, m.entryFor("material").orElseThrow().type());
        assertEquals(AttributeScope.PRODUCT, m.entryFor("material").orElseThrow().scope());
    }

    @Test
    public void variantNestedColorAttribute() {
        String tree = """
            {"products":{
                "shirt":{
                    "cq:commerceType":"product","sku":"SH-1","jcr:title":"Shirt","price":29.99,
                    "red":{"cq:commerceType":"variant","sku":"SH-1-R","color":"Red"},
                    "blue":{"cq:commerceType":"variant","sku":"SH-1-B","color":"Blue"}
                }
            }}""";
        AttributeManifest m = LegacyDiscovery.parse("venia", tree,
                new DiscoveryHints(List.of(), List.of()));
        assertEquals(AttributeScope.VARIANT, m.entryFor("color").orElseThrow().scope());
    }

    @Test
    public void skipsJcrCqSling() {
        String tree = """
            {"products":{
                "shirt":{
                    "cq:commerceType":"product","sku":"S","jcr:primaryType":"nt:unstructured",
                    "sling:resourceType":"foo","cq:lastModified":"2024-01-01","color":"Red"
                }
            }}""";
        AttributeManifest m = LegacyDiscovery.parse("venia", tree,
                new DiscoveryHints(List.of(), List.of()));
        assertFalse(m.entryFor("jcr:primaryType").isPresent());
        assertFalse(m.entryFor("sling:resourceType").isPresent());
        assertFalse(m.entryFor("cq:lastModified").isPresent());
        assertTrue(m.entryFor("color").isPresent());
    }

    @Test
    public void seedsShortDescriptionAlongsideTheStandardDescription() {
        String tree = """
            {"products":{
                "coat":{
                    "cq:commerceType":"product","sku":"C-1","jcr:title":"Coat",
                    "jcr:description":"coat","summary":"Warm without bulk."
                }
            }}""";
        AttributeManifest m = LegacyDiscovery.parse("we-retail", tree,
                new DiscoveryHints(List.of(), List.of()));
        assertEquals(NormalizedType.TEXT, m.entryFor("description").orElseThrow().type());
        assertEquals(NormalizedType.TEXT, m.entryFor("short_description").orElseThrow().type());
        assertEquals(AttributeScope.PRODUCT, m.entryFor("short_description").orElseThrow().scope());
        assertFalse(m.entryFor("jcr:description").isPresent());
        assertFalse(m.entryFor("summary").isPresent());
    }

    @Test
    public void seedsNameForProductsAndVariants() {
        String tree = """
            {"products":{
                "jacket":{
                    "cq:commerceType":"product","sku":"J-1","jcr:title":"Jacket","cq:productVariantAxes":["color"],
                    "green":{"cq:commerceType":"variant","jcr:title":"Jacket Green","color":"green"}
                }
            }}""";
        AttributeManifest m = LegacyDiscovery.parse("we-retail", tree,
                new DiscoveryHints(List.of(), List.of()));
        assertEquals(AttributeScope.BOTH, m.entryFor("name").orElseThrow().scope());
        assertFalse(m.entryFor("jcr:title").isPresent());
    }

    @Test
    public void denyListExcludes() {
        String tree = """
            {"products":{
                "shirt":{
                    "cq:commerceType":"product","sku":"S","margin":0.4,"color":"Red"
                }
            }}""";
        AttributeManifest m = LegacyDiscovery.parse("venia", tree,
                new DiscoveryHints(List.of(), List.of("margin")));
        assertFalse(m.entryFor("margin").isPresent());
        assertTrue(m.entryFor("color").isPresent());
    }

    @Test
    public void inferenceFromJcrTypes() {
        String tree = """
            {"products":{
                "p":{
                    "cq:commerceType":"product","sku":"S",
                    "weight":120,"carat":0.5,"in_stock":true,"release":"2024-01-01T00:00:00.000Z"
                }
            }}""";
        AttributeManifest m = LegacyDiscovery.parse("venia", tree,
                new DiscoveryHints(List.of(), List.of()));
        assertEquals(NormalizedType.INT, m.entryFor("weight").orElseThrow().type());
        assertEquals(NormalizedType.FLOAT, m.entryFor("carat").orElseThrow().type());
        assertEquals(NormalizedType.BOOLEAN, m.entryFor("in_stock").orElseThrow().type());
    }

    @Test
    public void exposesDistinctSelectValues() {
        // Need enough variant observations of color with low cardinality ratio (<= 0.2)
        // to promote to SELECT. 15 variants / 3 distinct colors = 0.2 ratio.
        StringBuilder shirtVariants = new StringBuilder();
        String[] palette = {"Red", "Blue", "Green"};
        for (int i = 0; i < 15; i++) {
            shirtVariants.append(",\"v").append(i)
                .append("\":{\"cq:commerceType\":\"variant\",\"sku\":\"S1-V").append(i)
                .append("\",\"color\":\"").append(palette[i % palette.length]).append("\"}");
        }
        String tree = "{\"products\":{\"shirt\":{\"cq:commerceType\":\"product\",\"sku\":\"S1\""
                + shirtVariants + "}}}";
        var result = LegacyDiscovery.parseWithOptions("venia", tree,
                new DiscoveryHints(List.of(), List.of()));
        var values = result.options().get("color");
        assertNotNull(values);
        assertTrue(values.containsAll(java.util.Set.of("Red", "Blue", "Green")));
    }

    @Test
    public void declaredVariantAxisPromotedToSelectDespiteHighCardinality() {
        // Geometrixx shape: a master declares cq:productVariantAxes=[color,size] and models them as a
        // two-level tree (master -> colour-group variant -> size-leaf variant). Colour sits on the
        // intermediate nodes and is high-cardinality (every value distinct), so the cardinality
        // heuristic alone would keep it a STRING and drop it as a configurable axis. The declared axis
        // must override that, promoting colour to a SELECT axis with enumerable values.
        String tree = """
            {"products":{
                "wmapsp":{
                    "cq:commerceType":"product","sku":"wmapsp","jcr:title":"Saskatoon Parka",
                    "cq:productVariantAxes":["color","size"],
                    "p":{"cq:commerceType":"variant","sku":"wmapsp.p","color":"Purple",
                        "size-xs":{"cq:commerceType":"variant","size":"XS"},
                        "size-s":{"cq:commerceType":"variant","size":"S"}
                    },
                    "b":{"cq:commerceType":"variant","sku":"wmapsp.b","color":"Blue",
                        "size-m":{"cq:commerceType":"variant","size":"M"}
                    }
                }
            }}""";
        var result = LegacyDiscovery.parseWithOptions("venia", tree,
                new DiscoveryHints(List.of(), List.of()));
        AttributeManifest m = result.manifest();
        assertEquals(NormalizedType.SELECT, m.entryFor("color").orElseThrow().type());
        assertEquals(AttributeScope.VARIANT, m.entryFor("color").orElseThrow().scope());
        assertTrue(result.options().get("color").containsAll(java.util.Set.of("Purple", "Blue")));
    }

    @Test
    public void recordsConflictsInDiscoveryResult() {
        String tree = """
            {"products":{
                "a":{"cq:commerceType":"product","sku":"A","weight":100},
                "b":{"cq:commerceType":"product","sku":"B","weight":"heavy"}
            }}""";
        var result = LegacyDiscovery.parseWithOptions("venia", tree,
                new DiscoveryHints(List.of(), List.of()));
        assertTrue(result.conflicts().contains("weight"));
        assertEquals(NormalizedType.STRING, result.manifest().entryFor("weight").orElseThrow().type());
    }
}

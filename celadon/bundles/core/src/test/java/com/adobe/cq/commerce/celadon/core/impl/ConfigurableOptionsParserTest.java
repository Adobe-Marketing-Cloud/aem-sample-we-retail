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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurableOptionsParserTest {
    @Test
    public void shouldReturnEmptyForNullOrEmptyInput() {
        Assert.assertEquals(List.of(), ConfigurableOptionsParser.parseDefinitions(null));
        Assert.assertEquals(List.of(), ConfigurableOptionsParser.parseDefinitions(List.of()));
    }

    @Test
    public void shouldParseOptionDefinitionFragmentsWithColorSwatch() {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("label", "Fashion Color");
        definition.put("attributeCode", "fashion_color");
        definition.put("productField", "color");
        definition.put("swatchType", "color");
        definition.put("values", List.of("92;Peach;#fee1d2", "98;Lilac;#dcd5e1"));

        List<Map<String, Object>> options = ConfigurableOptionsParser.parseDefinitions(List.of(definition));

        Assert.assertEquals(1, options.size());
        Map<String, Object> option = options.getFirst();
        Assert.assertEquals("Fashion Color", option.get("label"));
        Assert.assertEquals("fashion_color", option.get("attribute_code"));
        Assert.assertEquals("celadon-opt-fashion_color", option.get("uid"));
        Assert.assertEquals("celadon-attr-fashion_color", option.get("attribute_uid"));

        List<Map<String, Object>> values = (List<Map<String, Object>>) option.get("values");
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("Peach", values.getFirst().get("label"));
        Assert.assertEquals(92, values.getFirst().get("value_index"));
        Assert.assertEquals("celadon-fashion_color-92", values.getFirst().get("uid"));
        Assert.assertEquals("Peach", values.getFirst().get("default_label"));
        Map<String, Object> swatch = (Map<String, Object>) values.getFirst().get("swatch_data");
        Assert.assertEquals("ColorSwatchData", swatch.get("__typename"));
        Assert.assertEquals("ColorSwatchData", swatch.get("__resolveType"));
        Assert.assertEquals("#fee1d2", swatch.get("value"));
    }

    @Test
    public void shouldParseOptionDefinitionFragmentsWithoutSwatches() {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("label", "Size");
        definition.put("attributeCode", "size");
        definition.put("productField", "size");
        definition.put("swatchType", "");
        definition.put("values", List.of("1;XS", "2;S", "3;M"));

        List<Map<String, Object>> options = ConfigurableOptionsParser.parseDefinitions(List.of(definition));

        Map<String, Object> option = options.getFirst();
        List<Map<String, Object>> values = (List<Map<String, Object>>) option.get("values");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals("XS", values.getFirst().get("label"));
        Assert.assertEquals(1, values.getFirst().get("value_index"));
        Assert.assertEquals("celadon-size-1", values.getFirst().get("uid"));
        Assert.assertNull(values.getFirst().get("swatch_data"));
    }

    @Test
    public void shouldParseOptionDefinitionFragmentsWithTextSwatches() {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("label", "Fashion Size");
        definition.put("attributeCode", "fashion_size");
        definition.put("productField", "size");
        definition.put("swatchType", "text");
        definition.put("values", List.of("128;L;L", "137;M;M"));

        List<Map<String, Object>> options = ConfigurableOptionsParser.parseDefinitions(List.of(definition));

        List<Map<String, Object>> values = (List<Map<String, Object>>) options.getFirst().get("values");
        Map<String, Object> swatch = (Map<String, Object>) values.getFirst().get("swatch_data");
        Assert.assertEquals("TextSwatchData", swatch.get("__typename"));
        Assert.assertEquals("L", swatch.get("value"));
    }

    @Test
    public void shouldDropOptionDefinitionsWithoutAttributeCode() {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("label", "Orphan");
        definition.put("attributeCode", "");
        definition.put("values", List.of("1;One"));

        List<Map<String, Object>> options = ConfigurableOptionsParser.parseDefinitions(List.of(definition));
        Assert.assertEquals(List.of(), options);
    }
}

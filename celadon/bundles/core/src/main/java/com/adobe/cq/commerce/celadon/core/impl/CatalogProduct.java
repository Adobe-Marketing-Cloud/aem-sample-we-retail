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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CatalogProduct {
    private final String categoryPath;
    private final String nodeName;
    private final Map<String, Object> properties;
    private final int position;

    CatalogProduct(String categoryPath, String nodeName, Map<String, Object> properties, int position) {
        this.categoryPath = PathSupport.normalizeRelativePath(categoryPath);
        this.nodeName = nodeName;
        this.properties = properties;
        this.position = position;
    }

    String categoryPath() {
        return categoryPath;
    }

    String nodeName() {
        return nodeName;
    }

    Map<String, Object> properties() {
        return properties;
    }

    int position() {
        return position;
    }

    String fullPath() {
        return categoryPath.isBlank() ? nodeName : categoryPath + "/" + nodeName;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> elements() {
        Object value = properties.get("elements");
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    String displayName() {
        String elementName = elementText("name");
        if (!elementName.isBlank()) {
            return elementName;
        }
        Object name = properties.get("name");
        return name == null ? nodeName : String.valueOf(name);
    }

    String descriptionHtml() {
        String description = elementText("description");
        return description.isBlank() ? "" : description;
    }

    String shortDescription() {
        return elementText("short_description");
    }

    String sku() {
        String elementSku = elementText("sku");
        if (!elementSku.isBlank()) {
            return elementSku;
        }
        Object propertySku = properties.get("sku");
        return propertySku == null ? "" : String.valueOf(propertySku);
    }

    String variationSku(String variationId) {
        return variationText("sku", variationId);
    }

    Object imageValue() {
        return elementValue("image");
    }

    List<String> additionalCategories() {
        return stringList(elementValue("additionalCategories"));
    }

    Object elementValue(String elementName) {
        Map<String, Object> element = elementMap(elementName);
        return element.get("value");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> elementMap(String elementName) {
        Object value = elements().get(elementName);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> elementVariationMap(String elementName, String variationId) {
        Map<String, Object> element = elementMap(elementName);
        Object variations = element.get("variations");
        if (variations instanceof Map<?, ?> map) {
            Object variation = map.get(variationId);
            if (variation instanceof Map<?, ?> variationMap) {
                return (Map<String, Object>) variationMap;
            }
        }
        return Map.of();
    }

    List<String> variationOrder() {
        Map<String, Object> nameElement = elementMap("name");
        List<String> order = stringList(nameElement.get("variationsOrder"));
        if (!order.isEmpty()) {
            return order;
        }
        Object variations = nameElement.get("variations");
        if (variations instanceof Map<?, ?> map && !map.isEmpty()) {
            List<String> keys = new ArrayList<>();
            for (Object key : map.keySet()) {
                if (key != null) {
                    keys.add(String.valueOf(key));
                }
            }
            return keys;
        }

        Set<String> union = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : elements().entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> elementMap)) {
                continue;
            }
            Object variationsValue = elementMap.get("variations");
            if (variationsValue instanceof Map<?, ?> variationsMap) {
                for (Object key : variationsMap.keySet()) {
                    if (key != null) {
                        union.add(String.valueOf(key));
                    }
                }
            }
        }
        return union.stream().sorted().toList();
    }

    private String elementText(String elementName) {
        Object value = elementValue(elementName);
        return value == null ? "" : String.valueOf(value);
    }

    private String variationText(String elementName, String variationId) {
        Object value = elementVariationMap(elementName, variationId).get("value");
        return value == null ? "" : String.valueOf(value);
    }

    private static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            List<String> result = new ArrayList<>();
            for (Object entry : collection) {
                if (entry != null && !entry.toString().isBlank()) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        if (value instanceof Object[] array) {
            List<String> result = new ArrayList<>();
            for (Object entry : array) {
                if (entry != null && !entry.toString().isBlank()) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        if (!value.toString().isBlank()) {
            return List.of(value.toString());
        }
        return List.of();
    }
}

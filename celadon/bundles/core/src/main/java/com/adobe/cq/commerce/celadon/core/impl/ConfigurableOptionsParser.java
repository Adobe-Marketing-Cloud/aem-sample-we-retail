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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigurableOptionsParser {
    private ConfigurableOptionsParser() {
    }

    /**
     * Parses a list of Option Definition CFs (as maps with {@code label}, {@code attributeCode},
     * {@code productField}, {@code swatchType}, {@code values}) into the output shape the
     * GraphQL layer consumes. Entries in {@code values} use the compact format
     * {@code <valueIndex>;<label>;<swatchValue>}.
     */
    static List<Map<String, Object>> parseDefinitions(List<Map<String, Object>> optionDefinitions) {
        if (optionDefinitions == null || optionDefinitions.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> definition : optionDefinitions) {
            if (definition == null) {
                continue;
            }
            String label = stringValue(definition.get("label"));
            String attributeCode = stringValue(definition.get("attributeCode"));
            String productField = stringValue(definition.get("productField"));
            String swatchType = stringValue(definition.get("swatchType"));
            List<String> values = stringListValue(definition.get("values"));
            if (attributeCode.isBlank()) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("label", label);
            option.put("attribute_code", attributeCode);
            option.put("uid", "celadon-opt-" + attributeCode);
            option.put("attribute_uid", "celadon-attr-" + attributeCode);
            if (!productField.isBlank()) {
                option.put("product_field", productField);
            }
            option.put("values", parseCompactValues(attributeCode, label, swatchType, values));
            normalized.add(option);
        }
        return normalized;
    }

    private static List<Map<String, Object>> parseCompactValues(String attributeCode,
                                                                String optionLabel,
                                                                String swatchType,
                                                                List<String> compactValues) {
        if (compactValues == null || compactValues.isEmpty()) {
            return List.of();
        }
        String swatchTypename = swatchTypename(swatchType);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String raw : compactValues) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(";", -1);
            int valueIndex = parts.length > 0 ? integerValue(parts[0]) : 0;
            String valueLabel = parts.length > 1 ? parts[1].trim() : "";
            String swatchValue = parts.length > 2 ? parts[2].trim() : "";
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("label", valueLabel);
            value.put("value_index", valueIndex);
            value.put("uid", "celadon-" + attributeCode + "-" + valueIndex);
            value.put("default_label", valueLabel.isBlank() ? optionLabel : valueLabel);
            if (swatchTypename != null && !swatchValue.isEmpty()) {
                Map<String, Object> swatch = new LinkedHashMap<>();
                swatch.put("__typename", swatchTypename);
                swatch.put("__resolveType", swatchTypename);
                swatch.put("value", swatchValue);
                value.put("swatch_data", swatch);
            } else {
                value.put("swatch_data", null);
            }
            result.add(value);
        }
        return result;
    }

    private static String swatchTypename(String swatchType) {
        if (swatchType == null || swatchType.isBlank()) {
            return null;
        }
        return switch (swatchType.trim().toLowerCase()) {
            case "color" -> "ColorSwatchData";
            case "text" -> "TextSwatchData";
            case "image" -> "ImageSwatchData";
            default -> null;
        };
    }

    private static List<String> stringListValue(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
        } else if (value != null && value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            for (int index = 0; index < length; index++) {
                Object entry = java.lang.reflect.Array.get(value, index);
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
        } else if (value instanceof String string) {
            result.add(string);
        }
        return result;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static int integerValue(Object value) {
        if (value instanceof Number number) {
            return (int) Math.round(number.doubleValue());
        }
        if (value == null) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString().trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Given a parsed option (from {@link #parseDefinitions}), find the value entry whose
     * {@code label}/{@code default_label} or numeric {@code value_index} matches {@code rawValue}.
     * Returns an empty map if no match is found.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> findValue(Map<String, Object> option, Object rawValue) {
        if (option == null || option.isEmpty() || rawValue == null) {
            return Map.of();
        }
        Object values = option.get("values");
        if (!(values instanceof List<?> list) || list.isEmpty()) {
            return Map.of();
        }
        String rawString = rawValue.toString();
        String normalized = normalizeToken(rawString);
        if (!normalized.isBlank()) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> map)) {
                    continue;
                }
                Map<String, Object> value = (Map<String, Object>) map;
                if (normalized.equals(normalizeToken(stringValue(value.get("label"))))
                        || normalized.equals(normalizeToken(stringValue(value.get("default_label"))))) {
                    return value;
                }
            }
        }
        Integer numeric = parseNumericValue(rawString);
        if (numeric != null) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> map)) {
                    continue;
                }
                Map<String, Object> value = (Map<String, Object>) map;
                if (integerValue(value.get("value_index")) == numeric) {
                    return value;
                }
            }
        }
        return Map.of();
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isLetterOrDigit(character)) {
                normalized.append(Character.toLowerCase(character));
            }
        }
        return normalized.toString();
    }

    private static Integer parseNumericValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(rawValue.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

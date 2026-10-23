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

import com.adobe.cq.commerce.celadon.aem.attribute.source.MagentoIntrospector;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Writes and reuses Option Definition Content Fragments under the
 * {@code _options/} folder of a catalog.
 *
 * <p>Each option carries an {@code attributeCode}, a {@code productField}
 * mapping, an optional {@code swatchType}, and a set of compact
 * {@code <valueIndex>;<label>;<swatchValue>} value entries. Definitions with
 * identical attribute code and value set are deduplicated within the catalog.
 */
final class OptionDefinitionWriter {
    private static final String OPTIONS_FOLDER = "_options";

    private final ResourceResolver resolver;
    private final Resource optionsFolder;
    private final Resource modelResource;
    private final Map<String, String> pathByKey = new LinkedHashMap<>();
    private final Set<String> usedNodeNames = new LinkedHashSet<>();

    OptionDefinitionWriter(ResourceResolver resolver,
                           Resource catalogRoot,
                           Resource optionDefinitionModel) throws PersistenceException {
        this.resolver = resolver;
        this.modelResource = optionDefinitionModel;
        this.optionsFolder = AemRepositorySupport.ensureOrderedFolder(
                resolver,
                catalogRoot.getPath() + "/" + OPTIONS_FOLDER,
                "Configurable Options"
        );
    }

    /**
     * Ensures an Option Definition CF for the given input. Returns the absolute
     * path to that CF, creating it on first use or reusing a matching one.
     */
    String ensureOption(OptionInput input) throws Exception {
        String key = cacheKey(input);
        String cached = pathByKey.get(key);
        if (cached != null) {
            return cached;
        }
        String nodeName = uniqueNodeName(input);
        Resource fragment = AemContentFragmentSupport.ensureFragment(
                resolver,
                modelResource,
                optionsFolder,
                nodeName,
                fragmentTitle(input)
        );
        AemContentFragmentSupport.writeText(fragment, "label", input.label(), "text/plain");
        AemContentFragmentSupport.writeText(fragment, "attributeCode", input.attributeCode(), "text/plain");
        AemContentFragmentSupport.writeText(fragment, "productField", input.productField(), "text/plain");
        AemContentFragmentSupport.writeText(fragment, "swatchType", input.swatchType(), "text/plain");
        AemContentFragmentSupport.writeTyped(fragment, "values", input.values());
        pathByKey.put(key, fragment.getPath());
        return fragment.getPath();
    }

    /**
     * Ensures an Option Definition CF for a manifest entry, using the
     * introspected {@code OptionValue}s as the value set. Each option value
     * becomes a {@code <valueIndex>;<label>} entry where {@code valueIndex}
     * is taken from {@link MagentoIntrospector.OptionValue#value()} when
     * numeric, otherwise the 1-based position.
     */
    String ensureOptionFromManifest(AttributeEntry entry,
                                    List<MagentoIntrospector.OptionValue> values) throws Exception {
        List<String> encoded = new ArrayList<>();
        int position = 1;
        for (MagentoIntrospector.OptionValue v : values) {
            int valueIndex = parseInt(v.value(), position);
            encoded.add(encodeValue(valueIndex, v.label(), ""));
            position++;
        }
        OptionInput input = new OptionInput(
                entry.label() == null ? entry.code() : entry.label(),
                entry.code(),
                entry.code(),
                "",
                encoded);
        return ensureOption(input);
    }

    /**
     * Ensures an Option Definition CF for a manifest entry, using a set of
     * discovered string values. Each value is encoded as both the option
     * value and its label.
     */
    String ensureOptionFromManifestValues(AttributeEntry entry,
                                          Set<String> values) throws Exception {
        List<String> encoded = new ArrayList<>();
        int position = 1;
        for (String value : values) {
            encoded.add(encodeValue(position++, value, ""));
        }
        OptionInput input = new OptionInput(
                entry.label() == null ? entry.code() : entry.label(),
                entry.code(),
                entry.code(),
                "",
                encoded);
        return ensureOption(input);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    void writeProductReferences(Resource productFragment, List<String> optionPaths) throws Exception {
        List<String> paths = optionPaths == null ? List.of() : optionPaths;
        AemContentFragmentSupport.writeTyped(productFragment, "configurableOptions", paths);
    }

    /**
     * Builds an {@link OptionInput} from a Magento-shaped configurable option
     * entry (label, attribute_code, values[{value_index, label, swatch_data}]).
     *
     * <p>The {@code productField} is set to {@code attributeCode}. The manifest
     * is the source of truth for product-field resolution at read time; this
     * field is kept on the CF for backward compatibility / debugging only.</p>
     */
    static OptionInput fromMagento(Map<String, Object> magentoOption) {
        String label = stringValue(magentoOption.get("label"));
        String attributeCode = stringValue(magentoOption.get("attribute_code"));
        String productField = attributeCode;
        String swatchType = "";
        List<String> values = new ArrayList<>();
        Object valuesObject = magentoOption.get("values");
        if (valuesObject instanceof List<?> list) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> valueMap)) {
                    continue;
                }
                int valueIndex = integerValue(valueMap.get("value_index"));
                String valueLabel = stringValue(valueMap.get("label"));
                String swatchValue = "";
                Object swatch = valueMap.get("swatch_data");
                if (swatch instanceof Map<?, ?> swatchMap) {
                    String typename = stringValue(swatchMap.get("__typename"));
                    String typedSwatch = fromTypename(typename);
                    if (!typedSwatch.isBlank()) {
                        swatchType = typedSwatch;
                    }
                    swatchValue = stringValue(swatchMap.get("value"));
                }
                values.add(encodeValue(valueIndex, valueLabel, swatchValue));
            }
        }
        return new OptionInput(label, attributeCode, productField, swatchType, values);
    }

    private String uniqueNodeName(OptionInput input) {
        String base = buildNodeName(input);
        if (base.isBlank()) {
            base = "option";
        }
        if (usedNodeNames.add(base)) {
            return base;
        }
        int suffix = 2;
        while (true) {
            String candidate = base + "-" + suffix;
            if (usedNodeNames.add(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    private static String buildNodeName(OptionInput input) {
        StringBuilder builder = new StringBuilder();
        builder.append(slug(input.attributeCode()));
        builder.append("--");
        List<String> labels = input.valueLabels();
        int visibleCount = Math.min(4, labels.size());
        for (int index = 0; index < visibleCount; index++) {
            if (index > 0) {
                builder.append('-');
            }
            builder.append(slug(labels.get(index)));
        }
        if (labels.size() > 4) {
            builder.append("-plus").append(labels.size() - 4);
        }
        return builder.toString();
    }

    private static String fragmentTitle(OptionInput input) {
        List<String> labels = input.valueLabels();
        String head = labels.stream().limit(4).collect(Collectors.joining(", "));
        String suffix = labels.size() > 4 ? ", ... (" + labels.size() + " values)" : "";
        if (labels.isEmpty()) {
            return input.label();
        }
        return input.label() + ": " + head + suffix;
    }

    private static String cacheKey(OptionInput input) {
        return input.attributeCode() + "|"
                + input.productField() + "|"
                + input.swatchType() + "|"
                + String.join(",", sortedCopy(input.values()));
    }

    private static List<String> sortedCopy(List<String> values) {
        List<String> copy = new ArrayList<>(values);
        copy.sort(String::compareTo);
        return copy;
    }

    private static String slug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        boolean lastDash = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '.') {
                continue;
            }
            if (Character.isLetterOrDigit(character)) {
                builder.append(Character.toLowerCase(character));
                lastDash = false;
            } else if (!lastDash && builder.length() > 0) {
                builder.append('-');
                lastDash = true;
            }
        }
        while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '-') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private static String fromTypename(String typename) {
        if (typename == null) {
            return "";
        }
        return switch (typename) {
            case "ColorSwatchData" -> "color";
            case "TextSwatchData" -> "text";
            case "ImageSwatchData" -> "image";
            default -> "";
        };
    }

    private static String encodeValue(int valueIndex, String label, String swatchValue) {
        String safeLabel = label == null ? "" : label.replace(';', ',').trim();
        String safeSwatch = swatchValue == null ? "" : swatchValue.replace(';', ',').trim();
        if (safeSwatch.isEmpty()) {
            return valueIndex + ";" + safeLabel;
        }
        return valueIndex + ";" + safeLabel + ";" + safeSwatch;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int integerValue(Object value) {
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

    static final class OptionInput {
        private final String label;
        private final String attributeCode;
        private final String productField;
        private final String swatchType;
        private final List<String> values;

        OptionInput(String label, String attributeCode, String productField, String swatchType, List<String> values) {
            this.label = label == null ? "" : label;
            this.attributeCode = attributeCode == null ? "" : attributeCode;
            this.productField = productField == null ? "" : productField;
            this.swatchType = swatchType == null ? "" : swatchType;
            this.values = values == null ? List.of() : List.copyOf(values);
        }

        String label() { return label; }
        String attributeCode() { return attributeCode; }
        String productField() { return productField; }
        String swatchType() { return swatchType; }
        List<String> values() { return values; }

        boolean isEmpty() {
            return attributeCode.isBlank() || values.isEmpty();
        }

        List<String> valueLabels() {
            List<String> labels = new ArrayList<>(values.size());
            for (String entry : values) {
                String[] parts = entry.split(";", -1);
                labels.add(parts.length > 1 ? parts[1] : "");
            }
            return labels;
        }
    }
}

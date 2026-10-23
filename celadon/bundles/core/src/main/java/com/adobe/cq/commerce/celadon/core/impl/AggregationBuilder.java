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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

final class AggregationBuilder {

    private static final int PRICE_BUCKET_WIDTH = 100;

    private AggregationBuilder() {
    }

    @FunctionalInterface
    interface ConfigurableOptionsLookup {
        List<Map<String, Object>> get(CatalogProduct product) throws IOException, InterruptedException;
    }

    /**
     * Manifest-driven overload: in addition to the legacy facets produced by
     * {@link #build(List, Map, ConfigurableOptionsLookup)}, surface aggregations
     * for every {@link AttributeEntry#aggregatable() aggregatable} manifest entry
     * whose type is not already covered (price, category, select/multiselect).
     */
    static List<Map<String, Object>> build(List<CatalogProduct> products,
                                            Map<String, CatalogCategory> categoriesByPath,
                                            ConfigurableOptionsLookup lookup,
                                            AttributeManifest manifest,
                                            BiFunction<CatalogProduct, String, Object> valueAccessor)
            throws IOException, InterruptedException {
        List<Map<String, Object>> result = new ArrayList<>(build(products, categoriesByPath, lookup));
        if (manifest == null || products == null || products.isEmpty()) {
            return result;
        }
        for (AttributeEntry entry : manifest.aggregatable()) {
            switch (entry.type()) {
                case INT, FLOAT -> result.add(numericFacet(products, entry, valueAccessor));
                case BOOLEAN -> result.add(booleanFacet(products, entry, valueAccessor));
                default -> {
                    // SELECT/MULTISELECT are already surfaced via the configurable-options pass;
                    // PRICE is handled by the price facet; STRING/TEXT/DATE/IMAGE_URL are skipped.
                }
            }
        }
        return result;
    }

    private static Map<String, Object> numericFacet(List<CatalogProduct> products,
                                                    AttributeEntry entry,
                                                    BiFunction<CatalogProduct, String, Object> values) {
        List<Double> nums = new ArrayList<>();
        for (CatalogProduct p : products) {
            Object v = values == null ? null : values.apply(p, entry.code());
            if (v instanceof Number n) {
                nums.add(n.doubleValue());
            } else if (v != null) {
                try {
                    nums.add(Double.parseDouble(v.toString().trim()));
                } catch (NumberFormatException ignored) {
                    // skip non-numeric value
                }
            }
        }
        String label = entry.label() == null || entry.label().isBlank() ? entry.code() : entry.label();
        if (nums.isEmpty()) {
            return facet(entry.code(), label, List.of());
        }
        double min = nums.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = nums.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        long lo = (long) Math.floor(min);
        long hi = (long) Math.ceil(max);
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("count", nums.size());
        option.put("label", lo + "-" + hi);
        option.put("value", lo + "_" + hi);
        return facet(entry.code(), label, List.of(option));
    }

    private static Map<String, Object> booleanFacet(List<CatalogProduct> products,
                                                    AttributeEntry entry,
                                                    BiFunction<CatalogProduct, String, Object> values) {
        long t = 0;
        long f = 0;
        for (CatalogProduct p : products) {
            Object v = values == null ? null : values.apply(p, entry.code());
            if (v == null) {
                continue;
            }
            if (Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(v.toString().trim())) {
                t++;
            } else if (Boolean.FALSE.equals(v) || "false".equalsIgnoreCase(v.toString().trim())) {
                f++;
            }
        }
        String label = entry.label() == null || entry.label().isBlank() ? entry.code() : entry.label();
        List<Map<String, Object>> options = new ArrayList<>();
        if (t > 0) {
            options.add(option("true", "Yes", (int) t));
        }
        if (f > 0) {
            options.add(option("false", "No", (int) f));
        }
        return facet(entry.code(), label, options);
    }

    static List<Map<String, Object>> build(List<CatalogProduct> products,
                                            Map<String, CatalogCategory> categoriesByPath,
                                            ConfigurableOptionsLookup lookup) throws IOException, InterruptedException {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        Map<String, String> configFacetLabels = new LinkedHashMap<>();
        Map<String, Map<Integer, OptionBucket>> configFacets = new LinkedHashMap<>();
        for (CatalogProduct product : products) {
            List<Map<String, Object>> options = lookup.get(product);
            for (Map<String, Object> option : options) {
                accumulateConfigurableOption(option, configFacetLabels, configFacets);
            }
        }

        Map<Integer, Integer> priceBuckets = new TreeMap<>();
        for (CatalogProduct product : products) {
            int bucket = priceBucketStart(productMinPrice(product));
            priceBuckets.merge(bucket, 1, Integer::sum);
        }

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        for (CatalogProduct product : products) {
            String current = "";
            for (String segment : PathSupport.segments(product.categoryPath())) {
                current = PathSupport.joinPath(current, segment);
                categoryCounts.merge(current, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> aggregations = new ArrayList<>();
        if (!priceBuckets.isEmpty()) {
            aggregations.add(buildPriceFacet(priceBuckets));
        }
        if (!categoryCounts.isEmpty()) {
            aggregations.add(buildCategoryFacet(categoryCounts, categoriesByPath));
        }
        for (Map.Entry<String, Map<Integer, OptionBucket>> entry : configFacets.entrySet()) {
            aggregations.add(buildConfigurableFacet(entry.getKey(), configFacetLabels.get(entry.getKey()), entry.getValue()));
        }
        return aggregations;
    }

    private static void accumulateConfigurableOption(Map<String, Object> option,
                                                     Map<String, String> facetLabels,
                                                     Map<String, Map<Integer, OptionBucket>> facets) {
        String code = stringValue(option.get("attribute_code"));
        if (code.isBlank()) {
            return;
        }
        facetLabels.putIfAbsent(code, stringValue(option.get("label")));
        Map<Integer, OptionBucket> bucketByValue = facets.computeIfAbsent(code, key -> new LinkedHashMap<>());
        Object values = option.get("values");
        if (!(values instanceof List<?> list)) {
            return;
        }
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> map)) {
                continue;
            }
            int valueIndex = ConfigurableOptionsParser.integerValue(map.get("value_index"));
            String label = stringValue(((Map<String, Object>) map).getOrDefault("label", ((Map<String, Object>) map).get("default_label")));
            OptionBucket bucket = bucketByValue.computeIfAbsent(valueIndex, key -> new OptionBucket(key, label));
            bucket.increment();
        }
    }

    private static double productMinPrice(CatalogProduct product) {
        Map<String, Object> priceElement = product.elementMap("price");
        double base = numberValue(priceElement.get("value"), 0.0d);
        double min = base;
        Object variations = priceElement.get("variations");
        if (variations instanceof Map<?, ?> map) {
            for (Object entry : map.values()) {
                if (!(entry instanceof Map<?, ?> variationMap)) {
                    continue;
                }
                double value = numberValue(variationMap.get("value"), base);
                min = Math.min(min, value);
            }
        }
        return min;
    }

    private static int priceBucketStart(double price) {
        if (price < 0) {
            return 0;
        }
        return (int) (price / PRICE_BUCKET_WIDTH) * PRICE_BUCKET_WIDTH;
    }

    private static Map<String, Object> buildPriceFacet(Map<Integer, Integer> priceBuckets) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : priceBuckets.entrySet()) {
            int lo = entry.getKey();
            int hi = lo + PRICE_BUCKET_WIDTH;
            options.add(option(lo + "_" + hi, lo + "-" + hi, entry.getValue()));
        }
        return facet("price", "Price", options);
    }

    private static Map<String, Object> buildCategoryFacet(Map<String, Integer> counts,
                                                           Map<String, CatalogCategory> categoriesByPath) {
        List<Map<String, Object>> options = new ArrayList<>();
        counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .forEach(entry -> {
                    String path = entry.getKey();
                    CatalogCategory category = categoriesByPath == null ? null : categoriesByPath.get(path);
                    String label = category != null ? category.displayName() : PathSupport.leaf(path);
                    options.add(option(path, label, entry.getValue()));
                });
        return facet("category_uid", "Category", options);
    }

    private static Map<String, Object> buildConfigurableFacet(String code, String label, Map<Integer, OptionBucket> buckets) {
        List<Map<String, Object>> options = buckets.values().stream()
                .sorted(Comparator.<OptionBucket>comparingInt(OptionBucket::count).reversed()
                        .thenComparingInt(OptionBucket::valueIndex))
                .map(bucket -> option(String.valueOf(bucket.valueIndex()), bucket.label(), bucket.count()))
                .toList();
        return facet(code, label == null || label.isBlank() ? code : label, options);
    }

    private static Map<String, Object> facet(String code, String label, List<Map<String, Object>> options) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("attribute_code", code);
        map.put("count", options.size());
        map.put("label", label);
        map.put("options", options);
        return map;
    }

    private static Map<String, Object> option(String value, String label, int count) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", count);
        map.put("label", label);
        map.put("value", value);
        return map;
    }

    private static double numberValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static final class OptionBucket {
        private final int valueIndex;
        private final String label;
        private int count;

        OptionBucket(int valueIndex, String label) {
            this.valueIndex = valueIndex;
            this.label = label;
        }

        void increment() {
            count++;
        }

        int valueIndex() {
            return valueIndex;
        }

        String label() {
            return label;
        }

        int count() {
            return count;
        }
    }
}

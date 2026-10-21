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

import com.adobe.cq.commerce.celadon.core.api.CatalogGateway;
import com.adobe.cq.commerce.celadon.core.api.JsonSupport;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import graphql.GraphqlErrorException;
import graphql.schema.DataFetchingEnvironment;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CatalogService {
    private static final String REQUEST_CONTEXT_KEY = "celadonRequestContext";
    private static final Logger LOG = Logger.getLogger(CatalogService.class.getName());

    private final AttributeManifest manifest;
    // Field names already declared on the base product output types. Manifest codes
    // that collide with these are NOT injected into the result maps, since the base
    // schema already owns those fields (e.g. sku, name, price).
    private final Set<String> reservedFields;

    public CatalogService() {
        this(AttributeManifest.empty(""), Set.of());
    }

    public CatalogService(AttributeManifest manifest) {
        this(manifest, Set.of());
    }

    public CatalogService(AttributeManifest manifest, Set<String> reservedFields) {
        this.manifest = manifest == null ? AttributeManifest.empty("") : manifest;
        this.reservedFields = reservedFields == null ? Set.of() : Set.copyOf(reservedFields);
    }

    AttributeManifest manifest() {
        return manifest;
    }

    Map<String, Object> products(DataFetchingEnvironment environment) throws IOException, InterruptedException {
        GraphqlRequestContext requestContext = requestContext(environment);
        CatalogSnapshot snapshot = requestContext.snapshot();
        Map<String, Object> filter = JsonSupport.map(environment.getArgument("filter"));
        String search = stringValue(environment.getArgument("search"));
        boolean searchPresent = search != null;
        boolean nonBlankSearch = search != null && !search.isBlank();

        int pageSize = normalizePositiveArg(environment.getArgument("pageSize"), 20, "pageSize");
        int currentPage = normalizePositiveArg(environment.getArgument("currentPage"), 1, "currentPage");

        List<CatalogProduct> resolvedProducts;
        if (filter.isEmpty() && !nonBlankSearch && environment.getArgument("filter") == null) {
            resolvedProducts = List.of();
        } else {
            resolvedProducts = resolveProducts(requestContext, snapshot, filter, search);
        }

        SortPlan sortPlan = buildSortPlan(environment.getArgument("sort"), nonBlankSearch, filter);
        List<ScoredProduct> scored = scoreAndSort(resolvedProducts, sortPlan, search);
        int totalCount = scored.size();
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
        if (totalCount > 0 && currentPage > totalPages) {
            throw GraphqlErrorException.newErrorException()
                    .message("currentPage exceeds total_pages")
                    .build();
        }
        int start = Math.max(0, (currentPage - 1) * pageSize);
        int end = Math.min(totalCount, start + pageSize);
        List<Map<String, Object>> items = scored.subList(start, end).stream()
                .map(hit -> buildProductMap(requestContext, snapshot, hit.product(), false))
                .toList();

        List<CatalogProduct> filteredProducts = scored.stream().map(ScoredProduct::product).toList();
        List<Map<String, Object>> aggregations =
                AggregationBuilder.build(
                        filteredProducts,
                        snapshot.categoriesByPath(),
                        requestContext::configurableOptions,
                        manifest,
                        this::readAttributeValue);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("total_count", totalCount);
        response.put("aggregations", aggregations);
        response.put("sort_fields", buildSortFields(nonBlankSearch));
        response.put("page_info", Map.of(
                "current_page", currentPage,
                "page_size", pageSize,
                "total_pages", totalPages
        ));
        return response;
    }

    List<Map<String, Object>> categoryList(DataFetchingEnvironment environment) throws IOException, InterruptedException {
        GraphqlRequestContext requestContext = requestContext(environment);
        CatalogSnapshot snapshot = requestContext.snapshot();
        Set<String> paths = new LinkedHashSet<>(resolveCategoryFilters(snapshot, JsonSupport.map(environment.getArgument("filters"))));
        if (paths.isEmpty()) {
            paths.add("/");
        }
        boolean includeProducts = selectionIncludesProducts(environment);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String path : paths) {
            result.add(buildCategoryMap(requestContext, snapshot, path, includeProducts));
        }
        return result;
    }

    /**
     * Whether the GraphQL selection under a category actually requests the
     * (expensive) product list. Navigation and breadcrumb queries don't, so we
     * skip building a full product map per product — which for the root category
     * means the entire catalog and otherwise dominates response time.
     */
    private static boolean selectionIncludesProducts(DataFetchingEnvironment environment) {
        try {
            return environment.getSelectionSet().contains("products")
                    || environment.getSelectionSet().contains("**/products");
        } catch (RuntimeException ex) {
            return true;
        }
    }

    Map<String, Object> category(DataFetchingEnvironment environment) throws IOException, InterruptedException {
        GraphqlRequestContext requestContext = requestContext(environment);
        CatalogSnapshot snapshot = requestContext.snapshot();
        String path = stringValue(environment.getArgument("id"));
        if (path.isBlank()) {
            path = stringValue(environment.getArgument("uid"));
        }
        if (path.isBlank()) {
            path = stringValue(environment.getArgument("url_path"));
        }
        if (path.isBlank()) {
            List<String> fromFilters = resolveCategoryFilters(snapshot, JsonSupport.map(environment.getArgument("filters")));
            path = fromFilters.isEmpty() ? "/" : fromFilters.getFirst();
        }
        if (path.isBlank()) {
            path = "/";
        }
        if (!"/".equals(path)) {
            path = snapshot.resolveCategoryPathFromIdOrPath(path);
        }
        return buildCategoryMap(requestContext, snapshot, path, selectionIncludesProducts(environment));
    }

    Map<String, Object> categories(DataFetchingEnvironment environment) throws IOException, InterruptedException {
        GraphqlRequestContext requestContext = requestContext(environment);
        CatalogSnapshot snapshot = requestContext.snapshot();
        Map<String, Object> filters = JsonSupport.map(environment.getArgument("filters"));
        boolean includeProducts = selectionIncludesProducts(environment);
        List<Map<String, Object>> items = new ArrayList<>();
        if (filters.containsKey("category_uid")) {
            String path = firstEqOrInValue(filters.get("category_uid"));
            items.add(buildCategoryMap(requestContext, snapshot, snapshot.resolveCategoryPathFromIdOrPath(path), includeProducts));
        } else if (filters.containsKey("parent_category_uid")) {
            String parentPath = snapshot.resolveCategoryPathFromIdOrPath(firstEqOrInValue(filters.get("parent_category_uid")));
            for (String childPath : directChildren(snapshot, parentPath)) {
                items.add(buildCategoryMap(requestContext, snapshot, childPath, includeProducts));
            }
        } else if (filters.containsKey("url_path")) {
            String path = firstEqOrInValue(filters.get("url_path"));
            for (String childPath : directChildren(snapshot, path)) {
                items.add(buildCategoryMap(requestContext, snapshot, childPath, includeProducts));
            }
        }
        return Map.of(
                "total_count", items.size(),
                "items", items,
                "page_info", Map.of("total_pages", items.isEmpty() ? 0 : 1)
        );
    }

    Map<String, Object> customAttributeMetadata(DataFetchingEnvironment environment, Map<String, Map<String, String>> metadata)
            throws IOException, InterruptedException {
        Object rawAttributes = environment.getArgument("attributes");
        Set<String> requested = new LinkedHashSet<>();
        if (rawAttributes instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry instanceof Map<?, ?> map) {
                    Object attributeCode = map.get("attribute_code");
                    if (attributeCode != null) {
                        requested.add(attributeCode.toString());
                    }
                }
            }
        }

        // Manifest entries are the source of truth when populated.
        Map<String, Map<String, String>> combined = new LinkedHashMap<>();
        for (AttributeEntry entry : manifest.filterable()) {
            String label = entry.label() == null || entry.label().isBlank() ? entry.code() : entry.label();
            combined.put(entry.code(), Map.of(
                    "attribute_code", entry.code(),
                    "attribute_type", magentoTypeFor(entry.type()),
                    "input_type", magentoInputFor(entry.type()),
                    "label", label
            ));
        }
        // Legacy hardcoded fallbacks: schema-introspected scalar metadata + dynamic product
        // attributes. Kept until Phase 9-10 populates the manifest end-to-end.
        for (Map<String, String> entry : metadata.values()) {
            combined.putIfAbsent(entry.get("attribute_code"), entry);
        }
        for (Map<String, String> entry : dynamicAttributeMetadata(environment).values()) {
            combined.putIfAbsent(entry.get("attribute_code"), entry);
        }

        List<Map<String, String>> items = new ArrayList<>();
        for (Map<String, String> item : combined.values()) {
            if (requested.isEmpty() || requested.contains(item.get("attribute_code"))) {
                items.add(new LinkedHashMap<>(item));
            }
        }
        return Map.of("items", items);
    }

    private static String magentoTypeFor(com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType t) {
        return switch (t) {
            case INT, SELECT, MULTISELECT, BOOLEAN -> "Int";
            case FLOAT, PRICE -> "Float";
            default -> "String";
        };
    }

    private static String magentoInputFor(com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType t) {
        return switch (t) {
            case SELECT -> "select";
            case MULTISELECT -> "multiselect";
            case BOOLEAN -> "boolean";
            case DATE -> "date";
            case TEXT -> "textarea";
            default -> "text";
        };
    }

    private Map<String, Map<String, String>> dynamicAttributeMetadata(DataFetchingEnvironment environment)
            throws IOException, InterruptedException {
        GraphqlRequestContext requestContext = requestContext(environment);
        CatalogSnapshot snapshot = requestContext.snapshot();
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        result.put("category_uid", Map.of(
                "attribute_code", "category_uid",
                "attribute_type", "String",
                "input_type", "select"
        ));
        for (CatalogProduct product : snapshot.products()) {
            for (Map<String, Object> option : requestContext.configurableOptions(product)) {
                String code = stringValue(option.get("attribute_code"));
                if (code.isBlank()) {
                    continue;
                }
                result.computeIfAbsent(code, key -> Map.of(
                        "attribute_code", key,
                        "attribute_type", "Int",
                        "input_type", "select"
                ));
            }
        }
        return result;
    }

    private List<String> directChildren(CatalogSnapshot snapshot, String parentPath) {
        String normalized = parentPath == null || parentPath.isBlank() ? "/" : parentPath;
        CatalogCategory category = snapshot.categoriesByPath().get(normalized);
        if (category == null) {
            return List.of();
        }
        return List.copyOf(category.childPaths());
    }

    private List<String> resolveCategoryFilters(CatalogSnapshot snapshot, Map<String, Object> filters) {
        if (filters.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String name = entry.getKey();
            List<String> values = allEqOrInValues(entry.getValue());
            for (String value : values) {
                if ("url_path".equals(name)) {
                    resolved.add(PathSupport.normalizeRelativePath(value));
                } else {
                    resolved.add(snapshot.resolveCategoryPathFromIdOrPath(value));
                }
            }
        }
        return resolved.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
    }

    private List<CatalogProduct> resolveProducts(GraphqlRequestContext requestContext,
                                                  CatalogSnapshot snapshot,
                                                  Map<String, Object> filter,
                                                  String search) throws IOException, InterruptedException {
        List<CatalogProduct> base = new ArrayList<>(snapshot.products());
        boolean hasExplicitProductFilter = false;

        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> condition = JsonSupport.map(entry.getValue());
            // Relational filter special-cases (NOT attribute filters; never manifest-driven).
            if ("category_uid".equals(key)) {
                hasExplicitProductFilter = true;
                base.retainAll(snapshot.productsInCategorySubtree(firstEqOrInValue(condition)));
                continue;
            }
            if ("category_id".equals(key)) {
                hasExplicitProductFilter = true;
                base.retainAll(snapshot.productsInCategorySubtree(firstEqOrInValue(condition)));
                continue;
            }
            // Manifest-driven attribute filters take precedence when an entry exists.
            AttributeEntry attr = manifest.entryFor(key).orElse(null);
            if (attr != null) {
                FilterResult applied = applyAttributeFilter(requestContext, snapshot, base, attr, entry.getValue(), condition);
                base = applied.base();
                if (applied.matched()) {
                    hasExplicitProductFilter = hasExplicitProductFilter || applied.explicitProductFilter();
                    continue;
                }
            }
            // Legacy hardcoded branches (kept until Phase 9-10 populates the manifest).
            if ("sku".equals(key)) {
                hasExplicitProductFilter = true;
                base = filterByExactProducts(snapshot, condition);
            } else if ("url_key".equals(key)) {
                hasExplicitProductFilter = true;
                base = filterByExactProducts(snapshot, condition);
            } else if ("name".equals(key)) {
                String match = stringValue(condition.get("match")).toLowerCase(Locale.ROOT);
                if (!match.isBlank()) {
                    base = base.stream()
                            .filter(product -> product.displayName().toLowerCase(Locale.ROOT).contains(match))
                            .toList();
                }
            } else if ("price".equals(key)) {
                Double from = parseDouble(condition.get("from"));
                Double to = parseDouble(condition.get("to"));
                if (from != null || to != null) {
                    hasExplicitProductFilter = true;
                    double lowerBound = from == null ? Double.NEGATIVE_INFINITY : from;
                    double upperBound = to == null ? Double.POSITIVE_INFINITY : to;
                    base = base.stream()
                            .filter(product -> {
                                double value = priceRange(product).min();
                                return value >= lowerBound && value <= upperBound;
                            })
                            .toList();
                }
            } else {
                List<String> requestedValues = allEqOrInValues(entry.getValue());
                if (!requestedValues.isEmpty()) {
                    hasExplicitProductFilter = true;
                    base = filterByConfigurableOption(requestContext, base, key, requestedValues);
                }
            }
        }

        if (filter.isEmpty() && search != null) {
            return new ArrayList<>(snapshot.products());
        }
        if (filter.isEmpty()) {
            return base;
        }
        if (!hasExplicitProductFilter && search == null) {
            return List.of();
        }
        return base;
    }

    private record FilterResult(List<CatalogProduct> base, boolean matched, boolean explicitProductFilter) {}

    private FilterResult applyAttributeFilter(GraphqlRequestContext requestContext,
                                              CatalogSnapshot snapshot,
                                              List<CatalogProduct> base,
                                              AttributeEntry attr,
                                              Object rawValue,
                                              Map<String, Object> condition) throws IOException, InterruptedException {
        String code = attr.code();
        switch (attr.type()) {
            case SELECT, MULTISELECT -> {
                List<String> requestedValues = allEqOrInValues(rawValue);
                if (requestedValues.isEmpty()) {
                    return new FilterResult(base, true, false);
                }
                return new FilterResult(
                        filterByConfigurableOption(requestContext, base, code, requestedValues),
                        true,
                        true);
            }
            case INT, FLOAT, PRICE, DATE -> {
                Double from = parseDouble(condition.get("from"));
                Double to = parseDouble(condition.get("to"));
                if (from == null && to == null) {
                    // Fall through to legacy handling if neither bound was supplied
                    // (e.g. "price" still expects the existing eq/range branches).
                    return new FilterResult(base, false, false);
                }
                double lowerBound = from == null ? Double.NEGATIVE_INFINITY : from;
                double upperBound = to == null ? Double.POSITIVE_INFINITY : to;
                List<CatalogProduct> filtered = base.stream()
                        .filter(product -> {
                            // PRICE filters compare against the displayed minimum price
                            // (price_range.minimum_price), not the flat master element,
                            // so configurable products with cheaper variants are matched
                            // the same way the storefront shows them.
                            double value = attr.type() == com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType.PRICE
                                    ? priceRange(product).min()
                                    : numericElementValue(product, code);
                            return value >= lowerBound && value <= upperBound;
                        })
                        .toList();
                return new FilterResult(filtered, true, true);
            }
            case BOOLEAN -> {
                Object eq = condition.get("eq");
                if (eq == null) {
                    return new FilterResult(base, false, false);
                }
                boolean expected = Boolean.parseBoolean(eq.toString().trim());
                List<CatalogProduct> filtered = base.stream()
                        .filter(product -> {
                            Object value = product.elementMap(code).get("value");
                            return value != null && Boolean.parseBoolean(value.toString().trim()) == expected;
                        })
                        .toList();
                return new FilterResult(filtered, true, true);
            }
            case STRING, TEXT, IMAGE_URL -> {
                // sku and url_key retain the legacy exact/path-aware product lookup so
                // CIF clients can address a product by either its SKU or its catalog path.
                if (("sku".equals(code) || "url_key".equals(code)) && !allEqOrInValues(rawValue).isEmpty()) {
                    return new FilterResult(filterByExactProducts(snapshot, condition), true, true);
                }
                String match = stringValue(condition.get("match")).toLowerCase(Locale.ROOT);
                String eq = stringValue(condition.get("eq"));
                if (match.isBlank() && eq.isBlank()) {
                    return new FilterResult(base, false, false);
                }
                List<CatalogProduct> filtered = base.stream()
                        .filter(product -> {
                            Object value = product.elementMap(code).get("value");
                            if (value == null) {
                                return false;
                            }
                            String text = value.toString();
                            if (!match.isBlank() && text.toLowerCase(Locale.ROOT).contains(match)) {
                                return true;
                            }
                            return !eq.isBlank() && text.equals(eq);
                        })
                        .toList();
                return new FilterResult(filtered, true, !match.isBlank() || !eq.isBlank());
            }
            default -> {
                LOG.log(Level.FINE, "Unhandled attribute type {0} for filter {1}",
                        new Object[]{attr.type(), code});
                return new FilterResult(base, false, false);
            }
        }
    }

    private double numericElementValue(CatalogProduct product, String code) {
        Object value = product.elementMap(code).get("value");
        return numberValue(value, Double.NaN);
    }

    /**
     * Reads the value of a manifest-declared attribute from a product's CF.
     * Looks up the CF element name via the manifest (falling back to {@code code}).
     */
    private Object readAttributeValue(CatalogProduct product, String code) {
        String elementName = cfElementNameFor(code, null);
        return product.elementMap(elementName).get("value");
    }

    /**
     * Injects every manifest attribute that is selectable in the output (i.e. not a
     * reserved base-schema field) into {@code target}, shaping the stored CF value to
     * match the GraphQL scalar declared by {@link SchemaAddendumBuilder}. For variants
     * the per-variation value is preferred, falling back to the master element value.
     */
    private void populateManifestAttributes(Map<String, Object> target, CatalogProduct product, String variationId) {
        for (AttributeEntry entry : manifest.entries()) {
            String code = entry.code();
            if (reservedFields.contains(code)) {
                continue;
            }
            Object rawValue = readManifestValue(product, entry, variationId);
            target.put(code, shapeOutputValue(entry.type(), rawValue));
        }
    }

    private Object readManifestValue(CatalogProduct product, AttributeEntry entry, String variationId) {
        String elementName = entry.cfElementName();
        Object value = null;
        if (variationId != null) {
            value = product.elementVariationMap(elementName, variationId).get("value");
        }
        if (value == null) {
            value = product.elementMap(elementName).get("value");
        }
        return value;
    }

    /**
     * Coerces a raw CF value to the Java type matching the field's declared GraphQL
     * scalar: INT to {@link Integer}, FLOAT/PRICE to {@link Double}, BOOLEAN to
     * {@link Boolean}, MULTISELECT to {@code List<String>}, everything else to a plain
     * {@link String}. A {@code null} raw value (or an unparseable number) yields {@code null}.
     */
    private static Object shapeOutputValue(NormalizedType type, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (type) {
            case INT -> integerOrNull(rawValue);
            case FLOAT, PRICE -> doubleOrNull(rawValue);
            case BOOLEAN -> Boolean.parseBoolean(rawValue.toString().trim());
            case MULTISELECT -> multiSelectList(rawValue);
            case STRING, TEXT, SELECT, DATE, IMAGE_URL -> rawValue.toString();
        };
    }

    private static Integer integerOrNull(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString().trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double doubleOrNull(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> multiSelectList(Object rawValue) {
        List<String> result = new ArrayList<>();
        if (rawValue instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry != null && !entry.toString().isBlank()) {
                    result.add(entry.toString());
                }
            }
        } else if (rawValue.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(rawValue);
            for (int index = 0; index < length; index++) {
                Object entry = java.lang.reflect.Array.get(rawValue, index);
                if (entry != null && !entry.toString().isBlank()) {
                    result.add(entry.toString());
                }
            }
        } else {
            for (String part : rawValue.toString().split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    /**
     * Resolves the CF element name for a given attribute code.
     *
     * <p>Manifest entries are consulted first. If the manifest is empty (pre-Phase 9-10
     * cutover) or doesn't contain the code, the legacy {@code product_field} option override
     * is honoured, then the code itself.</p>
     */
    private String cfElementNameFor(String code, String legacyProductFieldOverride) {
        AttributeEntry entry = manifest.entryFor(code).orElse(null);
        if (entry != null) {
            return entry.cfElementName();
        }
        if (legacyProductFieldOverride != null && !legacyProductFieldOverride.isBlank()) {
            return legacyProductFieldOverride;
        }
        return code;
    }

    private List<CatalogProduct> filterByConfigurableOption(GraphqlRequestContext requestContext,
                                                             List<CatalogProduct> products,
                                                             String attributeCode,
                                                             List<String> requestedValues) throws IOException, InterruptedException {
        List<CatalogProduct> result = new ArrayList<>();
        for (CatalogProduct product : products) {
            if (matchesConfigurableOption(requestContext.configurableOptions(product), attributeCode, requestedValues)) {
                result.add(product);
            }
        }
        return result;
    }

    private static boolean matchesConfigurableOption(List<Map<String, Object>> options,
                                                     String attributeCode,
                                                     List<String> requestedValues) {
        for (Map<String, Object> option : options) {
            if (!attributeCode.equals(stringValue(option.get("attribute_code")))) {
                continue;
            }
            Object rawValues = option.get("values");
            if (!(rawValues instanceof Collection<?> collection)) {
                continue;
            }
            for (Object value : collection) {
                if (!(value instanceof Map<?, ?> map)) {
                    continue;
                }
                String valueIndex = String.valueOf(map.get("value_index"));
                Object labelObject = map.get("label");
                Object defaultLabelObject = map.get("default_label");
                String label = stringValue(labelObject == null ? defaultLabelObject : labelObject);
                for (String requested : requestedValues) {
                    if (requested == null) {
                        continue;
                    }
                    String trimmed = requested.trim();
                    if (trimmed.equals(valueIndex) || trimmed.equalsIgnoreCase(label)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<CatalogProduct> filterByExactProducts(CatalogSnapshot snapshot, Map<String, Object> condition) {
        if (condition.containsKey("eq")) {
            CatalogProduct product = snapshot.resolveProduct(stringValue(condition.get("eq")));
            return product == null ? List.of() : List.of(product);
        }
        if (condition.containsKey("in")) {
            LinkedHashSet<CatalogProduct> products = new LinkedHashSet<>();
            Object raw = condition.get("in");
            if (raw instanceof Collection<?> collection) {
                for (Object value : collection) {
                    CatalogProduct product = snapshot.resolveProduct(stringValue(value));
                    if (product != null) {
                        products.add(product);
                    }
                }
            }
            return new ArrayList<>(products);
        }
        return List.of();
    }

    private List<ScoredProduct> scoreAndSort(List<CatalogProduct> products, SortPlan sortPlan, String search) {
        String normalizedSearch = search == null ? "" : search.trim();
        List<ScoredProduct> scored = products.stream()
                .map(product -> new ScoredProduct(product, relevanceScore(product, normalizedSearch)))
                .filter(hit -> normalizedSearch.isBlank() || hit.score() > 0)
                .toList();
        Comparator<ScoredProduct> comparator = null;
        for (Map.Entry<String, String> entry : sortPlan.orderedKeys().entrySet()) {
            Comparator<ScoredProduct> next = comparatorFor(entry.getKey(), entry.getValue());
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }
        if (comparator == null) {
            comparator = Comparator.comparingInt(hit -> hit.product().position());
        }
        return scored.stream().sorted(comparator).toList();
    }

    private Comparator<ScoredProduct> comparatorFor(String key, String direction) {
        boolean desc = "DESC".equalsIgnoreCase(direction);
        Comparator<ScoredProduct> comparator;
        switch (key) {
            case "relevance" -> comparator = Comparator.comparingInt(ScoredProduct::score);
            case "name" -> comparator = Comparator.comparing(hit -> hit.product().displayName().toLowerCase(Locale.ROOT),
                    Comparator.nullsFirst(String::compareTo));
            case "price" -> comparator = Comparator.comparingDouble(hit -> priceValue(hit.product()));
            case "position" -> comparator = Comparator.comparingInt(hit -> hit.product().position());
            default -> comparator = Comparator.comparingInt(hit -> hit.product().position());
        }
        comparator = comparator.thenComparing(hit -> hit.product().fullPath());
        return desc ? comparator.reversed() : comparator;
    }

    private double priceValue(CatalogProduct product) {
        return priceRange(product).min();
    }

    private int relevanceScore(CatalogProduct product, String search) {
        if (search == null || search.isBlank()) {
            return 1;
        }
        String[] tokens = search.toLowerCase(Locale.ROOT).trim().split("\\s+");
        String name = product.displayName().toLowerCase(Locale.ROOT);
        String description = product.descriptionHtml().toLowerCase(Locale.ROOT);
        String shortDescription = product.shortDescription().toLowerCase(Locale.ROOT);
        String all = name + " " + description + " " + shortDescription;
        int score = 0;
        for (String token : tokens) {
            if (!all.contains(token)) {
                return 0;
            }
            if (name.contains(token)) {
                score += 100;
            }
            if (description.contains(token)) {
                score += 10;
            }
            if (shortDescription.contains(token)) {
                score += 5;
            }
        }
        return score;
    }

    private Map<String, Object> buildCategoryMap(GraphqlRequestContext requestContext,
                                                 CatalogSnapshot snapshot,
                                                 String path,
                                                 boolean includeProducts) throws IOException, InterruptedException {
        String normalizedPath = path == null || path.isBlank() ? "/" : path;
        CatalogCategory category = snapshot.categoriesByPath().get(normalizedPath);
        if (category == null && !"/".equals(normalizedPath)) {
            return Map.of();
        }
        String name = "/".equals(normalizedPath) ? "Default Category" : category.displayName();
        String urlPath = normalizedPath;
        String urlKey = "/".equals(normalizedPath) ? "/" : PathSupport.leaf(normalizedPath);
        List<CatalogProduct> subtreeProducts = snapshot.productsInCategorySubtree(normalizedPath);
        // Building a full product map per product is expensive; only do it when the
        // query actually selects the category's products. product_count still uses
        // the (cheap) subtree size regardless.
        List<Map<String, Object>> products = includeProducts
                ? subtreeProducts.stream()
                        .map(product -> buildProductMap(requestContext, snapshot, product, false))
                        .toList()
                : List.of();
        List<Map<String, Object>> children = directChildren(snapshot, normalizedPath).stream()
                .map(childPath -> {
                    try {
                        return buildCategoryMap(requestContext, snapshot, childPath, includeProducts);
                    } catch (IOException | InterruptedException e) {
                        return Map.<String, Object>of();
                    }
                })
                .filter(child -> !child.isEmpty())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", snapshot.stableIdRegistry().toStableNumericId(normalizedPath));
        result.put("uid", normalizedPath);
        result.put("name", name);
        result.put("url_path", urlPath);
        result.put("url_key", urlKey);
        result.put("position", 0);
        result.put("include_in_menu", 1);
        result.put("staged", false);
        result.put("image", categoryImageUrl(requestContext, category));
        result.put("updated_at", null);
        result.put("children_count", directChildren(snapshot, normalizedPath).size());
        result.put("product_count", subtreeProducts.size());
        result.put("breadcrumbs", buildCategoryBreadcrumbs(snapshot, normalizedPath));
        result.put("children", children);
        result.put("products", Map.of(
                "total_count", subtreeProducts.size(),
                "page_info", Map.of("total_pages", subtreeProducts.isEmpty() ? 0 : 1),
                "items", products
        ));
        result.put("__typename", "CategoryTree");
        result.put("__resolveType", "CategoryTree");
        return result;
    }

    private String categoryImageUrl(GraphqlRequestContext requestContext, CatalogCategory category) {
        if (category == null) {
            return "";
        }
        String imagePath = category.imagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        return requestContext.catalogGateway().toAssetUrl(imagePath);
    }

    private List<Map<String, Object>> buildCategoryBreadcrumbs(CatalogSnapshot snapshot, String path) {
        List<String> segments = PathSupport.segments(path);
        List<Map<String, Object>> breadcrumbs = new ArrayList<>();
        String current = "";
        for (int i = 0; i < segments.size() - 1; i++) {
            current = PathSupport.joinPath(current, segments.get(i));
            CatalogCategory category = snapshot.categoriesByPath().get(current);
            String name = category == null ? segments.get(i) : category.displayName();
            breadcrumbs.add(Map.of(
                    "category_uid", current,
                    "category_name", name,
                    "category_url_path", current,
                    "category_url_key", segments.get(i),
                    "__typename", "Breadcrumb"
            ));
        }
        return breadcrumbs;
    }

    private Map<String, Object> buildProductMap(GraphqlRequestContext requestContext,
                                                CatalogSnapshot snapshot,
                                                CatalogProduct product,
                                                boolean variantProduct) {
        try {
            List<Map<String, Object>> configurableOptions = requestContext.configurableOptions(product);
            List<Map<String, Object>> variants = buildVariants(requestContext, snapshot, product, configurableOptions);
            String type = variants.isEmpty() ? "SimpleProduct" : "ConfigurableProduct";
            String firstImagePath = ImageValueExtractor.extractFirstImagePath(product.imageValue());
            List<String> allImagePaths = ImageValueExtractor.extractAllImagePaths(product.imageValue());
            PriceRange range = priceRange(product);
            String productSku = product.sku();
            if (productSku.isBlank()) {
                productSku = product.fullPath();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", snapshot.stableIdRegistry().toStableNumericId(product.fullPath()));
            result.put("uid", product.fullPath());
            result.put("sku", productSku);
            result.put("name", product.displayName());
            result.put("description", Map.of("html", product.descriptionHtml()));
            result.put("short_description", complexText(product.shortDescription()));
            result.put("image", imageObject(requestContext.catalogGateway(), firstImagePath, ""));
            result.put("small_image", imageObject(requestContext.catalogGateway(), firstImagePath, ""));
            result.put("thumbnail", imageObject(requestContext.catalogGateway(), firstImagePath, ""));
            result.put("media_gallery", mediaGallery(requestContext.catalogGateway(), allImagePaths));
            // Null url_path (mirroring Magento) so CIF scores url_rewrites against the browsing
            // context instead of short-circuiting to a single canonical path. This preserves the
            // entry category in PDP URLs/breadcrumbs for multi-category products.
            result.put("url_path", null);
            result.put("url_key", product.nodeName());
            result.put("url_rewrites", buildProductUrlRewrites(product));
            result.put("price_range", range.toMap());
            result.put("categories", buildProductCategories(requestContext, snapshot, product.categoryPath(), product.additionalCategories()));
            result.put("configurable_options", configurableOptions);
            result.put("variants", variants);
            result.put("position", 0);
            result.put("include_in_menu", 1);
            result.put("staged", false);
            String timestamp = Long.toString(Instant.now().getEpochSecond());
            result.put("updated_at", timestamp);
            result.put("created_at", timestamp);
            result.put("stock_status", "IN_STOCK");
            result.put("meta_description", "");
            result.put("meta_keyword", "");
            result.put("meta_title", "");
            result.put("special_price", null);
            result.put("special_to_date", null);
            result.put("related_products", List.of());
            result.put("crosssell_products", List.of());
            result.put("upsell_products", List.of());
            populateManifestAttributes(result, product, null);
            result.put("__typename", variantProduct ? "SimpleProduct" : type);
            result.put("__resolveType", variantProduct ? "SimpleProduct" : type);
            return result;
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to build product payload", e);
        }
    }

    // Project one url_rewrite per category membership (primary + each additional and their
    // ancestors), mirroring Magento so CIF can hydrate a PDP from any category context, plus
    // the bare leaf. Every entry carries the storefront product_url_suffix.
    private List<Map<String, Object>> buildProductUrlRewrites(CatalogProduct product) {
        List<Map<String, Object>> rewrites = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String nodeName = product.nodeName();
        addUrlRewrite(rewrites, seen, nodeName + PathSupport.PRODUCT_URL_SUFFIX);
        List<String> categoryPaths = new ArrayList<>();
        categoryPaths.add(product.categoryPath());
        categoryPaths.addAll(product.additionalCategories());
        for (String categoryPath : PathSupport.categoryChainPaths(categoryPaths)) {
            addUrlRewrite(rewrites, seen,
                    PathSupport.joinPath(categoryPath, nodeName) + PathSupport.PRODUCT_URL_SUFFIX);
        }
        return rewrites;
    }

    private static void addUrlRewrite(List<Map<String, Object>> rewrites, Set<String> seen, String url) {
        if (url != null && !url.isBlank() && seen.add(url)) {
            rewrites.add(Map.of("url", url));
        }
    }

    private List<Map<String, Object>> buildProductCategories(GraphqlRequestContext requestContext,
                                                              CatalogSnapshot snapshot,
                                                              String primaryPath,
                                                              List<String> additionalPaths) {
        List<Map<String, Object>> categories = new ArrayList<>();
        Set<String> seenUids = new LinkedHashSet<>();
        appendCategoryChain(requestContext, snapshot, primaryPath, categories, seenUids);
        for (String additionalPath : additionalPaths) {
            appendCategoryChain(requestContext, snapshot, additionalPath, categories, seenUids);
        }
        return categories;
    }

    private void appendCategoryChain(GraphqlRequestContext requestContext,
                                     CatalogSnapshot snapshot,
                                     String categoryPath,
                                     List<Map<String, Object>> categories,
                                     Set<String> seenUids) {
        String current = "";
        for (String segment : PathSupport.segments(categoryPath)) {
            current = PathSupport.joinPath(current, segment);
            if (!seenUids.add(current)) {
                continue;
            }
            CatalogCategory category = snapshot.categoriesByPath().get(current);
            String name = category == null ? segment : category.displayName();
            categories.add(Map.of(
                    "uid", current,
                    "name", name,
                    "url_path", current,
                    "url_key", segment,
                    "image", categoryImageUrl(requestContext, category),
                    "__typename", "CategoryTree",
                    "__resolveType", "CategoryTree",
                    "breadcrumbs", buildCategoryBreadcrumbs(snapshot, current)
            ));
        }
    }

    private List<Map<String, Object>> buildVariants(GraphqlRequestContext requestContext,
                                                    CatalogSnapshot snapshot,
                                                    CatalogProduct product,
                                                    List<Map<String, Object>> configurableOptions) throws IOException, InterruptedException {
        List<String> variationIds = product.variationOrder();
        if (variationIds.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> variantImages = variantImageLookup(requestContext.catalogGateway(), product);
        List<Map<String, Object>> variants = new ArrayList<>();
        for (String variationId : variationIds) {
            List<Map<String, Object>> attributes = resolveVariantAttributes(product, variationId, configurableOptions);
            List<String> images = ImageValueExtractor.extractAllImagePaths(
                    product.elementVariationMap("image", variationId).get("value"));
            if (images.isEmpty()) {
                images = variantImages.getOrDefault(variationId, List.of());
            }
            if (images.isEmpty()) {
                images = ImageValueExtractor.extractAllImagePaths(product.imageValue());
            }
            String firstImage = images.isEmpty() ? null : images.getFirst();
            double price = numberValue(product.elementVariationMap("price", variationId).get("value"), priceRange(product).min());
            String colorValue = null;
            for (Map<String, Object> attribute : attributes) {
                String code = stringValue(attribute.get("code"));
                if ("color".equals(code) || "fashion_color".equals(code)) {
                    colorValue = String.valueOf(attribute.get("value_index"));
                }
            }
            Map<String, Object> variantProduct = new LinkedHashMap<>();
            String variantSku = product.variationSku(variationId);
            if (variantSku.isBlank()) {
                String variantSkuPrefix = product.categoryPath().replace("/", "+");
                if (!variantSkuPrefix.isBlank()) {
                    variantSkuPrefix += "+";
                }
                variantSku = variantSkuPrefix + product.nodeName() + "+" + variationId;
            }
            variantProduct.put("id", snapshot.stableIdRegistry().toStableNumericId(product.fullPath() + "#" + variationId));
            variantProduct.put("sku", variantSku);
            variantProduct.put("name", stringValue(product.elementVariationMap("name", variationId).getOrDefault("value", product.displayName())));
            variantProduct.put("description", Map.of("html",
                    stringValue(product.elementVariationMap("description", variationId).getOrDefault("value", product.descriptionHtml()))));
            variantProduct.put("short_description", complexText(
                    stringValue(product.elementVariationMap("short_description", variationId).getOrDefault("value", product.shortDescription()))));
            variantProduct.put("image", imageObject(requestContext.catalogGateway(), firstImage, ""));
            variantProduct.put("small_image", imageObject(requestContext.catalogGateway(), firstImage, ""));
            variantProduct.put("thumbnail", imageObject(requestContext.catalogGateway(), firstImage, ""));
            variantProduct.put("media_gallery", mediaGallery(requestContext.catalogGateway(), images));
            variantProduct.put("url_key", product.nodeName());
            variantProduct.put("url_path", null);
            variantProduct.put("url_rewrites", buildProductUrlRewrites(product));
            variantProduct.put("stock_status", "IN_STOCK");
            variantProduct.put("color", colorValue == null ? null : Integer.parseInt(colorValue));
            variantProduct.put("price_range", PriceRange.single(price).toMap());
            variantProduct.put("categories", buildProductCategories(requestContext, snapshot, product.categoryPath(), product.additionalCategories()));
            variantProduct.put("staged", false);
            populateManifestAttributes(variantProduct, product, variationId);
            variantProduct.put("__typename", "SimpleProduct");
            variantProduct.put("__resolveType", "SimpleProduct");
            variants.add(Map.of(
                    "attributes", attributes,
                    "product", variantProduct
            ));
        }
        return variants;
    }

    private Map<String, List<String>> variantImageLookup(CatalogGateway gateway, CatalogProduct product) throws IOException, InterruptedException {
        Map<String, Object> listing = gateway.getListing(product.categoryPath());
        Map<String, List<String>> byVariant = new LinkedHashMap<>();
        for (Map<String, Object> entity : JsonSupport.listOfMaps(listing.get("entities"))) {
            if (!JsonSupport.containsClass(entity, "assets/asset")) {
                continue;
            }
            if (JsonSupport.isProductEntity(entity)) {
                continue;
            }
            String name = stringValue(JsonSupport.map(entity.get("properties")).get("name"));
            String productPrefix = product.nodeName() + "_";
            int imgIndex = name.indexOf("_img_");
            if (!name.startsWith(productPrefix) || imgIndex <= productPrefix.length()) {
                continue;
            }
            String variantTitle = name.substring(productPrefix.length(), imgIndex);
            if (variantTitle.isBlank()) {
                continue;
            }
            String assetPath = stringValue(JsonSupport.map(entity.get("properties")).get("path"));
            if (assetPath.isBlank()) {
                continue;
            }
            byVariant.computeIfAbsent(variantTitle, key -> new ArrayList<>()).add(assetPath);
        }
        return byVariant;
    }

    private List<Map<String, Object>> resolveVariantAttributes(CatalogProduct product,
                                                                String variationId,
                                                                List<Map<String, Object>> configurableOptions) {
        if (configurableOptions.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> attributes = new ArrayList<>();
        for (Map<String, Object> option : configurableOptions) {
            String code = stringValue(option.get("attribute_code"));
            if (code.isBlank()) {
                continue;
            }
            String elementName = cfElementNameFor(code, stringValue(option.get("product_field")));
            Object rawValue = product.elementVariationMap(elementName, variationId).get("value");
            Map<String, Object> match = ConfigurableOptionsParser.findValue(option, rawValue);
            int valueIndex = match.isEmpty() ? 0 : ConfigurableOptionsParser.integerValue(match.get("value_index"));
            String label = match.isEmpty()
                    ? (rawValue == null ? "" : rawValue.toString())
                    : stringValue(match.getOrDefault("label", match.get("default_label")));
            String uid = match.isEmpty()
                    ? "celadon-" + code + "-" + valueIndex
                    : stringValue(match.getOrDefault("uid", "celadon-" + code + "-" + valueIndex));
            Map<String, Object> attribute = new LinkedHashMap<>();
            attribute.put("code", code);
            attribute.put("value_index", valueIndex);
            attribute.put("uid", uid);
            attribute.put("label", label);
            attributes.add(attribute);
        }
        return attributes;
    }

    private PriceRange priceRange(CatalogProduct product) {
        Map<String, Object> priceElement = product.elementMap("price");
        double base = numberValue(priceElement.get("value"), 10.0d);
        double min = base;
        double max = base;
        Object variations = priceElement.get("variations");
        if (variations instanceof Map<?, ?> map) {
            for (Object entry : map.values()) {
                double value = numberValue(JsonSupport.map(entry).get("value"), base);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        return new PriceRange(min, max);
    }

    private Map<String, Object> buildSortFields(boolean nonBlankSearch) {
        return Map.of(
                "default", nonBlankSearch ? "relevance" : "position",
                "options", List.of(
                        Map.of("label", "Relevance", "value", "relevance"),
                        Map.of("label", "Name", "value", "name"),
                        Map.of("label", "Position", "value", "position"),
                        Map.of("label", "Price", "value", "price")
                )
        );
    }

    private static int normalizePositiveArg(Object value, int defaultValue, String name) {
        if (value == null) {
            return defaultValue;
        }
        int normalized = value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
        if (normalized <= 0) {
            throw GraphqlErrorException.newErrorException()
                    .message(name + " must be > 0")
                    .build();
        }
        return normalized;
    }

    private static String firstEqOrInValue(Object raw) {
        Map<String, Object> condition = JsonSupport.map(raw);
        if (condition.containsKey("eq")) {
            return stringValue(condition.get("eq"));
        }
        List<String> values = allEqOrInValues(raw);
        return values.isEmpty() ? "" : values.getFirst();
    }

    private static List<String> allEqOrInValues(Object raw) {
        Map<String, Object> condition = JsonSupport.map(raw);
        if (condition.containsKey("eq")) {
            return List.of(stringValue(condition.get("eq")));
        }
        Object in = condition.get("in");
        List<String> values = new ArrayList<>();
        if (in instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (value == null) {
                    continue;
                }
                for (String part : value.toString().split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        values.add(trimmed);
                    }
                }
            }
        } else if (in != null) {
            for (String part : in.toString().split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        return values;
    }

    private static Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
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

    private static Map<String, Object> imageObject(CatalogGateway gateway, String path, String label) {
        return Map.of(
                "label", label,
                "url", path == null ? "" : gateway.toAssetUrl(path)
        );
    }

    private static List<Map<String, Object>> mediaGallery(CatalogGateway gateway, List<String> paths) {
        List<Map<String, Object>> items = new ArrayList<>();
        int position = 1;
        for (String path : paths) {
            items.add(Map.of(
                    "label", "Main",
                    "position", position++,
                    "disabled", false,
                    "url", gateway.toAssetUrl(path),
                    "__typename", "ProductImage",
                    "__resolveType", "ProductImage"
            ));
        }
        return items;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static Map<String, Object> complexText(String html) {
        return html == null || html.isBlank() ? null : Map.of("html", html);
    }

    private static GraphqlRequestContext requestContext(DataFetchingEnvironment environment) {
        return environment.getGraphQlContext().get(REQUEST_CONTEXT_KEY);
    }

    public static String requestContextKey() {
        return REQUEST_CONTEXT_KEY;
    }

    private record PriceRange(double min, double max) {
        private static PriceRange single(double value) {
            return new PriceRange(value, value);
        }

        private Map<String, Object> toMap() {
            return Map.of(
                    "minimum_price", priceMap(min),
                    "maximum_price", priceMap(max)
            );
        }

        private Map<String, Object> priceMap(double value) {
            return Map.of(
                    "regular_price", Map.of("value", value, "currency", "USD"),
                    "final_price", Map.of("value", value, "currency", "USD"),
                    "discount", Map.of("amount_off", 0, "percent_off", 0)
            );
        }
    }

    private record SortPlan(LinkedHashMap<String, String> orderedKeys) {
    }

    private SortPlan buildSortPlan(Object rawSort, boolean nonBlankSearch, Map<String, Object> filter) {
        LinkedHashMap<String, String> orderedKeys = new LinkedHashMap<>();
        Map<String, Object> sort = JsonSupport.map(rawSort);
        if (!sort.isEmpty()) {
            for (Map.Entry<String, Object> entry : sort.entrySet()) {
                orderedKeys.put(entry.getKey(), stringValue(entry.getValue()));
            }
        } else if (nonBlankSearch) {
            orderedKeys.put("relevance", "DESC");
        } else if (!filter.isEmpty()) {
            orderedKeys.put("position", "ASC");
        }
        return new SortPlan(orderedKeys);
    }

    private record ScoredProduct(CatalogProduct product, int score) {
    }
}

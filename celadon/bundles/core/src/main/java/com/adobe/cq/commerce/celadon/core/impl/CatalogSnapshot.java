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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogSnapshot {
    private final Map<String, CatalogCategory> categoriesByPath;
    private final List<CatalogProduct> products;
    private final StableIdRegistry stableIdRegistry;
    private final Map<String, CatalogProduct> productsBySku;
    private final Map<String, Set<CatalogProduct>> productsByCategoryPath;

    private CatalogSnapshot(Map<String, CatalogCategory> categoriesByPath,
                            List<CatalogProduct> products,
                            StableIdRegistry stableIdRegistry,
                            Map<String, CatalogProduct> productsBySku,
                            Map<String, Set<CatalogProduct>> productsByCategoryPath) {
        this.categoriesByPath = categoriesByPath;
        this.products = products;
        this.stableIdRegistry = stableIdRegistry;
        this.productsBySku = productsBySku;
        this.productsByCategoryPath = productsByCategoryPath;
    }

    static CatalogSnapshot load(CatalogGateway gateway) throws IOException, InterruptedException {
        Map<String, CatalogCategory> categoriesByPath = new LinkedHashMap<>();
        List<CatalogProduct> products = new ArrayList<>();
        CatalogCategory root = resolveCategory(gateway, "/", "", "Default Category");
        categoriesByPath.put("/", root);
        traverse(gateway, "", categoriesByPath, products);
        StableIdRegistry ids = new StableIdRegistry();
        ids.toStableNumericId("/");
        categoriesByPath.keySet().stream()
                .filter(path -> !"/".equals(path))
                .sorted()
                .forEach(ids::toStableNumericId);
        products.stream()
                .map(CatalogProduct::fullPath)
                .sorted()
                .forEach(ids::toStableNumericId);
        products.stream()
                .sorted(Comparator.comparing(CatalogProduct::fullPath))
                .forEach(product -> product.variationOrder().stream()
                        .sorted()
                        .forEach(variationId -> ids.toStableNumericId(product.fullPath() + "#" + variationId)));
        Map<String, CatalogProduct> productsBySku = new LinkedHashMap<>();
        products.stream()
                .sorted(Comparator.comparing(CatalogProduct::fullPath))
                .forEach(product -> {
                    String sku = product.sku();
                    if (!sku.isBlank()) {
                        productsBySku.putIfAbsent(sku, product);
                    }
                });
        List<CatalogProduct> frozenProducts = List.copyOf(products);
        Map<String, Set<CatalogProduct>> productsByCategoryPath = buildCategoryIndex(frozenProducts);
        return new CatalogSnapshot(categoriesByPath, frozenProducts, ids, productsBySku, productsByCategoryPath);
    }

    /**
     * Reverse index: catalog-relative category path -> ordered set of member products.
     * A product is a member of its primary category, every additionalCategories path,
     * AND every ancestor of those (ancestors are expanded here at read time, never
     * stored). Products are visited in traversal order and held in LinkedHashSet so a
     * subtree lookup returns members in a deterministic, traversal-stable order. The
     * root "/" is intentionally NOT a key — productsInCategorySubtree handles it as
     * "all products" directly.
     */
    private static Map<String, Set<CatalogProduct>> buildCategoryIndex(List<CatalogProduct> products) {
        Map<String, Set<CatalogProduct>> index = new LinkedHashMap<>();
        for (CatalogProduct product : products) {
            List<String> membershipPaths = new ArrayList<>();
            membershipPaths.add(product.categoryPath());
            membershipPaths.addAll(product.additionalCategories());
            for (String membershipPath : membershipPaths) {
                String current = "";
                for (String segment : PathSupport.segments(membershipPath)) {
                    current = PathSupport.joinPath(current, segment);
                    index.computeIfAbsent(current, key -> new LinkedHashSet<>()).add(product);
                }
            }
        }
        return index;
    }

    Map<String, CatalogCategory> categoriesByPath() {
        return categoriesByPath;
    }

    List<CatalogProduct> products() {
        return products;
    }

    StableIdRegistry stableIdRegistry() {
        return stableIdRegistry;
    }

    // Magento's conventional root category id. CIF cloud configs ship
    // magentoRootCategoryId="Mg==" (base64 of "2") and the navigation component
    // queries the category tree starting from this uid.
    private static final String MAGENTO_ROOT_CATEGORY_ID = "2";

    String resolveCategoryPathFromIdOrPath(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = value.trim();
        // Magento-style category uids are base64-encoded numeric ids (e.g. "Mg=="
        // = "2"). CIF clients address categories by these uids, so decode them to
        // the underlying numeric id before resolving.
        String decoded = decodeBase64NumericId(normalized);
        if (decoded != null) {
            normalized = decoded;
        }
        if (normalized.matches("\\d+")) {
            // The Magento root category (id 2) maps to our catalog root path.
            if (MAGENTO_ROOT_CATEGORY_ID.equals(normalized)) {
                return "/";
            }
            int id = Integer.parseInt(normalized);
            if (id == stableIdRegistry.toStableNumericId("/")) {
                return "/";
            }
            String mapped = stableIdRegistry.keyForId(id);
            return mapped == null ? normalized : mapped;
        }
        if ("/".equals(normalized)) {
            return "/";
        }
        return PathSupport.normalizeRelativePath(normalized);
    }

    /**
     * If {@code value} is a base64-encoded run of ASCII digits (the Magento
     * category uid scheme, e.g. "Mg==" → "2", "NjU1" → "655"), returns the
     * decoded digit string; otherwise returns null. Guards against treating
     * path-style uids like "mens-coats" as base64.
     */
    private static String decodeBase64NumericId(String value) {
        if (!value.matches("[A-Za-z0-9+/]+={0,2}") || value.length() % 4 != 0) {
            return null;
        }
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(value),
                    java.nio.charset.StandardCharsets.UTF_8);
            return decoded.matches("\\d+") ? decoded : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    CatalogProduct resolveProduct(String filterValue) {
        if (filterValue == null || filterValue.isBlank()) {
            return null;
        }
        CatalogProduct exactBySku = productsBySku.get(filterValue.trim());
        if (exactBySku != null) {
            return exactBySku;
        }
        String address = PathSupport.stripProductUrlSuffix(PathSupport.normalizeFilterPathValue(filterValue));
        if (address.isBlank()) {
            return null;
        }
        if (address.contains("/")) {
            // Path-style address (a url_rewrite, e.g. women/pants/foo.html): resolve only against a
            // real category membership path. Mirrors Magento, which 404s a product addressed under a
            // category it does not belong to rather than falling back to a bare leaf match.
            return products.stream()
                    .filter(product -> matchesMembershipPath(product, address))
                    .findFirst()
                    .orElse(null);
        }
        // Bare leaf (a url_key, e.g. candace-dress): match the product node name directly.
        return products.stream()
                .filter(product -> product.nodeName().equals(address))
                .findFirst()
                .orElse(null);
    }

    // True when candidate equals the product's primary fullPath or any of its category
    // membership paths (primary + additional, each ancestor and self) joined with the leaf.
    // Mirrors the url_rewrites projection so additional-category PDP URLs resolve.
    private static boolean matchesMembershipPath(CatalogProduct product, String candidate) {
        if (product.fullPath().equals(candidate)) {
            return true;
        }
        List<String> categoryPaths = new ArrayList<>();
        categoryPaths.add(product.categoryPath());
        categoryPaths.addAll(product.additionalCategories());
        for (String categoryPath : PathSupport.categoryChainPaths(categoryPaths)) {
            if (PathSupport.joinPath(categoryPath, product.nodeName()).equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    List<CatalogProduct> productsInCategorySubtree(String categoryPath) {
        String normalized = resolveCategoryPathFromIdOrPath(categoryPath);
        if (normalized == null || normalized.isBlank() || "/".equals(normalized)) {
            return products;
        }
        Set<CatalogProduct> members = productsByCategoryPath.get(normalized);
        return members == null ? List.of() : List.copyOf(members);
    }

    private static void traverse(CatalogGateway gateway,
                                 String currentPath,
                                 Map<String, CatalogCategory> categoriesByPath,
                                 List<CatalogProduct> products) throws IOException, InterruptedException {
        Map<String, Object> listing = gateway.getListing(currentPath);
        List<Map<String, Object>> entities = JsonSupport.listOfMaps(listing.get("entities"));
        // Preserve the gateway's listing order (JCR sibling order, including
        // sling:OrderedFolder authored order) for both child categories and
        // products. Catalog navigation follows the authored category order and
        // unsorted product results follow the authored product order rather
        // than being re-sorted alphabetically.
        entities.stream()
                .filter(entity -> JsonSupport.containsClass(entity, "assets/folder"))
                .filter(entity -> !name(entity).startsWith("_"))
                .forEach(entity -> {
                    String folderName = PathSupport.normalizeRelativePath(name(entity));
                    String childPath = PathSupport.joinPath(currentPath, folderName);
                    categoriesByPath.put(childPath, resolveCategory(gateway, childPath, childPath, folderName));
                    categoriesByPath.get(currentPath.isBlank() ? "/" : currentPath).childPaths().add(childPath);
                    try {
                        traverse(gateway, childPath, categoriesByPath, products);
                    } catch (IOException | InterruptedException e) {
                        throw new SnapshotTraversalException(e);
                    }
                });

        entities.stream()
                .filter(JsonSupport::isProductEntity)
                .forEach(entity -> {
                    Map<String, Object> props = JsonSupport.map(JsonSupport.map(entity).get("properties"));
                    String nodeName = PathSupport.normalizeRelativePath(name(entity));
                    products.add(new CatalogProduct(currentPath, nodeName, props, products.size()));
                });
    }

    private static CatalogCategory resolveCategory(CatalogGateway gateway,
                                                   String categoryPath,
                                                   String lookupPath,
                                                   String fallbackName) {
        String title = fallbackName;
        String imagePath = "";
        try {
            Map<String, Object> json = gateway.getFolderJcrContent(lookupPath);
            Object rawTitle = json.get("jcr:title");
            if (rawTitle != null && !rawTitle.toString().isBlank()) {
                title = rawTitle.toString();
            }
            Object rawImage = json.get("folderThumbnailPath");
            if (rawImage != null && !rawImage.toString().isBlank()) {
                imagePath = rawImage.toString();
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return new CatalogCategory(categoryPath, title, imagePath);
    }

    private static String name(Map<String, Object> entity) {
        return String.valueOf(JsonSupport.map(entity.get("properties")).getOrDefault("name", ""));
    }

    private static final class SnapshotTraversalException extends RuntimeException {
        private SnapshotTraversalException(Throwable cause) {
            super(cause);
        }
    }
}

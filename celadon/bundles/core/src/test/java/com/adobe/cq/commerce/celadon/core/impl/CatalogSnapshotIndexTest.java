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

import static org.junit.Assert.assertEquals;

import com.adobe.cq.commerce.celadon.core.api.CatalogGateway;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class CatalogSnapshotIndexTest {

    // --- minimal in-package fake gateway -------------------------------------

    /**
     * Folders: man/ (-> pants/ -> summer/), sale/ (-> clearance/), outlet/.
     * Products:
     *   man/pants/summer/p-shorts  additionalCategories = [sale/clearance]
     *   sale/p-sale-only           (no additionalCategories)
     */
    private static final class FakeGateway implements CatalogGateway {
        @Override
        public Map<String, Object> getListing(String relativePath) {
            String path = normalize(relativePath);
            return switch (path) {
                case "" -> folderListing("root", folder("man"), folder("sale"), folder("outlet"));
                case "man" -> folderListing("man", folder("pants"));
                case "man/pants" -> folderListing("pants", folder("summer"));
                case "man/pants/summer" -> folderListing("summer",
                        product("p-shorts", List.of("sale/clearance")));
                case "sale" -> folderListing("sale", folder("clearance"),
                        product("p-sale-only", List.of()));
                case "sale/clearance" -> folderListing("clearance");
                case "outlet" -> folderListing("outlet");
                default -> Map.of("entities", List.of());
            };
        }

        @Override
        public Map<String, Object> getFolderJcrContent(String categoryPath) {
            return Map.of();
        }

        @Override
        public Map<String, Object> getProductJcrContent(String categoryPath, String productName) {
            return Map.of();
        }

        @Override
        public String toAssetUrl(String imagePath) {
            return imagePath;
        }

        private static String normalize(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            String n = value.trim();
            while (n.startsWith("/")) {
                n = n.substring(1);
            }
            while (n.endsWith("/")) {
                n = n.substring(0, n.length() - 1);
            }
            return n;
        }
    }

    private static Map<String, Object> folderListing(String name, Map<String, Object>... entities) {
        return map("properties", map("name", name), "entities", new ArrayList<>(List.of(entities)));
    }

    private static Map<String, Object> folder(String name) {
        return map("class", List.of("assets/folder"), "properties", map("name", name));
    }

    private static Map<String, Object> product(String name, List<String> additionalCategories) {
        Map<String, Object> elements = new LinkedHashMap<>();
        elements.put("name", map("value", name));
        elements.put("sku", map("value", name + "-sku"));
        if (!additionalCategories.isEmpty()) {
            elements.put("additionalCategories", map("value", additionalCategories));
        }
        return map(
                "class", List.of("assets/asset"),
                "properties", map("name", name, "contentFragment", true, "elements", elements));
    }

    private static Map<String, Object> map(Object... values) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            m.put(values[i].toString(), values[i + 1]);
        }
        return m;
    }

    private static List<String> skus(List<CatalogProduct> products) {
        return products.stream().map(CatalogProduct::sku).toList();
    }

    // --- the assertions ------------------------------------------------------

    @Test
    public void primaryAncestorsAreIndexed() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new FakeGateway());
        // p-shorts lives at man/pants/summer -> indexed under summer + every ancestor.
        assertEquals(List.of("p-shorts-sku"), skus(snapshot.productsInCategorySubtree("man/pants/summer")));
        assertEquals(List.of("p-shorts-sku"), skus(snapshot.productsInCategorySubtree("man/pants")));
        assertEquals(List.of("p-shorts-sku"), skus(snapshot.productsInCategorySubtree("man")));
    }

    @Test
    public void additionalCategoryAncestorsAreIndexed() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new FakeGateway());
        // p-shorts has additionalCategories=[sale/clearance] -> appears under
        // sale/clearance AND sale (ancestor). p-sale-only lives under sale.
        assertEquals(List.of("p-shorts-sku"), skus(snapshot.productsInCategorySubtree("sale/clearance")));
        // Traversal order: man/... is visited before sale/... so p-shorts precedes p-sale-only.
        assertEquals(List.of("p-shorts-sku", "p-sale-only-sku"), skus(snapshot.productsInCategorySubtree("sale")));
    }

    @Test
    public void emptyCategoryHasNoProducts() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new FakeGateway());
        assertEquals(List.of(), skus(snapshot.productsInCategorySubtree("outlet")));
    }

    @Test
    public void rootReturnsAllProductsDedupedInTraversalOrder() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new FakeGateway());
        // p-shorts is reachable via both man/... and sale/... but must appear ONCE under /.
        assertEquals(List.of("p-shorts-sku", "p-sale-only-sku"), skus(snapshot.productsInCategorySubtree("/")));
        assertEquals(2, snapshot.productsInCategorySubtree("/").size());
    }

    @Test
    public void unknownCategoryReturnsEmpty() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new FakeGateway());
        assertEquals(List.of(), skus(snapshot.productsInCategorySubtree("does/not/exist")));
    }

    @Test
    public void numericIdStillResolvesThroughFrontDoorIntoIndex() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new FakeGateway());
        // productsInCategorySubtree must still accept a stable numeric id (not just a
        // path): resolveCategoryPathFromIdOrPath maps id -> "man/pants/summer", then the
        // index lookup runs. Resolve the id from the snapshot rather than hardcoding it.
        int summerId = snapshot.stableIdRegistry().toStableNumericId("man/pants/summer");
        assertEquals(List.of("p-shorts-sku"), skus(snapshot.productsInCategorySubtree(Integer.toString(summerId))));
    }

    @Test
    public void rootCategoryChildrenFollowListingOrderNotAlphabetical() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new FakeGateway());
        // The gateway lists root folders as man, sale, outlet (the authored JCR sibling
        // order). The snapshot must preserve that order rather than re-sorting it
        // alphabetically (which would yield man, outlet, sale).
        assertEquals(List.of("man", "sale", "outlet"),
                snapshot.categoriesByPath().get("/").childPaths());
    }

    /** A single folder holding three products listed in a deliberately non-alphabetical order. */
    private static final class OrderedProductsGateway implements CatalogGateway {
        @Override
        public Map<String, Object> getListing(String relativePath) {
            String path = FakeGateway.normalize(relativePath);
            return switch (path) {
                case "" -> folderListing("root", folder("shirts"));
                case "shirts" -> folderListing("shirts",
                        product("gamma", List.of()),
                        product("alpha", List.of()),
                        product("beta", List.of()));
                default -> Map.of("entities", List.of());
            };
        }

        @Override
        public Map<String, Object> getFolderJcrContent(String categoryPath) {
            return Map.of();
        }

        @Override
        public Map<String, Object> getProductJcrContent(String categoryPath, String productName) {
            return Map.of();
        }

        @Override
        public String toAssetUrl(String imagePath) {
            return imagePath;
        }
    }

    @Test
    public void unsortedProductsFollowListingOrderNotAlphabetical() throws Exception {
        CatalogSnapshot snapshot = CatalogSnapshot.load(new OrderedProductsGateway());
        // Products are listed gamma, alpha, beta (authored order). The snapshot must
        // preserve that order rather than re-sorting alphabetically (alpha, beta, gamma).
        assertEquals(List.of("gamma-sku", "alpha-sku", "beta-sku"),
                skus(snapshot.productsInCategorySubtree("shirts")));
    }
}

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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class CeladonCatalogServletCategoriesTest {

    private final CeladonCatalogServlet servlet = new CeladonCatalogServlet();

    private static Map<String, Object> category(String urlPath) {
        return Map.of("url_path", urlPath);
    }

    private static Map<String, Object> product(Map<String, Object>... categories) {
        return Map.of("categories", List.of(categories));
    }

    @Test
    public void primaryIsDeepestAssignedPath() {
        Map<String, Object> product = product(
                category("sale"), category("man/pants/summer"));
        assertEquals("man/pants/summer", servlet.canonicalCategoryPath(product));
    }

    @Test
    public void assignedPathsCollectsEveryBranch() {
        Map<String, Object> product = product(
                category("man/pants/summer"), category("sale"), category("sale/clearance"));
        assertEquals(Set.of("man/pants/summer", "sale", "sale/clearance"),
                servlet.assignedCategoryPaths(product));
    }

    @Test
    public void additionalCategoriesAreSeparateBranchesMinusPrimaryAncestors() {
        // primary = man/pants/summer; man + man/pants are ancestors of primary (dropped);
        // sale + sale/clearance collapse to the deepest sale/clearance.
        Map<String, Object> product = product(
                category("man"), category("man/pants"), category("man/pants/summer"),
                category("sale"), category("sale/clearance"));
        String primary = servlet.canonicalCategoryPath(product);
        List<String> additional = CategoryMembership.deriveAdditionalCategories(
                servlet.assignedCategoryPaths(product), primary);
        assertEquals("man/pants/summer", primary);
        assertEquals(List.of("sale/clearance"), additional);
    }

    @Test
    public void perEntryPathReconstructedFromBreadcrumbsWhenUrlPathBlank() {
        Map<String, Object> category = Map.of(
                "breadcrumbs", List.of(Map.of("category_url_path", "man")),
                "url_key", "pants");
        assertEquals("man/pants", servlet.categoryPathFor(category));
    }
}

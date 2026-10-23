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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Derives the minimal {@code additionalCategories} set a product stores, given the
 * full set of category paths it is assigned and the chosen primary (home) path.
 *
 * <p>Stored membership is "minimal truth": ancestors of the primary are dropped
 * (they are implied by the primary's location), and the remaining paths are reduced
 * to a minimal antichain — any path that is an ancestor of another kept path is
 * dropped, keeping the deepest path per branch. Ancestors are re-expanded at read
 * time by the storefront, never stored.
 *
 * <p>Shared by the Magento importer ({@code CeladonCatalogServlet}) and the authoring
 * service's {@code set_primary_category} so both apply identical rules. Inputs are
 * catalog-relative, already-normalized, slash-delimited paths (the form the importer's
 * {@code normalizeCategoryPath} produces).
 */
public final class CategoryMembership {

    private CategoryMembership() {
    }

    /**
     * @param assignedPaths every catalog-relative path the product belongs to
     *                      (including the primary); may contain blanks/nulls.
     * @param primaryPath   the chosen primary (home) path.
     * @return the additional category paths to store: {@code assignedPaths} with the
     *         primary's ancestor-or-self chain removed, reduced to a minimal antichain.
     *         Sorted for deterministic output. Never {@code null}.
     */
    public static List<String> deriveAdditionalCategories(Collection<String> assignedPaths, String primaryPath) {
        String primary = normalize(primaryPath);
        List<String> candidates = new ArrayList<>();
        for (String raw : assignedPaths) {
            String path = normalize(raw);
            if (path.isBlank()) {
                continue;
            }
            // Drop the primary and every ancestor of the primary (implied by location).
            if (isAncestorOrSelf(path, primary)) {
                continue;
            }
            if (!candidates.contains(path)) {
                candidates.add(path);
            }
        }
        // Minimal antichain: drop any candidate that is an ancestor of another candidate.
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            boolean hasDescendant = false;
            for (String other : candidates) {
                if (!candidate.equals(other) && isAncestorOrSelf(candidate, other)) {
                    hasDescendant = true;
                    break;
                }
            }
            if (!hasDescendant) {
                result.add(candidate);
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    /**
     * @return {@code true} if {@code maybeAncestor} equals {@code path} or is a
     *         proper ancestor of it (segment-boundary aware: {@code man/pant} is NOT
     *         an ancestor of {@code man/pants/summer}).
     */
    public static boolean isAncestorOrSelf(String maybeAncestor, String path) {
        String ancestor = normalize(maybeAncestor);
        String descendant = normalize(path);
        if (ancestor.isBlank()) {
            // The catalog root is an ancestor of everything (never stored anyway).
            return true;
        }
        if (ancestor.equals(descendant)) {
            return true;
        }
        return descendant.startsWith(ancestor + "/");
    }

    private static String normalize(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Pure (Sling-free, JCR-free) decision surface for the legacy importer's category modes.
 *
 * <p>Turns a raw product map plus a folder-derived primary into a
 * {@code (primary, additionalCategories)} pair, delegating minimal-antichain reduction to
 * {@link CategoryMembership}. All tag paths are namespace-stripped (everything up to and
 * including the first {@code ':'}) and each path segment is escaped via
 * {@link AemRepositorySupport#escapeNodeName} so tag-derived paths are byte-identical to the
 * folders the pre-pass creates.
 */
public final class LegacyTagCategories {

    private static final String CQ_TAGS = "cq:tags";

    private LegacyTagCategories() {
    }

    /** Selects how a product's category placement is derived. Default-safe to {@link #FOLDER}. */
    public enum CategoryMode {
        FOLDER, TAG, HYBRID, BLUEPRINT;

        /** Lenient parse: {@code null}/blank/unknown maps to {@link #FOLDER}. */
        public static CategoryMode fromParam(String raw) {
            if (raw == null || raw.isBlank()) {
                return FOLDER;
            }
            switch (raw.trim().toLowerCase()) {
                case "tag":
                    return TAG;
                case "hybrid":
                    return HYBRID;
                case "blueprint":
                    return BLUEPRINT;
                default:
                    return FOLDER;
            }
        }
    }

    /** Resolution result: the chosen primary (home) path and the minimal additional membership set. */
    public record CategoryResolution(String primary, List<String> additionalCategories) {
    }

    /**
     * Strips the namespace (up to and including the first {@code ':'}) and returns the remaining
     * slash path with each segment escaped. Blank / namespace-only / colon-less inputs yield {@code ""}.
     */
    public static String parseTagPath(String tagId) {
        if (tagId == null) {
            return "";
        }
        int colon = tagId.indexOf(':');
        if (colon < 0) {
            return "";
        }
        String remainder = tagId.substring(colon + 1).trim();
        if (remainder.isBlank()) {
            return "";
        }
        List<String> segments = new ArrayList<>();
        for (String segment : remainder.split("/")) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            segments.add(AemRepositorySupport.escapeNodeName(segment));
        }
        return String.join("/", segments);
    }

    /**
     * Reads the {@code cq:tags} multi-value off {@code product} (tolerating {@code List}, array, or a
     * single scalar), maps each via {@link #parseTagPath}, drops blanks and dedups in stable order.
     */
    public static List<String> tagPaths(Map<String, Object> product) {
        Object raw = product == null ? null : product.get(CQ_TAGS);
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        for (Object value : asIterable(raw)) {
            String path = parseTagPath(value == null ? null : value.toString());
            if (!path.isBlank()) {
                paths.add(path);
            }
        }
        return new ArrayList<>(paths);
    }

    /** Deepest path (most segments) wins; ties broken lexicographically. Empty input yields {@code null}. */
    public static String pickPrimary(Collection<String> tagPaths) {
        String best = null;
        int bestDepth = -1;
        for (String path : tagPaths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            int depth = depth(path);
            if (depth > bestDepth || (depth == bestDepth && path.compareTo(best) < 0)) {
                best = path;
                bestDepth = depth;
            }
        }
        return best;
    }

    /**
     * Source-favored primary selection. When {@code sourceFolderPath} (the catalog-relative folder a
     * product physically lived in on the source instance) shares trailing path segments with one or more
     * candidates, the candidate with the greatest such overlap wins — <em>overriding</em> depth, so a
     * product whose source folder maps to a depth-2 section stays there even when a deeper unrelated
     * section also matches. Overlap is name-independent at the top of the tree (it compares from the leaf
     * backward), so a source folder like {@code me/shorts} favors {@code men/shorts} over
     * {@code equipment/hiking}. Among equal-overlap candidates, and when no candidate overlaps the source
     * folder (a pure bucket, or a blank hint), this falls back to {@link #pickPrimary(Collection)}'s
     * deepest-then-lexicographic rule.
     */
    public static String pickPrimary(Collection<String> tagPaths, String sourceFolderPath) {
        if (sourceFolderPath == null || sourceFolderPath.isBlank()) {
            return pickPrimary(tagPaths);
        }
        String[] sourceSegments = sourceFolderPath.split("/");
        String best = null;
        int bestOverlap = 0;
        int bestDepth = -1;
        for (String path : tagPaths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            int overlap = trailingOverlap(sourceSegments, path.split("/"));
            int depth = depth(path);
            if (overlap > bestOverlap
                    || (overlap == bestOverlap && depth > bestDepth)
                    || (overlap == bestOverlap && depth == bestDepth && (best == null || path.compareTo(best) < 0))) {
                best = path;
                bestOverlap = overlap;
                bestDepth = depth;
            }
        }
        // No candidate shares the source folder leaf -> the source folder is a bucket; use the plain rule.
        return bestOverlap == 0 ? pickPrimary(tagPaths) : best;
    }

    /** Number of equal trailing segments (compared leaf-first, case-insensitive). */
    private static int trailingOverlap(String[] a, String[] b) {
        int overlap = 0;
        int i = a.length - 1;
        int j = b.length - 1;
        while (i >= 0 && j >= 0) {
            String sa = a[i].trim();
            String sb = b[j].trim();
            if (sa.isEmpty() || sb.isEmpty() || !sa.equalsIgnoreCase(sb)) {
                break;
            }
            overlap++;
            i--;
            j--;
        }
        return overlap;
    }

    /**
     * Mode dispatch. {@code folderPrimary} is the folder-derived (catalog-relative) path, always supplied.
     * Delegates additional-category reduction to {@link CategoryMembership#deriveAdditionalCategories}.
     */
    public static CategoryResolution resolve(CategoryMode mode, String folderPrimary, Map<String, Object> product) {
        switch (mode) {
            case TAG: {
                List<String> tags = tagPaths(product);
                if (tags.isEmpty()) {
                    return resolve(CategoryMode.FOLDER, folderPrimary, product);
                }
                String primary = pickPrimary(tags);
                return new CategoryResolution(primary,
                        CategoryMembership.deriveAdditionalCategories(tags, primary));
            }
            case HYBRID: {
                List<String> assigned = new ArrayList<>();
                assigned.add(folderPrimary);
                assigned.addAll(tagPaths(product));
                return new CategoryResolution(folderPrimary,
                        CategoryMembership.deriveAdditionalCategories(assigned, folderPrimary));
            }
            case FOLDER:
            default:
                return new CategoryResolution(folderPrimary, List.of());
        }
    }

    private static Iterable<?> asIterable(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (raw instanceof Object[] array) {
            return List.of(array);
        }
        return List.of(raw);
    }

    private static int depth(String path) {
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }
}

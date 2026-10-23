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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure (Sling-free, JCR-free) parser that turns a source AEM QueryBuilder result into a
 * {@code catalog-relative-path -> editorial title} map for the legacy importer's {@code BLUEPRINT} mode.
 *
 * <p>Classic AEM commerce authors two parallel trees: the commerce <em>blueprint</em> (which defines
 * the catalog structure and tag-based membership) and the editorial <em>page tree</em>
 * ({@code /content/<site>/.../products}, which the classic storefront navigation renders). The two
 * trees share the same node names at every level, but their {@code jcr:title} values can diverge — the
 * blueprint tends to carry the merchandising form ("Men's"/"Women's") while the editorial pages carry
 * the live navigation labels ("Men"/"Women"). The blueprint title can lag the editorial one, so the
 * importer prefers the editorial title when both are present.
 *
 * <p>The same widened QueryBuilder call that backs {@link LegacyProductSlugs} returns every page under
 * the tree with its {@code jcr:path}, {@code jcr:content/cq:productMaster} and {@code jcr:content/jcr:title}.
 * This parser keys the map by the page's <em>catalog-relative escaped path</em> (each segment passed
 * through {@link AemRepositorySupport#escapeNodeName}, so keys are byte-identical to the section paths
 * {@link LegacyBlueprintCategories} produces) and stores the editorial title. Product pages (those that
 * carry a {@code cq:productMaster}) are skipped — they are slugs, not category sections.
 */
public final class LegacyPageTitles {

    private final Map<String, String> titleByPath;

    private LegacyPageTitles(Map<String, String> titleByPath) {
        this.titleByPath = titleByPath;
    }

    /** An empty title map — used when no editorial page tree was supplied. */
    public static LegacyPageTitles empty() {
        return new LegacyPageTitles(new LinkedHashMap<>());
    }

    /** The editorial title for a catalog-relative section path, or {@code null} when none was harvested. */
    public String titleFor(String path) {
        return path == null ? null : titleByPath.get(path);
    }

    /** Number of harvested path→title mappings. */
    public int size() {
        return titleByPath.size();
    }

    /**
     * Parse a QueryBuilder {@code .json} result into a {@code catalog-relative-path -> title} map. Each
     * non-product page under {@code basePath} contributes {@code relative-escaped-path -> jcr:title}.
     * The base prefix is stripped and each remaining segment escaped via
     * {@link AemRepositorySupport#escapeNodeName}. The base page itself (empty relative path), product
     * pages and title-less hits are skipped; on duplicate paths the first hit wins.
     */
    public static LegacyPageTitles parse(Map<String, Object> queryBuilderJson, String basePath) {
        Map<String, String> map = new LinkedHashMap<>();
        String base = normalizeBase(basePath);
        if (queryBuilderJson != null && !base.isEmpty()) {
            for (Map<String, Object> hit : listOfMaps(queryBuilderJson.get("hits"))) {
                Map<String, Object> content = asMap(hit.get("jcr:content"));
                // Product pages carry a commerce identifier — they are slugs, not category sections.
                if (!stringValue(content.get("cq:productMaster")).isBlank()) {
                    continue;
                }
                String relativePath = relativeEscapedPath(stringValue(hit.get("jcr:path")), base);
                String title = firstTitle(content.get("jcr:title"));
                if (relativePath.isBlank() || title.isBlank()) {
                    continue;
                }
                map.putIfAbsent(relativePath, title);
            }
        }
        return new LegacyPageTitles(map);
    }

    /** Trailing-slash-tolerant base path; empty for blank input. */
    private static String normalizeBase(String basePath) {
        if (basePath == null) {
            return "";
        }
        String trimmed = basePath.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Catalog-relative path with each segment escaped, or empty when {@code absolutePath} is the base
     * itself or falls outside the base subtree.
     */
    private static String relativeEscapedPath(String absolutePath, String base) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return "";
        }
        String path = absolutePath.endsWith("/") ? absolutePath.substring(0, absolutePath.length() - 1) : absolutePath;
        String relative;
        if (path.equals(base)) {
            return "";
        } else if (path.startsWith(base + "/")) {
            relative = path.substring(base.length() + 1);
        } else {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String segment : relative.split("/")) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(AemRepositorySupport.escapeNodeName(segment));
        }
        return builder.toString();
    }

    /** First non-blank entry of a (possibly multi-value) title property; empty when none. */
    private static String firstTitle(Object titleValue) {
        if (titleValue instanceof List<?> list) {
            for (Object value : list) {
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            }
            return "";
        }
        return titleValue == null ? "" : titleValue.toString();
    }

    /** Coerce a value into a {@code List<Map<String,Object>>}, dropping non-map entries. */
    private static List<Map<String, Object>> listOfMaps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?>) {
                    result.add(asMap(entry));
                }
            }
        }
        return result;
    }

    /** Coerce a value into a {@code Map<String,Object>} with string keys; empty for non-maps. */
    private static Map<String, Object> asMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return result;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}

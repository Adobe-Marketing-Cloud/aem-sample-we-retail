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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Pure (Sling-free, JCR-free) parser that turns a source AEM QueryBuilder result into an
 * {@code identifier -> candidate pages} map for the legacy importer's {@code BLUEPRINT} mode.
 *
 * <p>Classic AEM commerce keeps the editorial slug and the commerce identifier in two parallel
 * JCR trees. The authored product-page tree ({@code /content/<site>/.../products}) carries the
 * human slug as the page node name and links to the commerce data node via
 * {@code jcr:content/cq:productMaster}. The commerce tree ({@code /var/commerce/products/...})
 * names its nodes after the technical identifier (the SKU) — exactly the {@code productKey} the
 * importer walk passes to {@code importLegacyProduct}.
 *
 * <p>A single source QueryBuilder call returns one hit per product page with its {@code jcr:path},
 * {@code jcr:content/cq:productMaster} and {@code jcr:content/jcr:title}. This parser keys the map
 * by the {@code cq:productMaster} leaf (the identifier) and retains <em>all</em> candidate pages in
 * source order, because a product is frequently referenced by both its canonical PDP and one or more
 * cross-merchandising pages whose leaf slug differs. {@link #slugFor(String)} keeps the historical
 * first-hit-wins behavior; {@link #slugFor(String, String, String, UnaryOperator)} selects the
 * canonical page when the candidates disagree.
 */
public final class LegacyProductSlugs {

    /** One authored product page: its editorial slug, its parent section path, and its display title. */
    record Candidate(String slug, String sectionPath, String title) {
    }

    private final Map<String, List<Candidate>> candidatesByIdentifier;

    private LegacyProductSlugs(Map<String, List<Candidate>> candidatesByIdentifier) {
        this.candidatesByIdentifier = candidatesByIdentifier;
    }

    /** The authored editorial slug for a commerce identifier, or {@code null} when none was harvested. */
    public String slugFor(String identifier) {
        if (identifier == null) {
            return null;
        }
        List<Candidate> candidates = candidatesByIdentifier.get(identifier);
        return candidates == null || candidates.isEmpty() ? null : candidates.get(0).slug();
    }

    /**
     * The canonical editorial slug for a commerce identifier, selecting among multiple candidate pages.
     *
     * <p>When the candidates carry a single distinct leaf slug (single page, locale copies, or the same
     * product listed under several sections with the same slug) the shared slug is returned directly and
     * the selection rules are <em>not</em> consulted — byte-identical to {@link #slugFor(String)}. This is
     * the regression-safety invariant the existing We.Retail import relies on.
     *
     * <p>When candidates disagree, the rules apply in order:
     * <ol>
     *   <li><b>Primary-category alignment.</b> Prefer the candidate whose section path ends with the
     *       segments of {@code primaryCategoryPath}, comparing per-segment under {@code segmentNormalizer}
     *       so the raw authored {@code jcr:path} aligns with the escaped catalog-relative primary.</li>
     *   <li><b>Title match.</b> Prefer the candidate whose page title equals {@code productName}, trimmed
     *       and case-insensitive.</li>
     *   <li><b>Source order.</b> Fall back to the first candidate (historical behavior).</li>
     * </ol>
     */
    public String slugFor(String identifier, String primaryCategoryPath, String productName,
                          UnaryOperator<String> segmentNormalizer) {
        if (identifier == null) {
            return null;
        }
        List<Candidate> candidates = candidatesByIdentifier.get(identifier);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String firstSlug = candidates.get(0).slug();
        Set<String> distinctSlugs = new LinkedHashSet<>();
        for (Candidate candidate : candidates) {
            distinctSlugs.add(candidate.slug());
        }
        if (distinctSlugs.size() <= 1) {
            return firstSlug;
        }

        // Rule 1: primary-category alignment (trailing-segment match under the shared normalizer).
        List<String> primarySegments = normalizeSegments(primaryCategoryPath, segmentNormalizer);
        if (!primarySegments.isEmpty()) {
            for (Candidate candidate : candidates) {
                if (endsWithSegments(normalizeSegments(candidate.sectionPath(), segmentNormalizer), primarySegments)) {
                    return candidate.slug();
                }
            }
        }

        // Rule 2: title match (trimmed, case-insensitive).
        if (productName != null && !productName.isBlank()) {
            String wanted = productName.trim().toLowerCase();
            for (Candidate candidate : candidates) {
                if (candidate.title() != null && candidate.title().trim().toLowerCase().equals(wanted)) {
                    return candidate.slug();
                }
            }
        }

        // Rule 3: source order.
        return firstSlug;
    }

    /** Number of harvested identifiers (each with one or more candidate pages). */
    public int size() {
        return candidatesByIdentifier.size();
    }

    /**
     * Parse a QueryBuilder {@code .json} result into an {@code identifier -> candidates} map. Each hit
     * contributes a {@link Candidate} of {@code (jcr:path-leaf, jcr:path-parent, jcr:content/jcr:title)}
     * keyed by the {@code cq:productMaster} leaf. Hits missing either leaf are skipped; candidates retain
     * source order.
     */
    public static LegacyProductSlugs parse(Map<String, Object> queryBuilderJson) {
        Map<String, List<Candidate>> map = new LinkedHashMap<>();
        if (queryBuilderJson != null) {
            for (Map<String, Object> hit : listOfMaps(queryBuilderJson.get("hits"))) {
                String path = stringValue(hit.get("jcr:path"));
                String slug = leaf(path);
                Map<String, Object> content = asMap(hit.get("jcr:content"));
                String identifier = leaf(stringValue(content.get("cq:productMaster")));
                if (slug.isBlank() || identifier.isBlank()) {
                    continue;
                }
                Candidate candidate = new Candidate(slug, parent(path), stringValue(content.get("jcr:title")));
                map.computeIfAbsent(identifier, key -> new ArrayList<>()).add(candidate);
            }
        }
        return new LegacyProductSlugs(map);
    }

    /** Split a path into normalized non-blank segments, applying {@code normalizer} per segment. */
    private static List<String> normalizeSegments(String path, UnaryOperator<String> normalizer) {
        List<String> segments = new ArrayList<>();
        if (path == null) {
            return segments;
        }
        for (String raw : path.split("/")) {
            if (raw.isBlank()) {
                continue;
            }
            segments.add(normalizer == null ? raw : normalizer.apply(raw));
        }
        return segments;
    }

    /** Whether {@code segments} ends with the full {@code suffix} sequence. */
    private static boolean endsWithSegments(List<String> segments, List<String> suffix) {
        if (suffix.isEmpty() || suffix.size() > segments.size()) {
            return false;
        }
        int offset = segments.size() - suffix.size();
        for (int i = 0; i < suffix.size(); i++) {
            if (!segments.get(offset + i).equals(suffix.get(i))) {
                return false;
            }
        }
        return true;
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

    /** Trailing-slash-tolerant final path segment; empty for blank input. */
    private static String leaf(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    /** Trailing-slash-tolerant parent path (everything before the final segment); empty for blank input. */
    private static String parent(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(0, slash) : "";
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}

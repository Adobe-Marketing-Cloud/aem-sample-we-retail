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

import com.adobe.cq.commerce.celadon.aem.LegacyTagCategories.CategoryResolution;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Pure (Sling-free, JCR-free) parser + resolver for an authored catalog blueprint
 * ({@code cq:catalogBlueprint} / {@code commerce/components/catalog}) used by the legacy
 * importer's {@code BLUEPRINT} mode.
 *
 * <p>A blueprint is a tree of {@code cq:Page} sections. Each section carries a display title
 * ({@code jcr:content/jcr:title}) and, when it is a smart collection, a
 * {@code jcr:content/filter/matchTags} multi-value. A product belongs to a section when it
 * carries <em>all</em> of that section's {@code matchTags} (AND logic), where a required tag is
 * satisfied by an equal or descendant product tag. Sections without {@code matchTags} are pure
 * navigation containers — materialised as folders but never matched against products.
 *
 * <p>Resolution is two-tiered. Tier 1 is the strict leaf match described above. When it yields
 * nothing — a product carries only some of the dimensions every leaf demands (e.g. a women's item
 * tagged by gender and season but missing the {@code apparel/*} type) — a tier-2 <em>department
 * fallback</em> homes it in the nearest meaningful top-level section instead of the catalog root.
 * Each top-level section's <em>invariant</em> is the intersection of {@code matchTags} across all
 * its filter-bearing descendant leaves; a product satisfying that invariant lands in the department.
 * Heterogeneous departments (whose leaves share no common tag, e.g. {@code equipment}) have an empty
 * invariant and never catch products. Well-tagged products never reach tier 2 (their leaf matches in
 * tier 1), so the fallback is purely additive and cannot move an existing placement.
 *
 * <p>Section paths are catalog-relative and escaped via {@link AemRepositorySupport#escapeNodeName}
 * so they are byte-identical to the folders the pre-pass creates. Primary selection and
 * additional-category reduction are delegated to {@link LegacyTagCategories#pickPrimary} and
 * {@link CategoryMembership#deriveAdditionalCategories}.
 */
public final class LegacyBlueprintCategories {

    private static final String CQ_TAGS = "cq:tags";

    /** A single blueprint section: catalog-relative escaped path, display title, AND-combined raw match tags. */
    public record Section(String path, String title, List<String> matchTags) {
    }

    private final List<Section> sections;
    private final List<Section> departments;

    private LegacyBlueprintCategories(List<Section> sections) {
        this.sections = sections;
        this.departments = computeDepartments(sections);
    }

    /** Pre-ordered (parents before children) sections, suitable for folder materialisation. */
    public List<Section> sections() {
        return sections;
    }

    /** Parse a blueprint {@code .infinity.json} map into a pre-ordered section list. */
    public static LegacyBlueprintCategories parse(Map<String, Object> blueprintJson) {
        List<Section> sections = new ArrayList<>();
        if (blueprintJson != null) {
            collect(blueprintJson, "", sections);
        }
        return new LegacyBlueprintCategories(sections);
    }

    /**
     * Resolves a product to a blueprint primary + additional set. Sections whose {@code matchTags}
     * are all satisfied by the product's tags are collected; the deepest is primary and the rest are
     * reduced to a minimal antichain. When no section matches, a department fallback (top-level sections
     * whose subtree invariant the product satisfies) is tried; failing that it falls back to
     * {@code folderPrimary} (empty additional).
     */
    public CategoryResolution resolve(String folderPrimary, Map<String, Object> product) {
        return resolve(folderPrimary, null, product);
    }

    /**
     * Source-favored variant of {@link #resolve(String, Map)}. {@code sourceFolderPath} is the
     * catalog-relative folder the product lived in on the source instance; when a matched section shares
     * trailing path segments with it (e.g. source {@code me/shorts} vs section {@code men/shorts}), that
     * section becomes primary — overriding depth — so editorial homes are preserved while activity
     * sections remain additional memberships. A blank hint, or a source folder that maps to no matched
     * section (a pure navigation bucket), keeps the deepest-then-lexicographic selection.
     */
    public CategoryResolution resolve(String folderPrimary, String sourceFolderPath, Map<String, Object> product) {
        List<String> productTags = rawTags(product);
        List<String> matched = new ArrayList<>();
        for (Section section : sections) {
            if (satisfies(productTags, section.matchTags())) {
                matched.add(section.path());
            }
        }
        if (matched.isEmpty()) {
            // Tier 2: department fallback. Home the product in any top-level section whose subtree
            // invariant it satisfies, before bailing to the (root) folder fallback.
            for (Section department : departments) {
                if (satisfies(productTags, department.matchTags())) {
                    matched.add(department.path());
                }
            }
        }
        if (matched.isEmpty()) {
            return new CategoryResolution(folderPrimary, List.of());
        }
        String primary = LegacyTagCategories.pickPrimary(matched, sourceFolderPath);
        return new CategoryResolution(primary, CategoryMembership.deriveAdditionalCategories(matched, primary));
    }

    /**
     * Computes the department fallback table: one pseudo-section per top-level blueprint section whose
     * filter-bearing descendant leaves share a common required tag. The match tags of each returned
     * section are the <em>invariant</em> — the AND-intersection of {@code matchTags} across every
     * descendant leaf that carries any (pure navigation containers and empty catch-alls are skipped so
     * they cannot poison an otherwise coherent department). Top-level sections whose leaves are
     * heterogeneous (empty intersection) are omitted, so they never catch products.
     */
    private static List<Section> computeDepartments(List<Section> sections) {
        List<Section> departments = new ArrayList<>();
        for (Section top : sections) {
            if (top.path().contains("/")) {
                continue;
            }
            String prefix = top.path() + "/";
            List<String> invariant = null;
            for (Section section : sections) {
                if (!section.path().startsWith(prefix) || section.matchTags().isEmpty()) {
                    continue;
                }
                invariant = invariant == null
                        ? new ArrayList<>(section.matchTags())
                        : intersect(invariant, section.matchTags());
                if (invariant.isEmpty()) {
                    break;
                }
            }
            if (invariant != null && !invariant.isEmpty()) {
                departments.add(new Section(top.path(), top.title(), invariant));
            }
        }
        return departments;
    }

    /** Tags present in both lists, compared trimmed/case-insensitively, preserving {@code a}'s form/order. */
    private static List<String> intersect(List<String> a, List<String> b) {
        List<String> result = new ArrayList<>();
        for (String x : a) {
            String nx = x.trim().toLowerCase();
            for (String y : b) {
                if (nx.equals(y.trim().toLowerCase())) {
                    result.add(x);
                    break;
                }
            }
        }
        return result;
    }

    private static void collect(Map<String, Object> node, String parentPath, List<Section> out) {
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.startsWith("jcr:") || key.startsWith("cq:") || key.startsWith("sling:")) {
                continue;
            }
            Map<String, Object> child = asMap(entry.getValue());
            if (!"cq:Page".equals(stringValue(child.get("jcr:primaryType")))) {
                continue;
            }
            Map<String, Object> content = asMap(child.get("jcr:content"));
            if (content.isEmpty()) {
                continue;
            }
            String segment = AemRepositorySupport.escapeNodeName(key);
            String path = parentPath.isEmpty() ? segment : parentPath + "/" + segment;
            String title = firstTitle(content.get("jcr:title"), key);
            List<String> matchTags = readStrings(asMap(content.get("filter")).get("matchTags"));
            out.add(new Section(path, title, matchTags));
            // Pre-order: the parent is recorded before recursing into its child sections.
            collect(child, path, out);
        }
    }

    /** A required tag is satisfied by an equal or descendant product tag (case-insensitive, trimmed). */
    private static boolean satisfies(List<String> productTags, List<String> matchTags) {
        if (matchTags.isEmpty()) {
            return false;
        }
        for (String required : matchTags) {
            String req = required.trim().toLowerCase();
            boolean found = false;
            for (String tag : productTags) {
                String t = tag.trim().toLowerCase();
                if (t.equals(req) || t.startsWith(req + "/")) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static List<String> rawTags(Map<String, Object> product) {
        return readStrings(product == null ? null : product.get(CQ_TAGS));
    }

    private static List<String> readStrings(Object raw) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Object value : asIterable(raw)) {
            if (value == null) {
                continue;
            }
            String id = value.toString().trim();
            if (!id.isBlank()) {
                values.add(id);
            }
        }
        return new ArrayList<>(values);
    }

    private static String firstTitle(Object titleValue, String fallback) {
        for (Object value : asIterable(titleValue)) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return fallback;
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}

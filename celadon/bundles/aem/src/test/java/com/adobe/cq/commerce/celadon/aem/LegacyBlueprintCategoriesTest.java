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

import com.adobe.cq.commerce.celadon.aem.LegacyBlueprintCategories.Section;
import com.adobe.cq.commerce.celadon.aem.LegacyTagCategories.CategoryResolution;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LegacyBlueprintCategoriesTest {

    /** Builds an ordered map from alternating key/value pairs. */
    private static Map<String, Object> m(Object... kv) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i].toString(), kv[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> page(Map<String, Object> content, Map<String, Object> children) {
        Map<String, Object> node = m("jcr:primaryType", "cq:Page", "jcr:content", content);
        node.putAll(children);
        return node;
    }

    private static Map<String, Object> content(String title, List<String> matchTags) {
        if (matchTags == null) {
            return m("jcr:title", title);
        }
        return m("jcr:title", title, "filter", m("matchTags", matchTags));
    }

    /** website-catalog root: equipment/running (smart) + men/coats (AND of two tags). */
    private static Map<String, Object> blueprintJson() {
        return m(
                "jcr:primaryType", "cq:Page",
                "jcr:content", content("Catalog", null),
                "equipment", page(content("Equipment", null),
                        m("running", page(content("Running",
                                List.of("we-retail:activity/running")), m()))),
                "men", page(content("Men", null),
                        m("coats", page(content("Coats",
                                List.of("we-retail:gender/men", "we-retail:apparel/coats")), m()))));
    }

    private static Map<String, Object> product(Object tags) {
        return Map.of("cq:tags", tags);
    }

    @Test
    public void parse_collectsSectionsPreOrderWithTitlesAndTags() {
        List<Section> sections = LegacyBlueprintCategories.parse(blueprintJson()).sections();
        assertEquals(4, sections.size());
        assertEquals("equipment", sections.get(0).path());
        assertEquals("Equipment", sections.get(0).title());
        assertEquals(List.of(), sections.get(0).matchTags());
        assertEquals("equipment/running", sections.get(1).path());
        assertEquals(List.of("we-retail:activity/running"), sections.get(1).matchTags());
        assertEquals("men", sections.get(2).path());
        assertEquals("men/coats", sections.get(3).path());
        assertEquals(List.of("we-retail:gender/men", "we-retail:apparel/coats"), sections.get(3).matchTags());
    }

    @Test
    public void resolve_singleTagMatchesSmartCollection() {
        CategoryResolution res = LegacyBlueprintCategories.parse(blueprintJson())
                .resolve("eq/eqsm", product(List.of("we-retail:activity/running")));
        assertEquals("equipment/running", res.primary());
        assertEquals(List.of(), res.additionalCategories());
    }

    @Test
    public void resolve_andLogicRequiresAllTags() {
        LegacyBlueprintCategories blueprint = LegacyBlueprintCategories.parse(blueprintJson());
        // both tags present -> coats matches
        CategoryResolution both = blueprint.resolve("mn/mnap",
                product(List.of("we-retail:gender/men", "we-retail:apparel/coats")));
        assertEquals("men/coats", both.primary());
        // only one of the two -> no match -> folder fallback
        CategoryResolution one = blueprint.resolve("mn/mnap", product(List.of("we-retail:gender/men")));
        assertEquals("mn/mnap", one.primary());
        assertEquals(List.of(), one.additionalCategories());
    }

    @Test
    public void resolve_descendantTagSatisfiesRequiredTag() {
        CategoryResolution res = LegacyBlueprintCategories.parse(blueprintJson())
                .resolve("eq/eqsm", product(List.of("we-retail:activity/running/trail")));
        assertEquals("equipment/running", res.primary());
    }

    @Test
    public void resolve_multipleMatchesDeepestPrimaryRestAdditional() {
        CategoryResolution res = LegacyBlueprintCategories.parse(blueprintJson())
                .resolve("eq/eqsm", product(List.of(
                        "we-retail:activity/running",
                        "we-retail:gender/men",
                        "we-retail:apparel/coats")));
        // equipment/running and men/coats both depth 1; lexicographic tie-break picks equipment/running.
        assertEquals("equipment/running", res.primary());
        assertEquals(List.of("men/coats"), res.additionalCategories());
    }

    @Test
    public void resolve_noTagsFallsBackToFolder() {
        CategoryResolution res = LegacyBlueprintCategories.parse(blueprintJson())
                .resolve("wm/wmap", Map.of());
        assertEquals("wm/wmap", res.primary());
        assertEquals(List.of(), res.additionalCategories());
    }

    /** equipment/hiking (activity) + men/shorts (gender AND apparel), both depth 2. */
    private static Map<String, Object> dualSectionBlueprint() {
        return m(
                "jcr:primaryType", "cq:Page",
                "jcr:content", content("Catalog", null),
                "equipment", page(content("Equipment", null),
                        m("hiking", page(content("Hiking",
                                List.of("we-retail:activity/hiking")), m()))),
                "men", page(content("Men", null),
                        m("shorts", page(content("Shorts",
                                List.of("we-retail:gender/men", "we-retail:apparel/shorts")), m()))));
    }

    @Test
    public void resolve_sourceFolderFavorsEditorialHomeOverLexicographic() {
        Map<String, Object> hikingShorts = product(List.of(
                "we-retail:activity/hiking", "we-retail:gender/men", "we-retail:apparel/shorts"));
        LegacyBlueprintCategories blueprint = LegacyBlueprintCategories.parse(dualSectionBlueprint());
        // Without a source hint both sections are depth 2 -> lexicographic tie-break picks equipment/hiking.
        CategoryResolution plain = blueprint.resolve("me/shorts", hikingShorts);
        assertEquals("equipment/hiking", plain.primary());
        assertEquals(List.of("men/shorts"), plain.additionalCategories());
        // With the source folder hint, the "shorts" leaf overlap re-homes it to its editorial section.
        CategoryResolution favored = blueprint.resolve("me/shorts", "me/shorts", hikingShorts);
        assertEquals("men/shorts", favored.primary());
        assertEquals(List.of("equipment/hiking"), favored.additionalCategories());
    }

    @Test
    public void resolve_bucketSourceFolderKeepsDeepestSelection() {
        Map<String, Object> hikingShorts = product(List.of(
                "we-retail:activity/hiking", "we-retail:gender/men", "we-retail:apparel/shorts"));
        // A source folder that maps to no matched section leaf falls back to the plain rule.
        CategoryResolution res = LegacyBlueprintCategories.parse(dualSectionBlueprint())
                .resolve("products", "products", hikingShorts);
        assertEquals("equipment/hiking", res.primary());
        assertEquals(List.of("men/shorts"), res.additionalCategories());
    }

    /**
     * Three departments: {@code women} (leaves all share {@code gender/women} -> coherent invariant),
     * {@code equipment} (distinct activities -> empty invariant), and {@code seasonal} (summer vs winter
     * -> empty invariant). Used to exercise the tier-2 department fallback.
     */
    private static Map<String, Object> departmentBlueprint() {
        return m(
                "jcr:primaryType", "cq:Page",
                "jcr:content", content("Catalog", null),
                "women", page(content("Women", null), m(
                        "coats", page(content("Coats",
                                List.of("we-retail:gender/women", "we-retail:apparel/coat")), m()),
                        "pants", page(content("Pants",
                                List.of("we-retail:gender/women", "we-retail:apparel/pants")), m()))),
                "equipment", page(content("Equipment", null), m(
                        "biking", page(content("Biking",
                                List.of("we-retail:activity/biking")), m()),
                        "hiking", page(content("Hiking",
                                List.of("we-retail:activity/hiking")), m()))),
                "seasonal", page(content("Seasonal", null), m(
                        "summer", page(content("Summer", null), m(
                                "apparel", page(content("Apparel", null), m(
                                        "hats", page(content("Hats",
                                                List.of("we-retail:season/summer", "we-retail:apparel/hat")), m()))))),
                        "winter", page(content("Winter", null), m(
                                "apparel", page(content("Apparel", null), m(
                                        "scarves", page(content("Scarves",
                                                List.of("we-retail:season/winter", "we-retail:apparel/scarf")), m()))))))));
    }

    @Test
    public void resolve_departmentFallbackHomesUnderTaggedProductInDepartment() {
        // gender/women + season/winter, no apparel type: no leaf matches (tier 1); the women subtree
        // invariant (gender/women) catches it (tier 2), so it homes in women rather than the root.
        CategoryResolution res = LegacyBlueprintCategories.parse(departmentBlueprint())
                .resolve("", product(List.of("we-retail:gender/women", "we-retail:season/winter")));
        assertEquals("women", res.primary());
        assertEquals(List.of(), res.additionalCategories());
    }

    @Test
    public void resolve_departmentFallbackSkippedWhenLeafMatches() {
        // A fully tagged coat full-matches women/coats in tier 1, so the department fallback never fires
        // and the broad women department does not appear as a redundant ancestor.
        CategoryResolution res = LegacyBlueprintCategories.parse(departmentBlueprint())
                .resolve("", product(List.of("we-retail:gender/women", "we-retail:apparel/coat")));
        assertEquals("women/coats", res.primary());
        assertEquals(List.of(), res.additionalCategories());
    }

    @Test
    public void resolve_heterogeneousDepartmentNeverCatches() {
        // season/winter alone matches no leaf, and seasonal's leaves (summer vs winter) share no common
        // tag -> empty invariant -> the product falls through to the folder (root) fallback.
        CategoryResolution res = LegacyBlueprintCategories.parse(departmentBlueprint())
                .resolve("", product(List.of("we-retail:season/winter")));
        assertEquals("", res.primary());
        assertEquals(List.of(), res.additionalCategories());
    }
}

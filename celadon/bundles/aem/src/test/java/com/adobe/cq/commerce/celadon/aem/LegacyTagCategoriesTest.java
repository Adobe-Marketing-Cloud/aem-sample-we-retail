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
import static org.junit.Assert.assertNull;

import com.adobe.cq.commerce.celadon.aem.LegacyTagCategories.CategoryMode;
import com.adobe.cq.commerce.celadon.aem.LegacyTagCategories.CategoryResolution;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LegacyTagCategoriesTest {

    private static Map<String, Object> product(Object tags) {
        return Map.of("cq:tags", tags);
    }

    private static final List<String> EQSMMR_TAGS = List.of(
            "geometrixx-outdoors:activity/running",
            "geometrixx-outdoors:apparel/footwear",
            "geometrixx-outdoors:gender/women",
            "geometrixx-outdoors:season/summer");

    @Test
    public void parseTagPath_stripsNamespaceAndEscapes() {
        assertEquals("activity/running", LegacyTagCategories.parseTagPath("geometrixx-outdoors:activity/running"));
        assertEquals("equipment", LegacyTagCategories.parseTagPath("we-retail:equipment"));
        assertEquals("", LegacyTagCategories.parseTagPath("we-retail:"));
        assertEquals("", LegacyTagCategories.parseTagPath("nocolon"));
        assertEquals("", LegacyTagCategories.parseTagPath(null));
        assertEquals("", LegacyTagCategories.parseTagPath(""));
        assertEquals("apparel/footwear", LegacyTagCategories.parseTagPath("we-retail:Apparel/Footwear"));
    }

    @Test
    public void tagPaths_dedupsDropsBlanksToleratesScalar() {
        assertEquals(
                List.of("activity/running", "apparel/footwear", "gender/women", "season/summer"),
                LegacyTagCategories.tagPaths(product(EQSMMR_TAGS)));
        assertEquals(List.of(), LegacyTagCategories.tagPaths(Map.of()));
        assertEquals(List.of("equipment"), LegacyTagCategories.tagPaths(product("we-retail:equipment")));
        assertEquals(
                List.of("equipment"),
                LegacyTagCategories.tagPaths(product(new String[]{"we-retail:equipment", "we-retail:equipment"})));
    }

    @Test
    public void pickPrimary_deepestThenLexicographic() {
        assertEquals("activity/running", LegacyTagCategories.pickPrimary(
                List.of("activity/running", "apparel/footwear", "gender/women", "season/summer")));
        assertEquals("a/b/c", LegacyTagCategories.pickPrimary(List.of("a", "a/b/c", "d/e")));
        assertNull(LegacyTagCategories.pickPrimary(List.of()));
    }

    @Test
    public void pickPrimary_sourceFolderLeafWinsOverLexicographic() {
        // Both depth 2; plain rule picks "equipment/hiking" (lexicographically first). The "me/shorts"
        // source folder shares its "shorts" leaf with "men/shorts", so the editorial home wins instead.
        assertEquals("men/shorts", LegacyTagCategories.pickPrimary(
                List.of("equipment/hiking", "men/shorts"), "me/shorts"));
    }

    @Test
    public void pickPrimary_sourceFolderLeafOverridesDeeperSection() {
        // The source folder is depth 2 ("me/shorts") while an unrelated section is depth 3; the source
        // overlap still wins so the product stays in its editorial home rather than the deeper match.
        assertEquals("men/shorts", LegacyTagCategories.pickPrimary(
                List.of("equipment/hiking/trail", "men/shorts"), "me/shorts"));
    }

    @Test
    public void pickPrimary_bucketSourceFolderFallsBackToDeepest() {
        // A source folder that shares no trailing segment with any candidate (a pure bucket) keeps the
        // deepest-then-lexicographic rule.
        assertEquals("equipment/hiking/trail", LegacyTagCategories.pickPrimary(
                List.of("equipment/hiking/trail", "men/shorts"), "products"));
        assertEquals("equipment/hiking", LegacyTagCategories.pickPrimary(
                List.of("equipment/hiking", "men/shorts"), "products"));
    }

    @Test
    public void pickPrimary_blankSourceHintMatchesPlainRule() {
        assertEquals("equipment/hiking", LegacyTagCategories.pickPrimary(
                List.of("equipment/hiking", "men/shorts"), null));
        assertEquals("equipment/hiking", LegacyTagCategories.pickPrimary(
                List.of("equipment/hiking", "men/shorts"), "  "));
    }

    @Test
    public void resolve_folderIgnoresTags() {
        CategoryResolution res = LegacyTagCategories.resolve(CategoryMode.FOLDER, "eq/eqsm", product(EQSMMR_TAGS));
        assertEquals("eq/eqsm", res.primary());
        assertEquals(List.of(), res.additionalCategories());
    }

    @Test
    public void resolve_tagPicksPrimaryAndAdditional() {
        CategoryResolution res = LegacyTagCategories.resolve(CategoryMode.TAG, "eq/eqsm", product(EQSMMR_TAGS));
        assertEquals("activity/running", res.primary());
        assertEquals(List.of("apparel/footwear", "gender/women", "season/summer"), res.additionalCategories());
    }

    @Test
    public void resolve_tagEmptyFallsBackToFolder() {
        CategoryResolution res = LegacyTagCategories.resolve(CategoryMode.TAG, "eq/eqsm", Map.of());
        assertEquals("eq/eqsm", res.primary());
        assertEquals(List.of(), res.additionalCategories());
    }

    @Test
    public void resolve_hybridFolderPrimaryTagsAdditional() {
        Map<String, Object> waterBottle = product(List.of(
                "we-retail:equipment", "we-retail:season/summer", "we-retail:activity/running"));
        CategoryResolution res = LegacyTagCategories.resolve(CategoryMode.HYBRID, "eq/running", waterBottle);
        assertEquals("eq/running", res.primary());
        assertEquals(List.of("activity/running", "equipment", "season/summer"), res.additionalCategories());
    }

    @Test
    public void categoryMode_fromParamLenientDefault() {
        assertEquals(CategoryMode.TAG, CategoryMode.fromParam("tag"));
        assertEquals(CategoryMode.HYBRID, CategoryMode.fromParam("HYBRID"));
        assertEquals(CategoryMode.BLUEPRINT, CategoryMode.fromParam("blueprint"));
        assertEquals(CategoryMode.BLUEPRINT, CategoryMode.fromParam(" Blueprint "));
        assertEquals(CategoryMode.FOLDER, CategoryMode.fromParam("folder"));
        assertEquals(CategoryMode.FOLDER, CategoryMode.fromParam(null));
        assertEquals(CategoryMode.FOLDER, CategoryMode.fromParam(""));
        assertEquals(CategoryMode.FOLDER, CategoryMode.fromParam("bogus"));
    }
}

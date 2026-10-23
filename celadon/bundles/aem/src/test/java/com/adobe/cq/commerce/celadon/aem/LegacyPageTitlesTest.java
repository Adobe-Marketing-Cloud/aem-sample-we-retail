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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LegacyPageTitlesTest {

    private static final String BASE = "/content/we-retail/us/en/products";

    /** Builds an ordered map from alternating key/value pairs. */
    private static Map<String, Object> m(Object... kv) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i].toString(), kv[i + 1]);
        }
        return map;
    }

    /** A section page hit: jcr:path + jcr:content/jcr:title, no cq:productMaster. */
    private static Map<String, Object> section(String pagePath, Object title) {
        return m("jcr:path", pagePath, "jcr:content", m("jcr:title", title));
    }

    /** A product page hit: carries a cq:productMaster identifier (and possibly a title). */
    private static Map<String, Object> product(String pagePath, String productMaster, Object title) {
        return m("jcr:path", pagePath, "jcr:content", m("cq:productMaster", productMaster, "jcr:title", title));
    }

    private static Map<String, Object> result(Map<String, Object>... hits) {
        return m("success", Boolean.TRUE, "hits", List.of(hits));
    }

    @Test
    public void parse_keysByCatalogRelativeEscapedPath() {
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/men", "Men"),
                section(BASE + "/men/coats", "Coats")), BASE);
        assertEquals(2, titles.size());
        assertEquals("Men", titles.titleFor("men"));
        assertEquals("Coats", titles.titleFor("men/coats"));
    }

    @Test
    public void parse_overridesPossessiveTopCategories() {
        // The editorial nav uses "Men"/"Women" where the blueprint carries "Men's"/"Women's".
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/men", "Men"),
                section(BASE + "/women", "Women")), BASE);
        assertEquals("Men", titles.titleFor("men"));
        assertEquals("Women", titles.titleFor("women"));
    }

    @Test
    public void parse_skipsProductPages() {
        // Product pages carry a cq:productMaster — they are slugs, not category sections.
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/men", "Men"),
                product(BASE + "/men/coats/portland-hooded-jacket",
                        "/var/commerce/products/we-retail/me/coats/meotwipot", "Portland")), BASE);
        assertEquals(1, titles.size());
        assertEquals("Men", titles.titleFor("men"));
        assertNull(titles.titleFor("men/coats/portland-hooded-jacket"));
    }

    @Test
    public void parse_skipsBasePageItself() {
        // The base page has an empty relative path and must never become a key.
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE, "Products"),
                section(BASE + "/equipment", "Equipment")), BASE);
        assertEquals(1, titles.size());
        assertEquals("Equipment", titles.titleFor("equipment"));
    }

    @Test
    public void parse_escapesSegmentsToMatchBlueprintPaths() {
        // Segment escaping (lower-case, spaces->dash, apostrophe stripped) keeps keys byte-identical to
        // the blueprint section paths produced via AemRepositorySupport.escapeNodeName.
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/Men's/Snow Sports", "Snow Sports")), BASE);
        assertEquals("Snow Sports", titles.titleFor("mens/snow-sports"));
    }

    @Test
    public void parse_skipsHitsOutsideBaseSubtree() {
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section("/content/other/site/men", "Men"),
                section(BASE + "/women", "Women")), BASE);
        assertEquals(1, titles.size());
        assertEquals("Women", titles.titleFor("women"));
    }

    @Test
    public void parse_toleratesTrailingSlashOnBaseAndPath() {
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/equipment/", "Equipment")), BASE + "/");
        assertEquals("Equipment", titles.titleFor("equipment"));
    }

    @Test
    public void parse_multiValueTitleTakesFirstNonBlank() {
        // we-retail authoring artifact: a stray multi-value jcr:title — the first non-blank wins.
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/equipment/water-sports", Arrays.asList("", "Water Sports", "Swimming"))), BASE);
        assertEquals("Water Sports", titles.titleFor("equipment/water-sports"));
    }

    @Test
    public void parse_skipsTitlelessSections() {
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/men", ""),
                section(BASE + "/women", "Women")), BASE);
        assertEquals(1, titles.size());
        assertNull(titles.titleFor("men"));
        assertEquals("Women", titles.titleFor("women"));
    }

    @Test
    public void parse_duplicatePathFirstWins() {
        LegacyPageTitles titles = LegacyPageTitles.parse(result(
                section(BASE + "/men", "Men"),
                section(BASE + "/men", "Gentlemen")), BASE);
        assertEquals(1, titles.size());
        assertEquals("Men", titles.titleFor("men"));
    }

    @Test
    public void parse_nullOrEmptyJsonYieldsEmptyMap() {
        assertEquals(0, LegacyPageTitles.parse(null, BASE).size());
        assertEquals(0, LegacyPageTitles.parse(Map.of(), BASE).size());
    }

    @Test
    public void parse_blankBaseYieldsEmptyMap() {
        assertEquals(0, LegacyPageTitles.parse(result(section(BASE + "/men", "Men")), null).size());
        assertEquals(0, LegacyPageTitles.parse(result(section(BASE + "/men", "Men")), "").size());
    }

    @Test
    public void empty_andTitleForUnknownOrNull() {
        assertEquals(0, LegacyPageTitles.empty().size());
        LegacyPageTitles titles = LegacyPageTitles.parse(result(section(BASE + "/men", "Men")), BASE);
        assertNull(titles.titleFor("absent"));
        assertNull(titles.titleFor(null));
    }
}

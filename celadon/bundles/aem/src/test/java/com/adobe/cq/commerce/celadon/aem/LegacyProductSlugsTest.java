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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.Test;

public class LegacyProductSlugsTest {

    /** The real per-segment normalizer the servlet injects, exercised here for parity. */
    private static final UnaryOperator<String> NORM = AemRepositorySupport::escapeNodeName;

    /** Builds an ordered map from alternating key/value pairs. */
    private static Map<String, Object> m(Object... kv) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i].toString(), kv[i + 1]);
        }
        return map;
    }

    /** One QueryBuilder hit: jcr:path (page) + jcr:content/cq:productMaster (commerce node). */
    private static Map<String, Object> hit(String pagePath, String productMaster) {
        return m("jcr:path", pagePath, "jcr:content", m("cq:productMaster", productMaster));
    }

    /** A hit that also carries the page's jcr:content/jcr:title (needed for the title rule). */
    private static Map<String, Object> hit(String pagePath, String productMaster, String title) {
        return m("jcr:path", pagePath, "jcr:content", m("cq:productMaster", productMaster, "jcr:title", title));
    }

    private static Map<String, Object> result(Map<String, Object>... hits) {
        return m("success", Boolean.TRUE, "hits", List.of(hits));
    }

    @Test
    public void parse_keysByProductMasterLeaf_notByPageLeaf() {
        // Page slug leaf (bpa-free-water-bottle) differs from the identifier leaf (eqrusubpe);
        // the map must key on the identifier so the SKU-keyed importer walk can join.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/we-retail/us/en/products/equipment/running/bpa-free-water-bottle",
                        "/var/commerce/products/we-retail/eq/running/eqrusubpe")));
        assertEquals(1, slugs.size());
        assertEquals("bpa-free-water-bottle", slugs.slugFor("eqrusubpe"));
        // The page-leaf is not a key.
        assertNull(slugs.slugFor("bpa-free-water-bottle"));
    }

    @Test
    public void parse_pageTreePlacementDivergesFromCommerceTree() {
        // The authored page lives under .../products/men/coats while the commerce node lives under
        // .../we-retail/me/coats; only the productMaster leaf reliably joins the two trees.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/we-retail/us/en/products/men/coats/portland-hooded-jacket",
                        "/var/commerce/products/we-retail/me/coats/meotwipot")));
        assertEquals("portland-hooded-jacket", slugs.slugFor("meotwipot"));
    }

    @Test
    public void parse_skipsHitsMissingEitherLeaf() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/has-both", "/var/commerce/products/site/skuok"),
                m("jcr:path", "/content/site/products/no-master"),                 // missing productMaster
                m("jcr:content", m("cq:productMaster", "/var/commerce/x/orphan")))); // missing jcr:path
        assertEquals(1, slugs.size());
        assertEquals("has-both", slugs.slugFor("skuok"));
    }

    @Test
    public void parse_duplicateIdentifierFirstWins() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/first-slug", "/var/commerce/products/site/dup"),
                hit("/content/site/products/second-slug", "/var/commerce/products/site/dup")));
        assertEquals(1, slugs.size());
        assertEquals("first-slug", slugs.slugFor("dup"));
    }

    @Test
    public void parse_toleratesTrailingSlashInPaths() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/trailing/", "/var/commerce/products/site/skux/")));
        assertEquals("trailing", slugs.slugFor("skux"));
    }

    @Test
    public void parse_nullOrEmptyJsonYieldsEmptyMap() {
        assertEquals(0, LegacyProductSlugs.parse(null).size());
        assertEquals(0, LegacyProductSlugs.parse(Map.of()).size());
    }

    @Test
    public void slugFor_unknownOrNullIdentifierReturnsNull() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/known", "/var/commerce/products/site/known")));
        assertNull(slugs.slugFor("absent"));
        assertNull(slugs.slugFor(null));
    }

    // --- canonical-page selection (3 rules) -------------------------------------------------

    @Test
    public void select_singleCandidateReturnsThatSlug() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/geo/en/women/shirts/maui-marine",
                        "/var/commerce/products/geo/wm/wmap/wmapmm", "Maui Marine")));
        assertEquals("maui-marine", slugs.slugFor("wmapmm", "women/shirts", "Maui Marine", NORM));
    }

    @Test
    public void select_multipleCandidatesSameSlug_rulesNotConsulted() {
        // Two pages, one slug (e.g. listed under two sections). Even with a primary/name that
        // align with neither candidate, the shared slug must come back byte-identical to today.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/geo/en/equipment/running/bora-bora", "/var/commerce/products/geo/wm/wmap/wmapbb", "Biking"),
                hit("/content/geo/en/women/shirts/bora-bora", "/var/commerce/products/geo/wm/wmap/wmapbb", "Bora Bora")));
        assertEquals("bora-bora", slugs.slugFor("wmapbb", "seasonal/summer", "Nothing Matches", NORM));
        // 1-arg path stays first-hit (regression).
        assertEquals("bora-bora", slugs.slugFor("wmapbb"));
    }

    @Test
    public void select_differingSlugs_rule1PrimaryCategoryWins() {
        // Cross-merch page sorts first; canonical PDP under the primary category must win.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/geo/en/activities/jola-summer-surfing",
                        "/var/commerce/products/geo/wm/wmap/wmapmm", "Surfing"),
                hit("/content/geo/en/women/shirts/maui-marine",
                        "/var/commerce/products/geo/wm/wmap/wmapmm", "Maui Marine")));
        assertEquals("maui-marine", slugs.slugFor("wmapmm", "women/shirts", "Maui Marine", NORM));
    }

    @Test
    public void select_differingSlugs_primaryAbsent_rule2TitleWins() {
        // No candidate aligns with the primary category, so the title match disambiguates.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/geo/en/activities/jola-summer-surfing",
                        "/var/commerce/products/geo/wm/wmap/wmapmm", "Surfing"),
                hit("/content/geo/en/women/shirts/maui-marine",
                        "/var/commerce/products/geo/wm/wmap/wmapmm", "Maui Marine")));
        assertEquals("maui-marine", slugs.slugFor("wmapmm", "equipment/running", "  maui marine  ", NORM));
    }

    @Test
    public void select_differingSlugs_noPrimaryNoTitle_sourceOrderFallback() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/geo/en/activities/jola-summer-surfing",
                        "/var/commerce/products/geo/wm/wmap/wmapmm", "Surfing"),
                hit("/content/geo/en/women/shirts/maui-marine",
                        "/var/commerce/products/geo/wm/wmap/wmapmm", "Maui Marine")));
        assertEquals("jola-summer-surfing", slugs.slugFor("wmapmm", "no/such-section", "Totally Different", NORM));
    }

    @Test
    public void select_rule1NormalizationIsSymmetric() {
        // Primary path is the already-escaped catalog-relative form (women/sun-glasses); the candidate
        // section comes raw from the authored jcr:path with a space. The injected normalizer aligns both.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/geo/en/activities/cuzco-hiking",
                        "/var/commerce/products/geo/eq/eqsm/eqsmsm", "Hiking"),
                hit("/content/geo/en/women/sun glasses/sumatra",
                        "/var/commerce/products/geo/eq/eqsm/eqsmsm", "Sumatra")));
        assertEquals("sumatra", slugs.slugFor("eqsmsm", "women/sun-glasses", "Sumatra", NORM));
    }
}

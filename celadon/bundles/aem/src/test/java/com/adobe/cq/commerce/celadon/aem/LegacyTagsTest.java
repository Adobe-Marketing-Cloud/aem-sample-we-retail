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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LegacyTagsTest {

    @Test
    public void tagIdsKeepsNamespacedIdsInOrderWithoutDuplicates() {
        assertEquals(List.of("we-retail:activity/skiing", "we-retail:gender/men"),
                LegacyTags.tagIds(List.of(" we-retail:activity/skiing", "we-retail:gender/men", "we-retail:activity/skiing", "plain", "")));
        assertEquals(List.of("we-retail:season/winter"), LegacyTags.tagIds(new String[] { "we-retail:season/winter" }));
        assertEquals(List.of("we-retail:apparel/coat"), LegacyTags.tagIds("we-retail:apparel/coat"));
        assertEquals(List.of(), LegacyTags.tagIds(null));
    }

    @Test
    public void selfAndAncestorIdsRunFromTheNamespaceDown() {
        assertEquals(List.of("we-retail:", "we-retail:activity", "we-retail:activity/skiing"),
                LegacyTags.selfAndAncestorIds("we-retail:activity/skiing"));
        assertEquals(List.of("we-retail:"), LegacyTags.selfAndAncestorIds("we-retail:"));
        assertEquals(List.of(), LegacyTags.selfAndAncestorIds("plain"));
    }

    @Test
    public void taxonomyPathMapsIdsBelowTheTagRoot() {
        assertEquals("we-retail/activity/skiing", LegacyTags.taxonomyPath("we-retail:activity/skiing"));
        assertEquals("we-retail", LegacyTags.taxonomyPath("we-retail:"));
    }

    @Test
    public void localizedTitlesKeepsTranslatedTitlesOnly() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("jcr:title", "Skiing");
        node.put("jcr:title.de", "Skifahren");
        node.put("jcr:title.pt_br", "Esqui");
        node.put("jcr:title.fr", "");
        node.put("jcr:description", "Winter sports");
        node.put("jcr:title.de.bak", "ignored");

        assertEquals(Map.of("jcr:title.de", "Skifahren", "jcr:title.pt_br", "Esqui"), LegacyTags.localizedTitles(node));
    }
}

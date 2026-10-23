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

import java.util.Map;
import org.junit.Test;

public class LegacyAttributeKeyTest {

    @Test
    public void summaryCatalogsKeepTheStandardDescriptionAsShortDescription() {
        Map<String, Object> source = Map.of("jcr:title", "Sleek Insulated Coat", "jcr:description", "coat",
                "summary", "Stay warm but avoid that 'overstuffed' look.");

        assertEquals("jcr:title", CeladonLegacyImportServlet.legacyAttributeKey("name", source));
        assertEquals("summary", CeladonLegacyImportServlet.legacyAttributeKey("description", source));
        assertEquals("jcr:description", CeladonLegacyImportServlet.legacyAttributeKey("short_description", source));
    }

    @Test
    public void plainClassicCatalogsImportTheStandardDescription() {
        Map<String, Object> source = Map.of("jcr:title", "Shirt", "jcr:description", "A cotton shirt.");

        assertEquals("jcr:description", CeladonLegacyImportServlet.legacyAttributeKey("description", source));
        assertEquals("short_description", CeladonLegacyImportServlet.legacyAttributeKey("short_description", source));
    }

    @Test
    public void sourcesWithoutLegacyKeysUseTheAttributeCode() {
        Map<String, Object> source = Map.of("name", "Shirt");

        assertEquals("name", CeladonLegacyImportServlet.legacyAttributeKey("name", source));
        assertEquals("description", CeladonLegacyImportServlet.legacyAttributeKey("description", source));
        assertEquals("short_description", CeladonLegacyImportServlet.legacyAttributeKey("short_description", source));
        assertEquals("color", CeladonLegacyImportServlet.legacyAttributeKey("color", source));
    }
}

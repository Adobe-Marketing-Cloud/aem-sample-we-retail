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

import static org.junit.Assert.*;

import com.adobe.cq.commerce.celadon.core.api.CeladonGraphqlEngine;
import com.adobe.cq.commerce.celadon.core.api.FetcherContext;
import java.util.List;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.junit.Test;

/**
 * Unit coverage for the servlet's pure routing and per-catalog cache-eviction
 * logic. These branches are plain string/Map operations with no JCR dependency,
 * so they exercise the package-private seams directly without a request mock or
 * OSGi activation. The {@code engineFor}/{@code execute} paths require the live
 * AEM resolver and are covered by integration validation against a real instance.
 */
public class CeladonGraphqlServletTest {

    private final CeladonGraphqlServlet servlet = new CeladonGraphqlServlet();

    // ----- catalogForPathInfo: request routing -----

    @Test
    public void blankPathInfoFallsBackToDefaultCatalog() {
        servlet.defaultCatalog = "we-retail";
        assertEquals("we-retail", servlet.catalogForPathInfo(null));
        assertEquals("we-retail", servlet.catalogForPathInfo(""));
        assertEquals("we-retail", servlet.catalogForPathInfo("/"));
    }

    @Test
    public void blankPathInfoWithNoDefaultReturnsNull() {
        servlet.defaultCatalog = "";
        assertNull(servlet.catalogForPathInfo(null));
        assertNull(servlet.catalogForPathInfo("/"));
    }

    @Test
    public void firstSegmentNamesTheCatalog() {
        assertEquals("we-retail", servlet.catalogForPathInfo("/we-retail"));
        assertEquals("geometrixx-outdoors", servlet.catalogForPathInfo("/geometrixx-outdoors"));
    }

    @Test
    public void onlyTheFirstSegmentIsUsed() {
        assertEquals("we-retail", servlet.catalogForPathInfo("/we-retail/men/coats"));
    }

    @Test
    public void malformedSegmentReturnsNull() {
        assertNull(servlet.catalogForPathInfo("/.."));
        assertNull(servlet.catalogForPathInfo("/.secret"));
        assertNull(servlet.catalogForPathInfo("//we-retail"));
    }

    // ----- catalogFromPath: change-event attribution -----

    @Test
    public void changePathYieldsItsCatalog() {
        assertEquals("we-retail", servlet.catalogFromPath("/content/dam/celadon/we-retail/jcr:content"));
        assertEquals("we-retail", servlet.catalogFromPath("/content/dam/celadon/we-retail"));
    }

    @Test
    public void rootOrUnrelatedChangeYieldsNull() {
        assertNull(servlet.catalogFromPath("/content/dam/celadon"));
        assertNull(servlet.catalogFromPath("/content/dam/celadon/"));
        assertNull(servlet.catalogFromPath("/content/other/thing"));
        assertNull(servlet.catalogFromPath(null));
    }

    // ----- onChange: selective vs. full eviction -----

    @Test
    public void onChangeEvictsOnlyTheAffectedCatalog() {
        servlet.engines.put("we-retail", engine("we-retail"));
        servlet.engines.put("geometrixx-outdoors", engine("geometrixx-outdoors"));

        servlet.onChange(List.of(change("/content/dam/celadon/we-retail/men")));

        assertFalse(servlet.engines.containsKey("we-retail"));
        assertTrue(servlet.engines.containsKey("geometrixx-outdoors"));
    }

    @Test
    public void onChangeAtRootClearsEveryCatalog() {
        servlet.engines.put("we-retail", engine("we-retail"));
        servlet.engines.put("geometrixx-outdoors", engine("geometrixx-outdoors"));

        servlet.onChange(List.of(change("/content/dam/celadon")));

        assertTrue(servlet.engines.isEmpty());
    }

    private static CeladonGraphqlEngine engine(String catalog) {
        return new CeladonGraphqlEngine(new FetcherContext(catalog));
    }

    private static ResourceChange change(String path) {
        return new ResourceChange(ChangeType.CHANGED, path, false);
    }
}

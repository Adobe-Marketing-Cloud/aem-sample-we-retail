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
package com.adobe.cq.commerce.celadon.aem.attribute.source;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class DiscoveryHintsTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Test
    public void exactMatchPassesAllow() {
        DiscoveryHints h = new DiscoveryHints(List.of("sku", "name"), List.of());
        assertTrue(h.allows("sku"));
        assertTrue(h.allows("name"));
        assertFalse(h.allows("price"));
    }

    @Test
    public void emptyAllowMeansAllPass() {
        DiscoveryHints h = new DiscoveryHints(List.of(), List.of());
        assertTrue(h.allows("anything"));
    }

    @Test
    public void prefixGlobMatches() {
        DiscoveryHints h = new DiscoveryHints(List.of("fashion_*"), List.of());
        assertTrue(h.allows("fashion_color"));
        assertTrue(h.allows("fashion_size"));
        assertFalse(h.allows("color"));
    }

    @Test
    public void suffixGlobMatches() {
        DiscoveryHints h = new DiscoveryHints(List.of("*_internal"), List.of());
        assertTrue(h.allows("price_internal"));
        assertFalse(h.allows("internal_price"));
    }

    @Test
    public void denyOverridesAllow() {
        DiscoveryHints h = new DiscoveryHints(List.of("fashion_*"), List.of("fashion_internal"));
        assertTrue(h.allows("fashion_color"));
        assertFalse(h.allows("fashion_internal"));
    }

    @Test
    public void builtInSkipsAlwaysApply() {
        DiscoveryHints h = new DiscoveryHints(List.of(), List.of());
        assertFalse(h.allows("jcr:primaryType"));
        assertFalse(h.allows("cq:lastModified"));
        assertFalse(h.allows("sling:resourceType"));
    }

    @Test
    public void builtInSkipsIgnoredWhenExplicitlyAllowed() {
        DiscoveryHints h = new DiscoveryHints(List.of(), List.of());
        // Built-ins are non-negotiable — explicit allow does not override.
        assertFalse(h.allows("jcr:primaryType"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void allowListWithoutSkuTrips() {
        new DiscoveryHints(List.of("name", "price"), List.of()).validateRequired();
    }

    @Test
    public void allowListWithSkuPasses() {
        new DiscoveryHints(List.of("sku", "name"), List.of()).validateRequired();
    }

    @Test
    public void emptyAllowListPasses() {
        new DiscoveryHints(List.of(), List.of()).validateRequired();
    }

    @Test
    public void readsFromCatalogRoot() {
        Map<String, Object> props = new HashMap<>();
        props.put("celadonAllowList", new String[] {"sku", "name", "price"});
        props.put("celadonDenyList", new String[] {"internal_*"});
        context.create().resource("/content/dam/celadon/venia/jcr:content", props);

        Resource catalogRoot = context.resourceResolver().getResource("/content/dam/celadon/venia");
        DiscoveryHints h = DiscoveryHints.read(catalogRoot);
        assertTrue(h.allows("sku"));
        assertFalse(h.allows("internal_foo"));
        assertFalse(h.allows("description"));  // not in allow list
    }

    @Test
    public void absentPropertiesYieldEmptyHints() {
        context.create().resource("/content/dam/celadon/empty/jcr:content");
        Resource catalogRoot = context.resourceResolver().getResource("/content/dam/celadon/empty");
        DiscoveryHints h = DiscoveryHints.read(catalogRoot);
        assertTrue(h.allows("anything"));
    }
}

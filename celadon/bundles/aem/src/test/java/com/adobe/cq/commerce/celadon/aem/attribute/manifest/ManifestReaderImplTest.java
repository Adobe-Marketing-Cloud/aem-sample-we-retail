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
package com.adobe.cq.commerce.celadon.aem.attribute.manifest;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Reader test. The manifest CFs are written on a live AEM via the Content
 * Fragment API (see {@link ManifestWriter}), which is not available under Sling
 * Mock, so here we create the equivalent {@code jcr:content/data/master} node
 * structure directly — exactly what the CF API persists and what
 * {@link ManifestReaderImpl} reads.
 */
public class ManifestReaderImplTest {

    @Rule public final SlingContext context = new SlingContext();

    private void writeManifestEntry(String catalog, String code, String label,
                                    NormalizedType type, AttributeScope scope,
                                    boolean filterable, boolean aggregatable, int ordering) {
        Map<String, Object> master = new HashMap<>();
        master.put("code", code);
        master.put("label", label);
        master.put("type", type.name());
        master.put("scope", scope.name());
        master.put("filterable", filterable);
        master.put("aggregatable", aggregatable);
        master.put("ordering", (long) ordering);
        context.create().resource(
                "/content/dam/celadon/" + catalog + "/_manifest/" + code + "/jcr:content/data/master",
                master);
    }

    @Test
    public void readsBackWrittenManifest() {
        writeManifestEntry("venia", "sku", "SKU", NormalizedType.STRING,
                AttributeScope.BOTH, true, false, 0);
        writeManifestEntry("venia", "color", "Color", NormalizedType.SELECT,
                AttributeScope.VARIANT, true, true, 20);

        Optional<AttributeManifest> m = new ManifestReaderImpl().read(context.resourceResolver(), "venia");
        assertTrue(m.isPresent());
        assertEquals(2, m.get().entries().size());
        assertEquals(NormalizedType.SELECT, m.get().entryFor("color").orElseThrow().type());
        assertEquals(AttributeScope.VARIANT, m.get().entryFor("color").orElseThrow().scope());
    }

    @Test
    public void emptyWhenAbsent() {
        Optional<AttributeManifest> m = new ManifestReaderImpl().read(context.resourceResolver(), "ghost");
        assertTrue(m.isEmpty());
    }

    @Test
    public void cachesByCatalog() {
        writeManifestEntry("venia", "sku", "SKU", NormalizedType.STRING,
                AttributeScope.BOTH, true, false, 0);

        ManifestReaderImpl r = new ManifestReaderImpl();
        AttributeManifest first = r.read(context.resourceResolver(), "venia").orElseThrow();
        AttributeManifest second = r.read(context.resourceResolver(), "venia").orElseThrow();
        assertSame(first, second);
    }
}

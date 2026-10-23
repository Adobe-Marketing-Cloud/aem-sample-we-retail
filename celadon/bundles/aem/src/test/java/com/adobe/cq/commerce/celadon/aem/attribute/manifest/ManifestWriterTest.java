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

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit coverage for the parts of {@link ManifestWriter} that don't require the
 * AEM Content Fragment runtime. The actual fragment creation goes through
 * {@code FragmentTemplate.createFragment} (only available on a real instance,
 * like {@code OptionDefinitionWriter}), so the end-to-end write is exercised by
 * the integration tests, not here.
 */
public class ManifestWriterTest {

    @Rule public final SlingContext context = new SlingContext();

    @Test(expected = PersistenceException.class)
    public void failsWhenAttributeModelMissing() throws Exception {
        context.create().resource("/content/dam/celadon/venia/jcr:content");
        // No celadon-attribute model under /conf/venia -> writer must refuse.
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));
        new ManifestWriter().write(context.resourceResolver(), m);
    }

    @Test
    public void emptyManifestCreatesFolderAndCommits() throws Exception {
        context.create().resource("/content/dam/celadon/venia/jcr:content");
        com.adobe.cq.commerce.celadon.aem.AemRepositorySupport.ensureAttributeModel(
                context.resourceResolver(), "venia");

        // No entries -> no fragment creation needed, so this succeeds under Sling Mock
        // and proves the folder is (re)created and the write commits cleanly.
        new ManifestWriter().write(context.resourceResolver(),
                AttributeManifest.empty("venia"));

        assertNotNull(context.resourceResolver().getResource("/content/dam/celadon/venia/_manifest"));
        assertEquals(com.adobe.cq.commerce.celadon.aem.AemRepositorySupport.MANIFEST_FOLDER_TITLE,
                context.resourceResolver().getResource("/content/dam/celadon/venia/_manifest/jcr:content")
                        .getValueMap().get("jcr:title", String.class));
    }
}

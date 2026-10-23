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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class AemRepositorySupportAttributeModelTest {

    @Rule public final SlingContext context = new SlingContext();

    @Test
    public void createsModelOnFirstCall() throws Exception {
        String path = AemRepositorySupport.ensureAttributeModel(context.resourceResolver(), "venia");
        assertEquals("/conf/venia/settings/dam/cfm/models/celadon-attribute", path);
        Resource model = context.resourceResolver().getResource(path);
        assertNotNull(model);
        assertNotNull(context.resourceResolver().getResource(
                path + "/jcr:content/model/cq:dialog/content/items/code"));
        assertNotNull(context.resourceResolver().getResource(
                path + "/jcr:content/model/cq:dialog/content/items/type"));
    }

    @Test
    public void idempotentOnSecondCall() throws Exception {
        AemRepositorySupport.ensureAttributeModel(context.resourceResolver(), "venia");
        String secondPath = AemRepositorySupport.ensureAttributeModel(context.resourceResolver(), "venia");
        assertEquals("/conf/venia/settings/dam/cfm/models/celadon-attribute", secondPath);
    }
}

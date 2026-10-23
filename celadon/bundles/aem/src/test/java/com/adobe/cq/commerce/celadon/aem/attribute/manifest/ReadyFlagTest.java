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

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReadyFlagTest {

    @Rule public final SlingContext context = new SlingContext();

    @Test
    public void absentMeansNotReady() {
        context.create().resource("/content/dam/celadon/venia/jcr:content");
        assertFalse(ReadyFlag.isReady(context.resourceResolver(), "venia"));
    }

    @Test
    public void setTrueIsReady() throws Exception {
        context.create().resource("/content/dam/celadon/venia/jcr:content");
        ReadyFlag.set(context.resourceResolver(), "venia", true);
        assertTrue(ReadyFlag.isReady(context.resourceResolver(), "venia"));
    }

    @Test
    public void setFalseIsNotReady() throws Exception {
        context.create().resource("/content/dam/celadon/venia/jcr:content");
        ReadyFlag.set(context.resourceResolver(), "venia", true);
        ReadyFlag.set(context.resourceResolver(), "venia", false);
        assertFalse(ReadyFlag.isReady(context.resourceResolver(), "venia"));
    }

    @Test
    public void missingCatalogRootIsNotReady() {
        assertFalse(ReadyFlag.isReady(context.resourceResolver(), "ghost"));
    }
}

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
package com.adobe.cq.commerce.celadon.aem.mcp.tools;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GetProductToolTest {

    @Test
    public void projectsImageVariantsAndConfigurableOptions() {
        String q = GetProductTool.buildQuery("eqwntb");
        assertTrue("filters by sku", q.contains("sku:{eq:\"eqwntb\"}"));
        assertTrue("includes image", q.contains("image {"));
        assertTrue("includes media_gallery", q.contains("media_gallery"));
        // configurable_options + variants live on ConfigurableProduct, not ProductInterface,
        // so they must be queried through an inline fragment.
        assertTrue("uses ConfigurableProduct inline fragment", q.contains("... on ConfigurableProduct"));
        assertTrue("includes configurable_options", q.contains("configurable_options"));
        assertTrue("includes variants", q.contains("variants {"));
        assertTrue("variant attributes carry value_index", q.contains("value_index"));
    }

    @Test
    public void escapesQuotesInSku() {
        String q = GetProductTool.buildQuery("a\"b");
        assertTrue(q.contains("a\\\"b"));
    }
}

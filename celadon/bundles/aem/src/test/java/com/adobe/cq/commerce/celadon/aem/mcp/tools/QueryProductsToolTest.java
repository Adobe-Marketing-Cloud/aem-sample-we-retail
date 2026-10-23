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

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class QueryProductsToolTest {

    private JsonObject args(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test
    public void buildsPagedQueryWithNoFilter() {
        String q = QueryProductsTool.buildQuery(args("{}"));
        assertTrue(q.contains("pageSize:$pageSize,currentPage:$currentPage"));
        assertFalse(q.contains("filter:{"));
        assertTrue(q.contains("total_count"));
    }

    @Test
    public void buildsSkuFilter() {
        String q = QueryProductsTool.buildQuery(args("{\"sku\":\"ABC-1\"}"));
        assertTrue(q.contains("filter:{sku:{eq:\"ABC-1\"}}"));
    }

    @Test
    public void buildsSearchAndCategoryFilter() {
        String q = QueryProductsTool.buildQuery(args("{\"search\":\"shirt\",\"category_uid\":\"Mw==\"}"));
        assertTrue(q.contains("search:\"shirt\""));
        assertTrue(q.contains("category_uid:{eq:\"Mw==\"}"));
    }

    @Test
    public void escapesQuotesInSku() {
        String q = QueryProductsTool.buildQuery(args("{\"sku\":\"a\\\"b\"}"));
        assertTrue(q.contains("a\\\"b"));
    }
}

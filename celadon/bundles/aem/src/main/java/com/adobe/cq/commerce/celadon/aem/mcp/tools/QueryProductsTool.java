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

import com.adobe.cq.commerce.celadon.aem.authoring.CatalogAuthoringService;
import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.adobe.cq.commerce.celadon.aem.mcp.ProductReadEngine;
import com.adobe.cq.commerce.celadon.core.api.JsonSupport;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Search/list products via the existing read engine. Supports free-text search,
 * exact sku, category subtree, and paging. Richer manifest-driven attribute
 * filters are available through the GraphQL endpoint directly.
 */
@Component(service = McpTool.class)
public class QueryProductsTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Reference
    private ProductReadEngine readEngine;

    @Override
    public String name() {
        return "query_products";
    }

    @Override
    public String description() {
        return "Search or list products in a catalog. Args: search (free text), sku (exact), "
                + "category_uid (subtree), pageSize, currentPage. Omit 'catalog' for the default.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"search\":{\"type\":\"string\"},"
                + "\"sku\":{\"type\":\"string\"},"
                + "\"category_uid\":{\"type\":\"string\"},"
                + "\"pageSize\":{\"type\":\"integer\"},"
                + "\"currentPage\":{\"type\":\"integer\"}},"
                + "\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("pageSize", args.has("pageSize") ? args.get("pageSize").getAsInt() : 20);
        variables.put("currentPage", args.has("currentPage") ? args.get("currentPage").getAsInt() : 1);
        String query = buildQuery(args);
        Map<String, Object> result = readEngine.execute(resolver, catalog, query, variables);
        return JsonSupport.toJson(result);
    }

    static String buildQuery(JsonObject args) {
        StringBuilder filter = new StringBuilder();
        if (args.has("sku")) {
            filter.append("sku:{eq:\"").append(escape(args.get("sku").getAsString())).append("\"}");
        }
        if (args.has("category_uid")) {
            if (filter.length() > 0) {
                filter.append(",");
            }
            filter.append("category_uid:{eq:\"").append(escape(args.get("category_uid").getAsString())).append("\"}");
        }
        String search = args.has("search") ? escape(args.get("search").getAsString()) : "";
        StringBuilder productsArgs = new StringBuilder();
        if (!search.isEmpty()) {
            productsArgs.append("search:\"").append(search).append("\",");
        }
        if (filter.length() > 0) {
            productsArgs.append("filter:{").append(filter).append("},");
        }
        productsArgs.append("pageSize:$pageSize,currentPage:$currentPage");
        return "query Q($pageSize:Int,$currentPage:Int){products(" + productsArgs + "){"
                + "total_count items{sku name url_key price_range{minimum_price{regular_price{value currency}}}}}}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

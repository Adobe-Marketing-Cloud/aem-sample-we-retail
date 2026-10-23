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

/** Fetch a single product by SKU via the existing read engine. */
@Component(service = McpTool.class)
public class GetProductTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Reference
    private ProductReadEngine readEngine;

    @Override
    public String name() {
        return "get_product";
    }

    @Override
    public String description() {
        return "Fetch a single product by exact SKU, including image, media_gallery, "
                + "configurable_options (variant axes) and variants. Omit 'catalog' for the "
                + "default catalog.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"sku\":{\"type\":\"string\"}},"
                + "\"required\":[\"sku\"],\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        String query = buildQuery(args.get("sku").getAsString());
        Map<String, Object> result = readEngine.execute(resolver, catalog, query, new LinkedHashMap<>());
        return JsonSupport.toJson(result);
    }

    static String buildQuery(String rawSku) {
        String sku = rawSku.replace("\\", "\\\\").replace("\"", "\\\"");
        return "query Q{products(filter:{sku:{eq:\"" + sku + "\"}},pageSize:1,currentPage:1){"
                + "total_count items{"
                + "__typename sku name description { html } url_key "
                + "price_range { minimum_price { regular_price { value currency } } } "
                + "image { label url } thumbnail { label url } media_gallery { url label } "
                + "... on ConfigurableProduct { "
                + "configurable_options { label attribute_code values { value_index label uid } } "
                + "variants { attributes { code value_index label } "
                + "product { sku name image { url } "
                + "price_range { minimum_price { regular_price { value currency } } } } }"
                + " }"
                + "}}}";
    }
}

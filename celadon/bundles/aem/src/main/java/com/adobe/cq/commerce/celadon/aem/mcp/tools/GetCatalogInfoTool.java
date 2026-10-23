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
import com.adobe.cq.commerce.celadon.aem.authoring.CatalogInfo;
import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class GetCatalogInfoTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "get_catalog_info";
    }

    @Override
    public String description() {
        return "Get a catalog's state: ready flag, product/category counts, and whether "
                + "it is the catalog currently served by the GraphQL endpoint. Omit 'catalog' "
                + "to use the configured default catalog.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\",\"description\":\"Catalog name; defaults to the configured catalog\"}},"
                + "\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject arguments) {
        String catalog = arguments.has("catalog") ? arguments.get("catalog").getAsString() : authoring.defaultCatalog();
        CatalogInfo info = authoring.getCatalogInfo(resolver, catalog);
        JsonObject o = new JsonObject();
        o.addProperty("name", info.name());
        o.addProperty("ready", info.ready());
        o.addProperty("productCount", info.productCount());
        o.addProperty("categoryCount", info.categoryCount());
        o.addProperty("isDefault", info.isDefault());
        return o.toString();
    }
}

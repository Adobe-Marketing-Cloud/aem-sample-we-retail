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
import com.adobe.cq.commerce.celadon.aem.authoring.CategoryInfo;
import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class ListCategoriesTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "list_categories";
    }

    @Override
    public String description() {
        return "List a catalog's category folders as a flat, depth-first (path-ordered) tree. "
                + "Each entry has the catalog-relative 'path' (e.g. 'eq/eqwn'), its 'title', and "
                + "'productCount' (products living directly in that category, excluding nested "
                + "sub-categories). Category membership is encoded by folder path. Omit 'catalog' "
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
        JsonArray arr = new JsonArray();
        for (CategoryInfo c : authoring.listCategories(resolver, catalog)) {
            JsonObject o = new JsonObject();
            o.addProperty("path", c.path());
            o.addProperty("title", c.title());
            o.addProperty("productCount", c.productCount());
            arr.add(o);
        }
        return arr.toString();
    }
}

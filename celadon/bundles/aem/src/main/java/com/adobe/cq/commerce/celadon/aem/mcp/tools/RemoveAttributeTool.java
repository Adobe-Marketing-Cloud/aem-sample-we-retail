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
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class RemoveAttributeTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "remove_attribute";
    }

    @Override
    public String description() {
        return "Remove an attribute from the catalog manifest by code and regenerate the product "
                + "model. Complements define_attribute. Note: existing products keep any values "
                + "already written for the removed code; schema changes reach the GraphQL endpoint "
                + "only after the catalog is (re)selected via select_catalog.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"code\":{\"type\":\"string\"}},"
                + "\"required\":[\"code\"],\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) throws Exception {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        String code = ToolArgs.require(args, "code");
        authoring.removeAttribute(resolver, catalog, code);
        return "removed attribute '" + code + "' from catalog '" + catalog + "'";
    }
}

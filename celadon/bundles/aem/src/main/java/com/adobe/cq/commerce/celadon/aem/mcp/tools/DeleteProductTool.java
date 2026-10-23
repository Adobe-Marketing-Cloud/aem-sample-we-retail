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
import com.adobe.cq.commerce.celadon.aem.mcp.guard.GatedTool;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class DeleteProductTool extends GatedTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "delete_product";
    }

    @Override
    public String description() {
        return "Delete a product Content Fragment by SKU. Gated: first call returns a preview + "
                + "confirmToken; call again with the token to delete.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"sku\":{\"type\":\"string\"},"
                + "\"confirmToken\":{\"type\":\"string\"}},"
                + "\"required\":[\"sku\"],\"additionalProperties\":false}";
    }

    @Override
    protected String catalogOf(JsonObject args) {
        return args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
    }

    @Override
    protected String preview(ResourceResolver resolver, JsonObject args) {
        return "Will DELETE product '" + ToolArgs.require(args, "sku") + "' from catalog '"
                + catalogOf(args) + "'. This removes the Content Fragment and all its variations.";
    }

    @Override
    protected String execute(ResourceResolver resolver, JsonObject args) throws Exception {
        String sku = ToolArgs.require(args, "sku");
        authoring.deleteProduct(resolver, catalogOf(args), sku);
        return "deleted product '" + sku + "'";
    }
}

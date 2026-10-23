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
public class MoveProductTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "move_product";
    }

    @Override
    public String description() {
        return "Move a product (by SKU) to a different PRIMARY category. This re-paths the product CF "
                + "so its folder-based primary category changes; additional category memberships are "
                + "preserved. 'targetCategory' is a catalog-relative path (e.g. 'men/shirts'). To add a "
                + "membership without moving, use add_product_to_category; to repoint the primary among "
                + "existing memberships, use set_primary_category.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"sku\":{\"type\":\"string\"},"
                + "\"targetCategory\":{\"type\":\"string\",\"description\":\"catalog-relative path\"}},"
                + "\"required\":[\"sku\",\"targetCategory\"],\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) throws Exception {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        String sku = ToolArgs.require(args, "sku");
        String target = ToolArgs.require(args, "targetCategory");
        authoring.moveProduct(resolver, catalog, sku, target);
        return "moved '" + sku + "' to '" + target + "'";
    }
}

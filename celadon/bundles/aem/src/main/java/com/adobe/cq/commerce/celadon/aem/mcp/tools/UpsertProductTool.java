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
import com.adobe.cq.commerce.celadon.aem.authoring.ProductInput;
import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class UpsertProductTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "upsert_product";
    }

    @Override
    public String description() {
        return "Create or update a product in a category. Writes master-scope values only "
                + "(PRODUCT/BOTH attributes); per-variant values go through set_product_variants. "
                + "attributeValues keys must be manifest attribute codes.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"category\":{\"type\":\"string\",\"description\":\"catalog-relative category path\"},"
                + "\"sku\":{\"type\":\"string\"},"
                + "\"name\":{\"type\":\"string\"},"
                + "\"description\":{\"type\":\"string\"},"
                + "\"attributeValues\":{\"type\":\"object\"}},"
                + "\"required\":[\"category\",\"sku\"],\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) throws Exception {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        String category = ToolArgs.require(args, "category");
        ProductInput product = new ProductInput(
                ToolArgs.require(args, "sku"),
                ToolArgs.str(args, "name"),
                ToolArgs.str(args, "description"),
                ToolArgs.valueMap(args, "attributeValues"));
        authoring.upsertProduct(resolver, catalog, category, product);
        return "upserted product '" + product.sku() + "' under '" + category + "'";
    }
}

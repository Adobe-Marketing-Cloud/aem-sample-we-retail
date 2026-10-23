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
import com.adobe.cq.commerce.celadon.aem.authoring.VariantInput;
import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class SetProductVariantsTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "set_product_variants";
    }

    @Override
    public String description() {
        return "Create/update named CF variations on a product. Each variant: {variantId, "
                + "axisValues{}, overrides{}, imageRef?, sku?}. Master PRODUCT/BOTH values are "
                + "materialized onto each variant; a variant sku is generated (<masterSku>-<variantId>) "
                + "if omitted. Only VARIANT/BOTH attributes may be overridden per variant.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"sku\":{\"type\":\"string\"},"
                + "\"variants\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{"
                + "\"variantId\":{\"type\":\"string\"},"
                + "\"axisValues\":{\"type\":\"object\"},"
                + "\"overrides\":{\"type\":\"object\"},"
                + "\"imageRef\":{\"type\":\"string\"},"
                + "\"sku\":{\"type\":\"string\"}},\"required\":[\"variantId\"]}}},"
                + "\"required\":[\"sku\",\"variants\"],\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) throws Exception {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        String sku = ToolArgs.require(args, "sku");
        List<VariantInput> variants = new ArrayList<>();
        for (JsonElement el : ToolArgs.array(args, "variants")) {
            JsonObject v = el.getAsJsonObject();
            variants.add(new VariantInput(
                    ToolArgs.require(v, "variantId"),
                    ToolArgs.valueMap(v, "axisValues"),
                    ToolArgs.valueMap(v, "overrides"),
                    ToolArgs.str(v, "imageRef"),
                    ToolArgs.str(v, "sku")));
        }
        authoring.setProductVariants(resolver, catalog, sku, variants);
        return "wrote " + variants.size() + " variant(s) on '" + sku + "'";
    }
}

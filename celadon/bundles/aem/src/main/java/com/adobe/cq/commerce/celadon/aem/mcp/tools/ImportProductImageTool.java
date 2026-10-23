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
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class ImportProductImageTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "import_product_image";
    }

    @Override
    public String description() {
        return "Set a product's image (by SKU) from 'source'. A local /content DAM asset path is "
                + "referenced in place; an external http(s) URL is downloaded into the DAM co-located "
                + "with the product and that new asset path is set. Pass 'variantId' to set the image on "
                + "a named variation instead of the master. Optional 'headers' authenticate the "
                + "download. Returns the asset path set.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"sku\":{\"type\":\"string\"},"
                + "\"variantId\":{\"type\":\"string\"},"
                + "\"source\":{\"type\":\"string\",\"description\":\"local /content DAM path or external URL\"},"
                + "\"headers\":{\"type\":\"object\"}},"
                + "\"required\":[\"sku\",\"source\"],\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) throws Exception {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        String sku = ToolArgs.require(args, "sku");
        String variantId = ToolArgs.str(args, "variantId");
        String source = ToolArgs.require(args, "source");
        Map<String, String> headers = new LinkedHashMap<>();
        if (args.has("headers") && args.get("headers").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> e : args.getAsJsonObject("headers").entrySet()) {
                headers.put(e.getKey(), e.getValue().getAsString());
            }
        }
        String path = authoring.importProductImage(resolver, catalog, sku, variantId, source, headers);
        return "set image on '" + sku + (variantId == null ? "" : "/" + variantId) + "' to " + path;
    }
}

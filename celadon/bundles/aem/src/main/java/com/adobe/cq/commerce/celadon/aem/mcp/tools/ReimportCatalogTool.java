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

import com.adobe.cq.commerce.celadon.aem.CatalogImportService;
import com.adobe.cq.commerce.celadon.aem.authoring.CatalogAuthoringService;
import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.adobe.cq.commerce.celadon.aem.mcp.guard.GatedTool;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Triggers the full wipe-and-rebuild Magento import (same pipeline as
 * {@code POST /apps/celadon/catalog}). Gated because it drops all existing
 * products in the target catalog.
 */
@Component(service = McpTool.class)
public class ReimportCatalogTool extends GatedTool {

    @Reference
    private CatalogImportService importService;

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "reimport_catalog";
    }

    @Override
    public String description() {
        return "Run a full Magento import into a catalog (drops all existing products, then "
                + "re-introspects and re-imports). Args: endpoint (Magento GraphQL URL), root "
                + "(target catalog name), headers (optional auth header map). Gated (preview/confirm).";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"endpoint\":{\"type\":\"string\"},"
                + "\"root\":{\"type\":\"string\",\"description\":\"target catalog name\"},"
                + "\"headers\":{\"type\":\"object\"},"
                + "\"confirmToken\":{\"type\":\"string\"}},"
                + "\"required\":[\"endpoint\",\"root\"],\"additionalProperties\":false}";
    }

    @Override
    protected String catalogOf(JsonObject args) {
        return args.has("root") ? args.get("root").getAsString() : authoring.defaultCatalog();
    }

    @Override
    protected String preview(ResourceResolver resolver, JsonObject args) {
        return "Will DROP all products in catalog '" + ToolArgs.require(args, "root")
                + "' and re-import from " + ToolArgs.require(args, "endpoint") + ".";
    }

    @Override
    protected String execute(ResourceResolver resolver, JsonObject args) throws Exception {
        String endpoint = ToolArgs.require(args, "endpoint");
        String root = ToolArgs.require(args, "root");
        Map<String, String> headers = new LinkedHashMap<>();
        if (args.has("headers") && args.get("headers").isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> e : args.getAsJsonObject("headers").entrySet()) {
                headers.put(e.getKey(), e.getValue().getAsString());
            }
        }
        importService.importCatalog(resolver, endpoint, root, headers);
        return "reimported catalog '" + root + "' from " + endpoint;
    }
}

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
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class DefineAttributeTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "define_attribute";
    }

    @Override
    public String description() {
        return "Add or replace an attribute in the catalog manifest and regenerate the product "
                + "model. type is one of STRING,TEXT,INT,FLOAT,BOOLEAN,SELECT,MULTISELECT,DATE,PRICE,"
                + "IMAGE_URL; scope is PRODUCT, VARIANT or BOTH (BOTH = variant-overridable). "
                + "Note: schema changes are reflected in the GraphQL endpoint only after the catalog "
                + "is (re)selected via select_catalog (servlet re-activation reloads the manifest).";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"},"
                + "\"code\":{\"type\":\"string\"},"
                + "\"label\":{\"type\":\"string\"},"
                + "\"type\":{\"type\":\"string\",\"enum\":[\"STRING\",\"TEXT\",\"INT\",\"FLOAT\",\"BOOLEAN\",\"SELECT\",\"MULTISELECT\",\"DATE\",\"PRICE\",\"IMAGE_URL\"]},"
                + "\"scope\":{\"type\":\"string\",\"enum\":[\"PRODUCT\",\"VARIANT\",\"BOTH\"]},"
                + "\"filterable\":{\"type\":\"boolean\"},"
                + "\"aggregatable\":{\"type\":\"boolean\"},"
                + "\"ordering\":{\"type\":\"integer\"}},"
                + "\"required\":[\"code\",\"type\",\"scope\"],\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject args) throws Exception {
        String catalog = args.has("catalog") ? args.get("catalog").getAsString() : authoring.defaultCatalog();
        String code = ToolArgs.require(args, "code");
        String label = ToolArgs.str(args, "label");
        NormalizedType type = NormalizedType.valueOf(ToolArgs.require(args, "type"));
        AttributeScope scope = AttributeScope.valueOf(ToolArgs.require(args, "scope"));
        AttributeEntry entry = AttributeEntry.of(code, label == null ? code : label, type, scope,
                ToolArgs.bool(args, "filterable", false),
                ToolArgs.bool(args, "aggregatable", false),
                ToolArgs.integer(args, "ordering", 0));
        authoring.defineAttribute(resolver, catalog, entry);
        return "defined attribute '" + code + "' (" + type + "/" + scope + ") on catalog '" + catalog + "'";
    }
}

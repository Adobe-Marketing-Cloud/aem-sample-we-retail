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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = McpTool.class)
public class ListAttributesTool implements McpTool {

    @Reference
    private CatalogAuthoringService authoring;

    @Override
    public String name() {
        return "list_attributes";
    }

    @Override
    public String description() {
        return "List the attribute manifest for a catalog (code, label, type, scope, filterable). "
                + "These are the attributes you can set on products and filter on. Omit 'catalog' "
                + "to use the configured default catalog.";
    }

    @Override
    public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"catalog\":{\"type\":\"string\"}},\"additionalProperties\":false}";
    }

    @Override
    public String call(ResourceResolver resolver, JsonObject arguments) {
        String catalog = arguments.has("catalog") ? arguments.get("catalog").getAsString() : authoring.defaultCatalog();
        JsonArray arr = new JsonArray();
        for (AttributeEntry e : authoring.listAttributes(resolver, catalog)) {
            JsonObject o = new JsonObject();
            o.addProperty("code", e.code());
            o.addProperty("label", e.label());
            o.addProperty("type", e.type().name());
            o.addProperty("scope", e.scope().name());
            o.addProperty("filterable", e.filterable());
            o.addProperty("aggregatable", e.aggregatable());
            o.addProperty("ordering", e.ordering());
            arr.add(o);
        }
        return arr.toString();
    }
}

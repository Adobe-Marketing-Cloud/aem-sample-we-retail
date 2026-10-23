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
package com.adobe.cq.commerce.celadon.aem.mcp;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

public class McpToolRegistryTest {

    private McpTool tool(String name) {
        return new McpTool() {
            public String name() { return name; }
            public String description() { return "desc of " + name; }
            public String inputSchema() { return "{\"type\":\"object\",\"properties\":{}}"; }
            public String call(ResourceResolver r, JsonObject a) { return "ok"; }
        };
    }

    @Test
    public void listsAndLooksUpTools() {
        McpToolRegistry registry = new McpToolRegistry();
        registry.bindTool(tool("alpha"));
        registry.bindTool(tool("beta"));

        assertNotNull(registry.find("alpha"));
        assertNull(registry.find("missing"));
        String listJson = registry.toolsListJson().toString();
        assertTrue(listJson.contains("alpha"));
        assertTrue(listJson.contains("beta"));
    }

    @Test
    public void unbindRemovesTool() {
        McpToolRegistry registry = new McpToolRegistry();
        McpTool t = tool("gamma");
        registry.bindTool(t);
        registry.unbindTool(t);
        assertNull(registry.find("gamma"));
    }
}

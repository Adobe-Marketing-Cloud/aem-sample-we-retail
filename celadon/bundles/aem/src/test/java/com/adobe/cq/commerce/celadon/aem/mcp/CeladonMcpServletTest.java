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
import com.google.gson.JsonParser;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class CeladonMcpServletTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private CeladonMcpServlet servlet;

    @Before
    public void setUp() {
        McpToolRegistry registry = new McpToolRegistry();
        registry.bindTool(new McpTool() {
            public String name() { return "echo"; }
            public String description() { return "echoes msg"; }
            public String inputSchema() { return "{\"type\":\"object\"}"; }
            public String call(ResourceResolver r, JsonObject a) {
                return a.has("msg") ? a.get("msg").getAsString() : "";
            }
        });
        registry.bindTool(new McpTool() {
            public String name() { return "boom"; }
            public String description() { return "throws"; }
            public String inputSchema() { return "{\"type\":\"object\"}"; }
            public String call(ResourceResolver r, JsonObject a) throws Exception {
                throw new IllegalStateException("kaboom");
            }
        });
        servlet = new CeladonMcpServlet();
        servlet.registry = registry;
    }

    private JsonObject handle(String json) {
        return servlet.handleRpc(context.resourceResolver(), JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    public void initializeReturnsCapabilities() {
        JsonObject r = handle("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
        JsonObject result = r.getAsJsonObject("result");
        assertEquals("2025-06-18", result.get("protocolVersion").getAsString());
        assertTrue(result.getAsJsonObject("capabilities").has("tools"));
    }

    @Test
    public void pingReturnsEmptyResult() {
        JsonObject r = handle("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"ping\",\"params\":{}}");
        assertTrue(r.has("result"));
    }

    @Test
    public void toolsListEmitsRegisteredTool() {
        JsonObject r = handle("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
        assertTrue(r.getAsJsonObject("result").toString().contains("echo"));
    }

    @Test
    public void toolsCallDispatchesToTool() {
        JsonObject r = handle("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"echo\",\"arguments\":{\"msg\":\"hi\"}}}");
        JsonObject result = r.getAsJsonObject("result");
        assertFalse(result.get("isError").getAsBoolean());
        assertEquals("hi", result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString());
    }

    @Test
    public void unknownMethodReturnsJsonRpcError() {
        JsonObject r = handle("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"bogus\",\"params\":{}}");
        assertEquals(JsonRpc.METHOD_NOT_FOUND, r.getAsJsonObject("error").get("code").getAsInt());
    }

    @Test
    public void unknownToolReturnsIsErrorResult() {
        JsonObject r = handle("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"missing_tool\",\"arguments\":{}}}");
        assertTrue(r.getAsJsonObject("result").get("isError").getAsBoolean());
    }

    @Test
    public void throwingToolReturnsIsErrorResult() {
        JsonObject r = handle("{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"boom\",\"arguments\":{}}}");
        JsonObject result = r.getAsJsonObject("result");
        assertTrue(result.get("isError").getAsBoolean());
        assertTrue(result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().contains("kaboom"));
    }
}

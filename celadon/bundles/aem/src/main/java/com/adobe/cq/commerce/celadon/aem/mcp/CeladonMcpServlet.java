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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Hand-rolled MCP endpoint at {@code /apps/celadon/mcp}. Speaks JSON-RPC 2.0 over
 * HTTP POST (MCP Streamable HTTP, synchronous JSON responses — no SSE needed for a
 * tools-only server). Runs with the request's own ResourceResolver, so AEM
 * basic-auth gives run-as-user JCR permissions.
 */
@Component(
        service = javax.servlet.Servlet.class,
        property = {
                "sling.servlet.paths=/apps/celadon/mcp",
                "sling.servlet.methods=POST"
        }
)
public class CeladonMcpServlet extends SlingAllMethodsServlet {

    static final String PROTOCOL_VERSION = "2025-06-18";

    @Reference
    transient McpToolRegistry registry;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        JsonObject rpc;
        try {
            rpc = JsonParser.parseReader(request.getReader()).getAsJsonObject();
        } catch (Exception e) {
            response.getWriter().write(JsonRpc.error(null, JsonRpc.PARSE_ERROR, "parse error").toString());
            return;
        }
        if (!rpc.has("id")) {
            // JSON-RPC notification (e.g. notifications/initialized): ack, no body.
            response.setStatus(SlingHttpServletResponse.SC_ACCEPTED);
            return;
        }
        JsonObject out = handleRpc(request.getResourceResolver(), rpc);
        response.getWriter().write(out.toString());
    }

    /** Pure dispatch — unit-testable without HTTP. */
    JsonObject handleRpc(ResourceResolver resolver, JsonObject rpc) {
        JsonElement id = rpc.get("id");
        String method = rpc.has("method") ? rpc.get("method").getAsString() : "";
        JsonObject params = rpc.has("params") && rpc.get("params").isJsonObject()
                ? rpc.getAsJsonObject("params") : new JsonObject();
        switch (method) {
            case "initialize":
                return JsonRpc.result(id, initializeResult());
            case "ping":
                return JsonRpc.result(id, new JsonObject());
            case "tools/list":
                return JsonRpc.result(id, registry.toolsListJson());
            case "tools/call":
                return JsonRpc.result(id, callTool(resolver, params));
            default:
                return JsonRpc.error(id, JsonRpc.METHOD_NOT_FOUND, "unknown method: " + method);
        }
    }

    private JsonObject initializeResult() {
        JsonObject caps = new JsonObject();
        caps.add("tools", new JsonObject());
        JsonObject info = new JsonObject();
        info.addProperty("name", "celadon-authoring");
        info.addProperty("version", "1.0.0");
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", PROTOCOL_VERSION);
        result.add("capabilities", caps);
        result.add("serverInfo", info);
        return result;
    }

    private JsonObject callTool(ResourceResolver resolver, JsonObject params) {
        String name = params.has("name") ? params.get("name").getAsString() : "";
        JsonObject args = params.has("arguments") && params.get("arguments").isJsonObject()
                ? params.getAsJsonObject("arguments") : new JsonObject();
        McpTool tool = registry.find(name);
        if (tool == null) {
            return toolResult("unknown tool: " + name, true);
        }
        try {
            return toolResult(tool.call(resolver, args), false);
        } catch (Exception e) {
            return toolResult(e.getMessage() == null ? e.toString() : e.getMessage(), true);
        }
    }

    private JsonObject toolResult(String text, boolean isError) {
        JsonObject item = new JsonObject();
        item.addProperty("type", "text");
        item.addProperty("text", text);
        JsonArray content = new JsonArray();
        content.add(item);
        JsonObject result = new JsonObject();
        result.add("content", content);
        result.addProperty("isError", isError);
        return result;
    }
}

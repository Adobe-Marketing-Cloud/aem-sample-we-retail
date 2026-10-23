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
package com.adobe.cq.commerce.celadon.aem.mcp.guard;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

public class GatedToolTest {

    private GatedTool tool(StringBuilder sideEffect) {
        return new GatedTool() {
            public String name() { return "delete_product"; }
            public String description() { return "deletes"; }
            public String inputSchema() { return "{\"type\":\"object\"}"; }
            protected String catalogOf(JsonObject a) { return "demo"; }
            protected String preview(ResourceResolver r, JsonObject a) { return "would delete S1"; }
            protected String execute(ResourceResolver r, JsonObject a) { sideEffect.append("done"); return "deleted S1"; }
        };
    }

    @Test
    public void firstCallReturnsPreviewAndTokenWithoutExecuting() throws Exception {
        StringBuilder fx = new StringBuilder();
        String out = tool(fx).call(null, new JsonObject());
        JsonObject parsed = JsonParser.parseString(out).getAsJsonObject();
        assertTrue(parsed.get("preview").getAsString().contains("would delete S1"));
        assertTrue(parsed.has("confirmToken"));
        assertEquals("", fx.toString());
    }

    @Test
    public void confirmCallWithValidTokenExecutes() throws Exception {
        StringBuilder fx = new StringBuilder();
        GatedTool t = tool(fx);
        String preview = t.call(null, new JsonObject());
        String token = JsonParser.parseString(preview).getAsJsonObject().get("confirmToken").getAsString();
        JsonObject args = new JsonObject();
        args.addProperty("confirmToken", token);
        String out = t.call(null, args);
        assertEquals("deleted S1", out);
        assertEquals("done", fx.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void confirmWithBogusTokenRejected() throws Exception {
        GatedTool t = tool(new StringBuilder());
        JsonObject args = new JsonObject();
        args.addProperty("confirmToken", "not-a-real-token");
        t.call(null, args);
    }
}

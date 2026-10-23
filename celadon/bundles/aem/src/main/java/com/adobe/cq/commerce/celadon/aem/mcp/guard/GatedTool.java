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

import com.adobe.cq.commerce.celadon.aem.mcp.McpTool;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * An {@link McpTool} that requires a two-call preview&rarr;confirm handshake.
 * First call (no {@code confirmToken}) returns {@code {preview, confirmToken}}
 * without mutating. Second call (with a valid token bound to the same
 * catalog+operation+args) executes.
 */
public abstract class GatedTool implements McpTool {

    private final ConfirmToken tokens = new ConfirmToken(5 * 60_000L); // 5 minutes

    protected abstract String catalogOf(JsonObject arguments);

    protected abstract String preview(ResourceResolver resolver, JsonObject arguments) throws Exception;

    protected abstract String execute(ResourceResolver resolver, JsonObject arguments) throws Exception;

    @Override
    public final String call(ResourceResolver resolver, JsonObject arguments) throws Exception {
        String catalog = catalogOf(arguments);
        String argsForBinding = argsWithoutToken(arguments).toString();
        if (arguments.has("confirmToken") && !arguments.get("confirmToken").isJsonNull()) {
            String token = arguments.get("confirmToken").getAsString();
            if (!tokens.verify(token, catalog, name(), argsForBinding)) {
                throw new IllegalStateException(
                        "confirm token invalid or expired; call again without confirmToken to re-preview");
            }
            return execute(resolver, arguments);
        }
        String preview = preview(resolver, arguments);
        String token = tokens.issue(catalog, name(), argsForBinding);
        JsonObject out = new JsonObject();
        out.addProperty("preview", preview);
        out.addProperty("confirmToken", token);
        out.addProperty("note", "Show this preview to the user, then call again with the same arguments "
                + "plus confirmToken to execute.");
        return out.toString();
    }

    private static JsonObject argsWithoutToken(JsonObject arguments) {
        JsonObject copy = arguments.deepCopy();
        copy.remove("confirmToken");
        return copy;
    }
}

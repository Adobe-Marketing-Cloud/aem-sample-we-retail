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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Minimal JSON-RPC 2.0 helpers (Gson-based). */
public final class JsonRpc {
    private JsonRpc() {}

    public static final int PARSE_ERROR = -32700;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    public static JsonObject result(JsonElement id, JsonObject result) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        if (id != null) {
            o.add("id", id);
        }
        o.add("result", result);
        return o;
    }

    public static JsonObject error(JsonElement id, int code, String message) {
        JsonObject err = new JsonObject();
        err.addProperty("code", code);
        err.addProperty("message", message);
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        if (id != null) {
            o.add("id", id);
        }
        o.add("error", err);
        return o;
    }
}

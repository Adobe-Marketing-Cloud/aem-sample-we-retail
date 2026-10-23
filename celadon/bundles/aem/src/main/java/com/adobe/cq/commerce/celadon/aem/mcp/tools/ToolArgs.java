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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small helpers for reading MCP tool arguments out of a Gson {@link JsonObject}. */
final class ToolArgs {
    private ToolArgs() {}

    static String str(JsonObject args, String key) {
        return args.has(key) && !args.get(key).isJsonNull() ? args.get(key).getAsString() : null;
    }

    static String require(JsonObject args, String key) {
        String v = str(args, key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("missing required argument: " + key);
        }
        return v;
    }

    static boolean bool(JsonObject args, String key, boolean fallback) {
        return args.has(key) && !args.get(key).isJsonNull() ? args.get(key).getAsBoolean() : fallback;
    }

    static int integer(JsonObject args, String key, int fallback) {
        return args.has(key) && !args.get(key).isJsonNull() ? args.get(key).getAsInt() : fallback;
    }

    /** Convert a JSON object of scalar/array values to a typed value map. */
    static Map<String, Object> valueMap(JsonObject args, String key) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!args.has(key) || !args.get(key).isJsonObject()) {
            return out;
        }
        for (Map.Entry<String, JsonElement> e : args.getAsJsonObject(key).entrySet()) {
            out.put(e.getKey(), coerce(e.getValue()));
        }
        return out;
    }

    static List<String> stringList(JsonObject args, String key) {
        List<String> out = new ArrayList<>();
        if (args.has(key) && args.get(key).isJsonArray()) {
            for (JsonElement el : args.getAsJsonArray(key)) {
                out.add(el.getAsString());
            }
        }
        return out;
    }

    static JsonArray array(JsonObject args, String key) {
        return args.has(key) && args.get(key).isJsonArray() ? args.getAsJsonArray(key) : new JsonArray();
    }

    static Object coerce(JsonElement el) {
        if (el.isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (JsonElement child : el.getAsJsonArray()) {
                list.add(child.getAsString());
            }
            return list;
        }
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) {
                return p.getAsBoolean();
            }
            if (p.isNumber()) {
                String n = p.getAsString();
                return n.contains(".") ? (Object) p.getAsDouble() : (Object) p.getAsLong();
            }
            return p.getAsString();
        }
        return el.toString();
    }
}

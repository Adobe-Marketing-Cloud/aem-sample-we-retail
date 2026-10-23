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
package com.adobe.cq.commerce.celadon.core.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonSupport {
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private JsonSupport() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = GSON.fromJson(json, MAP_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    public static Map<String, Object> parseMap(Reader reader) {
        if (reader == null) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = GSON.fromJson(reader, MAP_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map(entry));
                }
            }
            return result;
        }
        return List.of();
    }

    public static boolean containsClass(Map<String, Object> entity, String className) {
        Object classes = entity.get("class");
        if (classes instanceof List<?> list) {
            return list.stream().anyMatch(className::equals);
        }
        return false;
    }

    public static boolean isProductEntity(Map<String, Object> entity) {
        if (!containsClass(entity, "assets/asset")) {
            return false;
        }
        Map<String, Object> properties = map(entity.get("properties"));
        if (!Boolean.TRUE.equals(properties.get("contentFragment"))) {
            return false;
        }
        return !map(properties.get("elements")).isEmpty();
    }
}

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
package com.adobe.cq.commerce.celadon.aem.attribute.source;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

public final class MagentoIntrospector {

    private MagentoIntrospector() {}

    private static JsonArray readItemsArray(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("data") || root.get("data").isJsonNull()) return new JsonArray();
        JsonObject data = root.getAsJsonObject("data");
        if (!data.has("customAttributeMetadata") || data.get("customAttributeMetadata").isJsonNull()) return new JsonArray();
        JsonObject meta = data.getAsJsonObject("customAttributeMetadata");
        if (!meta.has("items") || meta.get("items").isJsonNull() || !meta.get("items").isJsonArray()) return new JsonArray();
        return meta.getAsJsonArray("items");
    }

    private static final String INTROSPECTION_QUERY = """
        query CeladonIntrospect {
          customAttributeMetadata(attributes: []) {
            items { attribute_code attribute_type input_type label }
          }
        }""";

    public static IntrospectionResult introspect(String catalog, String sourceUrl,
                                                 String authorization, DiscoveryHints hints) {
        String responseBody = postGraphql(sourceUrl, authorization, INTROSPECTION_QUERY);
        return parseWithOptions(catalog, responseBody, hints);
    }

    private static String postGraphql(String sourceUrl, String authorization, String query) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            String body = "{\"query\":" + com.google.gson.JsonParser.parseString("\"" + query.replace("\"", "\\\"") + "\"").toString() + "}";
            var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(sourceUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authorization == null ? "" : authorization)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ManifestProductionException("introspection HTTP " + resp.statusCode());
            }
            return resp.body();
        } catch (java.io.IOException | InterruptedException ex) {
            throw new ManifestProductionException("introspection failed", ex);
        }
    }

    private static final List<AttributeEntry> UNIVERSAL = List.of(
            AttributeEntry.of("sku",         "SKU",         NormalizedType.STRING,    AttributeScope.BOTH,    true,  false, 0),
            AttributeEntry.of("name",        "Name",        NormalizedType.STRING,    AttributeScope.PRODUCT, true,  false, 1),
            AttributeEntry.of("description", "Description", NormalizedType.TEXT,      AttributeScope.PRODUCT, false, false, 2),
            AttributeEntry.of("price",       "Price",       NormalizedType.PRICE,     AttributeScope.BOTH,    true,  true,  3),
            AttributeEntry.of("image",       "Image",       NormalizedType.IMAGE_URL, AttributeScope.PRODUCT, false, false, 4)
    );

    private static java.util.Set<String> universalCodes() {
        return UNIVERSAL.stream().map(AttributeEntry::code).collect(java.util.stream.Collectors.toSet());
    }

    public static AttributeManifest parse(String catalog, String json, DiscoveryHints hints) {
        hints.validateRequired();
        List<AttributeEntry> entries = new ArrayList<>();
        var seen = universalCodes();
        for (AttributeEntry u : UNIVERSAL) {
            if (hints.allows(u.code())) entries.add(u);
        }
        JsonArray items = readItemsArray(json);
        int order = 100;
        for (var el : items) {
            JsonObject o = el.getAsJsonObject();
            String code = o.get("attribute_code").getAsString();
            if (!hints.allows(code)) continue;
            if (seen.contains(code)) continue;  // universal already added
            seen.add(code);
            String attrType = o.has("attribute_type") ? o.get("attribute_type").getAsString() : "String";
            String inputType = o.has("input_type") ? o.get("input_type").getAsString() : "text";
            NormalizedType type = mapType(attrType, inputType);
            entries.add(new AttributeEntry(
                    code, label(o, code), type,
                    AttributeScope.BOTH, true, type == NormalizedType.SELECT || type == NormalizedType.MULTISELECT,
                    (order += 10), null, attrType));
        }
        return new AttributeManifest(catalog, entries);
    }

    public record OptionValue(String value, String label) {}

    public record IntrospectionResult(
            AttributeManifest manifest,
            java.util.Map<String, java.util.List<OptionValue>> options
    ) {}

    public static IntrospectionResult parseWithOptions(String catalog, String json, DiscoveryHints hints) {
        var manifest = parse(catalog, json, hints);
        java.util.Map<String, java.util.List<OptionValue>> options = new java.util.LinkedHashMap<>();
        JsonArray items = readItemsArray(json);
        for (var el : items) {
            JsonObject o = el.getAsJsonObject();
            String code = o.get("attribute_code").getAsString();
            if (manifest.entryFor(code).isEmpty()) continue;
            if (!o.has("attribute_options") || o.get("attribute_options").isJsonNull()) continue;
            List<OptionValue> values = new ArrayList<>();
            for (var ov : o.getAsJsonArray("attribute_options")) {
                JsonObject vo = ov.getAsJsonObject();
                values.add(new OptionValue(vo.get("value").getAsString(), vo.get("label").getAsString()));
            }
            options.put(code, values);
        }
        return new IntrospectionResult(manifest, options);
    }

    private static String label(JsonObject o, String fallback) {
        return o.has("label") && !o.get("label").isJsonNull() ? o.get("label").getAsString() : fallback;
    }

    private static NormalizedType mapType(String attrType, String inputType) {
        if ("select".equals(inputType)) return NormalizedType.SELECT;
        if ("multiselect".equals(inputType)) return NormalizedType.MULTISELECT;
        if ("boolean".equals(inputType)) return NormalizedType.BOOLEAN;
        if ("date".equals(inputType)) return NormalizedType.DATE;
        if ("textarea".equals(inputType)) return NormalizedType.TEXT;
        return switch (attrType) {
            case "Int" -> NormalizedType.INT;
            case "Float", "Decimal" -> NormalizedType.FLOAT;
            case "Boolean" -> NormalizedType.BOOLEAN;
            case "Date" -> NormalizedType.DATE;
            default -> NormalizedType.STRING;
        };
    }
}

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class LegacyDiscovery {

    private LegacyDiscovery() {}

    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T.*");

    /**
     * Universal core attributes seeded on every legacy catalog so the resulting
     * model and GraphQL schema always carry the canonical product fields, even
     * when the source tree stores them under legacy keys ({@code jcr:title} for
     * the name, {@code summary} or {@code jcr:description} for the descriptions)
     * that discovery would otherwise skip or mis-name.
     */
    private static final List<AttributeEntry> UNIVERSAL = List.of(
            AttributeEntry.of("sku",         "SKU",         NormalizedType.STRING,    AttributeScope.BOTH,    true,  false, 0),
            AttributeEntry.of("name",        "Name",        NormalizedType.STRING,    AttributeScope.BOTH,    true,  false, 1),
            AttributeEntry.of("description", "Description", NormalizedType.TEXT,      AttributeScope.PRODUCT, false, false, 2),
            AttributeEntry.of("price",       "Price",       NormalizedType.PRICE,     AttributeScope.BOTH,    true,  true,  3),
            AttributeEntry.of("image",       "Image",       NormalizedType.IMAGE_URL, AttributeScope.PRODUCT, false, false, 4),
            AttributeEntry.of("short_description", "Short Description", NormalizedType.TEXT, AttributeScope.PRODUCT, false, false, 5)
    );

    /**
     * Discovered property names that are image-rendition/processing metadata or
     * internal bookkeeping rather than real product attributes. Skipped so the
     * generated model isn't polluted with them.
     */
    private static final Set<String> NOISE_CODES = Set.of(
            "fileReference", "height", "width", "jpegQuality",
            "unsharpMaskAmount", "unsharpMaskRadius", "unsharpMaskThreshold",
            "margin", "inventory"
    );

    public static AttributeManifest parse(String catalog, String infinityJson, DiscoveryHints hints) {
        hints.validateRequired();
        Map<String, AttributeScout> scouts = new LinkedHashMap<>();
        Set<String> declaredAxes = new java.util.LinkedHashSet<>();
        JsonObject root = JsonParser.parseString(infinityJson).getAsJsonObject();
        walk(root, scouts, hints, false, false, declaredAxes);
        markDeclaredAxes(scouts, declaredAxes, hints);
        return buildManifest(catalog, scouts);
    }

    public record DiscoveryResult(AttributeManifest manifest, Map<String, Set<String>> options, Set<String> conflicts) {}

    public static DiscoveryResult scan(String catalog, String sourceUrl,
                                       String authorization, DiscoveryHints hints) {
        String body = fetchInfinityJson(sourceUrl, authorization);
        return parseWithOptions(catalog, body, hints);
    }

    private static String fetchInfinityJson(String sourceUrl, String authorization) {
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            String url = sourceUrl.endsWith(".infinity.json") ? sourceUrl : sourceUrl + ".infinity.json";
            var b = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("Accept", "application/json")
                    .GET();
            if (authorization != null && !authorization.isBlank()) b.header("Authorization", authorization);
            var resp = client.send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ManifestProductionException("legacy fetch HTTP " + resp.statusCode());
            }
            return resp.body();
        } catch (java.io.IOException | InterruptedException ex) {
            throw new ManifestProductionException("legacy fetch failed", ex);
        }
    }

    public static DiscoveryResult parseWithOptions(String catalog, String infinityJson, DiscoveryHints hints) {
        hints.validateRequired();
        Map<String, AttributeScout> scouts = new LinkedHashMap<>();
        Set<String> declaredAxes = new java.util.LinkedHashSet<>();
        JsonObject root = JsonParser.parseString(infinityJson).getAsJsonObject();
        walk(root, scouts, hints, false, false, declaredAxes);
        markDeclaredAxes(scouts, declaredAxes, hints);
        AttributeManifest manifest = buildManifest(catalog, scouts);
        Map<String, Set<String>> options = new LinkedHashMap<>();
        for (AttributeEntry e : manifest.entries()) {
            if (e.type() == NormalizedType.SELECT || e.type() == NormalizedType.MULTISELECT) {
                options.put(e.code(), scouts.get(e.code()).distinctValues());
            }
        }
        Set<String> conflicts = new java.util.LinkedHashSet<>();
        for (var s : scouts.values()) if (s.hadConflict()) conflicts.add(s.code());
        return new DiscoveryResult(manifest, options, conflicts);
    }

    private static void walk(JsonObject node, Map<String, AttributeScout> scouts,
                             DiscoveryHints hints, boolean inProductSubtree, boolean inVariant,
                             Set<String> declaredAxes) {
        String ct = node.has("cq:commerceType") ? node.get("cq:commerceType").getAsString() : null;
        boolean thisIsProduct = "product".equals(ct);
        boolean thisIsVariant = "variant".equals(ct);
        boolean onProduct = thisIsProduct || (inProductSubtree && !inVariant && !thisIsVariant);
        boolean onVariant = thisIsVariant || inVariant;

        // The master product authoritatively names its configurable axes (e.g. [color, size]).
        // These are harvested separately from the scalar observation loop below (which skips
        // arrays) so a declared axis can override the cardinality heuristic later.
        if (thisIsProduct && node.has("cq:productVariantAxes")) {
            collectDeclaredAxes(node.get("cq:productVariantAxes"), declaredAxes);
        }

        if (onProduct || onVariant) {
            for (var e : node.entrySet()) {
                String key = e.getKey();
                JsonElement v = e.getValue();
                if (v.isJsonObject()) continue;  // child node
                if (v.isJsonArray()) continue;   // skip arrays for now
                if (!hints.allows(key)) continue;
                AttributeScout.JcrType type = jcrTypeOf(v);
                String value = v.isJsonNull() ? null : v.getAsString();
                scouts.computeIfAbsent(key, AttributeScout::new)
                        .observe(type, value, onProduct && !onVariant, onVariant);
            }
        }

        boolean nextInProduct = inProductSubtree || thisIsProduct;
        boolean nextInVariant = inVariant || thisIsVariant;
        for (var e : node.entrySet()) {
            if (e.getValue().isJsonObject()) {
                walk(e.getValue().getAsJsonObject(), scouts, hints, nextInProduct, nextInVariant, declaredAxes);
            }
        }
    }

    /**
     * Collects the axis names from a {@code cq:productVariantAxes} value. JCR multi-value
     * properties render as a JSON array, but a single-valued property may render as a bare
     * primitive; both shapes are accepted.
     */
    private static void collectDeclaredAxes(JsonElement axes, Set<String> declaredAxes) {
        if (axes.isJsonArray()) {
            for (JsonElement el : axes.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    String axis = el.getAsString().trim();
                    if (!axis.isEmpty()) declaredAxes.add(axis);
                }
            }
        } else if (axes.isJsonPrimitive()) {
            String axis = axes.getAsString().trim();
            if (!axis.isEmpty()) declaredAxes.add(axis);
        }
    }

    /**
     * Marks every scout whose code is an authoritatively declared variant axis. A declared axis
     * that passed the discovery hints and was actually observed (so it has values to enumerate)
     * is promoted to a configurable {@code SELECT} axis regardless of its cardinality ratio.
     */
    private static void markDeclaredAxes(Map<String, AttributeScout> scouts,
                                         Set<String> declaredAxes, DiscoveryHints hints) {
        for (String axis : declaredAxes) {
            if (!hints.allows(axis)) continue;
            AttributeScout scout = scouts.get(axis);
            if (scout != null) scout.markDeclaredAxis();
        }
    }

    private static AttributeScout.JcrType jcrTypeOf(JsonElement v) {
        if (v.isJsonPrimitive()) {
            JsonPrimitive p = v.getAsJsonPrimitive();
            if (p.isBoolean()) return AttributeScout.JcrType.BOOLEAN;
            if (p.isNumber()) {
                String s = p.getAsString();
                return s.contains(".") ? AttributeScout.JcrType.DOUBLE : AttributeScout.JcrType.LONG;
            }
            String s = p.getAsString();
            if (ISO_DATE.matcher(s).matches()) return AttributeScout.JcrType.DATE;
            return AttributeScout.JcrType.STRING;
        }
        return AttributeScout.JcrType.STRING;
    }

    private static AttributeManifest buildManifest(String catalog, Map<String, AttributeScout> scouts) {
        List<AttributeEntry> entries = new ArrayList<>(UNIVERSAL);
        Set<String> seen = new java.util.HashSet<>();
        for (AttributeEntry u : UNIVERSAL) {
            seen.add(u.code());
        }
        // The legacy keys 'summary' (→ description) and 'jcr:title' (→ name) are
        // covered by the universal core; don't re-emit them under their raw names.
        seen.add("summary");
        int order = 100;
        for (var s : scouts.values()) {
            String code = s.code();
            if (seen.contains(code) || NOISE_CODES.contains(code)) {
                continue;
            }
            // Discovery can pick up dotted/localized property names (e.g. "summary.ga")
            // that are not valid Content Fragment element names. Skip them.
            if (code.indexOf('.') >= 0 || code.indexOf(':') >= 0) {
                continue;
            }
            seen.add(code);
            NormalizedType t = s.inferType();
            boolean filterable = true;
            boolean aggregatable = t == NormalizedType.SELECT || t == NormalizedType.MULTISELECT
                    || t == NormalizedType.PRICE || t == NormalizedType.INT || t == NormalizedType.FLOAT;
            entries.add(new AttributeEntry(code, code, t, s.inferScope(),
                    filterable, aggregatable, order, null,
                    s.hadConflict() ? "conflict" : null));
            order += 10;
        }
        return new AttributeManifest(catalog, entries);
    }
}

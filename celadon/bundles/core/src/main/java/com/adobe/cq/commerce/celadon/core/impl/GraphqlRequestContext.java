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
package com.adobe.cq.commerce.celadon.core.impl;

import com.adobe.cq.commerce.celadon.core.api.CatalogGateway;
import com.adobe.cq.commerce.celadon.core.api.FetcherContext;
import com.adobe.cq.commerce.celadon.core.api.JsonSupport;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GraphqlRequestContext {
    private final FetcherContext fetcherContext;
    private final CatalogGateway catalogGateway;
    private final Map<String, List<Map<String, Object>>> configurableOptionsCache = new ConcurrentHashMap<>();
    // Optional cross-request snapshot cache, keyed by catalog. Loading the full
    // catalog from JCR is expensive (seconds for large catalogs), so the engine
    // shares one immutable snapshot per catalog across requests. Null in tests.
    private final Map<String, CatalogSnapshot> sharedSnapshotCache;
    private final String snapshotCacheKey;
    private volatile CatalogSnapshot snapshot;

    public GraphqlRequestContext(FetcherContext fetcherContext, CatalogGateway catalogGateway) {
        this(fetcherContext, catalogGateway, null, null);
    }

    public GraphqlRequestContext(FetcherContext fetcherContext, CatalogGateway catalogGateway,
                                 Map<String, CatalogSnapshot> sharedSnapshotCache, String snapshotCacheKey) {
        this.fetcherContext = fetcherContext;
        this.catalogGateway = catalogGateway;
        this.sharedSnapshotCache = sharedSnapshotCache;
        this.snapshotCacheKey = snapshotCacheKey;
    }

    FetcherContext fetcherContext() {
        return fetcherContext;
    }

    CatalogGateway catalogGateway() {
        return catalogGateway;
    }

    CatalogSnapshot snapshot() throws IOException, InterruptedException {
        // Cross-request cache: build the snapshot once per catalog and reuse it.
        if (sharedSnapshotCache != null && snapshotCacheKey != null) {
            CatalogSnapshot cached = sharedSnapshotCache.get(snapshotCacheKey);
            if (cached != null) {
                return cached;
            }
            synchronized (sharedSnapshotCache) {
                cached = sharedSnapshotCache.get(snapshotCacheKey);
                if (cached == null) {
                    cached = CatalogSnapshot.load(catalogGateway);
                    sharedSnapshotCache.put(snapshotCacheKey, cached);
                }
                return cached;
            }
        }
        // Fallback: per-request snapshot (no shared cache, e.g. tests).
        CatalogSnapshot value = snapshot;
        if (value == null) {
            synchronized (this) {
                value = snapshot;
                if (value == null) {
                    value = CatalogSnapshot.load(catalogGateway);
                    snapshot = value;
                }
            }
        }
        return value;
    }

    List<Map<String, Object>> configurableOptions(CatalogProduct product) throws IOException, InterruptedException {
        return configurableOptionsCache.computeIfAbsent(product.fullPath(), key -> {
            try {
                Map<String, Object> jcr = catalogGateway.getProductJcrContent(product.categoryPath(), product.nodeName());
                List<Map<String, Object>> definitions = JsonSupport.listOfMaps(jcr.get("configurableOptionDefinitions"));
                return ConfigurableOptionsParser.parseDefinitions(definitions);
            } catch (IOException | InterruptedException e) {
                return List.of();
            }
        });
    }
}

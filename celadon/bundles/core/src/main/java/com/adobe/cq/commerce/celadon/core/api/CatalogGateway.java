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

import java.io.IOException;
import java.util.Map;

/**
 * Public catalog read contract used by the Celadon GraphQL engine.
 *
 * Implementations should return Assets-API-compatible structures so the core
 * engine can stay agnostic about where catalog data came from.
 */
public interface CatalogGateway extends AutoCloseable {
    /**
     * Returns a folder listing shaped like the Assets HTTP API payload.
     */
    Map<String, Object> getListing(String relativePath) throws IOException, InterruptedException;

    /**
     * Returns folder metadata. The engine currently reads {@code jcr:title}.
     */
    Map<String, Object> getFolderJcrContent(String categoryPath) throws IOException, InterruptedException;

    /**
     * Returns product sidecar metadata. The engine currently reads
     * {@code configurableOptions}.
     */
    Map<String, Object> getProductJcrContent(String categoryPath, String productName) throws IOException, InterruptedException;

    String toAssetUrl(String imagePath);

    /**
     * Returns whether the backing catalog is in a servable state.
     *
     * <p>Catalogs are gated by the {@code celadon:ready} JCR property on the
     * catalog root's {@code jcr:content} node. The gate is closed (returns
     * {@code false}) while an import is in progress or after an import has
     * failed; the GraphQL engine responds with {@code CATALOG_NOT_READY}
     * for read requests in that state.</p>
     *
     * <p>The default returns {@code true} so test fixtures and HTTP-only
     * backends keep working without implementing the check.</p>
     */
    default boolean isCatalogReady() {
        return true;
    }

    @Override
    default void close() {
        // Default no-op so lightweight implementations do not need lifecycle code.
    }
}

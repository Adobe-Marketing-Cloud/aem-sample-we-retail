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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class CatalogGatewayFactoryTest {
    @Test
    public void shouldCreateAndClosePublicSpiGatewayPerExecution() {
        FetcherContext context = new FetcherContext("celadon/venia");
        AtomicInteger created = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        AtomicInteger listings = new AtomicInteger();
        CatalogGatewayFactory factory = () -> new PublicTestGateway(created, closed, listings);

        CeladonGraphqlEngine engine = new CeladonGraphqlEngine(context, factory);

        Map<String, Object> first = engine.execute("{ products(filter:{}) { total_count items { sku } } }", null, null);
        Map<String, Object> second = engine.execute("{ products(filter:{}) { total_count } }", null, null);

        // A gateway is still created and closed per execution...
        Assert.assertEquals(2, created.get());
        Assert.assertEquals(2, closed.get());
        // ...but the catalog snapshot is cached per catalog across executions, so
        // the listing is fetched only on the first execution (not once per call).
        Assert.assertTrue(listings.get() >= 1);
        Assert.assertEquals(0, totalCount(first));
        Assert.assertEquals(0, totalCount(second));
    }

    @Test
    public void shouldCloseProvidedGatewayAfterExecution() {
        FetcherContext context = new FetcherContext("celadon/venia");
        AtomicInteger created = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        AtomicInteger listings = new AtomicInteger();
        PublicTestGateway gateway = new PublicTestGateway(created, closed, listings);

        CeladonGraphqlEngine engine = new CeladonGraphqlEngine(context);
        Map<String, Object> response = engine.execute("{ products(filter:{}) { total_count } }", null, null, gateway);

        Assert.assertEquals(1, created.get());
        Assert.assertEquals(1, closed.get());
        Assert.assertTrue(listings.get() >= 1);
        Assert.assertEquals(0, totalCount(response));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static int totalCount(Map<String, Object> response) {
        Number totalCount = (Number) castMap(castMap(response.get("data")).get("products")).get("total_count");
        return totalCount.intValue();
    }

    private static final class PublicTestGateway implements CatalogGateway {
        private final AtomicInteger closed;
        private final AtomicInteger listings;

        private PublicTestGateway(AtomicInteger created, AtomicInteger closed, AtomicInteger listings) {
            this.closed = closed;
            this.listings = listings;
            created.incrementAndGet();
        }

        @Override
        public Map<String, Object> getListing(String relativePath) {
            listings.incrementAndGet();
            return Map.of("entities", List.of());
        }

        @Override
        public Map<String, Object> getFolderJcrContent(String categoryPath) {
            return Map.of();
        }

        @Override
        public Map<String, Object> getProductJcrContent(String categoryPath, String productName) {
            return Map.of();
        }

        @Override
        public String toAssetUrl(String imagePath) {
            return imagePath == null ? "" : imagePath;
        }

        @Override
        public void close() {
            closed.incrementAndGet();
        }
    }
}

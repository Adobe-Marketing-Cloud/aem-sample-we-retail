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
package com.adobe.cq.commerce.celadon.it;

import java.util.Map;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ProductsByPathSkuCompatibilityIT extends GraphqlITBase {
    @Test
    public void shouldResolveProductByLegacyPathSku() {
        postQuery(Queries.PRODUCTS_BY_SKU_EQ, Map.of("sku", SKU_CANDACE_PATH))
                .body("data.products.items", notNullValue())
                // The legacy path-style SKU still resolves to the canonical product...
                .body("data.products.items[0].sku", equalTo(SKU_CANDACE_DRESS))
                // ...but url_path is now null on products (Magento parity); context-aware
                // routing is driven by url_rewrites, not a static product url_path.
                .body("data.products.items[0].url_path", nullValue());
    }
}

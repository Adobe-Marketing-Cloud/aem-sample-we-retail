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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

public class ProductsSearchIT extends GraphqlITBase {
    @Test
    public void shouldSearchProductsCompact() {
        postQuery(Queries.PRODUCTS_SEARCH, Map.of("search", SEARCH_TERM, "page", 1, "pageSize", 2))
                .body("data.products.items", notNullValue())
                .body("data.products.items[0].__typename", notNullValue())
                .body("data.products.items[0].sku", notNullValue())
                .body("data.products.items[0].name", notNullValue())
                .body("data.products.items[0].url_key", notNullValue())
                .body("data.products.items[0].thumbnail.url", notNullValue());
    }

    @Test
    public void shouldSearchProductsPaginated() {
        postQuery(Queries.PRODUCTS_SEARCH, Map.of("search", SEARCH_TERM, "page", 1, "pageSize", 2))
                .body("data.products.total_count", greaterThanOrEqualTo(1))
                .body("data.products.items", notNullValue())
                .body("data.products.page_info.current_page", notNullValue())
                .body("data.products.page_info.page_size", notNullValue())
                .body("data.products.sort_fields.default", notNullValue());
    }
}

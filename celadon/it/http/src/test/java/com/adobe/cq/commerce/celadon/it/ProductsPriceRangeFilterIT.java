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

import org.junit.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class ProductsPriceRangeFilterIT extends GraphqlITBase {

    @Test
    public void shouldRestrictResultsToPriceRange() {
        postQuery("{ products(currentPage:1,pageSize:100,filter:{price:{from:\"0\",to:\"50\"}}) { total_count items { sku price_range { minimum_price { final_price { value } } } } } }")
                .body("data.products.items.price_range.minimum_price.final_price.value",
                        everyItem(lessThanOrEqualTo(50.0f)))
                .body("data.products.items.price_range.minimum_price.final_price.value",
                        everyItem(greaterThanOrEqualTo(0.0f)));
    }

    @Test
    public void shouldHonorOnlyFromBound() {
        postQuery("{ products(currentPage:1,pageSize:100,filter:{price:{from:\"100\"}}) { total_count items { sku price_range { minimum_price { final_price { value } } } } } }")
                .body("data.products.items.price_range.minimum_price.final_price.value",
                        everyItem(greaterThanOrEqualTo(100.0f)));
    }

    @Test
    public void shouldHonorOnlyToBound() {
        postQuery("{ products(currentPage:1,pageSize:100,filter:{price:{to:\"50\"}}) { total_count items { sku price_range { minimum_price { final_price { value } } } } } }")
                .body("data.products.items.price_range.minimum_price.final_price.value",
                        everyItem(lessThanOrEqualTo(50.0f)));
    }
}

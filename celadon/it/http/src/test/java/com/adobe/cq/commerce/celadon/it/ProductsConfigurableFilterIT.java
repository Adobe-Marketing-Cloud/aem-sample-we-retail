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
import static org.hamcrest.Matchers.hasItem;

public class ProductsConfigurableFilterIT extends GraphqlITBase {

    // Option value indices for the Venia catalog served by the current Magento
    // source (mcprod). fashion_color 98 = "Lilac"; fashion_size 128 = "L", 131 = "M".
    private static final int FASHION_COLOR_VALUE = 98;
    private static final String FASHION_SIZE_VALUES = "128,131";

    @Test
    public void shouldFilterByFashionColorEq() {
        postQuery("{ products(currentPage:1,pageSize:100,filter:{fashion_color:{eq:\"" + FASHION_COLOR_VALUE + "\"}}) { total_count items { sku ... on ConfigurableProduct { configurable_options { attribute_code values { value_index } } } } } }")
                .body("data.products.total_count", greaterThanOrEqualTo(1))
                .body("data.products.items.find { it.configurable_options != null }.configurable_options.find { it.attribute_code == 'fashion_color' }.values.value_index",
                        hasItem(FASHION_COLOR_VALUE));
    }

    @Test
    public void shouldFilterByFashionSizeIn() {
        postQuery("{ products(currentPage:1,pageSize:100,filter:{fashion_size:{in:\"" + FASHION_SIZE_VALUES + "\"}}) { total_count items { sku ... on ConfigurableProduct { configurable_options { attribute_code values { value_index } } } } } }")
                .body("data.products.total_count", greaterThanOrEqualTo(1));
    }

    @Test
    public void shouldNarrowResultsWhenColorFilterApplied() {
        // Unfiltered baseline must be >= the filtered count for the same search
        int unfiltered = postQuery("{ products(search:\"dress\") { total_count } }")
                .extract().path("data.products.total_count");
        int filtered = postQuery("{ products(search:\"dress\",filter:{fashion_color:{eq:\"" + FASHION_COLOR_VALUE + "\"}}) { total_count } }")
                .extract().path("data.products.total_count");
        org.junit.Assert.assertTrue(
                "filtered count (" + filtered + ") should be <= unfiltered (" + unfiltered + ")",
                filtered <= unfiltered);
    }
}

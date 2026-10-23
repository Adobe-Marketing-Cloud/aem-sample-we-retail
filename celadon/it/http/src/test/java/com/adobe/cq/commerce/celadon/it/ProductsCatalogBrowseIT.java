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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public class ProductsCatalogBrowseIT extends GraphqlITBase {
    @Test
    public void shouldBrowseCatalogWithEmptyFilter() {
        postQuery("{ products(currentPage:1,pageSize:20,filter:{}) { total_count items { __typename sku url_key url_path name small_image { url } price_range { minimum_price { final_price { value currency } } } } } }")
                .body("data.products.total_count", greaterThanOrEqualTo(1))
                .body("data.products.items", notNullValue())
                .body("data.products.items[0].__typename", notNullValue())
                .body("data.products.items[0].sku", notNullValue())
                .body("data.products.items[0].small_image.url", notNullValue());
    }

    @Test
    public void shouldBrowseCatalogWithSearchAndEmptyFilter() {
        postQuery("{ products(search:\"dress\",currentPage:1,pageSize:20,filter:{},sort:{ relevance:DESC }) { total_count items { __typename sku url_key url_path name small_image { url } } } }")
                .body("data.products.total_count", greaterThanOrEqualTo(1))
                .body("data.products.items", notNullValue());
    }

    @Test
    public void shouldReturnAggregationsWithExpectedFacets() {
        postQuery("{ products(currentPage:1,pageSize:20,filter:{}) { total_count aggregations { attribute_code count label options { count label value } } } }")
                .body("data.products.total_count", greaterThanOrEqualTo(1))
                .body("data.products.aggregations", notNullValue())
                .body("data.products.aggregations.attribute_code", hasItem("price"))
                .body("data.products.aggregations.attribute_code", hasItem("category_uid"))
                .body("data.products.aggregations.find { it.attribute_code == 'price' }.options.size()", greaterThanOrEqualTo(1))
                .body("data.products.aggregations.find { it.attribute_code == 'category_uid' }.options.size()", greaterThanOrEqualTo(1))
                .body("data.products.aggregations.find { it.attribute_code == 'price' }.options[0].count", greaterThanOrEqualTo(1));
    }
}

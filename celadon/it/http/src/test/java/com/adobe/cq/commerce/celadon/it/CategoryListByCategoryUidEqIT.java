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

import static org.hamcrest.Matchers.notNullValue;

public class CategoryListByCategoryUidEqIT extends GraphqlITBase {
    @Test
    public void shouldReturnPaginatedProducts() {
        postQuery(Queries.CATEGORY_LIST_BY_UID_EQ, Map.of("uid", CAT_VENIA_DRESSES))
                .body("data.categoryList[0].uid", notNullValue())
                .body("data.categoryList[0].url_key", notNullValue())
                .body("data.categoryList[0].url_path", notNullValue())
                .body("data.categoryList[0].products.items", notNullValue())
                .body("data.categoryList[0].products.items[0].sku", notNullValue())
                .body("data.categoryList[0].products.items[0].thumbnail.url", notNullValue());
    }
}

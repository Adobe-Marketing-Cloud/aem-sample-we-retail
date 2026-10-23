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

public class CategoryListByUrlPathEqIT extends GraphqlITBase {
    @Test
    public void shouldResolveCategoryListByUrlPathEq() {
        postQuery("query($path:String!){ categoryList(filters:{ url_path:{ eq:$path } }) { uid name url_key url_path product_count children { uid } } }",
                Map.of("path", CAT_VENIA_DRESSES))
                .body("data.categoryList[0].uid", notNullValue())
                .body("data.categoryList[0].name", notNullValue())
                .body("data.categoryList[0].url_key", notNullValue())
                .body("data.categoryList[0].url_path", notNullValue());
    }
}

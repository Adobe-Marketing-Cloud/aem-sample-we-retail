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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public class CustomAttributeMetadataIT extends GraphqlITBase {
    @Test
    public void shouldReturnAllCustomAttributeMetadata() {
        postQuery(Queries.CUSTOM_ATTRIBUTE_METADATA_ALL)
                .body("data.customAttributeMetadata.items", notNullValue());
    }

    @Test
    public void shouldFilterCustomAttributeMetadata() {
        postQuery(Queries.CUSTOM_ATTRIBUTE_METADATA_FILTERED)
                .body("data.customAttributeMetadata.items", notNullValue())
                .body("data.customAttributeMetadata.items[0].attribute_code", notNullValue())
                .body("data.customAttributeMetadata.items[0].attribute_type", notNullValue())
                .body("data.customAttributeMetadata.items[0].input_type", notNullValue());
    }

    @Test
    public void shouldExposeConfigurableAttributesAsFilterable() {
        postQuery("{ customAttributeMetadata(attributes:[{attribute_code:\"fashion_color\",entity_type:\"catalog_product\"},{attribute_code:\"fashion_size\",entity_type:\"catalog_product\"},{attribute_code:\"category_uid\",entity_type:\"catalog_product\"}]) { items { attribute_code attribute_type input_type } } }")
                .body("data.customAttributeMetadata.items.attribute_code", hasItem("fashion_color"))
                .body("data.customAttributeMetadata.items.attribute_code", hasItem("fashion_size"))
                .body("data.customAttributeMetadata.items.attribute_code", hasItem("category_uid"));
    }
}

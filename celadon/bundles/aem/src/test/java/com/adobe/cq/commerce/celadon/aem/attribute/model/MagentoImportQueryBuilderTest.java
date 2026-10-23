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
package com.adobe.cq.commerce.celadon.aem.attribute.model;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MagentoImportQueryBuilderTest {

    @Test
    public void emptyManifestProducesMinimalQuery() {
        String q = MagentoImportQueryBuilder.fromManifest(AttributeManifest.empty("venia"));
        assertTrue(q.contains("products"));
        assertTrue(q.contains("sku"));   // sku always
    }

    @Test
    public void universalsAlwaysSelected() {
        AttributeManifest m = new AttributeManifest("venia",
                List.of(AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0)));
        String q = MagentoImportQueryBuilder.fromManifest(m);
        assertTrue(q.contains("sku"));
        assertTrue(q.contains("name"));
        assertTrue(q.contains("price"));
    }

    @Test
    public void customAttributesAddedAsTopLevelFields() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                        AttributeScope.BOTH, true, false, 0),
                AttributeEntry.of("fashion_color", "Color", NormalizedType.SELECT,
                        AttributeScope.VARIANT, true, true, 20),
                AttributeEntry.of("weight", "Weight", NormalizedType.FLOAT,
                        AttributeScope.PRODUCT, false, false, 30)));
        String q = MagentoImportQueryBuilder.fromManifest(m);
        assertTrue(q.contains("fashion_color"));
        assertTrue(q.contains("weight"));
    }

    @Test
    public void priceUsesProductPriceShape() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("price", "Price", NormalizedType.PRICE,
                        AttributeScope.PRODUCT, true, true, 10)));
        String q = MagentoImportQueryBuilder.fromManifest(m);
        assertTrue(q.contains("price_range"));
        assertTrue(q.contains("regular_price"));
    }
}

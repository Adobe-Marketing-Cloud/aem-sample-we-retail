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
package com.adobe.cq.commerce.celadon.core.impl;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.Test;

public class CatalogProductTest {

    private static CatalogProduct withAdditionalCategoriesElement(Object value) {
        Map<String, Object> element = value == null ? Map.of() : Map.of("value", value);
        Map<String, Object> elements = Map.of("additionalCategories", element);
        Map<String, Object> properties = Map.of("elements", elements);
        return new CatalogProduct("man/pants/summer", "sku1", properties, 0);
    }

    @Test
    public void absentElement_returnsEmptyList() {
        Map<String, Object> properties = Map.of("elements", Map.of());
        CatalogProduct product = new CatalogProduct("man/pants/summer", "sku1", properties, 0);
        assertEquals(List.of(), product.additionalCategories());
    }

    @Test
    public void noElementsMap_returnsEmptyList() {
        CatalogProduct product = new CatalogProduct("man/pants/summer", "sku1", Map.of(), 0);
        assertEquals(List.of(), product.additionalCategories());
    }

    @Test
    public void listValue_returnedInOrder() {
        CatalogProduct product = withAdditionalCategoriesElement(List.of("sale", "outlet/shoes"));
        assertEquals(List.of("sale", "outlet/shoes"), product.additionalCategories());
    }

    @Test
    public void stringArrayValue_returnedAsList() {
        CatalogProduct product = withAdditionalCategoriesElement(new String[]{"sale", "outlet/shoes"});
        assertEquals(List.of("sale", "outlet/shoes"), product.additionalCategories());
    }

    @Test
    public void singleBareStringValue_returnedAsSingletonList() {
        // A single-element multifield can come back as a bare String.
        CatalogProduct product = withAdditionalCategoriesElement("sale");
        assertEquals(List.of("sale"), product.additionalCategories());
    }

    @Test
    public void blankEntries_areDropped() {
        CatalogProduct product = withAdditionalCategoriesElement(List.of("sale", "  ", ""));
        assertEquals(List.of("sale"), product.additionalCategories());
    }
}

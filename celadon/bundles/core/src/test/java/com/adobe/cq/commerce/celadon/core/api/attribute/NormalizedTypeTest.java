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
package com.adobe.cq.commerce.celadon.core.api.attribute;

import org.junit.Test;
import static org.junit.Assert.*;

public class NormalizedTypeTest {

    @Test
    public void hasTenValues() {
        assertEquals(10, NormalizedType.values().length);
    }

    @Test
    public void priceUsesProductPriceScalar() {
        assertEquals("ProductPrice", NormalizedType.PRICE.graphqlScalar());
    }

    @Test
    public void multiselectUsesListString() {
        assertEquals("[String]", NormalizedType.MULTISELECT.graphqlScalar());
    }

    @Test
    public void selectAndMultiselectAreTermBucketed() {
        assertEquals(AggregationStrategy.TERM, NormalizedType.SELECT.aggregationStrategy());
        assertEquals(AggregationStrategy.TERM, NormalizedType.MULTISELECT.aggregationStrategy());
    }

    @Test
    public void priceIsRangeBucketed() {
        assertEquals(AggregationStrategy.RANGE_PRICE, NormalizedType.PRICE.aggregationStrategy());
    }

    @Test
    public void textIsNotAggregatable() {
        assertEquals(AggregationStrategy.NONE, NormalizedType.TEXT.aggregationStrategy());
    }

    @Test
    public void cfFieldTypesAreCorrect() {
        assertEquals("text-single", NormalizedType.STRING.cfFieldType());
        assertEquals("text-multi", NormalizedType.TEXT.cfFieldType());
        assertEquals("number", NormalizedType.INT.cfFieldType());
        assertEquals("number", NormalizedType.FLOAT.cfFieldType());
        assertEquals("boolean", NormalizedType.BOOLEAN.cfFieldType());
        assertEquals("enumeration", NormalizedType.SELECT.cfFieldType());
        assertEquals("enumeration-multi", NormalizedType.MULTISELECT.cfFieldType());
        assertEquals("date", NormalizedType.DATE.cfFieldType());
        assertEquals("number", NormalizedType.PRICE.cfFieldType());
        assertEquals("text-single", NormalizedType.IMAGE_URL.cfFieldType());
    }
}

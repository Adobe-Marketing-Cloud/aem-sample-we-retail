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

public enum NormalizedType {
    STRING("text-single", "String", AggregationStrategy.TERM),
    TEXT("text-multi", "String", AggregationStrategy.NONE),
    INT("number", "Int", AggregationStrategy.RANGE_NUMERIC),
    FLOAT("number", "Float", AggregationStrategy.RANGE_NUMERIC),
    BOOLEAN("boolean", "Boolean", AggregationStrategy.TERM),
    SELECT("enumeration", "String", AggregationStrategy.TERM),
    MULTISELECT("enumeration-multi", "[String]", AggregationStrategy.TERM),
    DATE("date", "String", AggregationStrategy.NONE),
    PRICE("number", "ProductPrice", AggregationStrategy.RANGE_PRICE),
    IMAGE_URL("text-single", "String", AggregationStrategy.NONE);

    private final String cfFieldType;
    private final String graphqlScalar;
    private final AggregationStrategy aggregationStrategy;

    NormalizedType(String cfFieldType, String graphqlScalar, AggregationStrategy strategy) {
        this.cfFieldType = cfFieldType;
        this.graphqlScalar = graphqlScalar;
        this.aggregationStrategy = strategy;
    }

    public String cfFieldType() { return cfFieldType; }
    public String graphqlScalar() { return graphqlScalar; }
    public AggregationStrategy aggregationStrategy() { return aggregationStrategy; }
}

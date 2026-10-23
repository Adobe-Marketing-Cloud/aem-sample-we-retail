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
package com.adobe.cq.commerce.celadon.aem.authoring;

import java.util.Map;

/**
 * Master-scope product write request. {@code attributeValues} is keyed by
 * attribute code; each code must exist in the catalog manifest as a PRODUCT or
 * BOTH scoped attribute. {@code sku}, {@code name} and {@code description} are
 * written best-effort (only if the product model has a matching field).
 */
public record ProductInput(String sku, String name, String description,
                           Map<String, Object> attributeValues) {
    public ProductInput {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku is required");
        }
        attributeValues = attributeValues == null ? Map.of() : Map.copyOf(attributeValues);
    }
}

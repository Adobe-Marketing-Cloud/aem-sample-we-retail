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
 * A single product variant, written as a named Content Fragment variation on the
 * parent product. {@code variantId} is the variation name (e.g. "var-color-41").
 * {@code axisValues} and {@code overrides} are keyed by attribute code and must be
 * VARIANT- or BOTH-scoped attributes; {@code imageRef} is an optional per-variant
 * image reference.
 */
public record VariantInput(String variantId, Map<String, Object> axisValues,
                           Map<String, Object> overrides, String imageRef, String sku) {
    public VariantInput {
        if (variantId == null || variantId.isBlank()) {
            throw new IllegalArgumentException("variantId is required");
        }
        axisValues = axisValues == null ? Map.of() : Map.copyOf(axisValues);
        overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }
}

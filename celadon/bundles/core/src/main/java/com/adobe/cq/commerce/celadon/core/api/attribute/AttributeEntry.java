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

import java.util.Objects;

public record AttributeEntry(
        String code,
        String label,
        NormalizedType type,
        AttributeScope scope,
        boolean filterable,
        boolean aggregatable,
        int ordering,
        String optionDefinitionPath,
        String sourceHint
) {
    public AttributeEntry {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scope, "scope");
    }

    public static AttributeEntry of(String code, String label, NormalizedType type,
                                    AttributeScope scope, boolean filterable,
                                    boolean aggregatable, int ordering) {
        return new AttributeEntry(code, label, type, scope, filterable, aggregatable,
                ordering, null, null);
    }

    public String cfElementName() { return code; }
}

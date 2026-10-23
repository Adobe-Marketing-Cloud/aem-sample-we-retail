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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record AttributeManifest(String catalog, List<AttributeEntry> entries) {

    public AttributeManifest {
        Objects.requireNonNull(catalog, "catalog");
        entries = List.copyOf(entries);
    }

    public static AttributeManifest empty(String catalog) {
        return new AttributeManifest(catalog, List.of());
    }

    public Optional<AttributeEntry> entryFor(String code) {
        return entries.stream().filter(e -> e.code().equals(code)).findFirst();
    }

    public List<AttributeEntry> filterable() {
        return entries.stream()
                .filter(AttributeEntry::filterable)
                .sorted(Comparator.comparingInt(AttributeEntry::ordering))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<AttributeEntry> aggregatable() {
        return entries.stream()
                .filter(AttributeEntry::aggregatable)
                .sorted(Comparator.comparingInt(AttributeEntry::ordering))
                .collect(Collectors.toUnmodifiableList());
    }
}

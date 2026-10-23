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

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

public class AttributeManifestTest {

    @Test
    public void emptyManifestHasNoEntries() {
        AttributeManifest m = AttributeManifest.empty("venia");
        assertEquals("venia", m.catalog());
        assertTrue(m.entries().isEmpty());
        assertEquals(Optional.empty(), m.entryFor("anything"));
    }

    @Test
    public void entryForLookupByCode() {
        AttributeEntry sku = AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                AttributeScope.BOTH, true, false, 0);
        AttributeEntry price = AttributeEntry.of("price", "Price", NormalizedType.PRICE,
                AttributeScope.PRODUCT, true, true, 10);
        AttributeManifest m = new AttributeManifest("venia", List.of(sku, price));
        assertEquals(Optional.of(sku), m.entryFor("sku"));
        assertEquals(Optional.of(price), m.entryFor("price"));
        assertEquals(Optional.empty(), m.entryFor("missing"));
    }

    @Test
    public void filterableEntriesAreFilteredAndOrdered() {
        AttributeEntry sku = AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                AttributeScope.BOTH, true, false, 0);
        AttributeEntry text = AttributeEntry.of("desc", "Description", NormalizedType.TEXT,
                AttributeScope.PRODUCT, false, false, 5);
        AttributeEntry color = AttributeEntry.of("color", "Color", NormalizedType.SELECT,
                AttributeScope.VARIANT, true, true, 30);
        AttributeEntry size = AttributeEntry.of("size", "Size", NormalizedType.SELECT,
                AttributeScope.VARIANT, true, true, 20);
        AttributeManifest m = new AttributeManifest("venia", List.of(sku, text, color, size));
        List<AttributeEntry> filterable = m.filterable();
        assertEquals(3, filterable.size());
        assertEquals("sku", filterable.get(0).code());
        assertEquals("size", filterable.get(1).code());
        assertEquals("color", filterable.get(2).code());
    }
}

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

public class AttributeEntryTest {

    @Test
    public void minimalEntryHasNullOptionalFields() {
        AttributeEntry e = AttributeEntry.of("sku", "SKU", NormalizedType.STRING,
                AttributeScope.BOTH, true, false, 0);
        assertEquals("sku", e.code());
        assertNull(e.optionDefinitionPath());
        assertNull(e.sourceHint());
    }

    @Test
    public void fullEntryRoundTrips() {
        AttributeEntry e = new AttributeEntry("fashion_color", "Color", NormalizedType.SELECT,
                AttributeScope.VARIANT, true, true, 20,
                "/content/dam/celadon/venia/_options/fashion_color", "Int");
        assertEquals(NormalizedType.SELECT, e.type());
        assertEquals(AttributeScope.VARIANT, e.scope());
        assertTrue(e.filterable());
        assertTrue(e.aggregatable());
        assertEquals(20, e.ordering());
        assertEquals("Int", e.sourceHint());
    }

    @Test(expected = NullPointerException.class)
    public void codeIsRequired() {
        new AttributeEntry(null, "label", NormalizedType.STRING, AttributeScope.BOTH,
                true, false, 0, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void typeIsRequired() {
        new AttributeEntry("code", "label", null, AttributeScope.BOTH,
                true, false, 0, null, null);
    }
}

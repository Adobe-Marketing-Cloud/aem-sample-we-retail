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
package com.adobe.cq.commerce.celadon.aem.attribute.source;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import org.junit.Test;
import static org.junit.Assert.*;

public class AttributeScoutTest {

    @Test
    public void singleStringObservationYieldsString() {
        AttributeScout s = new AttributeScout("material");
        s.observe(AttributeScout.JcrType.STRING, "Cotton", true /*onProduct*/, false /*onVariant*/);
        assertEquals(NormalizedType.STRING, s.inferType());
        assertEquals(AttributeScope.PRODUCT, s.inferScope());
        assertFalse(s.hadConflict());
    }

    @Test
    public void longObservationsYieldInt() {
        AttributeScout s = new AttributeScout("weight");
        s.observe(AttributeScout.JcrType.LONG, "100", true, false);
        s.observe(AttributeScout.JcrType.LONG, "200", true, false);
        assertEquals(NormalizedType.INT, s.inferType());
    }

    @Test
    public void doubleYieldsFloat() {
        AttributeScout s = new AttributeScout("carat");
        s.observe(AttributeScout.JcrType.DOUBLE, "0.5", true, false);
        assertEquals(NormalizedType.FLOAT, s.inferType());
    }

    @Test
    public void booleanYieldsBoolean() {
        AttributeScout s = new AttributeScout("active");
        s.observe(AttributeScout.JcrType.BOOLEAN, "true", true, false);
        assertEquals(NormalizedType.BOOLEAN, s.inferType());
    }

    @Test
    public void dateYieldsDate() {
        AttributeScout s = new AttributeScout("release");
        s.observe(AttributeScout.JcrType.DATE, "2024-01-01", true, false);
        assertEquals(NormalizedType.DATE, s.inferType());
    }

    @Test
    public void conflictPromotesToString() {
        AttributeScout s = new AttributeScout("oddball");
        s.observe(AttributeScout.JcrType.LONG, "1", true, false);
        s.observe(AttributeScout.JcrType.STRING, "x", true, false);
        assertEquals(NormalizedType.STRING, s.inferType());
        assertTrue(s.hadConflict());
    }

    @Test
    public void scopeProductVariantBoth() {
        AttributeScout p = new AttributeScout("p");
        p.observe(AttributeScout.JcrType.STRING, "a", true, false);
        assertEquals(AttributeScope.PRODUCT, p.inferScope());

        AttributeScout v = new AttributeScout("v");
        v.observe(AttributeScout.JcrType.STRING, "a", false, true);
        assertEquals(AttributeScope.VARIANT, v.inferScope());

        AttributeScout b = new AttributeScout("b");
        b.observe(AttributeScout.JcrType.STRING, "a", true, false);
        b.observe(AttributeScout.JcrType.STRING, "b", false, true);
        assertEquals(AttributeScope.BOTH, b.inferScope());
    }

    @Test
    public void selectPromotionAtLowCardinalityOnVariant() {
        AttributeScout s = new AttributeScout("color");
        // 100 variant observations with 3 distinct values
        for (int i = 0; i < 33; i++) {
            s.observe(AttributeScout.JcrType.STRING, "Red", false, true);
            s.observe(AttributeScout.JcrType.STRING, "Green", false, true);
            s.observe(AttributeScout.JcrType.STRING, "Blue", false, true);
        }
        s.observe(AttributeScout.JcrType.STRING, "Red", false, true);
        assertEquals(NormalizedType.SELECT, s.inferType());
    }

    @Test
    public void noPromotionWithHighCardinality() {
        AttributeScout s = new AttributeScout("title");
        for (int i = 0; i < 100; i++) {
            s.observe(AttributeScout.JcrType.STRING, "title-" + i, true, false);
        }
        assertEquals(NormalizedType.STRING, s.inferType());
    }

    @Test
    public void declaredAxisForcesSelectDespiteHighCardinality() {
        AttributeScout s = new AttributeScout("color");
        // 6 variant observations, all distinct (ratio 1.0) — the cardinality heuristic alone
        // would keep this a STRING. Declaring it an authoritative axis overrides that.
        s.observe(AttributeScout.JcrType.STRING, "Purple", false, true);
        s.observe(AttributeScout.JcrType.STRING, "Blue", false, true);
        s.observe(AttributeScout.JcrType.STRING, "Water Red", false, true);
        s.observe(AttributeScout.JcrType.STRING, "Cuzco Orange", false, true);
        s.observe(AttributeScout.JcrType.STRING, "Sport Blue", false, true);
        s.observe(AttributeScout.JcrType.STRING, "Warm", false, true);
        assertEquals(NormalizedType.STRING, s.inferType());
        s.markDeclaredAxis();
        assertEquals(NormalizedType.SELECT, s.inferType());
        assertEquals(AttributeScope.VARIANT, s.inferScope());
    }

    @Test
    public void declaredAxisIsAtLeastVariantScopedWhenObservedOnProductOnly() {
        AttributeScout s = new AttributeScout("color");
        s.observe(AttributeScout.JcrType.STRING, "Purple", true /*onProduct*/, false /*onVariant*/);
        s.markDeclaredAxis();
        assertEquals(NormalizedType.SELECT, s.inferType());
        assertEquals(AttributeScope.BOTH, s.inferScope());
    }

    @Test
    public void distinctValuesReturned() {
        AttributeScout s = new AttributeScout("color");
        s.observe(AttributeScout.JcrType.STRING, "Red", false, true);
        s.observe(AttributeScout.JcrType.STRING, "Green", false, true);
        s.observe(AttributeScout.JcrType.STRING, "Red", false, true);
        assertEquals(java.util.Set.of("Red", "Green"), s.distinctValues());
    }
}

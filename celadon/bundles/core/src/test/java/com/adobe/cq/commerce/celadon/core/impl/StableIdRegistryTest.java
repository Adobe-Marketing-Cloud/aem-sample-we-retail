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

import org.junit.Assert;
import org.junit.Test;

public class StableIdRegistryTest {
    @Test
    public void shouldReturnZeroForNullOrBlankKeys() {
        StableIdRegistry registry = new StableIdRegistry();

        Assert.assertEquals(0, registry.toStableNumericId(null));
        Assert.assertEquals(0, registry.toStableNumericId(" "));
    }

    @Test
    public void shouldBeDeterministicAndPositive() {
        StableIdRegistry registry = new StableIdRegistry();

        int first = registry.toStableNumericId("venia-bottoms/venia-skirts");
        int second = registry.toStableNumericId("venia-bottoms/venia-skirts");

        Assert.assertTrue(first > 0);
        Assert.assertEquals(first, second);
    }

    @Test
    public void shouldKeepRootStable() {
        StableIdRegistry registry = new StableIdRegistry();

        Assert.assertEquals(registry.toStableNumericId("/"), registry.toStableNumericId("/"));
    }
}

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
package com.adobe.cq.commerce.celadon.core.api;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import com.adobe.cq.commerce.celadon.core.impl.attribute.SchemaAddendumBuilder;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class CeladonGraphqlEngineAddendumTest {

    @Test
    public void emptyManifestYieldsBlankAddendum() {
        assertEquals("", SchemaAddendumBuilder.fromManifest(AttributeManifest.empty("venia")));
    }

    @Test
    public void singleAttributeShowsUpInAddendum() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("metal_type", "Metal", NormalizedType.SELECT,
                        AttributeScope.PRODUCT, true, true, 100)));
        String addendum = SchemaAddendumBuilder.fromManifest(m);
        assertTrue(addendum.contains("metal_type: FilterEqualTypeInput"));
    }
}

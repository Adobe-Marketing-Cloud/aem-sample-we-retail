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
package common.mock;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockProductTest {

    @Test
    public void testVariantFallsBackToBaseProductPropertiesAndImage() {
        Resource baseProduct = mock(Resource.class);
        Resource variant = mock(Resource.class);
        Resource image = mock(Resource.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        when(baseProduct.getValueMap()).thenReturn(valueMap("identifier", "eqrusufle"));
        when(baseProduct.getChild("image")).thenReturn(image);
        when(variant.getValueMap()).thenReturn(valueMap("cq:commerceType", "variant", "size", "9"));
        when(variant.getParent()).thenReturn(baseProduct);
        when(variant.getChild("image")).thenReturn(null);
        when(image.getResourceResolver()).thenReturn(resourceResolver);

        MockProduct product = new MockProduct(variant);

        assertEquals("eqrusufle", product.getProperty("identifier", String.class));
        assertEquals("eqrusufle-9", product.getSKU());
        assertNotNull(product.getImage());
    }

    private ValueMapDecorator valueMap(String... entries) {
        Map<String, Object> values = new HashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) {
            values.put(entries[i], entries[i + 1]);
        }
        return new ValueMapDecorator(values);
    }
}

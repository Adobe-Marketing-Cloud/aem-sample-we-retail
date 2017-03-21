/*
 *   Copyright 2016 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package we.retail.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import com.adobe.cq.commerce.api.CommerceConstants;

public class WeRetailCommerceServiceImplTest {

    @Test
    public void testMethods() throws Exception {
        Resource mockResource = mock(Resource.class);
        WeRetailCommerceServiceImpl service = new WeRetailCommerceServiceImpl(null, mockResource);

        assertTrue(service.isAvailable(CommerceConstants.SERVICE_COMMERCE));
        assertFalse(service.isAvailable(null));

        assertEquals(CommerceConstants.OPEN_ORDERS_PREDICATE, service.getOrderPredicates().get(0));
        assertEquals("*", service.getCountries().get(0));
        assertEquals("*", service.getCreditCardTypes().get(0));
    }
}

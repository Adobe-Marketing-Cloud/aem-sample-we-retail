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
package we.retail.core.model;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.common.VendorJcrPlacedOrder;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;
import common.AppAemContext;
import common.mock.MockCommerceSession;
import common.mock.MockDefaultJcrPlacedOrder;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import we.retail.core.model.ShoppingCartModel.CartEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class VendorOrderModelTest extends OrderModelTest {

    @Mock
    private VendorJcrPlacedOrder vendorJcrPlacedOrder;

    private VendorOrderModel orderModel;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockSlingHttpServletRequest request = context.request();
        request.setAttribute("cq.commerce.vendorplacedorder", vendorJcrPlacedOrder);

        orderModel = request.adaptTo(VendorOrderModel.class);
    }

    @Test
    public void testOrder() throws Exception {
        assertEquals("Order Title", orderModel.getTitle());
        assertEquals(false, orderModel.showPrice());
    }

    @Test
    public void testOrderEntries() throws CommerceException {
        List<CartEntry> entries = orderModel.getEntries();
        assertEquals(0, entries.size());
    }
}

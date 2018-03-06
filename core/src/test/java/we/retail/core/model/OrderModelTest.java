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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;

import common.AppAemContext;
import common.mock.MockCommerceSession;
import common.mock.MockDefaultJcrPlacedOrder;
import io.wcm.testing.mock.aem.junit.AemContext;
import we.retail.core.model.ShoppingCartModel.CartEntry;

public class OrderModelTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    private OrderModel orderModel;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(Constants.TEST_ORDER_PAGE);
        context.currentResource(Constants.TEST_ORDER_RESOURCE);

        // The OrderModel expects the orderId as a request parameter
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Constants.ORDER_ID, Constants.TEST_ORDER_ID);
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(parameters);

        // This sets the instance attributes injected in OrderModel (and ShoppingCartModel) with @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.CURRENT_PAGE, page);
        slingBindings.put(WCMBindings.WCM_MODE, new SightlyWCMMode(request));

        // This creates the mocked order defined in src/test/resources/sample-order.json
        Resource orderResource = context.resourceResolver().getResource(Constants.TEST_ORDER_RESOURCE);
        MockDefaultJcrPlacedOrder mockDefaultJcrPlacedOrder = new MockDefaultJcrPlacedOrder(null, Constants.TEST_ORDER_ID, orderResource);

        // This registers the mocked order in the current session so that it can be retrieved 
        // in OrderModel by commerceSession.getPlacedOrder(String)
        CommerceService commerceService = page.getContentResource().adaptTo(CommerceService.class);
        MockCommerceSession commerceSession = (MockCommerceSession) commerceService.login(request, context.response());
        commerceSession.registerPlacedOrder(Constants.TEST_ORDER_ID, mockDefaultJcrPlacedOrder);

        orderModel = request.adaptTo(OrderModel.class);
    }

    @Test
    public void testOrder() throws Exception {
        assertEquals(Constants.TEST_ORDER_ID, orderModel.getOrderId());
        assertEquals(Constants.SUB_TOTAL, orderModel.getSubTotal());
        assertEquals(Constants.SHIPPING_TOTAL, orderModel.getShippingTotal());
        assertEquals(Constants.TAX_TOTAL, orderModel.getTaxTotal());
        assertEquals(Constants.TOTAL, orderModel.getTotal());
        assertEquals(Constants.BILLING_ADDRESS, orderModel.getBillingAddress());
        assertEquals(Constants.SHIPPING_ADDRESS, orderModel.getShippingAddress());
        assertEquals(Constants.ORDER_STATUS, orderModel.getOrderStatus());
        assertNotNull(orderModel.getOrderDate().getTime());
    }

    @Test
    public void testOrderEntries() throws CommerceException {
        List<CartEntry> entries = orderModel.getEntries();
        assertEquals(Constants.ENTRIES_SIZE, entries.size());

        CartEntry entry0 = entries.get(0);
        assertEquals(Constants.ENTRY_0_PATH, entry0.getProduct().getPath());
        assertEquals(Constants.ENTRY_0_PRICE, entry0.getPrice());
        assertEquals(Constants.ENTRY_0_QUANTITY, entry0.getEntry().getQuantity());
        assertEquals(Constants.ENTRY_0_TOTAL_PRICE, entry0.getTotalPrice());

        CartEntry entry1 = entries.get(1);
        assertEquals(Constants.ENTRY_1_PATH, entry1.getProduct().getPath());
        assertEquals(Constants.ENTRY_1_PRICE, entry1.getPrice());
        assertEquals(Constants.ENTRY_1_QUANTITY, entry1.getEntry().getQuantity());
        assertEquals(Constants.ENTRY_1_TOTAL_PRICE, entry1.getTotalPrice());
    }
}

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

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;
import common.AppAemContext;
import common.mock.MockCommerceSession;
import common.mock.MockDefaultJcrPlacedOrder;
import io.wcm.testing.mock.aem.junit.AemContext;
import we.retail.core.model.ShoppingCartModel.CartEntry;

import static org.junit.Assert.assertEquals;

public class ShoppingCartModelTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    private ShoppingCartModel shoppingCartModel;
    private ShoppingCartPricesModel shoppingCartPricesModel;

    private MockCommerceSession commerceSession;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(Constants.TEST_ORDER_PAGE);
        context.currentResource(Constants.TEST_ORDER_RESOURCE);

        // This sets the page attribute injected in the models with @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.CURRENT_PAGE, page);

        // To mock the shopping cart, the MockCommerceSession uses the first order registered in the session
        // We hence register the mocked order defined in src/test/resources/sample-order.json in the session
        Resource orderResource = context.resourceResolver().getResource(Constants.TEST_ORDER_RESOURCE);
        MockDefaultJcrPlacedOrder mockDefaultJcrPlacedOrder = new MockDefaultJcrPlacedOrder(null, Constants.TEST_ORDER_ID, orderResource);

        MockSlingHttpServletRequest request = context.request();
        CommerceService commerceService = page.getContentResource().adaptTo(CommerceService.class);
        commerceSession = (MockCommerceSession) commerceService.login(request, context.response());
        commerceSession.registerPlacedOrder(Constants.TEST_ORDER_ID, mockDefaultJcrPlacedOrder);

        shoppingCartModel = request.adaptTo(ShoppingCartModel.class);
        shoppingCartPricesModel = request.adaptTo(ShoppingCartPricesModel.class);
    }

    @Test
    public void testCartEntries() throws CommerceException {
        List<CartEntry> entries = shoppingCartModel.getEntries();
        assertEquals(Constants.ENTRIES_SIZE, entries.size());

        CartEntry entry0 = entries.get(0);
        assertEquals(Constants.ENTRY_0_PATH, entry0.getProduct().getPath());
        assertEquals(Constants.ENTRY_0_SKU, entry0.getProduct().getSku());
        assertEquals(Constants.ENTRY_0_PRICE, entry0.getPrice());
        assertEquals(Constants.ENTRY_0_QUANTITY, entry0.getEntry().getQuantity());
        assertEquals(Constants.ENTRY_0_TOTAL_PRICE, entry0.getTotalPrice());

        CartEntry entry1 = entries.get(1);
        assertEquals(Constants.ENTRY_1_PATH, entry1.getProduct().getPath());
        assertEquals(Constants.ENTRY_1_SKU, entry1.getProduct().getSku());
        assertEquals(Constants.ENTRY_1_PRICE, entry1.getPrice());
        assertEquals(Constants.ENTRY_1_QUANTITY, entry1.getEntry().getQuantity());
        assertEquals(Constants.ENTRY_1_TOTAL_PRICE, entry1.getTotalPrice());
    }

    @Test
    public void testCartPrices() throws Exception {
        assertEquals(Constants.SUB_TOTAL, shoppingCartPricesModel.getSubTotal());
        assertEquals(Constants.SHIPPING_TOTAL, shoppingCartPricesModel.getShippingTotal());
        assertEquals(Constants.TAX_TOTAL, shoppingCartPricesModel.getTaxTotal());
        assertEquals(Constants.TOTAL, shoppingCartPricesModel.getTotal());
    }

    @After
    public void tearDown() {
        commerceSession.clearCart();
        commerceSession.clearPlacedOrders();
    }
}

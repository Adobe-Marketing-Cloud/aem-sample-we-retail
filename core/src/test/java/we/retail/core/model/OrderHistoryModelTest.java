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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.common.PriceFilter;
import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.Page;

import common.AppAemContext;
import common.mock.MockCommerceSession;
import common.mock.MockDefaultJcrPlacedOrder;
import io.wcm.testing.mock.aem.junit.AemContext;
import we.retail.core.model.OrderHistoryModel.PlacedOrderWrapper;

public class OrderHistoryModelTest {

    private static final String DUMMY_ORDER_ID = "dummy-order-id";
    private static final String DUMMY_ORDER_DATE = "Sun Dec 11 2016 16:42:15 GMT+0100";
    private static final String DUMMY_ORDER_LIST_ID_INDEX = "0";

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private SimpleDateFormat dateParser = new SimpleDateFormat("EEE MMM d yyyy HH:mm:ss", Locale.US);

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    private OrderHistoryModel orderHistoryModel;
    private MockCommerceSession commerceSession;

    @Before
    public void setUp() throws Exception {
        Page page = context.currentPage(Constants.TEST_ORDER_PAGE);
        context.currentResource(Constants.TEST_ORDER_RESOURCE);

        // This sets the page attribute injected in OrderHistoryModel with @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.CURRENT_PAGE, page);

        MockSlingHttpServletRequest request = context.request();
        CommerceService commerceService = page.getContentResource().adaptTo(CommerceService.class);
        commerceSession = (MockCommerceSession) commerceService.login(request, context.response());

        // We will use the mocked order defined in src/test/resources/sample-order.json twice
        // in order to have 2 orders in the order history: the "dummy" order is a copy of the mocked
        // order for which we just change the order-id and the date of the order
        Resource orderResource = context.resourceResolver().getResource(Constants.TEST_ORDER_RESOURCE);

        // The dummy order is inserted first but should appear second in the test (see below)
        MockDefaultJcrPlacedOrder dummyOrder = new MockDefaultJcrPlacedOrder(null, DUMMY_ORDER_ID, orderResource);
        dummyOrder.setOrderId(DUMMY_ORDER_ID);
        Calendar cal = (Calendar) orderResource.getValueMap().get("orderPlaced");
        dummyOrder.setOrderPlacedDate(new Date(cal.getTimeInMillis()-86400000));
        commerceSession.registerPlacedOrder(DUMMY_ORDER_ID, dummyOrder);

        // This is the "original" mocked order
        MockDefaultJcrPlacedOrder mockDefaultJcrPlacedOrder = new MockDefaultJcrPlacedOrder(null, Constants.TEST_ORDER_ID, orderResource);
        commerceSession.registerPlacedOrder(Constants.TEST_ORDER_ID, mockDefaultJcrPlacedOrder);

        orderHistoryModel = request.adaptTo(OrderHistoryModel.class);
    }

    @Test
    public void testOrderHistory() throws Exception {
        List<PlacedOrderWrapper> orders = orderHistoryModel.getOrders();
        assertEquals(Constants.ENTRIES_SIZE, orders.size());

        // The dummy order should always appear second in the list because of the descending sorting by date
        // done in OrderHistoryModel
        Date date0 = ((Calendar) orders.get(0).getOrder().get(Constants.ORDER_PLACED)).getTime();
        Date date1 = (Date) orders.get(1).getOrder().get(Constants.ORDER_PLACED);
        assertEquals(1,date0.compareTo(date1));
        assertEquals(Constants.TEST_ORDER_ID, orders.get(0).getOrderId());
        assertEquals(DUMMY_ORDER_ID, orders.get(1).getOrderId());

        // The list id index is also descending, so that the last order has the highest index
        assertEquals(sdf.format(dateParser.parse(Constants.ORDER_DATE)) + Constants.ORDER_LIST_ID_INDEX, orders.get(0).getListOrderId());
        assertEquals(sdf.format(dateParser.parse(DUMMY_ORDER_DATE)) + DUMMY_ORDER_LIST_ID_INDEX, orders.get(1).getListOrderId());
    }

    @Test
    public void testOrder() throws Exception {
        List<PlacedOrderWrapper> orders = orderHistoryModel.getOrders();
        PlacedOrderWrapper order = orders.get(0);

        assertEquals(Constants.TEST_ORDER_ID, order.getOrderId());
        assertEquals(sdf.format(dateParser.parse(Constants.ORDER_DATE)) + Constants.ORDER_LIST_ID_INDEX, order.getListOrderId());
        assertEquals(Constants.ORDER_STATUS, order.getOrder().get("orderStatus"));
        assertEquals(Constants.TOTAL, order.getCartPrice(new PriceFilter("TOTAL")));
        assertEquals(Constants.ENTRIES_SIZE, order.getCartEntries().size());
        assertEquals(0, order.getPromotions().size());
        assertEquals(0, order.getVoucherInfos().size());
    }

    @After
    public void tearDown() {
        commerceSession.clearCart();
        commerceSession.clearPlacedOrders();
    }
}

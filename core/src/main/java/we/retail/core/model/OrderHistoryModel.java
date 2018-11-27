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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.Predicate;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.CommerceSession.CartEntry;
import com.adobe.cq.commerce.api.PlacedOrder;
import com.adobe.cq.commerce.api.PlacedOrderResult;
import com.adobe.cq.commerce.api.PriceInfo;
import com.adobe.cq.commerce.api.promotion.PromotionInfo;
import com.adobe.cq.commerce.api.promotion.VoucherInfo;
import com.adobe.granite.security.user.UserProperties;
import com.day.cq.personalization.UserPropertiesUtil;
import com.day.cq.wcm.api.Page;

import we.retail.core.WeRetailConstants;

@Model(adaptables = SlingHttpServletRequest.class)
public class OrderHistoryModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderHistoryModel.class);

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private SlingHttpServletResponse response;

    @ScriptVariable
    private Page currentPage;

    private List<PlacedOrderWrapper> wrappedOrders;

    @PostConstruct
    private void initModel() {
        try {
            CommerceService commerceService = currentPage.getContentResource().adaptTo(CommerceService.class);
            if (commerceService != null) {
                CommerceSession commerceSession = commerceService.login(request, response);
                PlacedOrderResult orderResult = commerceSession.getPlacedOrders(null, 0, 0, null);
                List<PlacedOrder> orders = orderResult.getOrders();
                Collections.sort(orders, orderComparator);
                wrappedOrders = convert(orders);
            }
        } catch (CommerceException e) {
            LOGGER.error("Failed to initialize sling model", e);
        }
    }

    // Sorting by date, descending
    private Comparator<PlacedOrder> orderComparator = new Comparator<PlacedOrder>() {

        @Override
        public int compare(PlacedOrder o1, PlacedOrder o2) {
            Object p1, p2;
            try {
                p1 = o1.getOrder().get(WeRetailConstants.ORDER_PLACED);
                p2 = o2.getOrder().get(WeRetailConstants.ORDER_PLACED);
            } catch (CommerceException e) {
                return 0;
            }

            if (p1 == null || p2 == null) {
                return p1 == null ? (p2 == null ? 0 : 1) : -1;
            }

            Date d1 = null, d2 = null;
            if (p1 instanceof Calendar) {
                d1 = ((Calendar) p1).getTime();
            } else if (p1 instanceof Date) {
                d1 = (Date) p1;
            }

            if (p2 instanceof Calendar) {
                d2 = ((Calendar) p2).getTime();
            } else if (p2 instanceof Date) {
                d2 = (Date) p2;
            }

            if (d1 != null && d2 != null) {
                return d2.compareTo(d1);
            } else {
                return d1 != null ? -1 : (d2 != null ? 1 : 0);
            }
        }
    };

    public boolean isAnonymous() {
        final UserProperties userProperties = request.adaptTo(UserProperties.class);
        return userProperties == null || UserPropertiesUtil.isAnonymous(userProperties);
    }

    public List<PlacedOrderWrapper> getOrders() {
        return Collections.unmodifiableList(wrappedOrders);
    }

    public boolean isEmpty() {
        return wrappedOrders == null || wrappedOrders.isEmpty();
    }

    private List<PlacedOrderWrapper> convert(List<PlacedOrder> orders) {
        List<PlacedOrderWrapper> wrappedOrders = new ArrayList<PlacedOrderWrapper>();
        for (int i = 0, l = orders.size(); i < l; i++) {
            // The list id index is descending, so that the last order (in time) has the highest index
            // This ensures that an old order keeps the same id when a new order is added 
            wrappedOrders.add(new PlacedOrderWrapper(orders.get(i), l - 1 - i));
        }
        return wrappedOrders;
    }

    public class PlacedOrderWrapper implements PlacedOrder {

        private PlacedOrder placedOrder;
        private int index;

        public PlacedOrderWrapper(PlacedOrder placedOrder, int index) {
            this.placedOrder = placedOrder;
            this.index = index;
        }

        public String getListOrderId() throws CommerceException {
            Date date = null;
            Object obj = placedOrder.getOrder().get(WeRetailConstants.ORDER_PLACED);
            if (obj instanceof Date) {
                date = (Date) obj;
            } else if (obj instanceof Calendar) {
                date = ((Calendar) obj).getTime();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            return sdf.format(date) + index;
        }

        @Override
        public String getOrderId() throws CommerceException {
            return placedOrder.getOrderId();
        }

        @Override
        public Map<String, Object> getOrder() throws CommerceException {
            return placedOrder.getOrder();
        }

        @Override
        public List<PriceInfo> getCartPriceInfo(Predicate filter) throws CommerceException {
            return placedOrder.getCartPriceInfo(filter);
        }

        @Override
        public String getCartPrice(Predicate filter) throws CommerceException {
            return placedOrder.getCartPrice(filter);
        }

        @Override
        public List<CartEntry> getCartEntries() throws CommerceException {
            return placedOrder.getCartEntries();
        }

        @Override
        public List<PromotionInfo> getPromotions() throws CommerceException {
            return placedOrder.getPromotions();
        }

        @Override
        public List<VoucherInfo> getVoucherInfos() throws CommerceException {
            return placedOrder.getVoucherInfos();
        }
    }
}

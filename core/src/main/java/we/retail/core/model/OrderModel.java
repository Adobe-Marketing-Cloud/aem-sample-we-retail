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

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.PlacedOrder;
import com.adobe.cq.commerce.common.PriceFilter;
import com.adobe.cq.sightly.SightlyWCMMode;
import we.retail.core.WeRetailConstants;

@Model(adaptables = SlingHttpServletRequest.class)
public class OrderModel extends ShoppingCartModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderModel.class);
    private static final String ORDER_ID = "orderId";
    private static final String ORDER_PLACED = "orderPlaced";
    private static final String ORDER_STATUS = "orderStatus";
    private static final String CART_SUB_TOTAL = "CART";
    private static final String ORDER_SHIPPING = "SHIPPING";
    private static final String ORDER_TOTAL_TAX = "TAX";
    private static final String ORDER_TOTAL_PRICE = "TOTAL";
    private static final String BILLING_PREFIX = CommerceConstants.BILLING_ADDRESS_PREDICATE + ".";
    private static final String SHIPPING_PREFIX = CommerceConstants.SHIPPING_ADDRESS_PREDICATE + ".";

    @ScriptVariable(name = "wcmmode")
    private SightlyWCMMode wcmMode;

    private String orderId;
    protected PlacedOrder placedOrder;
    protected Map<String, Object> orderDetails;

    @PostConstruct
    private void initModel() throws CommerceException {
        createCommerceSession();
        populateOrder();
        populatePromotions();
        populateCartEntries();
    }

    protected void populateOrder() throws CommerceException {
        boolean isEditMode = wcmMode.isEdit();
        orderId = request.getParameter(ORDER_ID);

        if (!isEditMode && StringUtils.isBlank(orderId)) {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } catch (IOException e) {
                LOGGER.error("Failed to set HTTP error response code", e);
            }
        }

        if (StringUtils.isNotBlank(orderId)) {
            placedOrder = commerceSession.getPlacedOrder(orderId);
            if (!isEditMode && placedOrder.getOrderId() == null) {
                try {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                } catch (IOException e) {
                    LOGGER.error("Failed to set HTTP error response code", e);
                }
            }
            orderDetails = placedOrder.getOrder();
            allPromotions = placedOrder.getPromotions();
        }
    }

    @Override
    protected void populateCartEntries() throws CommerceException {
        entries.clear();
        if (placedOrder != null) {
            final List<CommerceSession.CartEntry> cartEntries = placedOrder.getCartEntries();
            for (CommerceSession.CartEntry cartEntry : cartEntries) {
                CartEntry entry = new CartEntry(cartEntry);
                entries.add(entry);
            }
        }
    }

    private String getOrderProperty(String property) {
        Object obj = orderDetails.get(property);
        return obj != null ? obj.toString() : null;
    }

    public String getOrderId() {
        return orderId;
    }

    public Calendar getOrderDate() {
        return (Calendar) orderDetails.get(ORDER_PLACED);
    }

    public String getOrderStatus() {
        return getOrderProperty(ORDER_STATUS);
    }

    public String getSubTotal() throws CommerceException {
        return placedOrder.getCartPrice(new PriceFilter(CART_SUB_TOTAL));
    }

    public String getShippingTotal() throws CommerceException {
        return placedOrder.getCartPrice(new PriceFilter(ORDER_SHIPPING));
    }

    public String getTaxTotal() throws CommerceException {
        return placedOrder.getCartPrice(new PriceFilter(ORDER_TOTAL_TAX));
    }

    public String getTotal() throws CommerceException {
        return placedOrder.getCartPrice(new PriceFilter(ORDER_TOTAL_PRICE));
    }

    public String getShippingAddress() {
        return getAddress(SHIPPING_PREFIX);
    }

    public String getBillingAddress() {
        return getAddress(BILLING_PREFIX);
    }

    private String getAddress(String prefix) {
        String firstname = getOrderProperty(prefix + WeRetailConstants.FIRST_NAME);
        String lastname = getOrderProperty(prefix + WeRetailConstants.LAST_NAME);
        String street1 = getOrderProperty(prefix + WeRetailConstants.STREET_LINE1);
        String street2 = getOrderProperty(prefix + WeRetailConstants.STREET_LINE2);
        String zipCode = getOrderProperty(prefix + WeRetailConstants.ZIP_CODE);
        String city = getOrderProperty(prefix + WeRetailConstants.CITY);
        String state = getOrderProperty(prefix + WeRetailConstants.STATE);
        String country = getOrderProperty(prefix + WeRetailConstants.COUNTRY);

        String name = join(new String[] { firstname, lastname }, " ");
        String street = join(new String[] { street1, street2 }, " ");
        String countryZip = join(new String[] { country, zipCode }, "-");
        String countryZipCity = join(new String[] { countryZip, city }, " ");

        return join(new String[] { name, street, countryZipCity, state }, ", ");
    }

    private static String join(String[] strings, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (StringUtils.isNotBlank(s)) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }
}

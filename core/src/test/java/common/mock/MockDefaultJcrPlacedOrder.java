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
package common.mock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.CommerceSession.CartEntry;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.AbstractJcrCommerceService;
import com.adobe.cq.commerce.common.AbstractJcrCommerceSession;
import com.adobe.cq.commerce.common.DefaultJcrPlacedOrder;

import we.retail.core.model.Constants;

public class MockDefaultJcrPlacedOrder extends DefaultJcrPlacedOrder {

    public MockDefaultJcrPlacedOrder(AbstractJcrCommerceSession abstractJcrCommerceSession, String orderId) {
        super(abstractJcrCommerceSession, orderId);
    }

    public MockDefaultJcrPlacedOrder(AbstractJcrCommerceSession abstractJcrCommerceSession, String orderId, Resource order) {
        super(abstractJcrCommerceSession, orderId);
        this.order = order;
    }

    @Override
    protected Resource getPlacedOrder(String orderId) {
        return order;
    }

    @Override
    public Map<String, Object> getOrder() throws CommerceException {
        if (details == null) {
            lazyLoadOrderDetails();
        }
        return details;
    }

    @Override
    public String getOrderId() throws CommerceException {
        if (details == null) {
            lazyLoadOrderDetails();
        }
        return (String) details.get(Constants.ORDER_ID);
    }

    public void setOrderId(String orderId) throws CommerceException {
        if (details == null) {
            lazyLoadOrderDetails();
        }
        details.put(Constants.ORDER_ID, orderId);
    }

    public void setOrderPlacedDate(Date orderPlacedDate) throws CommerceException {
        if (details == null) {
            lazyLoadOrderDetails();
        }
        details.put(Constants.ORDER_PLACED, orderPlacedDate);
    }

    /**
     * A one-to-one copy of {@link DefaultJcrPlacedOrder#lazyLoadOrderDetails()} except for the last line being removed. The problem is that
     * this method is declared private in {@link DefaultJcrPlacedOrder} so duplicating the code is the easiest way to mock this method.
     * 
     * @throws CommerceException
     */
    private void lazyLoadOrderDetails() throws CommerceException {
        details = new HashMap<String, Object>();
        if (order != null) {
            final SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM, yyyy");

            details.put("orderPath", order.getPath());

            ValueMap orderProperties = order.getValueMap();
            for (Map.Entry<String, Object> entry : orderProperties.entrySet()) {
                String key = entry.getKey();
                if ("cartItems".equals(key)) {
                    // returned by getPlacedOrderEntries()
                } else {
                    Object property = entry.getValue();
                    if (property instanceof Calendar) {
                        // explode date into 'property' and 'propertyFormatted'
                        details.put(key, property);
                        details.put(key + "Formatted", dateFmt.format(((Calendar) property).getTime()));
                    } else {
                        details.put(key, property);
                    }
                }
            }

            Resource orderDetailsChild = order.getChild("order-details");
            if (orderDetailsChild != null) {
                ValueMap orderDetailProperties = orderDetailsChild.getValueMap();
                for (ValueMap.Entry<String, Object> detailProperty : orderDetailProperties.entrySet()) {
                    String key = detailProperty.getKey();
                    Object property = detailProperty.getValue();
                    if (property instanceof Calendar) {
                        // explode date into 'property' and 'propertyFormatted'
                        details.put(key, property);
                        details.put(key + "Formatted", dateFmt.format(((Calendar) property).getTime()));
                    } else {
                        details.put(key, property);
                    }
                }
            }
        }
    }

    /**
     * A one-to-one copy of {@link DefaultJcrPlacedOrder#lazyLoadCartEntries()} except for the deserialization part. The problem is that
     * this method is declared private in {@link DefaultJcrPlacedOrder} so duplicating the code is the easiest way to mock this method.
     * 
     * @throws CommerceException
     */
    @Override
    protected void lazyLoadCartEntries() throws CommerceException {
        entries = new ArrayList<CommerceSession.CartEntry>();

        if (order != null) {
            String[] serializedEntries = order.getValueMap().get("cartItems", String[].class);
            for (String serializedEntry : serializedEntries) {
                try {
                    CommerceSession.CartEntry entry = deserializeCartEntry(serializedEntry, entries.size());
                    entries.add(entry);
                } catch (Exception e) { // NOSONAR (catch any errors thrown attempting to parse/decode entry)
                    log.error("Unable to load product from order: {}", serializedEntry);
                }
            }
        }
    }

    /**
     * This is almost a one-to-one copy of {@link AbstractJcrCommerceSession#deserializeCartEntry(String, int)}. We had to copy that method
     * here because it is the easiest way to mock that code.
     * 
     * @throws CommerceException
     */
    protected CartEntry deserializeCartEntry(String str, int index) throws CommerceException {
        Object[] entryData = deserializeCartEntryData(str);
        Product product = (Product) entryData[0];
        int quantity = (Integer) entryData[1];
        MockDefaultJcrCartEntry entry = new MockDefaultJcrCartEntry(index, product, quantity);
        if (entryData[2] == null) {
            return entry;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) entryData[2];
        entry.updateProperties(properties);
        return entry;
    }

    /**
     * This is a one-to-one copy of {@link AbstractJcrCommerceService#deserializeCartEntryData(String)}. We had to copy that method here
     * because it is the easiest way to mock that code.
     * 
     * @throws CommerceException
     */
    public Object[] deserializeCartEntryData(String str) throws CommerceException {
        Object[] entryData = new Object[3];
        String[] entryFields = str.split(";", 3);
        Product product = new MockProduct(order.getResourceResolver().resolve(entryFields[0]));
        entryData[0] = product;
        int quantity = Integer.parseInt(entryFields[1]);
        entryData[1] = quantity;
        if (entryFields.length == 2) {
            return entryData;
        }

        Map<String, Object> properties = new HashMap<String, Object>();
        String[] propertyFields = entryFields[2].split("\f");
        for (String field : propertyFields) {
            if (StringUtils.isNotBlank(field)) {
                String[] property = field.split("=", 2);
                properties.put(property[0], property[1]);
            }
        }

        entryData[2] = properties;
        return entryData;
    }
}

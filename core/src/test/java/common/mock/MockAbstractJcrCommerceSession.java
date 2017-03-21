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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.PlacedOrder;
import com.adobe.cq.commerce.common.AbstractJcrCommerceService;
import com.adobe.cq.commerce.common.AbstractJcrCommerceSession;

public class MockAbstractJcrCommerceSession extends AbstractJcrCommerceSession {

    private Map<String, PlacedOrder> placedOrders = new HashMap<String, PlacedOrder>();

    public MockAbstractJcrCommerceSession(AbstractJcrCommerceService commerceService, SlingHttpServletRequest request,
            SlingHttpServletResponse response, Resource resource) throws CommerceException {
        super(commerceService, request, response, resource);
    }

    @Override
    public PlacedOrder getPlacedOrder(String orderId) throws CommerceException {
        return placedOrders.get(orderId);
    }

    public void registerPlacedOrder(String orderId, PlacedOrder placedOrder) {
        placedOrders.put(orderId, placedOrder);
    }
}

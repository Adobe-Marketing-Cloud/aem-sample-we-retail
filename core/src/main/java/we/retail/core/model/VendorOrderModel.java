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

import javax.annotation.PostConstruct;

import com.adobe.cq.commerce.api.CommerceException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.commerce.common.VendorJcrPlacedOrder;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class VendorOrderModel extends OrderModel {

    @RequestAttribute(name = "cq.commerce.vendorplacedorder")
    private VendorJcrPlacedOrder order;

    @ValueMapValue
    @Default(booleanValues = false)
    private boolean showPrice;

    @ValueMapValue
    private String title;

    @PostConstruct
    private void initModel() throws CommerceException {
        placedOrder = order;
        populateCartEntries();
    }

    public boolean showPrice() {
        return showPrice;
    }

    public String getTitle() {
        return title;
    }
}

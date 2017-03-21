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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.commons.WCMUtils;

@Model(adaptables = SlingHttpServletRequest.class)
public class WishlistModel {

    @SlingObject
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    private String smartListUrl;
    private String cartPageUrl;

    @PostConstruct
    private void initModel() throws Exception {
        populatePages();
    }

    private void populatePages() {
        String cartPageProperty = WCMUtils.getInheritedProperty(currentPage, resourceResolver, CommerceConstants.PN_CART_PAGE_PATH);
        if (StringUtils.isNotEmpty(cartPageProperty)) {
            cartPageUrl = resourceResolver.map(request, cartPageProperty) + ".html";
        } else {
            cartPageUrl = resourceResolver.map(request, currentPage.getPath() + ".html");
        }

        smartListUrl = resourceResolver.map(request, currentPage.getPath() + ".html");
    }

    /**
     * Get the smartlist page url.
     * 
     * @return the smart list page url.
     */
    public String getSmartListUrl() {
        return smartListUrl;
    }

    /**
     * Get the cart page url.
     * 
     * @return the cart page url.
     */
    public String getCartPageUrl() {
        return cartPageUrl;
    }
}

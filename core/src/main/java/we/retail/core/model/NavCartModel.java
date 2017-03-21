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
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.commons.WCMUtils;

@Model(adaptables = SlingHttpServletRequest.class)
public class NavCartModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavCartModel.class);

    @SlingObject
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(booleanValues = false)
    protected boolean isReadOnly;

    private String checkoutPage;
    private String currentPageUrl;

    @PostConstruct
    private void initModel() throws Exception {
        populatePageUrls();
    }

    protected void populatePageUrls() {
        final String checkoutPageProperty = WCMUtils.getInheritedProperty(currentPage, resourceResolver,
                CommerceConstants.PN_CHECKOUT_PAGE_PATH);
        if (StringUtils.isNotEmpty(checkoutPageProperty)) {
            checkoutPage = resourceResolver.map(request, checkoutPageProperty) + ".html";
        }
        currentPageUrl = resourceResolver.map(request, currentPage.getPath() + ".html");
    }

    public String getCheckoutPage() {
        return checkoutPage;
    }

    public String getCurrentPageUrl() {
        return currentPageUrl;
    }

    public boolean getIsReadOnly() {
        return isReadOnly;
    }
}

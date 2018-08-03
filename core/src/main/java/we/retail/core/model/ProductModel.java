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

import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.adobe.granite.security.user.UserManagementService;
import com.day.cq.wcm.api.Page;
import we.retail.core.model.handler.CommerceHandler;

@Model(adaptables = SlingHttpServletRequest.class)
public class ProductModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductModel.class);

    @SlingObject
    private Resource resource;

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private SlingHttpServletResponse response;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @Self
    private CommerceHandler commerceHandler;

    @OSGiService
    private UserManagementService ums;

    private CommerceService commerceService;
    private ProductItem productItem;
    private boolean isAnonymous;

    @PostConstruct
    private void initModel() {
        try {
            commerceService = currentPage.getContentResource().adaptTo(CommerceService.class);
            if (commerceService != null) {
                CommerceSession commerceSession = commerceService.login(request, response);
                Product product;
                //for proxy page use product from commerce handler
                if (commerceHandler.isProductPageProxy()) {
                    product = commerceHandler.getProduct();
                } else {
                    product = resource.adaptTo(Product.class);
                }

                if (product != null) {
                    productItem = new ProductItem(product, commerceSession, request, currentPage);
                }
            }
        } catch (CommerceException e) {
            LOGGER.error("Can't extract product from page", e);
        }

        String anonymousId = ums != null ? ums.getAnonymousId() : UserConstants.DEFAULT_ANONYMOUS_ID;
        isAnonymous = resourceResolver.getUserID() == null || anonymousId.equals(resourceResolver.getUserID());
    }

    public ProductItem getProductItem() {
        return productItem;
    }

    public boolean hasVariants() {
        return productItem != null && !productItem.getVariants().isEmpty();
    }

    public String getAddToCartUrl() {
        return commerceHandler.getAddToCardUrl();
    }

    public String getAddToSmartListUrl() {
        return commerceHandler.getAddToSmartListUrl();
    }

    public String getProductTrackingPath() {
        return commerceHandler.getProductTrackingPath();
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }
}

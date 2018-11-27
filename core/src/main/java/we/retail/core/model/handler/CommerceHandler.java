/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package we.retail.core.model.handler;


import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.CommerceHelper;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Style;

import we.retail.core.WeRetailConstants;

@Model(adaptables = SlingHttpServletRequest.class)
public class CommerceHandler {

    private static final String ADD_CART_ENTRY_SELECTOR = "." + WeRetailConstants.ADD_CARTENTRY_SELECTOR + ".html";
    private static final String ADD_SMARTLIST_ENTRY_SELECTOR = ".commerce.smartlist.management.html";
    private static final String ADD_SELECTOR = ".add.html";
    private static final String PN_ADD_TO_CART_REDIRECT = "addToCartRedirect";
    private static final String PN_CART_ERROR_REDIRECT = "cartErrorRedirect";
    private static final String REQ_ATTR_CQ_COMMERCE_PRODUCT = "cq.commerce.product";
    private static final String PN_PRODUCT_DATA = "productData";

    @SlingObject
    private Resource resource;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Style currentStyle;

    @RequestAttribute(name = CommerceConstants.REQ_ATTR_CARTPAGE, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String cartPage;

    @RequestAttribute(name = CommerceConstants.REQ_ATTR_CARTOBJECT, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String cartObject;

    @RequestAttribute(name = CommerceConstants.REQ_ATTR_PRODNOTFOUNDPAGE, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String productNotFound;

    @RequestAttribute(name = REQ_ATTR_CQ_COMMERCE_PRODUCT, injectionStrategy = InjectionStrategy.OPTIONAL)
    private Product product;

    private String addToCardUrl;
    private String addToSmartListUrl;
    private Page currentPage;
    private String redirectUrl;
    private String errorRedirectUrl;
    private boolean productPageProxy = false;
    private String productTrackingPath;

    @PostConstruct
    private void initHandler() throws CommerceException {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        currentPage = pageManager.getContainingPage(resource);
        addToCardUrl = currentPage.getPath() + ADD_CART_ENTRY_SELECTOR;
        addToSmartListUrl = currentPage.getPath() + ADD_SMARTLIST_ENTRY_SELECTOR;
        redirectUrl = CommerceHelper.mapPathToCurrentLanguage(currentPage,
                currentStyle.get(PN_ADD_TO_CART_REDIRECT, StringUtils.EMPTY));
        errorRedirectUrl = CommerceHelper.mapPathToCurrentLanguage(currentPage,
                currentStyle.get(PN_CART_ERROR_REDIRECT, StringUtils.EMPTY));

        if (StringUtils.isEmpty(redirectUrl) && StringUtils.isNotEmpty(cartObject)) {
            redirectUrl = cartPage;
            errorRedirectUrl = productNotFound;
            addToCardUrl = cartObject + ADD_SELECTOR;
        }

        if (StringUtils.isEmpty(redirectUrl) || StringUtils.equals(redirectUrl, ".")) {
            redirectUrl = currentPage.getPath();
        }

        if (StringUtils.isEmpty(errorRedirectUrl)) {
            errorRedirectUrl = currentPage.getPath();
        }

        if (StringUtils.isNotBlank(addToCardUrl)) {
            addToCardUrl = resourceResolver.map(request, addToCardUrl);
        }

        if (StringUtils.isNotBlank(addToSmartListUrl)) {
            addToSmartListUrl = resourceResolver.map(request, addToSmartListUrl);
        }

        if (product == null) {
            product = resource.adaptTo(Product.class);
        } else {
            productPageProxy = true;
        }

        if (product != null) {
            productTrackingPath = product.getProperty(PN_PRODUCT_DATA, String.class);
            if(StringUtils.isEmpty(productTrackingPath)) {
                productTrackingPath = product.getPagePath();
            }
            setRequestAttributes();
        }
    }

    private void setRequestAttributes() {
        request.setAttribute(REQ_ATTR_CQ_COMMERCE_PRODUCT, product);

    }

    public String getAddToCardUrl() {
        return addToCardUrl;
    }

    public String getAddToSmartListUrl() {
        return addToSmartListUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getErrorRedirectUrl() {
        return errorRedirectUrl;
    }

    public boolean isProductPageProxy() {
        return productPageProxy;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductTrackingPath() {
        return productTrackingPath;
    }
}

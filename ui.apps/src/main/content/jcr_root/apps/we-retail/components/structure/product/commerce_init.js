/*
 *  Copyright 2016 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * Initializes commerce information
 */
"use strict";
var global = this;
(function(){

    if (currentPage && global.Packages) {
        var addToCartUrl = currentPage.path + ".commerce.addcartentry.html";
        var addToSmartListUrl = currentPage.path + ".commerce.smartlist.management.html";
        var redirect = global.Packages.com.adobe.cq.commerce.common.CommerceHelper.mapPathToCurrentLanguage(global.currentPage, currentStyle.get("addToCartRedirect", ""));
        var errorRedirect = global.Packages.com.adobe.cq.commerce.common.CommerceHelper.mapPathToCurrentLanguage(global.currentPage, currentStyle.get("cartErrorRedirect", ""));
        var smartListRedirect = global.Packages.com.adobe.cq.commerce.common.CommerceHelper.mapPathToCurrentLanguage(global.currentPage,
            global.Packages.com.day.cq.wcm.commons.WCMUtils.getInheritedProperty(global.currentPage, request.getResourceResolver(), "cq:smartListPage"));

        if (redirect == ""
            && request.getAttribute(global.Packages.com.adobe.cq.commerce.api.CommerceConstants.REQ_ATTR_CARTPAGE) != null) {
            redirect = request.getAttribute(global.Packages.com.adobe.cq.commerce.api.CommerceConstants.REQ_ATTR_CARTPAGE);
            errorRedirect = request.getAttribute(global.Packages.com.adobe.cq.commerce.api.CommerceConstants.REQ_ATTR_PRODNOTFOUNDPAGE);

            addToCartUrl = request.getAttribute(global.Packages.com.adobe.cq.commerce.api.CommerceConstants.REQ_ATTR_CARTOBJECT) + ".add.html";
        }
        if (redirect == "" || redirect == ".") {
            redirect = currentPage.path;
        }
        if (errorRedirect == "") {
            errorRedirect = currentPage.path;
        }
        if (smartListRedirect == "") {
            smartListRedirect = currentPage.path;
        }

        var baseProduct = null;
        var productPageProxy = false
        if (request.getAttribute("cq.commerce.product") != null) {
            // Handle product-page proxies.
            //   In this case our resource is the (empty) product component on the product template page, and
            //   the baseProduct is supplied on the cq.commerce.product request attribute.
            productPageProxy = true
            baseProduct = request.getAttribute("cq.commerce.product");
        } else {
            baseProduct = resource.adaptTo(global.Packages.com.adobe.cq.commerce.api.Product);
        }

        if (baseProduct) {
            var productTrackingPath = baseProduct.getProperty("productData", global.Packages.java.lang.String);
            if (productTrackingPath == null) {
                productTrackingPath = baseProduct.getPagePath();
            }

            request.setAttribute("cq.commerce.addToCartUrl", addToCartUrl);
            request.setAttribute("cq.commerce.addToSmartListUrl", addToSmartListUrl);
            request.setAttribute("cq.commerce.redirect", redirect);
            request.setAttribute("cq.commerce.smartListRedirect", smartListRedirect);
            request.setAttribute("cq.commerce.errorRedirect", errorRedirect);
            request.setAttribute("cq.commerce.product", baseProduct);
            request.setAttribute("cq.commerce.productPagePath", baseProduct.getPagePath());
            request.setAttribute("cq.commerce.productTrackingPath", productTrackingPath);
        }



    }

    return {
        productPageProxy: productPageProxy
    }
})();

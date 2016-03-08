/*
 * ADOBE CONFIDENTIAL
 *
 * Copyright 2014 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
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

    return {
        productPageProxy: productPageProxy
    }
})();

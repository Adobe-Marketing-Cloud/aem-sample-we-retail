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
"use strict";

var global = this;

/**
 *
 */
use(function () {

    var commerceService = resource.adaptTo(com.adobe.cq.commerce.api.CommerceService);
    var session = commerceService.login(request, response);
    var resolver = resource.getResourceResolver();

    var types = properties.get("types") || [];
    var filter = new com.adobe.cq.commerce.common.PriceFilter(types);
    var orderId = request.getParameter("orderId");
    var entries, allPromotions;
    var _entries = [], total;

    if (orderId != null) {
        var placedOrder = session.getPlacedOrder(orderId);
        entries = placedOrder.getCartEntries();
        allPromotions = placedOrder.getPromotions();
        total = placedOrder.getCartPrice(filter)
    } else {
        entries = session.getCartEntries();
        allPromotions = session.getPromotions();
        total = session.getCartPrice(filter);
    }

    var checkoutPage = com.day.cq.wcm.commons.WCMUtils.getInheritedProperty(currentPage, resolver, "cq:checkoutPage");
    if (checkoutPage) {
        checkoutPage = resolver.map(request, checkoutPage) + ".html";
    }


    var iter = entries.iterator();
    while(iter.hasNext()) {
        var entry = iter.next();
        var productImage = resolver.getResource(entry.product.getImage().getPath());

        _entries.push({
            entry: entry,
            product: entry.product,
            price: session.getProductPrice(entry.product),
            image: productImage != null ?
                   productImage.adaptTo(org.apache.sling.api.resource.ValueMap).get("fileReference", java.lang.String) : ""
        })
    }

    return {
        checkoutPage: checkoutPage,
        total: total,
        entries: _entries
    }
});
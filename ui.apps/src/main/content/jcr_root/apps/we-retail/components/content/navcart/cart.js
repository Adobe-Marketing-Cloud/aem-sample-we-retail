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
    log.error("FILTER ");
    log.error("FILTER " + filter + ", " + types);
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
        /*
        log.error("entry " + entry.getEntryIndex());
        log.error("productData" + entry.product.getBaseProduct().getProperty("productData", java.lang.String));
        log.error("pagePath" + entry.product.getBaseProduct().getPagePath());

        log.error("productData" + entry.product.getProperty("productData", java.lang.String));
        log.error("pagePath" + entry.product.getPagePath());
        */
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
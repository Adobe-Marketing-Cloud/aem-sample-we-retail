'use strict';

var global = this;

use(function () {
    var product = {};

    var resolver = resource.getResourceResolver()
        , commerceService = resource.adaptTo(com.adobe.cq.commerce.api.CommerceService)
        , commerceSession = commerceService.login(request, response)
        , productPage = global.pageManager.getContainingPage(granite.resource.path)
        , productPath = productPage.getProperties().get("cq:productMaster", java.lang.String)
        , productResource = resolver.getResource(productPath)
        , baseProduct = commerceService.getProduct(productPath)
        , productData = productResource.adaptTo(org.apache.sling.api.resource.ValueMap)
        , imageResource = resolver.getResource(productPage.getProperties().get("cq:productMaster", java.lang.String) + "/image")
        , variationAxis = baseProduct.getProperty("cq:productVariantAxes", java.lang.String)
        , variationTitle = baseProduct.getProperty("variationTitle", java.lang.String)
        , variationLead = baseProduct.getProperty("variationLead", java.lang.String)
        , variants = [];

    product.image = imageResource.adaptTo(org.apache.sling.api.resource.ValueMap).get("fileReference", java.lang.String);
    product.name = productData.get("jcr:title", java.lang.String);
    product.description = productData.get("jcr:description", java.lang.String);
    product.price = commerceSession.getProductPrice(baseProduct);
    product.path = productPage.getPath();

    product.filters = {};
    product.filters.colors = [];
    product.filters.sizes = [];
    product.filters.prices = [];

    if (variationAxis) {
        var unorderedVariations = baseProduct.getVariants();

        while (unorderedVariations.hasNext()) {
            var productVariation = unorderedVariations.next()
                , variation = getProductProperties(productVariation);

            variants.push(variation);

            if (variation.color) {
                product.filters.colors.push(variation.color.toLowerCase());
            }

            if (variation.size) {
                product.filters.sizes.push(variation.size);
            }

            product.filters.prices.push(variation.price);
        }
    }
    else {
        var color = ('' + baseProduct.getProperty('color', java.lang.String)).toLowerCase();

        if (color) {
            product.filters.colors.push();
        }

        product.filters.sizes.push(baseProduct.getProperty('size', java.lang.String));
        product.filters.prices.push(commerceSession.getProductPrice(baseProduct));
    }

    return product;

    function getProductProperties(product) {
        var productImage = resolver.getResource(product.getImage().getPath());

        return {
            path: product.getPath(),
            variants: product.getVariantAxes(),
            sku: product.getSKU(),
            title: product.getTitle(),
            description: product.getDescription(),
            color: product.getProperty('color', java.lang.String),
            colorClass: ('' + product.getProperty('color', java.lang.String)).toLowerCase(),
            size: product.getProperty('size', java.lang.String),
            price: commerceSession.getProductPrice(product),
            summary: product.getProperty('summary', java.lang.String),
            features: product.getProperty("features", java.lang.String),
            image: productImage != null ?
                productImage.adaptTo(org.apache.sling.api.resource.ValueMap).get("fileReference", java.lang.String) : ""
        };
    }
});
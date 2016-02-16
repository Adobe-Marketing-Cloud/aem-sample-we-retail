'use strict';

use(function () {
    var product = {};

    var resolver = resource.getResourceResolver();
    var commerceService = resource.adaptTo(com.adobe.cq.commerce.api.CommerceService);
    var commerceSession = commerceService.login(request, response);
    var productPath = currentPage.getProperties().get("cq:productMaster", java.lang.String);
    var baseProduct = commerceService.getProduct(productPath);

    var baseProductProperties = getProductProperties(baseProduct);

    var variants = [];

    var variationAxis = baseProduct.getProperty("cq:productVariantAxes", java.lang.String);
    var variationTitle = baseProduct.getProperty("variationTitle", java.lang.String);
    var variationLead = baseProduct.getProperty("variationLead", java.lang.String);

    var variations = {
        type: variationAxis,
        colors: {},
        sizes: []
    };

    if (variationAxis) {
        var unorderedVariations = baseProduct.getVariants();

        while (unorderedVariations.hasNext()) {
            var productVariation = unorderedVariations.next();

            var variation = getProductProperties(productVariation);

            if (variationLead !== '' && productVariation.getProperty(variationAxis, java.lang.String) === variationLead) {
                variants.unshift(variation);
            }
            else {
                variants.push(variation);
            }

            if ('' + variations.type === 'color') {
                if (!variations.colors[variation.color.toLowerCase()]) {
                    variations.colors[variation.color.toLowerCase()] = [];
                }
                variations.colors[variation.color.toLowerCase()].push(variation);
            }
            else if ('' + variations.type === 'size') {
                variations.sizes.push(variation);
            }
        }
    }

    if (!variants.length) {
        variants.push(baseProductProperties);
    }
    else {
        baseProductProperties = variants[0];
        baseProductProperties.path = baseProduct.getPath();
    }

    product.base = baseProductProperties;
    product.variants = variants;
    product.variations = variations;

    product.variationTitle = variationTitle;
    product.variationLead = variationLead;

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
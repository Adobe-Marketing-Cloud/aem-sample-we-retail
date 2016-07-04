'use strict';
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

var global = this;

use(function () {
    var product = {};

    var resolver = resource.getResourceResolver()
        , commerceService = resource.adaptTo(com.adobe.cq.commerce.api.CommerceService)
        , commerceSession = commerceService.login(request, response)
        , productPage = global.pageManager.getContainingPage(granite.resource.path)
        , productPath = productPage.getProperties().get("cq:productMaster", java.lang.String);

    var productResource = resolver.getResource(productPath);

    if(productResource == null) {
        return null;
    }

    var baseProduct = commerceService.getProduct(productPath)
        , variationAxis = baseProduct.getProperty("cq:productVariantAxes", java.lang.String)
        , variationTitle = baseProduct.getProperty("variationTitle", java.lang.String)
        , variationLead = baseProduct.getProperty("variationLead", java.lang.String)
        , variants = [];

    var imageResource = baseProduct.getImage();
    product.image = imageResource.adaptTo(org.apache.sling.api.resource.ValueMap).get("fileReference", java.lang.String);
    product.name = baseProduct.getTitle();
    product.description = baseProduct.getDescription();
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
        var productImage = product.getImage();
        var productImageResource = null;
        if (productImage != null) {
            productImageResource = resolver.getResource(productImage.getPath());
        }

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
            image: productImageResource != null ?
                productImageResource.adaptTo(org.apache.sling.api.resource.ValueMap).get("fileReference", java.lang.String) : ""
        };
    }
});

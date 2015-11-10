'use strict';
use(function() {
    var product = {};
	var resolver = resource.getResourceResolver();
	var commerceService = resource.adaptTo(com.adobe.cq.commerce.api.CommerceService);
    var commerceSession = commerceService.login(request, response);
    var productPath = currentPage.getProperties().get("cq:productMaster", java.lang.String);
	var productResource = resolver.getResource(productPath);
    var prod = commerceService.getProduct(productPath);
	var productData = productResource.adaptTo(org.apache.sling.api.resource.ValueMap);
    var imageResource = resolver.getResource(currentPage.getProperties().get("cq:productMaster", java.lang.String)+"/image");

    product.image = imageResource.adaptTo(org.apache.sling.api.resource.ValueMap).get("fileReference", java.lang.String);
    product.name = productData.get("jcr:title", java.lang.String);
    product.description = productData.get("jcr:description", java.lang.String);
    product.summary = productData.get("summary", java.lang.String);
    product.features = productData.get("features", java.lang.String);
	product.price = commerceSession.getProductPrice(prod);
	return product;
});
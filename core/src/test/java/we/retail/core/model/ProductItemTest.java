/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package we.retail.core.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

import org.apache.sling.api.SlingHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductItemTest {

    @Test
    public void testBuildsVariantContractFromCifData() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html");

        ProductItem item = new ProductItem(createProduct(), request, null);

        assertEquals("meotwisus", item.getSku());
        assertEquals("Sussex Rain Boots", item.getTitle());
        assertEquals("Footwear", item.getDescription());
        assertEquals("$65.00", item.getPrice());
        assertEquals("Current CIF summary", item.getSummary());
        assertEquals("https://cdn.example.com/current-cif-image.jpg", item.getImageUrl());
        assertEquals("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html", item.getPath());
        assertEquals(1, item.getVariants().size());
        assertEquals("meotwisus-9", item.getVariants().get(0).getSku());
        assertEquals("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html#meotwisus-9",
            item.getVariants().get(0).getPath());
        assertEquals("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html#meotwisus-9",
            item.getVariants().get(0).getPagePath());
        assertEquals("9", item.getVariants().get(0).getVariantValueForAxis("size"));
        assertTrue(item.getVariantsAxesValues().get("size").contains("9"));
    }

    @Test
    public void testPrefersCifSkusWhenAvailable() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/content/we-retail/us/en/products/product-page.html/wo/coats/sonja-insulated-jacket.html");

        ProductItem item = new ProductItem(createProduct("wr-sonja-jacket", "wr-sonja-jacket-green-xs"), request, null);

        assertEquals("wr-sonja-jacket", item.getSku());
        assertEquals("wr-sonja-jacket-green-xs", item.getVariants().get(0).getSku());
        assertEquals("/content/we-retail/us/en/products/product-page.html/wo/coats/sonja-insulated-jacket.html#wr-sonja-jacket-green-xs",
            item.getVariants().get(0).getPagePath());
    }

    @Test
    public void testFormatsTheMinimumOfAPriceRange() {
        Price price = createPrice("$39.00");
        when(price.isRange()).thenReturn(Boolean.TRUE);
        when(price.getFormattedFinalPriceMax()).thenReturn("$42.00");

        assertEquals("$39.00", ProductItem.formatPrice(price));
    }

    private Product createProduct() {
        return createProduct("me/footwear/meotwisus", "meotwisus-9");
    }

    private Product createProduct(String productSku, String variantSku) {
        Product product = mock(Product.class);
        Price price = createPrice("$65.00");
        Asset asset = createAsset("https://cdn.example.com/current-cif-image.jpg");
        Variant variant = createVariant(variantSku);
        VariantAttribute attribute = createVariantAttribute();
        ProductInterface productData = createProductData();
        AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);

        when(product.getFound()).thenReturn(Boolean.TRUE);
        when(product.getSku()).thenReturn(productSku);
        when(product.getName()).thenReturn("Sussex Rain Boots");
        when(product.getPriceRange()).thenReturn(price);
        when(product.getAssets()).thenReturn(Collections.singletonList(asset));
        when(product.getVariants()).thenReturn(Collections.singletonList(variant));
        when(product.getVariantAttributes()).thenReturn(Collections.singletonList(attribute));
        when(retriever.fetchProduct()).thenReturn(productData);
        when(product.getProductRetriever()).thenReturn(retriever);

        return product;
    }

    private Variant createVariant() {
        return createVariant("meotwisus-9");
    }

    private Variant createVariant(String sku) {
        Variant variant = mock(Variant.class);
        Price price = createPrice("$65.00");
        when(variant.getSku()).thenReturn(sku);
        when(variant.getName()).thenReturn("Sussex Rain Boots");
        when(variant.getDescription()).thenReturn("");
        when(variant.getPriceRange()).thenReturn(price);
        when(variant.getAssets()).thenReturn(Collections.<Asset>emptyList());
        LinkedHashMap<String, Integer> attributes = new LinkedHashMap<String, Integer>();
        attributes.put("size", Integer.valueOf(9));
        when(variant.getVariantAttributes()).thenReturn(attributes);
        return variant;
    }

    private VariantAttribute createVariantAttribute() {
        VariantAttribute attribute = mock(VariantAttribute.class);
        VariantValue value = createVariantValue();
        when(attribute.getId()).thenReturn("size");
        when(attribute.getValues()).thenReturn(Collections.singletonList(value));
        return attribute;
    }

    private VariantValue createVariantValue() {
        VariantValue value = mock(VariantValue.class);
        when(value.getId()).thenReturn(Integer.valueOf(9));
        when(value.getLabel()).thenReturn("9");
        return value;
    }

    private ProductInterface createProductData() {
        ProductInterface productData = mock(ProductInterface.class);
        CategoryInterface category = createCategory("Footwear");
        ComplexTextValue shortDescription = createTextValue("<p>Current CIF summary</p>");
        ComplexTextValue description = createTextValue("<p>Current CIF description</p>");
        ProductImage image = createImage("https://cdn.example.com/current-cif-image.jpg");

        when(productData.getCategories()).thenReturn(Collections.singletonList(category));
        when(productData.getShortDescription()).thenReturn(shortDescription);
        when(productData.getDescription()).thenReturn(description);
        when(productData.getSmallImage()).thenReturn(image);
        return productData;
    }

    private CategoryInterface createCategory(String name) {
        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getName()).thenReturn(name);
        return category;
    }

    private ComplexTextValue createTextValue(String html) {
        ComplexTextValue textValue = mock(ComplexTextValue.class);
        when(textValue.getHtml()).thenReturn(html);
        return textValue;
    }

    private ProductImage createImage(String url) {
        ProductImage image = mock(ProductImage.class);
        when(image.getUrl()).thenReturn(url);
        return image;
    }

    private Asset createAsset(String path) {
        Asset asset = mock(Asset.class);
        when(asset.getPath()).thenReturn(path);
        return asset;
    }

    private Price createPrice(String formattedFinalPrice) {
        Price price = mock(Price.class);
        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn(formattedFinalPrice);
        return price;
    }
}

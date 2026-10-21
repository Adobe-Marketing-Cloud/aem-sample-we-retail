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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductGridItemTest {

    @Test
    public void testBuildsFiltersFromConfigurableProduct() {
        ProductListItem listItem = mock(ProductListItem.class);
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Price price = createPrice("$64.99");
        ConfigurableProduct product = createConfigurableProduct();

        when(listItem.getImageURL()).thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg");
        when(listItem.getTitle()).thenReturn("Sussex Rain Boots");
        when(listItem.getURL()).thenReturn("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html");
        when(listItem.getPriceRange()).thenReturn(price);
        when(listItem.getProduct()).thenReturn(product);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.map(request, "/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg"))
            .thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg");

        ProductGridItem item = ProductGridItem.fromProductListItem(listItem, request, listItem.getURL());

        assertTrue(item.exists());
        assertEquals("Lightweight training shoe", item.getDescription());
        assertEquals("/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg", item.getImage());
        assertTrue(item.getFilters().getColors().contains("red"));
        assertTrue(item.getFilters().getSizes().contains("9"));
        assertTrue(item.getFilters().getPrices().contains("$64.99"));
    }

    @Test
    public void testShowsMinimumPriceAndFiltersOnBothEndsOfARange() {
        ProductListItem listItem = mock(ProductListItem.class);
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Price price = createPrice("$119.00");
        when(price.isRange()).thenReturn(Boolean.TRUE);
        when(price.getFormattedFinalPriceMax()).thenReturn("$130.00");
        ConfigurableProduct product = createConfigurableProduct();

        when(listItem.getTitle()).thenReturn("El Gordo Down Jacket");
        when(listItem.getURL()).thenReturn("/content/we-retail/us/en/products/product-page.html/men/coats/el-gordo-down-jacket.html");
        when(listItem.getPriceRange()).thenReturn(price);
        when(listItem.getProduct()).thenReturn(product);

        ProductGridItem item = ProductGridItem.fromProductListItem(listItem, request, listItem.getURL());

        assertEquals("$119.00", item.getPrice());
        assertEquals(Arrays.asList("$119.00", "$130.00"), new ArrayList<String>(item.getFilters().getPrices()));
    }

    @Test
    public void testBuildsSizeFilterFromSimpleProduct() {
        JsonElement size = mock(JsonElement.class);
        when(size.isJsonPrimitive()).thenReturn(true);
        when(size.getAsString()).thenReturn("Regular");

        ProductGridItem item = ProductGridItem.fromProductListItem(createSimpleListItem("Hiking Poles", size),
            mock(SlingHttpServletRequest.class), "/content/we-retail/us/en/products/product-page.html/equipment/hiking/hiking-poles.html");

        assertEquals(Collections.singletonList("Regular"), new ArrayList<String>(item.getFilters().getSizes()));
        assertTrue(item.getFilters().getColors().isEmpty());
    }

    @Test
    public void testSkipsSizeFilterWhenSimpleProductHasNoSize() {
        ProductGridItem withNullSize = ProductGridItem.fromProductListItem(createSimpleListItem("Cabana Stripe Towel", JsonNull.INSTANCE),
            mock(SlingHttpServletRequest.class), "/cabana-stripe-towel.html");
        ProductGridItem withoutSizeField = ProductGridItem.fromProductListItem(createSimpleListItem("Cabana Stripe Towel", null),
            mock(SlingHttpServletRequest.class), "/cabana-stripe-towel.html");

        assertTrue(withNullSize.getFilters().getSizes().isEmpty());
        assertTrue(withoutSizeField.getFilters().getSizes().isEmpty());
    }

    @Test
    public void testListsColorsInClassicOrder() {
        ConfigurableProduct product = mock(ConfigurableProduct.class);
        ConfigurableProductOptions colorOption = createOption("color", "blue", "green", "purple", "red");
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(colorOption));
        Price price = createPrice("$12.00");
        ProductListItem listItem = mock(ProductListItem.class);
        when(listItem.getTitle()).thenReturn("Seastar Flip Flops");
        when(listItem.getPriceRange()).thenReturn(price);
        when(listItem.getProduct()).thenReturn(product);

        ProductGridItem item = ProductGridItem.fromProductListItem(listItem, mock(SlingHttpServletRequest.class), "/seastar-flip-flops.html");

        assertEquals(Arrays.asList("red", "green", "blue", "purple"), new ArrayList<String>(item.getFilters().getColors()));
    }

    @Test
    public void testUsesSelectedVariantForImageAndPrice() {
        Product product = mock(Product.class);
        Variant variant = mock(Variant.class);
        AbstractProductRetriever productRetriever = mock(AbstractProductRetriever.class);
        ProductInterface productData = mock(ProductInterface.class);
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Asset variantAsset = mock(Asset.class);
        Price basePrice = createPrice("$64.99");
        Price variantPrice = createPrice("$74.99");

        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(product.getName()).thenReturn("Sussex Rain Boots");
        when(product.getPriceRange()).thenReturn(basePrice);
        when(product.getVariants()).thenReturn(Collections.singletonList(variant));
        when(product.getProductRetriever()).thenReturn(productRetriever);
        when(productRetriever.fetchProduct()).thenReturn(productData);
        when(variant.getSku()).thenReturn("meotwisus-red-9");
        when(variant.getName()).thenReturn("Sussex Rain Boots");
        when(variant.getPriceRange()).thenReturn(variantPrice);
        when(variant.getAssets()).thenReturn(Collections.singletonList(variantAsset));
        when(variantAsset.getPath()).thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg");
        when(resourceResolver.map(request, "/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg"))
            .thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg");

        ProductGridItem item = ProductGridItem.fromProduct(product, null, request,
            "/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html#meotwisus-red-9",
            "meotwisus-red-9");

        assertTrue(item.exists());
        assertEquals("$74.99", item.getPrice());
        assertEquals("/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg", item.getImage());
        assertEquals("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html#meotwisus-red-9",
            item.getPath());
    }

    private ConfigurableProduct createConfigurableProduct() {
        ConfigurableProduct product = mock(ConfigurableProduct.class);
        CategoryInterface category = createCategory("Footwear");
        ComplexTextValue shortDescription = createTextValue("<p>Lightweight training shoe</p>");
        ConfigurableProductOptions colorOption = createOption("color", "Red");
        ConfigurableProductOptions sizeOption = createOption("size", "9");

        when(product.getCategories()).thenReturn(Collections.singletonList(category));
        when(product.getShortDescription()).thenReturn(shortDescription);
        when(product.getConfigurableOptions()).thenReturn(Arrays.asList(colorOption, sizeOption));
        return product;
    }

    private ConfigurableProductOptions createOption(String attributeCode, String... labels) {
        ConfigurableProductOptions option = mock(ConfigurableProductOptions.class);
        List<ConfigurableProductOptionsValues> values = new ArrayList<ConfigurableProductOptionsValues>();
        for (String label : labels) {
            ConfigurableProductOptionsValues value = mock(ConfigurableProductOptionsValues.class);
            when(value.getLabel()).thenReturn(label);
            values.add(value);
        }
        when(option.getAttributeCode()).thenReturn(attributeCode);
        when(option.getValues()).thenReturn(values);
        return option;
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

    private ProductListItem createSimpleListItem(String title, Object sizeValue) {
        SimpleProduct product = mock(SimpleProduct.class);
        when(product.get("size")).thenReturn(sizeValue);
        Price price = createPrice("$79.99");
        ProductListItem listItem = mock(ProductListItem.class);
        when(listItem.getTitle()).thenReturn(title);
        when(listItem.getPriceRange()).thenReturn(price);
        when(listItem.getProduct()).thenReturn(product);
        return listItem;
    }

    private Price createPrice(String formattedFinalPrice) {
        Price price = mock(Price.class);
        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn(formattedFinalPrice);
        return price;
    }
}

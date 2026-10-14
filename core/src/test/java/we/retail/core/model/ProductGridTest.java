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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.models.productlist.CategoryRetriever;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

public class ProductGridTest {

    private static final String SEARCH_RESULTS_PAGE = "/content/we-retail/us/en/products/search";

    @Test
    public void testUsesExplicitCifCategorySelectionOnCatalogRoot() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        UrlProvider urlProvider = mock(UrlProvider.class);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever categoryRetriever = mock(CategoryRetriever.class);
        ProductListItem productListItem = mock(ProductListItem.class);
        ProductInterface product = mock(ProductInterface.class);
        Price price = mock(Price.class);
        CategoryInterface category = createCategory("Men");

        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getValueMap()).thenReturn(valueMap("category", "/"));
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(currentPageContent.getValueMap()).thenReturn(valueMap("cq:cifProductPage",
            "/content/we-retail/us/en/products/product-page"));
        when(productList.getCategoryRetriever()).thenReturn(categoryRetriever);
        when(productList.getTitle()).thenReturn("Default Category");
        when(productList.getProducts()).thenReturn(Collections.singletonList(productListItem));

        when(productListItem.getProduct()).thenReturn(product);
        when(productListItem.getTitle()).thenReturn("Corona Shorts");
        when(productListItem.getURL()).thenReturn("/content/we-retail/us/en/products/product-page.html/me/shorts/corona-shorts.html");
        when(productListItem.getPath()).thenReturn("/content/we-retail/us/en/products/product-page.html/me/shorts/corona-shorts.html");
        when(productListItem.getImageURL()).thenReturn("/content/dam/celadon/we-retail/me/shorts/mehisucos_img_0.jpeg");
        when(productListItem.getPriceRange()).thenReturn(price);
        when(product.getSku()).thenReturn("mehisucos");
        when(product.getUrlPath()).thenReturn("me/shorts/corona-shorts");
        when(urlProvider.toProductUrl(eq(request), eq(currentPage), isA(ProductUrlFormat.Params.class)))
            .thenReturn("/content/we-retail/us/en/products/product-page.html/me/shorts/corona-shorts.html");

        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn("$39.00");

        when(product.getCategories()).thenReturn(Collections.singletonList(category));
        when(resourceResolver.map(request, "/content/dam/celadon/we-retail/me/shorts/mehisucos_img_0.jpeg"))
            .thenReturn("/content/dam/celadon/we-retail/me/shorts/mehisucos_img_0.jpeg");

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "cifProductList", productList);
        setField(productGrid, "urlProvider", urlProvider);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        Collection<ProductGridItem> items = productGrid.getProducts();
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("/content/we-retail/us/en/products/product-page.html/me/shorts/corona-shorts.html",
            items.iterator().next().getPath());
        verify(urlProvider).toProductUrl(eq(request), eq(currentPage), isA(ProductUrlFormat.Params.class));
    }

    @Test
    public void testBuildRouteProductUrlUsesUrlProviderOnly() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Page currentPage = mock(Page.class);
        UrlProvider urlProvider = mock(UrlProvider.class);
        ProductListItem productListItem = mock(ProductListItem.class);
        ProductInterface product = mock(ProductInterface.class);

        when(productListItem.getProduct()).thenReturn(product);
        when(product.getSku()).thenReturn("eqbisublp");
        when(product.getUrlPath()).thenReturn("eq/biking/eqbisublp");
        when(urlProvider.toProductUrl(eq(request), eq(currentPage), isA(ProductUrlFormat.Params.class)))
            .thenReturn("/content/we-retail/us/en/products/product-page.html/eq/biking/eqbisublp.html");

        setField(productGrid, "request", request);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "urlProvider", urlProvider);

        Method routeUrlMethod = ProductGrid.class.getDeclaredMethod("buildRouteProductUrl", ProductListItem.class);
        routeUrlMethod.setAccessible(true);
        String routeUrl = (String) routeUrlMethod.invoke(productGrid, productListItem);

        assertEquals("/content/we-retail/us/en/products/product-page.html/eq/biking/eqbisublp.html", routeUrl);
        verify(urlProvider).toProductUrl(eq(request), eq(currentPage), isA(ProductUrlFormat.Params.class));
    }

    @Test
    public void testExtendsProductQueryBeforeReadingTitle() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever categoryRetriever = mock(CategoryRetriever.class);

        when(resource.getValueMap()).thenReturn(valueMap("category", "/"));
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(currentPageContent.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(productList.getCategoryRetriever()).thenReturn(categoryRetriever);
        when(productList.getTitle()).thenReturn("Default Category");
        when(productList.getProducts()).thenReturn(Collections.<ProductListItem>emptyList());

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "cifProductList", productList);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        InOrder inOrder = inOrder(categoryRetriever, productList);
        ArgumentCaptor<Consumer<ProductInterfaceQuery>> queryExtension = productQueryCaptor();
        inOrder.verify(categoryRetriever).extendProductQueryWith(queryExtension.capture());
        inOrder.verify(productList).getTitle();
        assertTrue(renderProductQuery(queryExtension.getValue()).contains("... on SimpleProduct{size_custom_:size}"));
    }

    @Test
    public void testUsesCifProductPickerSelectionForManualGrid() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        ModelFactory modelFactory = mock(ModelFactory.class);
        UrlProvider urlProvider = mock(UrlProvider.class);
        Product product = mock(Product.class);
        AbstractProductRetriever productRetriever = mock(AbstractProductRetriever.class);
        ProductInterface productData = mock(ProductInterface.class);
        Price price = createPrice("$39.00");

        when(resource.getValueMap()).thenReturn(valueMap("product", new String[] { "mehisucos#mehisucos-blue" }));
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(currentPageContent.getValueMap()).thenReturn(valueMap("cq:cifProductPage",
            "/content/we-retail/us/en/products/product-page"));
        when(modelFactory.getModelFromWrappedRequest(any(), isA(Resource.class), eq(Product.class))).thenReturn(product);
        when(product.getFound()).thenReturn(Boolean.TRUE);
        when(product.getName()).thenReturn("Corona Shorts");
        when(product.getPriceRange()).thenReturn(price);
        when(product.getProductRetriever()).thenReturn(productRetriever);
        when(productRetriever.fetchProduct()).thenReturn(productData);
        when(urlProvider.toProductUrl(request, currentPage, "mehisucos"))
            .thenReturn("/content/we-retail/us/en/products/product-page.html/me/shorts/mehisucos.html");

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "modelFactory", modelFactory);
        setField(productGrid, "urlProvider", urlProvider);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        Collection<ProductGridItem> items = productGrid.getProducts();
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("/content/we-retail/us/en/products/product-page.html/me/shorts/mehisucos.html#mehisucos-blue",
            items.iterator().next().getPath());
    }

    @Test
    public void testProductsModeDoesNotFallBackToLegacyPageSelections() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        Resource resource = mock(Resource.class);
        com.adobe.cq.wcm.core.components.models.List delegate = mock(com.adobe.cq.wcm.core.components.models.List.class);
        com.adobe.cq.wcm.core.components.models.ListItem listItem = mock(com.adobe.cq.wcm.core.components.models.ListItem.class);

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("listFrom", "products");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(values));
        when(delegate.getListItems()).thenReturn(Collections.singletonList(listItem));

        setField(productGrid, "resource", resource);
        setField(productGrid, "delegate", delegate);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        Collection<ProductGridItem> items = productGrid.getProducts();
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    public void testSearchQueryOnSearchResultsPageShowsSearchResults() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        UrlProvider urlProvider = mock(UrlProvider.class);
        SiteStructure siteStructure = createSiteStructure(SEARCH_RESULTS_PAGE);
        SearchResults searchResults = mock(SearchResults.class);
        ProductListItem productListItem = createProductListItem(request, resourceResolver, "Eton Short-Sleeve Shirt",
            "meotsuett", "men/shirts/eton-short-sleeve-shirt");

        when(request.getParameter("search_query")).thenReturn("shirt");
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPage.getPath()).thenReturn(SEARCH_RESULTS_PAGE);
        when(searchResults.getProducts()).thenReturn(Collections.singletonList(productListItem));
        when(urlProvider.toProductUrl(eq(request), eq(currentPage), isA(ProductUrlFormat.Params.class)))
            .thenReturn("/content/we-retail/us/en/products/product-page.html/men/shirts/eton-short-sleeve-shirt.html");

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "siteStructure", siteStructure);
        setField(productGrid, "cifSearchResults", searchResults);
        setField(productGrid, "urlProvider", urlProvider);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        Collection<ProductGridItem> items = productGrid.getProducts();
        assertEquals(1, items.size());
        assertEquals("Eton Short-Sleeve Shirt", items.iterator().next().getName());
        assertEquals("/content/we-retail/us/en/products/product-page.html/men/shirts/eton-short-sleeve-shirt.html",
            items.iterator().next().getPath());
        ArgumentCaptor<Consumer<ProductInterfaceQuery>> queryExtension = productQueryCaptor();
        verify(searchResults).extendProductQueryWith(queryExtension.capture());
        assertTrue(renderProductQuery(queryExtension.getValue()).contains("... on SimpleProduct{size_custom_:size}"));
    }

    @Test
    public void testSearchQueryOnCategoryPageKeepsCategoryProducts() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        UrlProvider urlProvider = mock(UrlProvider.class);
        SiteStructure siteStructure = createSiteStructure(SEARCH_RESULTS_PAGE);
        SearchResults searchResults = mock(SearchResults.class);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever categoryRetriever = mock(CategoryRetriever.class);
        ProductListItem productListItem = createProductListItem(request, resourceResolver, "Corona Shorts",
            "mehisucos", "men/shorts/corona-shorts");

        when(request.getParameter("search_query")).thenReturn("shirt");
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/category-page");
        when(siteStructure.isCategoryPage(currentPage)).thenReturn(true);
        when(productList.getCategoryRetriever()).thenReturn(categoryRetriever);
        when(productList.getTitle()).thenReturn("Men");
        when(productList.getProducts()).thenReturn(Collections.singletonList(productListItem));
        when(urlProvider.toProductUrl(eq(request), eq(currentPage), isA(ProductUrlFormat.Params.class)))
            .thenReturn("/content/we-retail/us/en/products/product-page.html/men/shorts/corona-shorts.html");

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "siteStructure", siteStructure);
        setField(productGrid, "cifProductList", productList);
        setField(productGrid, "cifSearchResults", searchResults);
        setField(productGrid, "urlProvider", urlProvider);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        Collection<ProductGridItem> items = productGrid.getProducts();
        assertEquals(1, items.size());
        assertEquals("Corona Shorts", items.iterator().next().getName());
        verify(searchResults, never()).getProducts();
    }

    @Test
    public void testSearchResultsPageWithoutQueryShowsNoProducts() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        SiteStructure siteStructure = createSiteStructure(SEARCH_RESULTS_PAGE);
        SearchResults searchResults = mock(SearchResults.class);

        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPage.getPath()).thenReturn(SEARCH_RESULTS_PAGE);

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "siteStructure", siteStructure);
        setField(productGrid, "cifSearchResults", searchResults);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        assertTrue(productGrid.getProducts().isEmpty());
        verify(searchResults, never()).getProducts();
    }

    private SiteStructure createSiteStructure(String searchResultsPagePath) {
        SiteStructure siteStructure = mock(SiteStructure.class);
        SiteStructure.Entry searchResultsEntry = mock(SiteStructure.Entry.class);
        Page searchResultsPage = mock(Page.class);
        when(searchResultsPage.getPath()).thenReturn(searchResultsPagePath);
        when(searchResultsEntry.getPage()).thenReturn(searchResultsPage);
        when(siteStructure.getSearchResultsPage()).thenReturn(searchResultsEntry);
        return siteStructure;
    }

    private ProductListItem createProductListItem(SlingHttpServletRequest request, ResourceResolver resourceResolver,
        String title, String sku, String urlPath) {
        ProductListItem productListItem = mock(ProductListItem.class);
        ProductInterface product = mock(ProductInterface.class);
        Price price = createPrice("$24.00");
        String imageUrl = "/content/dam/celadon/we-retail/" + urlPath + "_img_0.jpeg";

        when(productListItem.getProduct()).thenReturn(product);
        when(productListItem.getTitle()).thenReturn(title);
        when(productListItem.getImageURL()).thenReturn(imageUrl);
        when(productListItem.getPriceRange()).thenReturn(price);
        when(product.getSku()).thenReturn(sku);
        when(product.getUrlPath()).thenReturn(urlPath);
        when(resourceResolver.map(request, imageUrl)).thenReturn(imageUrl);
        return productListItem;
    }

    private CategoryInterface createCategory(String name) {
        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getName()).thenReturn(name);
        return category;
    }

    private ValueMapDecorator valueMap(String key, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key, value);
        return new ValueMapDecorator(values);
    }

    private ValueMapDecorator valueMap(String key, String[] value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key, value);
        return new ValueMapDecorator(values);
    }

    private Price createPrice(String formattedFinalPrice) {
        Price price = mock(Price.class);
        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn(formattedFinalPrice);
        return price;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Consumer<ProductInterfaceQuery>> productQueryCaptor() {
        return ArgumentCaptor.forClass((Class<Consumer<ProductInterfaceQuery>>) (Class<?>) Consumer.class);
    }

    private String renderProductQuery(Consumer<ProductInterfaceQuery> extension) {
        return Operations.query(query -> query.products(arguments -> arguments.search(""), products -> products.items(extension::accept)))
            .toString();
    }
}

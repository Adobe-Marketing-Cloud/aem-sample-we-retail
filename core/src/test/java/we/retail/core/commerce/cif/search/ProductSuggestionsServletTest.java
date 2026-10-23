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
package we.retail.core.commerce.cif.search;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductSuggestionsServletTest {

    private static final String PAGE_PATH = "/content/we-retail/us/en";
    private static final String SEARCH_RESULTS_PAGE_PATH = "/content/we-retail/us/en/products/search";
    private static final String PRODUCT_PAGE_URL = "/content/we-retail/us/en/products/product-page.html/";

    private ProductSuggestionsServlet servlet;
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private StringWriter output;
    private Page page;
    private ModelFactory modelFactory;
    private UrlProvider urlProvider;
    private SearchResults searchResults;
    private SearchResultsSet searchResultsSet;
    private SiteStructure.Entry searchResultsEntry;
    private SiteStructure siteStructure;

    @Before
    public void setUp() throws Exception {
        servlet = new ProductSuggestionsServlet();
        request = mock(SlingHttpServletRequest.class);
        response = mock(SlingHttpServletResponse.class);
        output = new StringWriter();
        page = mock(Page.class);
        modelFactory = mock(ModelFactory.class);
        urlProvider = mock(UrlProvider.class);
        searchResults = mock(SearchResults.class);
        searchResultsSet = mock(SearchResultsSet.class);
        searchResultsEntry = mock(SiteStructure.Entry.class);

        Resource pageResource = mock(Resource.class);
        Resource contentResource = mock(Resource.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        siteStructure = mock(SiteStructure.class);
        Page searchResultsPage = mock(Page.class);

        when(request.getResource()).thenReturn(pageResource);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        when(pageResource.adaptTo(Page.class)).thenReturn(page);
        when(page.getPath()).thenReturn(PAGE_PATH);
        when(page.getContentResource()).thenReturn(contentResource);
        when(page.getLanguage(false)).thenReturn(Locale.US);
        when(page.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(siteStructure.getSearchResultsPage()).thenReturn(searchResultsEntry);
        when(searchResultsEntry.getPage()).thenReturn(searchResultsPage);
        when(searchResultsPage.getPath()).thenReturn(SEARCH_RESULTS_PAGE_PATH);
        when(resourceResolver.map(request, SEARCH_RESULTS_PAGE_PATH)).thenReturn(SEARCH_RESULTS_PAGE_PATH);
        when(modelFactory.getModelFromWrappedRequest(eq(request), any(Resource.class), eq(SearchResults.class)))
            .thenReturn(searchResults);
        when(searchResults.getSearchResultsSet()).thenReturn(searchResultsSet);
        when(urlProvider.toProductUrl(eq(request), eq(page), isA(ProductUrlFormat.Params.class))).thenAnswer(invocation ->
            PRODUCT_PAGE_URL + ((ProductUrlFormat.Params) invocation.getArguments()[2]).getUrlPath() + ".html");

        setField("modelFactory", modelFactory);
        setField("urlProvider", urlProvider);
    }

    @Test
    public void testSuggestsTopProductsWithTotalAndResultsUrl() throws Exception {
        List<ProductListItem> products = new ArrayList<ProductListItem>();
        for (int i = 1; i <= 6; i++) {
            products.add(createProduct("Shirt " + i, "sku" + i, "men/shirts/shirt-" + i));
        }
        when(request.getParameter("search_query")).thenReturn(" short sleeve ");
        when(searchResults.getProducts()).thenReturn(products);
        when(searchResultsSet.getTotalResults()).thenReturn(6);

        JsonObject suggestions = get();

        JsonArray suggestedProducts = suggestions.getJsonArray("products");
        assertEquals(ProductSuggestionsServlet.MAX_SUGGESTIONS, suggestedProducts.size());
        assertEquals("Shirt 1", suggestedProducts.getJsonObject(0).getString("title"));
        assertEquals(PRODUCT_PAGE_URL + "men/shirts/shirt-1.html", suggestedProducts.getJsonObject(0).getString("url"));
        assertEquals("short sleeve", suggestions.getString("searchTerm"));
        assertEquals(6, suggestions.getInt("total"));
        assertEquals(SEARCH_RESULTS_PAGE_PATH + ".html?search_query=short+sleeve", suggestions.getString("resultsUrl"));
        assertEquals("Products", suggestions.getJsonObject("labels").getString("products"));
        assertEquals("Pages", suggestions.getJsonObject("labels").getString("pages"));
        assertEquals("See all results for \"short sleeve\" (6)", suggestions.getJsonObject("labels").getString("seeAll"));

        ArgumentCaptor<Resource> searchResource = ArgumentCaptor.forClass(Resource.class);
        verify(modelFactory).getModelFromWrappedRequest(eq(request), searchResource.capture(), eq(SearchResults.class));
        assertEquals("core/cif/components/commerce/searchresults/v1/searchresults", searchResource.getValue().getResourceType());
        assertEquals(ProductSuggestionsServlet.MAX_SUGGESTIONS,
            searchResource.getValue().getValueMap().get("pageSize", Integer.class).intValue());
    }

    @Test
    public void testBlankSearchTermDoesNotSearch() throws Exception {
        when(request.getParameter("search_query")).thenReturn("   ");

        JsonObject suggestions = get();

        assertTrue(suggestions.getJsonArray("products").isEmpty());
        assertEquals(0, suggestions.getInt("total"));
        assertTrue(suggestions.isNull("resultsUrl"));
        verify(modelFactory, never()).getModelFromWrappedRequest(any(SlingHttpServletRequest.class), any(Resource.class),
            eq(SearchResults.class));
    }

    @Test
    public void testMissingSearchResultsPageHasNoResultsUrl() throws Exception {
        ProductListItem product = createProduct("Eton Shirt", "meotsuett", "men/shirts/eton-short-sleeve-shirt");
        when(request.getParameter("search_query")).thenReturn("shirt");
        when(searchResults.getProducts()).thenReturn(Collections.singletonList(product));
        when(searchResultsSet.getTotalResults()).thenReturn(1);
        when(searchResultsEntry.getPage()).thenReturn(null);

        JsonObject suggestions = get();

        assertEquals(1, suggestions.getJsonArray("products").size());
        assertTrue(suggestions.isNull("resultsUrl"));
    }

    @Test
    public void testSkipsProductsWithoutUrl() throws Exception {
        ProductListItem withoutProduct = mock(ProductListItem.class);
        when(withoutProduct.getTitle()).thenReturn("Broken");
        List<ProductListItem> products = new ArrayList<ProductListItem>();
        products.add(withoutProduct);
        products.add(createProduct("Eton Shirt", "meotsuett", "men/shirts/eton-short-sleeve-shirt"));
        when(request.getParameter("search_query")).thenReturn("shirt");
        when(searchResults.getProducts()).thenReturn(products);
        when(searchResultsSet.getTotalResults()).thenReturn(2);

        JsonArray suggestedProducts = get().getJsonArray("products");

        assertEquals(1, suggestedProducts.size());
        assertEquals("Eton Shirt", suggestedProducts.getJsonObject(0).getString("title"));
    }

    @Test
    public void testSearchFailureSuggestsNothing() throws Exception {
        when(request.getParameter("search_query")).thenReturn("shirt");
        when(searchResults.getProducts()).thenThrow(new IllegalStateException("GraphQL unavailable"));

        JsonObject suggestions = get();

        assertTrue(suggestions.getJsonArray("products").isEmpty());
        assertEquals(0, suggestions.getInt("total"));
        assertEquals(SEARCH_RESULTS_PAGE_PATH + ".html?search_query=shirt", suggestions.getString("resultsUrl"));
    }

    @Test
    public void testScopesSuggestionsAndResultsToTheCurrentCategory() throws Exception {
        ProductListItem product = createProduct("Corona Shorts", "meshcor", "men/shorts/corona-shorts");
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("search_query", new String[] { "shorts" });
        when(request.getParameter("search_query")).thenReturn("shorts");
        when(request.getParameterMap()).thenReturn(parameters);
        when(siteStructure.isCategoryPage(page)).thenReturn(true);
        when(urlProvider.getCategoryIdentifier(request)).thenReturn("men");
        mockCategoryName("Men");
        SearchResults scopedResults = mock(SearchResults.class);
        when(scopedResults.getProducts()).thenReturn(Collections.singletonList(product));
        when(scopedResults.getSearchResultsSet()).thenReturn(searchResultsSet);
        when(searchResultsSet.getTotalResults()).thenReturn(5);
        when(modelFactory.getModelFromWrappedRequest(any(SlingHttpServletRequest.class), any(Resource.class), eq(SearchResults.class)))
            .thenReturn(scopedResults);

        JsonObject suggestions = get();

        assertEquals(5, suggestions.getInt("total"));
        assertEquals(PRODUCT_PAGE_URL + "men/shorts/corona-shorts.html", suggestions.getJsonArray("products").getJsonObject(0).getString("url"));
        assertEquals(SEARCH_RESULTS_PAGE_PATH + ".html?search_query=shorts&category_uid=men", suggestions.getString("resultsUrl"));
        assertEquals("See all results for \"shorts\" in Men (5)", suggestions.getJsonObject("labels").getString("seeAll"));

        ArgumentCaptor<SlingHttpServletRequest> searchRequest = ArgumentCaptor.forClass(SlingHttpServletRequest.class);
        verify(modelFactory).getModelFromWrappedRequest(searchRequest.capture(), any(Resource.class), eq(SearchResults.class));
        assertEquals("men", searchRequest.getValue().getParameter("category_uid"));
        assertEquals("shorts", searchRequest.getValue().getParameter("search_query"));
        assertArrayEquals(new String[] { "men" }, (String[]) searchRequest.getValue().getParameterMap().get("category_uid"));
    }

    @Test
    public void testSearchesEverywhereWhenTheCategoryCannotBeResolved() throws Exception {
        when(request.getParameter("search_query")).thenReturn("shorts");
        when(siteStructure.isCategoryPage(page)).thenReturn(true);
        when(urlProvider.getCategoryIdentifier(request)).thenReturn("men");
        when(searchResults.getProducts()).thenReturn(Collections.<ProductListItem>emptyList());
        when(searchResultsSet.getTotalResults()).thenReturn(8);

        JsonObject suggestions = get();

        assertEquals(SEARCH_RESULTS_PAGE_PATH + ".html?search_query=shorts", suggestions.getString("resultsUrl"));
        assertEquals("See all results for \"shorts\" (8)", suggestions.getJsonObject("labels").getString("seeAll"));
        verify(modelFactory).getModelFromWrappedRequest(eq(request), any(Resource.class), eq(SearchResults.class));
    }

    private JsonObject get() throws Exception {
        servlet.doGet(request, response);
        verify(response).setContentType("application/json");
        return Json.createReader(new StringReader(output.toString())).readObject();
    }

    @SuppressWarnings("unchecked")
    private void mockCategoryName(String name) {
        MagentoGraphqlClient magentoGraphqlClient = mock(MagentoGraphqlClient.class);
        GraphqlResponse<Query, Error> graphqlResponse = mock(GraphqlResponse.class);
        Query data = mock(Query.class);
        CategoryTree category = mock(CategoryTree.class);
        when(category.getName()).thenReturn(name);
        when(data.getCategoryList()).thenReturn(Collections.singletonList(category));
        when(graphqlResponse.getData()).thenReturn(data);
        when(magentoGraphqlClient.execute(anyString())).thenReturn(graphqlResponse);
        when(request.adaptTo(MagentoGraphqlClient.class)).thenReturn(magentoGraphqlClient);
    }

    private ProductListItem createProduct(String title, String sku, String urlPath) {
        ProductListItem productListItem = mock(ProductListItem.class);
        ProductInterface product = mock(ProductInterface.class);
        when(productListItem.getTitle()).thenReturn(title);
        when(productListItem.getProduct()).thenReturn(product);
        when(product.getSku()).thenReturn(sku);
        when(product.getUrlPath()).thenReturn(urlPath);
        return productListItem;
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = ProductSuggestionsServlet.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(servlet, value);
    }
}

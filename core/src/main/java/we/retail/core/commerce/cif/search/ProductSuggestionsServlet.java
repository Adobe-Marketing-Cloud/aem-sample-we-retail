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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;

import we.retail.core.model.ProductGrid;

/**
 * Suggests the best matching products for a search term, together with the link to the site's search results page.
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=cq/Page",
        "sling.servlet.selectors=" + ProductSuggestionsServlet.SELECTOR,
        "sling.servlet.extensions=json",
        "sling.servlet.methods=GET"
    })
public class ProductSuggestionsServlet extends SlingSafeMethodsServlet {

    static final String SELECTOR = "productsuggestions";
    static final int MAX_SUGGESTIONS = 5;

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSuggestionsServlet.class);
    private static final String SEARCH_QUERY_PARAMETER = "search_query";
    private static final String PN_PAGE_SIZE = "pageSize";
    private static final String CIF_SEARCH_RESULTS_RESOURCE_TYPE = "core/cif/components/commerce/searchresults/v1/searchresults";

    private transient ModelFactory modelFactory;

    private transient UrlProvider urlProvider;

    @Reference
    void bindModelFactory(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Reference
    void bindUrlProvider(UrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String searchTerm = StringUtils.trimToEmpty(request.getParameter(SEARCH_QUERY_PARAMETER));
        Page page = request.getResource().adaptTo(Page.class);

        JsonArrayBuilder products = Json.createArrayBuilder();
        int total = 0;
        String resultsUrl = null;
        if (page != null && StringUtils.isNotEmpty(searchTerm)) {
            total = addProducts(request, page, searchTerm, products);
            resultsUrl = buildResultsUrl(request, page, searchTerm);
        }

        ResourceBundle resourceBundle = request.getResourceBundle(page != null ? page.getLanguage(false) : request.getLocale());
        JsonObjectBuilder labels = Json.createObjectBuilder()
            .add("products", I18n.get(resourceBundle, "Products"))
            .add("pages", I18n.get(resourceBundle, "Pages"))
            .add("seeAll", I18n.get(resourceBundle, "See all results for \"{0}\" ({1})", null, searchTerm, total));

        JsonObjectBuilder suggestions = Json.createObjectBuilder()
            .add("searchTerm", searchTerm)
            .add("total", total)
            .add("products", products)
            .add("labels", labels);
        if (resultsUrl != null) {
            suggestions.add("resultsUrl", resultsUrl);
        } else {
            suggestions.addNull("resultsUrl");
        }

        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(suggestions.build().toString());
    }

    private int addProducts(SlingHttpServletRequest request, Page page, String searchTerm, JsonArrayBuilder products) {
        try {
            SearchResults searchResults = searchProducts(request, page);
            if (searchResults == null) {
                return 0;
            }

            int count = 0;
            for (ProductListItem productListItem : searchResults.getProducts()) {
                if (count == MAX_SUGGESTIONS) {
                    break;
                }

                String title = productListItem.getTitle();
                String url = buildProductUrl(request, page, productListItem);
                if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(url)) {
                    products.add(Json.createObjectBuilder().add("title", title).add("url", url));
                    count++;
                }
            }

            SearchResultsSet searchResultsSet = searchResults.getSearchResultsSet();
            Integer totalResults = searchResultsSet != null ? searchResultsSet.getTotalResults() : null;
            return totalResults != null ? totalResults : count;
        } catch (RuntimeException e) {
            LOGGER.warn("Could not suggest products for '" + searchTerm + "' on " + page.getPath(), e);
            return 0;
        }
    }

    private SearchResults searchProducts(SlingHttpServletRequest request, Page page) {
        Resource contentResource = page.getContentResource();
        if (contentResource == null) {
            return null;
        }

        ValueMapResourceWrapper searchResultsResource = new ValueMapResourceWrapper(contentResource, CIF_SEARCH_RESULTS_RESOURCE_TYPE);
        searchResultsResource.getValueMap().put(PN_PAGE_SIZE, MAX_SUGGESTIONS);
        return modelFactory.getModelFromWrappedRequest(request, searchResultsResource, SearchResults.class);
    }

    private String buildProductUrl(SlingHttpServletRequest request, Page page, ProductListItem productListItem) {
        ProductInterface product = productListItem.getProduct();
        if (product == null) {
            return StringUtils.EMPTY;
        }

        return ProductGrid.toProductUrl(urlProvider, request, page, product.getSku(), product.getUrlPath(), null);
    }

    private String buildResultsUrl(SlingHttpServletRequest request, Page page, String searchTerm) throws IOException {
        SiteStructure siteStructure = page.adaptTo(SiteStructure.class);
        SiteStructure.Entry searchResultsEntry = siteStructure != null ? siteStructure.getSearchResultsPage() : null;
        Page searchResultsPage = searchResultsEntry != null ? searchResultsEntry.getPage() : null;
        if (searchResultsPage == null) {
            return null;
        }

        return request.getResourceResolver().map(request, searchResultsPage.getPath()) + ".html?" + SEARCH_QUERY_PARAMETER + "="
            + URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.name());
    }
}

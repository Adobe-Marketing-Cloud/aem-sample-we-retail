/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe Systems Incorporated
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ForcedResourceType;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.apache.sling.models.factory.ModelFactory;
import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = com.adobe.cq.wcm.core.components.models.List.class,
    resourceType = "weretail/components/content/productgrid")
public class ProductGrid implements com.adobe.cq.wcm.core.components.models.List {
    private static final String PN_CATEGORY = "category";
    private static final String PN_LIST_FROM = "listFrom";
    private static final String PN_PRODUCT = "product";
    private static final String PN_SELECTION = "selection";
    private static final String LIST_FROM_PRODUCTS = "products";
    private static final String SEARCH_QUERY_PARAMETER = "search_query";
    private static final String CIF_PRODUCT_RESOURCE_TYPE = "core/cif/components/commerce/product/v2/product";
    private static final String CIF_PRODUCT_LIST_RESOURCE_TYPE = "core/cif/components/commerce/productlist/v2/productlist";
    private static final String CIF_SEARCH_RESULTS_RESOURCE_TYPE = "core/cif/components/commerce/searchresults/v1/searchresults";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @Self
    @Via(type = ResourceSuperType.class)
    private com.adobe.cq.wcm.core.components.models.List delegate;

    @ScriptVariable
    private Page currentPage;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private SiteStructure siteStructure;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Via(type = ForcedResourceType.class, value = CIF_PRODUCT_LIST_RESOURCE_TYPE)
    private ProductList cifProductList;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Via(type = ForcedResourceType.class, value = CIF_SEARCH_RESULTS_RESOURCE_TYPE)
    private SearchResults cifSearchResults;

    @OSGiService
    private ModelFactory modelFactory;

    @OSGiService
    private UrlProvider urlProvider;

    private java.util.List<ProductGridItem> items = Collections.emptyList();

    @PostConstruct
    private void initModel() {
        if (isExplicitProductListMode() || hasExplicitCifProductSelection()) {
            items = buildSelectedProductItems();
            return;
        }

        if (hasSearchQuery() && isSearchResultsPage()) {
            items = buildSearchItems();
            return;
        }

        boolean routeCategoryPage = siteStructure != null && currentPage != null && siteStructure.isCategoryPage(currentPage);
        boolean explicitCategorySelection = hasExplicitCifCategorySelection();

        if (routeCategoryPage || explicitCategorySelection) {
            items = buildRouteItems();
            if (explicitCategorySelection || hasUsableRouteItems(items)) {
                return;
            }
        }

        items = Collections.emptyList();
    }

    public Collection<ProductGridItem> getProducts() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public Collection<ListItem> getListItems() {
        return delegate != null ? delegate.getListItems() : Collections.<ListItem>emptyList();
    }

    private java.util.List<ProductGridItem> buildSelectedProductItems() {
        java.util.List<ProductGridItem> selectedItems = new ArrayList<ProductGridItem>();
        for (SelectedProductSelection selection : getConfiguredProductSelections()) {
            try {
                Product product = adaptSelectedProduct(selection.getBaseSku());
                if (product == null || !Boolean.TRUE.equals(product.getFound()) || isPlaceholderProductName(product.getName())) {
                    continue;
                }

                String routeUrl = buildSelectedProductUrl(product, selection.getBaseSku(), selection.getVariantSku());
                ProductGridItem item = ProductGridItem.fromProduct(product, null, request, routeUrl, selection.getVariantSku());
                if (item.exists()) {
                    selectedItems.add(item);
                }
            } catch (RuntimeException e) {
                // Ignore broken selections so one bad picker value does not fail the entire grid.
            }
        }
        return selectedItems;
    }

    private java.util.List<ProductGridItem> buildRouteItems() {
        java.util.List<ProductGridItem> routeItems = new ArrayList<ProductGridItem>();
        try {
            ProductList productList = cifProductList;
            if (productList == null || productList.getCategoryRetriever() == null) {
                return routeItems;
            }
            productList.getCategoryRetriever().extendProductQueryWith(query -> query
                .shortDescription(description -> description.html())
                .categories(category -> category.name().urlPath())
                .onConfigurableProduct(configurableProduct -> configurableProduct.configurableOptions(option -> option
                    .attributeCode()
                    .label()
                    .values(value -> value
                        .label()
                        .valueIndex()
                        .defaultLabel()))));
            if (isPlaceholderCategoryTitle(productList.getTitle())) {
                return Collections.emptyList();
            }

            for (ProductListItem productListItem : productList.getProducts()) {
                try {
                    String productUrl = buildRouteProductUrl(productListItem);
                    ProductGridItem item = ProductGridItem.fromProductListItem(productListItem, request, productUrl);
                    if (item.exists()) {
                        routeItems.add(item);
                    }
                } catch (RuntimeException e) {
                    // Keep rendering the rest of the catalog if one product has bad URL metadata.
                }
            }
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }

        return routeItems;
    }

    private java.util.List<ProductGridItem> buildSearchItems() {
        java.util.List<ProductGridItem> searchItems = new ArrayList<ProductGridItem>();
        try {
            SearchResults searchResults = cifSearchResults;
            if (searchResults == null) {
                return searchItems;
            }
            searchResults.extendProductQueryWith(query -> query
                .shortDescription(description -> description.html())
                .categories(category -> category.name().urlPath())
                .onConfigurableProduct(configurableProduct -> configurableProduct.configurableOptions(option -> option
                    .attributeCode()
                    .label()
                    .values(value -> value
                        .label()
                        .valueIndex()
                        .defaultLabel()))));

            for (ProductListItem productListItem : searchResults.getProducts()) {
                try {
                    String productUrl = buildRouteProductUrl(productListItem);
                    ProductGridItem item = ProductGridItem.fromProductListItem(productListItem, request, productUrl);
                    if (item.exists()) {
                        searchItems.add(item);
                    }
                } catch (RuntimeException e) {
                    // Keep rendering the rest of the results if one product has bad URL metadata.
                }
            }
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }

        return searchItems;
    }

    private boolean hasSearchQuery() {
        return request != null && StringUtils.isNotBlank(request.getParameter(SEARCH_QUERY_PARAMETER));
    }

    private boolean isSearchResultsPage() {
        if (siteStructure == null || currentPage == null) {
            return false;
        }

        SiteStructure.Entry searchResultsPage = siteStructure.getSearchResultsPage();
        return searchResultsPage != null && searchResultsPage.getPage() != null
            && StringUtils.equals(searchResultsPage.getPage().getPath(), currentPage.getPath());
    }

    private boolean hasExplicitCifProductSelection() {
        return !getConfiguredProductSelections().isEmpty();
    }

    private boolean isExplicitProductListMode() {
        return resource != null && StringUtils.equals(resource.getValueMap().get(PN_LIST_FROM, String.class), LIST_FROM_PRODUCTS);
    }

    private boolean hasExplicitCifCategorySelection() {
        return resource != null && StringUtils.isNotBlank(resource.getValueMap().get(PN_CATEGORY, String.class));
    }

    private List<SelectedProductSelection> getConfiguredProductSelections() {
        if (resource == null) {
            return Collections.emptyList();
        }

        String[] configuredProducts = resource.getValueMap().get(PN_PRODUCT, String[].class);
        if (configuredProducts == null) {
            String configuredProduct = resource.getValueMap().get(PN_PRODUCT, String.class);
            if (StringUtils.isNotBlank(configuredProduct)) {
                configuredProducts = new String[] { configuredProduct };
            }
        }

        if (configuredProducts == null || configuredProducts.length == 0) {
            return Collections.emptyList();
        }

        List<SelectedProductSelection> selections = new ArrayList<SelectedProductSelection>();
        for (String configuredProduct : configuredProducts) {
            SelectedProductSelection selection = SelectedProductSelection.from(configuredProduct);
            if (selection != null) {
                selections.add(selection);
            }
        }
        return selections;
    }

    private Product adaptSelectedProduct(String sku) {
        if (modelFactory == null || request == null || resource == null || StringUtils.isBlank(sku)) {
            return null;
        }

        ValueMapResourceWrapper wrappedResource = new ValueMapResourceWrapper(resource, CIF_PRODUCT_RESOURCE_TYPE);
        wrappedResource.getValueMap().put(PN_SELECTION, sku);
        return modelFactory.getModelFromWrappedRequest(request, wrappedResource, Product.class);
    }

    private String buildRouteProductUrl(ProductListItem productListItem) {
        ProductInterface product = productListItem != null ? productListItem.getProduct() : null;
        String providerUrl = resolveProductUrl(
            product != null ? product.getSku() : null,
            product != null ? product.getUrlPath() : null,
            null);
        if (StringUtils.isNotBlank(providerUrl)) {
            return providerUrl;
        }

        return StringUtils.EMPTY;
    }

    private String buildSelectedProductUrl(Product product, String baseSku, String variantSku) {
        ProductInterface productData = ProductItem.fetchProduct(product);
        String providerUrl = resolveProductUrl(baseSku, productData != null ? productData.getUrlPath() : null, variantSku);
        if (StringUtils.isNotBlank(providerUrl)) {
            return providerUrl;
        }

        return StringUtils.EMPTY;
    }

    private boolean hasUsableRouteItems(List<ProductGridItem> routeItems) {
        if (routeItems.isEmpty()) {
            return false;
        }

        for (ProductGridItem item : routeItems) {
            if (!isPlaceholderProductListItem(item.getName())) {
                return true;
            }
        }

        return false;
    }

    private String resolveProductUrl(String productSku, String productUrlPath, String variantSku) {
        if (request == null || currentPage == null || urlProvider == null) {
            return StringUtils.EMPTY;
        }

        return toProductUrl(urlProvider, request, currentPage, productSku, productUrlPath, variantSku);
    }

    public static String toProductUrl(UrlProvider urlProvider, SlingHttpServletRequest request, Page page, String productSku,
        String productUrlPath, String variantSku) {
        if (StringUtils.isNotBlank(productUrlPath)) {
            ProductUrlFormat.Params params = new ProductUrlFormat.Params();
            params.setUrlPath(productUrlPath);
            params.setSku(productSku);
            params.setVariantSku(variantSku);

            String productUrl = urlProvider.toProductUrl(request, page, params);
            if (StringUtils.isNotBlank(productUrl)) {
                return appendVariantSkuFragment(productUrl, variantSku);
            }
        }

        if (StringUtils.isBlank(productSku)) {
            return StringUtils.EMPTY;
        }

        return appendVariantSkuFragment(urlProvider.toProductUrl(request, page, productSku), variantSku);
    }

    private static String appendVariantSkuFragment(String link, String variantSku) {
        if (StringUtils.isBlank(link) || StringUtils.isBlank(variantSku)) {
            return link;
        }

        return StringUtils.substringBefore(link, "#") + "#" + variantSku;
    }

    private static boolean isPlaceholderCategoryTitle(String title) {
        return StringUtils.equals(title, "Category name");
    }

    private static boolean isPlaceholderProductName(String name) {
        return StringUtils.equals(name, "Product name");
    }

    private static boolean isPlaceholderProductListItem(String name) {
        return StringUtils.startsWith(name, "Product #");
    }

    @Override
    public boolean linkItems() {
        return delegate != null && delegate.linkItems();
    }

    @Override
    public boolean showDescription() {
        return delegate != null && delegate.showDescription();
    }

    @Override
    public boolean showModificationDate() {
        return delegate != null && delegate.showModificationDate();
    }

    @Override
    public String getDateFormatString() {
        return delegate != null ? delegate.getDateFormatString() : StringUtils.EMPTY;
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }

    private static final class SelectedProductSelection {

        private final String baseSku;
        private final String variantSku;

        private SelectedProductSelection(String baseSku, String variantSku) {
            this.baseSku = baseSku;
            this.variantSku = variantSku;
        }

        private static SelectedProductSelection from(String configuredProduct) {
            String normalizedSelection = StringUtils.trimToEmpty(configuredProduct);
            if (StringUtils.isBlank(normalizedSelection)) {
                return null;
            }
            if (normalizedSelection.startsWith("/")) {
                normalizedSelection = StringUtils.substringAfterLast(normalizedSelection, "/");
            }

            CombinedSku combinedSku = CombinedSku.parse(normalizedSelection);
            if (StringUtils.isBlank(combinedSku.getBaseSku())) {
                return null;
            }
            return new SelectedProductSelection(combinedSku.getBaseSku(), combinedSku.getVariantSku());
        }

        private String getBaseSku() {
            return baseSku;
        }

        private String getVariantSku() {
            return variantSku;
        }
    }

}

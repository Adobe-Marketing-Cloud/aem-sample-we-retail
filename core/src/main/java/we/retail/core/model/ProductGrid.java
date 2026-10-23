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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ForcedResourceType;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductsRetriever;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;

import we.retail.core.commerce.cif.CatalogPlaceholders;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = com.adobe.cq.wcm.core.components.models.List.class,
    resourceType = "weretail/components/content/productgrid")
public class ProductGrid implements com.adobe.cq.wcm.core.components.models.List {
    private static final String PN_CATEGORY = "category";
    private static final String PN_LIST_FROM = "listFrom";
    private static final String PN_PRODUCT = "product";
    private static final String LIST_FROM_PRODUCTS = "products";
    private static final String SEARCH_QUERY_PARAMETER = "search_query";
    private static final String CIF_PRODUCT_CAROUSEL_RESOURCE_TYPE = "core/cif/components/commerce/productcarousel/v1/productcarousel";
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

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Via(type = ForcedResourceType.class, value = CIF_PRODUCT_CAROUSEL_RESOURCE_TYPE)
    private ProductCarousel cifProductCarousel;

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
        try {
            AbstractProductsRetriever productsRetriever = cifProductCarousel != null ? cifProductCarousel.getProductsRetriever() : null;
            if (productsRetriever == null) {
                return selectedItems;
            }
            productsRetriever.extendProductQueryWith(ProductGrid::extendProductQuery);

            // A selected variant is listed with its own name, image and price, but keeps the label and filters of its product.
            Map<String, ProductInterface> selectedProducts = new HashMap<String, ProductInterface>();
            for (ProductInterface product : productsRetriever.fetchProducts()) {
                if (product != null) {
                    selectedProducts.put(product.getSku(), product);
                }
            }

            for (ProductListItem productListItem : cifProductCarousel.getProducts()) {
                try {
                    ProductGridItem item = ProductGridItem.fromProductListItem(productListItem, request,
                        selectedProducts.get(productListItem.getSKU()));
                    if (item.exists()) {
                        selectedItems.add(item);
                    }
                } catch (RuntimeException e) {
                    // Ignore broken selections so one bad picker value does not fail the entire grid.
                }
            }
        } catch (RuntimeException e) {
            return Collections.emptyList();
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
            productList.getCategoryRetriever().extendProductQueryWith(ProductGrid::extendProductQuery);
            if (CatalogPlaceholders.isCategoryName(productList.getTitle())) {
                return Collections.emptyList();
            }

            for (ProductListItem productListItem : productList.getProducts()) {
                try {
                    ProductGridItem item = ProductGridItem.fromProductListItem(productListItem, request);
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
            searchResults.extendProductQueryWith(ProductGrid::extendProductQuery);

            for (ProductListItem productListItem : searchResults.getProducts()) {
                try {
                    ProductGridItem item = ProductGridItem.fromProductListItem(productListItem, request);
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

    private static void extendProductQuery(ProductInterfaceQuery query) {
        query
            .shortDescription(description -> description.html())
            .categories(category -> category.name().urlPath())
            .onConfigurableProduct(configurableProduct -> configurableProduct.configurableOptions(option -> option
                .attributeCode()
                .label()
                .values(value -> value
                    .label()
                    .valueIndex()
                    .defaultLabel())))
            .onSimpleProduct(simpleProduct -> simpleProduct.addCustomSimpleField(ProductGridItem.SIZE_ATTRIBUTE));
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
        String[] configuredProducts = resource != null ? resource.getValueMap().get(PN_PRODUCT, String[].class) : null;
        if (configuredProducts != null) {
            for (String configuredProduct : configuredProducts) {
                if (StringUtils.isNotBlank(configuredProduct)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isExplicitProductListMode() {
        return resource != null && StringUtils.equals(resource.getValueMap().get(PN_LIST_FROM, String.class), LIST_FROM_PRODUCTS);
    }

    private boolean hasExplicitCifCategorySelection() {
        return resource != null && StringUtils.isNotBlank(resource.getValueMap().get(PN_CATEGORY, String.class));
    }

    private boolean hasUsableRouteItems(List<ProductGridItem> routeItems) {
        if (routeItems.isEmpty()) {
            return false;
        }

        for (ProductGridItem item : routeItems) {
            if (!CatalogPlaceholders.isProductListItemTitle(item.getName())) {
                return true;
            }
        }

        return false;
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
}

/*
 *   Copyright 2026 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package we.retail.core.model;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PageHeadMetadata {
    private static final String WERETAIL_PRODUCT_RESOURCE_TYPE = "weretail/components/structure/product";
    private static final String WERETAIL_PRODUCT_GRID_RESOURCE_TYPE = "weretail/components/content/productgrid";
    private static final String CIF_PRODUCT_RESOURCE_TYPE = "core/cif/components/commerce/product/v2/product";
    private static final String CIF_PRODUCT_LIST_RESOURCE_TYPE = "core/cif/components/commerce/productlist/v2/productlist";
    private static final String CATEGORY_PAGE_TITLE = "Category Page";
    private static final String PRODUCT_PAGE_TITLE = "Product Page";
    private static final String CATEGORY_PLACEHOLDER = "Category name";
    private static final String PRODUCT_PLACEHOLDER = "Product name";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @Self
    private SiteStructure siteStructure;

    @OSGiService
    private ModelFactory modelFactory;

    private Product productModel;
    private ProductList productListModel;

    @PostConstruct
    private void initModel() {
        if (modelFactory == null || request == null || siteStructure == null || currentPage == null) {
            return;
        }

        Resource pageContent = currentPage.getContentResource();
        Resource templateStructure = getTemplateStructureResource();

        if (siteStructure.isProductPage(currentPage)) {
            productModel = adaptComponent(pageContent, templateStructure, WERETAIL_PRODUCT_RESOURCE_TYPE, CIF_PRODUCT_RESOURCE_TYPE,
                Product.class);
            return;
        }

        if (siteStructure.isCategoryPage(currentPage)) {
            productListModel = adaptComponent(pageContent, templateStructure, WERETAIL_PRODUCT_GRID_RESOURCE_TYPE,
                CIF_PRODUCT_LIST_RESOURCE_TYPE,
                ProductList.class);
        }
    }

    public String getTitle() {
        String commerceTitle = firstMeaningful(
            productModel != null ? productModel.getMetaTitle() : null,
            productModel != null ? productModel.getName() : null,
            productListModel != null ? productListModel.getMetaTitle() : null,
            productListModel != null ? productListModel.getTitle() : null);
        if (StringUtils.isNotBlank(commerceTitle)) {
            return commerceTitle;
        }

        return StringUtils.defaultIfBlank(firstMeaningful(
            currentPage != null ? currentPage.getTitle() : null,
            currentPage != null ? currentPage.getName() : null), StringUtils.EMPTY);
    }

    public String getDescription() {
        return StringUtils.defaultIfBlank(firstMeaningful(
            productModel != null ? productModel.getMetaDescription() : null,
            productListModel != null ? productListModel.getMetaDescription() : null,
            currentPage != null ? currentPage.getDescription() : null), StringUtils.EMPTY);
    }

    public String getCanonicalUrl() {
        String commerceCanonicalUrl = firstMeaningful(
            productModel != null ? productModel.getCanonicalUrl() : null,
            productListModel != null ? productListModel.getCanonicalUrl() : null);
        if (StringUtils.isNotBlank(commerceCanonicalUrl)) {
            return commerceCanonicalUrl;
        }

        if (request != null && StringUtils.isNotBlank(request.getRequestURI())) {
            return request.getRequestURI();
        }

        if (currentPage != null) {
            return currentPage.getPath() + ".html";
        }

        return StringUtils.EMPTY;
    }

    private <T> T adaptComponent(Resource contentRoot, Resource templateRoot, String sourceResourceType, String targetResourceType,
        Class<T> adapterType) {
        Resource component = findDescendantByResourceType(contentRoot, sourceResourceType);
        if (component == null) {
            component = findDescendantByResourceType(templateRoot, sourceResourceType);
        }
        if (component == null) {
            return null;
        }

        try {
            ValueMapResourceWrapper wrappedResource = new ValueMapResourceWrapper(component, targetResourceType);
            return modelFactory.getModelFromWrappedRequest(request, wrappedResource, adapterType);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Resource getTemplateStructureResource() {
        if (currentPage == null || resourceResolver == null) {
            return null;
        }

        Template template = currentPage.getTemplate();
        if (template == null) {
            return null;
        }

        return resourceResolver.getResource(template.getPath() + "/structure/jcr:content");
    }

    private Resource findDescendantByResourceType(Resource root, String resourceType) {
        if (root == null) {
            return null;
        }

        if (root.isResourceType(resourceType)) {
            return root;
        }

        for (Resource child : root.getChildren()) {
            Resource match = findDescendantByResourceType(child, resourceType);
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    private String firstMeaningful(String... values) {
        if (values == null) {
            return StringUtils.EMPTY;
        }

        for (String value : values) {
            if (isMeaningful(value)) {
                return value;
            }
        }

        return StringUtils.EMPTY;
    }

    private boolean isMeaningful(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        return !StringUtils.equals(value, CATEGORY_PAGE_TITLE)
            && !StringUtils.equals(value, PRODUCT_PAGE_TITLE)
            && !StringUtils.equals(value, CATEGORY_PLACEHOLDER)
            && !StringUtils.equals(value, PRODUCT_PLACEHOLDER);
    }
}

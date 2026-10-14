/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.day.cq.wcm.api.Page;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.google.gson.JsonElement;

public class ProductGridItem {

    static final String SIZE_ATTRIBUTE = "size";

    private final boolean exists;
    private final String image;
    private final String name;
    private final String description;
    private final String price;
    private final String path;
    private final ProductFilters filters;

    private ProductGridItem(String image, String name, String description, String price, String path, ProductFilters filters) {
        this.exists = StringUtils.isNotBlank(name) && StringUtils.isNotBlank(path);
        this.image = image;
        this.name = name;
        this.description = description;
        this.price = price;
        this.path = path;
        this.filters = filters;
    }

    public static ProductGridItem fromProduct(Product product, Page page, SlingHttpServletRequest request) {
        return fromProduct(product, page, request, null);
    }

    public static ProductGridItem fromProduct(Product product, Page page, SlingHttpServletRequest request, String resolvedPath) {
        return fromProduct(product, page, request, resolvedPath, null);
    }

    public static ProductGridItem fromProduct(Product product, Page page, SlingHttpServletRequest request, String resolvedPath,
        String selectedVariantSku) {
        ProductInterface productData = ProductItem.fetchProduct(product);
        String defaultImage = ProductItem.resolveImage(request, ProductItem.assetPath(product.getAssets()), ProductItem.imagePath(productData));
        String resolvedPrice = ProductItem.formatPrice(product.getPriceRange());
        Variant selectedVariant = resolveSelectedVariant(product, selectedVariantSku);
        String image = selectedVariant != null
            ? ProductItem.resolveImage(request, ProductItem.assetPath(selectedVariant.getAssets()), defaultImage)
            : defaultImage;
        String displayName = selectedVariant != null ? StringUtils.defaultIfBlank(selectedVariant.getName(), product.getName()) : product.getName();
        String displayPrice = selectedVariant != null
            ? StringUtils.defaultIfBlank(ProductItem.formatPrice(selectedVariant.getPriceRange()), resolvedPrice)
            : resolvedPrice;
        String pagePath = resolvePagePath(page, request);

        return new ProductGridItem(
            image,
            displayName,
            ProductItem.descriptionLabel(productData),
            displayPrice,
            StringUtils.defaultIfBlank(resolvedPath, pagePath),
            buildFilters(productData, displayPrice, selectedVariant != null ? null : product.getPriceRange()));
    }

    public static ProductGridItem fromProductListItem(ProductListItem productListItem, SlingHttpServletRequest request, String resolvedUrl) {
        ProductInterface product = productListItem.getProduct();
        String resolvedPrice = ProductItem.formatPrice(productListItem.getPriceRange());

        return new ProductGridItem(
            ProductItem.mapAssetPath(request, productListItem.getImageURL()),
            productListItem.getTitle(),
            ProductItem.descriptionLabel(product),
            resolvedPrice,
            StringUtils.defaultIfBlank(resolvedUrl, StringUtils.defaultIfBlank(productListItem.getURL(), productListItem.getPath())),
            buildFilters(product, resolvedPrice, productListItem.getPriceRange()));
    }

    private static String resolvePagePath(Page page, SlingHttpServletRequest request) {
        if (page == null || request == null || request.getResourceResolver() == null) {
            return StringUtils.EMPTY;
        }
        return request.getResourceResolver().map(request, page.getPath()) + ".html";
    }

    private static ProductFilters buildFilters(ProductInterface product, String price, Price priceRange) {
        ProductFilters productFilters = new ProductFilters();
        if (StringUtils.isNotBlank(price)) {
            productFilters.setPrice(price);
        }
        if (priceRange != null && !priceRange.isEmpty() && Boolean.TRUE.equals(priceRange.isRange())
            && StringUtils.isNotBlank(priceRange.getFormattedFinalPriceMax())) {
            productFilters.setPrice(priceRange.getFormattedFinalPriceMax());
        }

        if (product instanceof ConfigurableProduct) {
            ConfigurableProduct configurableProduct = (ConfigurableProduct) product;
            for (ConfigurableProductOptions option : configurableProduct.getConfigurableOptions()) {
                if ("color".equals(option.getAttributeCode())) {
                    for (ConfigurableProductOptionsValues value : option.getValues()) {
                        productFilters.setColor(StringUtils.lowerCase(value.getLabel()));
                    }
                } else if (SIZE_ATTRIBUTE.equals(option.getAttributeCode())) {
                    for (ConfigurableProductOptionsValues value : option.getValues()) {
                        productFilters.setSize(value.getLabel());
                    }
                }
            }
        } else if (product != null) {
            String size = customFieldValue(product, SIZE_ATTRIBUTE);
            if (StringUtils.isNotBlank(size)) {
                productFilters.setSize(size);
            }
        }

        return productFilters;
    }

    private static String customFieldValue(ProductInterface product, String field) {
        Object value = product.get(field);
        return value instanceof JsonElement && ((JsonElement) value).isJsonPrimitive() ? ((JsonElement) value).getAsString() : null;
    }

    private static Variant resolveSelectedVariant(Product product, String selectedVariantSku) {
        if (product == null || StringUtils.isBlank(selectedVariantSku) || product.getVariants() == null) {
            return null;
        }

        for (Variant variant : product.getVariants()) {
            if (variant != null && StringUtils.equals(selectedVariantSku, variant.getSku())) {
                return variant;
            }
        }

        return null;
    }

    public boolean exists() {
        return exists;
    }

    public String getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getPath() {
        return path;
    }

    public ProductFilters getFilters() {
        return filters;
    }

    public static class ProductFilters {

        private final Set<String> colors = new LinkedHashSet<String>();
        private final Set<String> sizes = new LinkedHashSet<String>();
        private final Set<String> prices = new LinkedHashSet<String>();

        public Set<String> getColors() {
            return Collections.unmodifiableSet(colors);
        }

        public void setColor(String color) {
            colors.add(color);
        }

        public Set<String> getSizes() {
            return Collections.unmodifiableSet(sizes);
        }

        public void setSize(String size) {
            sizes.add(size);
        }

        public Set<String> getPrices() {
            return Collections.unmodifiableSet(prices);
        }

        public void setPrice(String price) {
            prices.add(price);
        }

    }
}

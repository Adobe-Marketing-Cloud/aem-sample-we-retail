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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
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

    public static ProductGridItem fromProductListItem(ProductListItem productListItem, SlingHttpServletRequest request) {
        return fromProductListItem(productListItem, request, null);
    }

    /** Takes the label and the filters from {@code product} when given, e.g. the configurable product of a listed variant. */
    public static ProductGridItem fromProductListItem(ProductListItem productListItem, SlingHttpServletRequest request,
        ProductInterface product) {
        ProductInterface productData = product != null ? product : productListItem.getProduct();
        String resolvedPrice = ProductItem.formatPrice(productListItem.getPriceRange());

        return new ProductGridItem(
            ProductItem.mapAssetPath(request, productListItem.getImageURL()),
            productListItem.getTitle(),
            ProductItem.descriptionLabel(productData),
            resolvedPrice,
            StringUtils.defaultIfBlank(productListItem.getURL(), productListItem.getPath()),
            buildFilters(productData, resolvedPrice, productListItem.getPriceRange()));
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

        private final Set<String> colors = new HashSet<String>();
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

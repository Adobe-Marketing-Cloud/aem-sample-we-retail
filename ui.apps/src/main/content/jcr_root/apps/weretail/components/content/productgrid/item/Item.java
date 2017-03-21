/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package apps.weretail.components.content.productgrid.item;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.common.CommerceHelper;
import com.day.cq.commons.ImageResource;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.sightly.WCMUsePojo;

public class Item extends WCMUsePojo {

    public static final Logger LOGGER = LoggerFactory.getLogger(Item.class);

    private boolean exists;
    private String image;
    private String name;
    private String description;
    private String price;
    private String path;
    private ProductFilters filters;

    @Override
    public void activate() throws Exception {
        Resource resource = getResource();
        ResourceResolver resolver = getResourceResolver();
        SlingHttpServletRequest request = getRequest();
        SlingHttpServletResponse response = getResponse();
        PageManager pageManager = getPageManager();

        Page productPage = pageManager.getContainingPage(resource.getPath());
        Product currentProduct = CommerceHelper.findCurrentProduct(productPage);
        if(currentProduct == null) {
            exists = false;
            return;
        }
        ImageResource imageResource = currentProduct.getImage();
        if (imageResource == null) {
            exists = false;
            return;
        }
        exists = true;
        CommerceService commerceService = resource.adaptTo(CommerceService.class);
        CommerceSession commerceSession = commerceService.login(request, response);
        Product pimProduct = currentProduct.getPIMProduct();
        image = imageResource.getPath();
        name = pimProduct.getTitle();
        description = pimProduct.getDescription();
        price = commerceSession.getProductPrice(pimProduct);
        path = productPage.getPath();
        filters = getProductFilters(pimProduct, commerceSession);
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

    private ProductFilters getProductFilters(Product product, CommerceSession commerceSession) throws Exception {
        String variationAxis = product.getProperty("cq:productVariantAxes", String.class);
        ProductFilters productFilters = new ProductFilters();
        if (StringUtils.isNotEmpty(variationAxis)) {
            Iterator<Product> unorderedVariations = product.getVariants();
            while (unorderedVariations.hasNext()) {
                Product productVariation = unorderedVariations.next();
                ProductProperties variation = new ProductProperties(productVariation, commerceSession);
                if (StringUtils.isNotEmpty(variation.getColor())) {
                    productFilters.setColor(variation.getColor().toLowerCase());
                }
                if (StringUtils.isNotEmpty(variation.getSize())) {
                    productFilters.setSize(variation.getSize());
                }
                productFilters.setPrice(variation.getPrice());
            }
        } else {
            String color = product.getProperty("color", String.class);
            if (StringUtils.isNotEmpty(color)) {
                productFilters.setColor(color.toLowerCase());
            }
            productFilters.setSize(product.getProperty("size", String.class));
            productFilters.setPrice(price);
        }
        return productFilters;
    }

    public class ProductFilters {

        private Set<String> colors = new HashSet<String>();
        private Set<String> sizes = new HashSet<String>();
        private Set<String> prices = new HashSet<String>();

        public ProductFilters() {
        }

        public Set<String> getColors() {
            return colors;
        }

        public void setColor(String color) {
            colors.add(color);
        }

        public Set<String> getSizes() {
            return sizes;
        }

        public void setSize(String size) {
            sizes.add(size);
        }

        public Set<String> getPrices() {
            return prices;
        }

        public void setPrice(String price) {
            prices.add(price);
        }

    }

    public class ProductProperties {

        private Product product;
        private CommerceSession commerceSession;

        private String path;
        private Iterator<String> variants;
        private String sku;
        private String title;
        private String description;
        private String color;
        private String colorClass;
        private String size;
        private String price;
        private String summary;
        private String features;
        private String image;

        public ProductProperties(Product product, CommerceSession commerceSession) {
            this.product = product;
            this.commerceSession = commerceSession;
        }

        public String getPath() {
            if (path == null) {
                path = product.getPath();
            }
            return path;
        }

        public Iterator<String> getVariants() {
            if (variants == null) {
                variants = product.getVariantAxes();
            }
            return variants;
        }

        public String getSku() {
            if (sku == null) {
                sku = product.getSKU();
            }
            return sku;
        }

        public String getTitle() {
            if (title == null) {
                title = product.getTitle();
            }
            return title;
        }

        public String getDescription() {
            if (description == null) {
                description = product.getDescription();
            }
            return description;
        }

        public String getColor() {
            if (color == null) {
                color = product.getProperty("color", String.class);
            }
            return color;
        }

        public String getColorClass() {
            if (colorClass == null) {
                String color = getColor();
                if (color != null) {
                    colorClass = color.toLowerCase();
                }
            }
            return colorClass;
        }

        public String getSize() {
            if (size == null) {
                size = product.getProperty("size", String.class);
            }
            return size;
        }

        public String getPrice() {
            if (price == null) {
                try {
                    price = commerceSession.getProductPrice(product);
                } catch (CommerceException e) {
                    LOGGER.error("Error getting the product price: {}", e);
                }
            }
            return price;
        }

        public String getSummary() {
            if (summary == null) {
                summary = product.getProperty("summary", String.class);
            }
            return summary;
        }

        public String getFeatures() {
            if (features == null) {
                features = product.getProperty("features", String.class);
            }
            return features;
        }

        public String getImage() {
            if (image == null) {
                ImageResource imageResource = product.getImage();
                if (imageResource != null) {
                    image = imageResource.getFileReference();
                }
            }
            return image;
        }
    }

}

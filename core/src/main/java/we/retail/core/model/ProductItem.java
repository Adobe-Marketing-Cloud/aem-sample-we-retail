/*
 *   Copyright 2016 Adobe Systems Incorporated
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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.day.cq.commons.ImageResource;
import com.day.cq.wcm.api.Page;
import we.retail.core.WeRetailConstants;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;

/**
 * Generic UI product item model used by Sling Models like {@link ProductModel} or {@link ShoppingCartModel}.
 */
public class ProductItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductItem.class);

    private static final String PN_FEATURES = "features";
    private static final String PN_SUMMARY = "summary";

    private String path;
    private String pagePath;
    private String sku;
    private String title;
    private String description;
    private String price;
    private String summary;
    private String features;
    private String imageUrl;
    private String thumbnailUrl;

    private List<ProductItem> variants = new ArrayList<ProductItem>();

    private List<String> variantAxes = new ArrayList<String>();
    private Map<String, String> variantAxesMap = new LinkedHashMap<String, String>();

    public ProductItem(Product product, CommerceSession commerceSession, SlingHttpServletRequest request, Page currentPage) {
        this(product, commerceSession, request, currentPage, null);
    }

    private ProductItem(Product product, CommerceSession commerceSession, SlingHttpServletRequest request,  Page currentPage,
                        ProductItem baseProductItem) {

        ResourceResolver resourceResolver = request.getResourceResolver();

        path = product.getPath();
        pagePath = product.getPagePath();
        if (StringUtils.isNotBlank(pagePath)) {
            pagePath = resourceResolver.map(request, pagePath);
        }

        Locale currentLocale = currentPage.getLanguage(false);

        sku = product.getSKU();
        title = product.getTitle(currentLocale.getLanguage());
        description = product.getDescription(currentLocale.getLanguage());

        summary = product.getProperty(PN_SUMMARY, currentLocale.getLanguage(), String.class);
        features = product.getProperty(PN_FEATURES, currentLocale.getLanguage(), String.class);

        ImageResource image = product.getImage();
        imageUrl = image != null ? image.getFileReference() : null;
        if (StringUtils.isNotBlank(imageUrl)) {
            imageUrl = resourceResolver.map(request, imageUrl);
        }

        thumbnailUrl = product.getThumbnailUrl(WeRetailConstants.PRODUCT_THUMBNAIL_WIDTH);
        if (StringUtils.isNotBlank(thumbnailUrl)) {
            thumbnailUrl = resourceResolver.map(request, thumbnailUrl);
        }

        if (commerceSession != null) {
            try {
                price = commerceSession.getProductPrice(product);
            } catch (CommerceException e) {
                LOGGER.error("Error getting the product price: {}", e);
            }
        }

        if (baseProductItem == null) {
            String[] productVariantAxes = product.getProperty(CommerceConstants.PN_PRODUCT_VARIANT_AXES, String[].class);
            if (productVariantAxes != null) {
                setVariantAxes(productVariantAxes);
            }
            populateAllVariants(product, commerceSession, request, currentPage);
        } else {
            populateVariantAxesValues(baseProductItem.variantAxes, product);
        }
    }

    private void populateAllVariants(Product product, CommerceSession commerceSession, SlingHttpServletRequest request,  Page currentPage) {

        try {
            Iterator<Product> productVariants = product.getVariants();
            while (productVariants.hasNext()) {
                ProductItem variant = new ProductItem(productVariants.next(), commerceSession, request, currentPage, this);
                variants.add(variant);
            }

            // If there are no variants, the product itself is defined as the first variant
            if (variants.isEmpty()) {
                variants.add(this);
            }
        } catch (CommerceException e) {
            LOGGER.error("Error getting the product variants: {}", e);
        }
    }

    private void populateVariantAxesValues(List<String> variantAxes, Product product) {
        for (String variantAxis : variantAxes) {
            String value = product.getProperty(variantAxis, String.class);
            if (value != null && !variantAxesMap.containsKey(variantAxis)) {
                variantAxesMap.put(variantAxis, value);
            }
        }
    }

    private void setVariantAxes(String[] variantAxes) {
        if (variantAxes != null) {
            for (String axis : variantAxes) {
                this.variantAxes.add(axis.trim());
            }
        }
    }

    public String getPath() {
        return path;
    }

    public String getPagePath() {
        return pagePath;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getSummary() {
        return summary;
    }

    public String getFeatures() {
        return features;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public List<ProductItem> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    /**
     * This method returns the value (if any) for the given variant axis.
     *
     * @param axis
     *            The name of the variant axis, for example "color" or "size".
     * @return The value (for example, "red") for that axis, or null if the variant does not have a value for that axis.
     */
    public String getVariantValueForAxis(String axis) {
        return variantAxesMap.get(axis);
    }

    /**
     * This method returns a JSON representation of the variant axes and values for a product variant.<br>
     * For example and since the variant axes and values are typically represented as a map, this method might return the following
     * String for a variant product with 2 axes color and size:<br>
     * <br>
     * <code>{'color':'red','size':'XS'}</code>
     *
     * @return The JSON representation of the variant axes and values.
     */
    public String getVariantAxesMapJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        variantAxesMap.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build().toString();
    }

    /**
     * This method returns a map of variant axes and all their respective values by axis.<br>
     * The keys of the map represent the axis names (e.g. color, size), and the values are stored in a Collection.<br>
     * <br>
     * For example, the returned map can look like<br>
     * <code>color --&gt; red, green, blue<br>
     * size --&gt; XS, S, M</code>
     *
     * @return The map of all variant axes and their respective values.
     */
    public Map<String, Collection<String>> getVariantsAxesValues() {
        if (variants.isEmpty() || variantAxes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Collection<String>> map = new LinkedHashMap<String, Collection<String>>();
        for (String axis : variantAxes) {
            for (ProductItem variant : variants) {
                String axisValue = variant.variantAxesMap.get(axis);
                if (axisValue != null) {
                    Collection<String> set = map.get(axis);
                    if (set == null) {
                        set = new LinkedHashSet<String>();
                        map.put(axis, set);
                    }

                    if (!set.contains(axisValue)) {
                        set.add(axisValue);
                    }
                }
            }
        }

        return map;
    }
}

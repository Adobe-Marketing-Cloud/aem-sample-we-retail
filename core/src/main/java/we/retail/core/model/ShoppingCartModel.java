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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.PriceInfo;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.promotion.PromotionInfo;
import com.adobe.cq.commerce.common.PriceFilter;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.commons.WCMUtils;

import we.retail.core.WeRetailConstants;

@Model(adaptables = SlingHttpServletRequest.class)
public class ShoppingCartModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingCartModel.class);

    @SlingObject
    protected SlingHttpServletRequest request;

    @SlingObject
    protected SlingHttpServletResponse response;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(booleanValues = false)
    protected boolean isReadOnly;

    protected CommerceSession commerceSession;
    protected List<CartEntry> entries = new ArrayList<CartEntry>();
    protected List<PromotionInfo> allPromotions;

    private String checkoutPage;
    private String currentPageUrl;
    private List<PromotionInfo> orderPromotions = new ArrayList<PromotionInfo>();
    private Map<Integer, List<PromotionInfo>> cartEntryPromotions = new HashMap<Integer, List<PromotionInfo>>();

    @PostConstruct
    private void initModel() throws Exception {
        createCommerceSession();
        populatePageUrls();
        populatePromotions();
        populateCartEntries();
    }

    protected void createCommerceSession() {
        CommerceService commerceService = currentPage.getContentResource().adaptTo(CommerceService.class);
        try {
            commerceSession = commerceService.login(request, response);
            allPromotions = commerceSession.getPromotions();
        } catch (CommerceException e) {
            LOGGER.error("Failed to create commerce session", e);
        }
    }

    protected void populateCartEntries() throws CommerceException {
        for (CommerceSession.CartEntry cartEntry : commerceSession.getCartEntries()) {
            CartEntry entry = new CartEntry(cartEntry);
            entries.add(entry);
        }
    }

    protected void populatePromotions() throws CommerceException {
        if (allPromotions != null && !allPromotions.isEmpty()) {
            for (PromotionInfo promo : allPromotions) {
                if (promo.getCartEntryIndex() == null) {
                    orderPromotions.add(promo);
                } else {
                    final Integer cartEntryIndex = promo.getCartEntryIndex();
                    List<PromotionInfo> promoList = cartEntryPromotions.get(cartEntryIndex);
                    if (promoList == null) {
                        promoList = new ArrayList<PromotionInfo>();
                    }
                    promoList.add(promo);
                    cartEntryPromotions.put(cartEntryIndex, promoList);
                }
            }
        }
    }

    protected void populatePageUrls() {
        String checkoutPageProperty = WCMUtils.getInheritedProperty(currentPage, resourceResolver,
                CommerceConstants.PN_CHECKOUT_PAGE_PATH);
        if (StringUtils.isNotEmpty(checkoutPageProperty)) {
            checkoutPage = resourceResolver.map(request, checkoutPageProperty) + ".html";
        }

        currentPageUrl = resourceResolver.map(request, currentPage.getPath() + ".html");
    }

    public String getCheckoutPage() {
        return checkoutPage;
    }

    public String getCurrentPageUrl() {
        return currentPageUrl;
    }

    public List<CartEntry> getEntries() {
        return entries;
    }

    public List<PromotionInfo> getOrderPromotions() {
        return orderPromotions;
    }
    public boolean getIsReadOnly() {
        return isReadOnly;
    }

    public class CartEntry {
        private CommerceSession.CartEntry entry;
        private ProductItem productItem;
        private Map<String, String> variantAxesMap = new LinkedHashMap<String, String>();
        private List<PromotionInfo> entryPromotions = new ArrayList<PromotionInfo>();

        public CartEntry(CommerceSession.CartEntry entry) {
            this.entry = entry;

            try {
                Product product = entry.getProduct();
                productItem = new ProductItem(product, commerceSession, request, currentPage);
                Product baseProduct = product.getBaseProduct();
                String[] variantAxes = baseProduct.getProperty(CommerceConstants.PN_PRODUCT_VARIANT_AXES,
                        String[].class);
                if (variantAxes != null) {
                    for (String variantAxis : variantAxes) {
                        String value = product.getProperty(variantAxis, String.class);
                        if (value != null && !variantAxesMap.containsKey(variantAxis)) {
                            variantAxesMap.put(StringUtils.capitalize(variantAxis), value);
                        }
                    }
                }

                if (cartEntryPromotions.containsKey(entry.getEntryIndex())) {
                    entryPromotions = cartEntryPromotions.get(entry.getEntryIndex());
                }

            } catch (CommerceException e) {
                LOGGER.error("Failed to the product variant axes data", e);
            }
        }

        public CommerceSession.CartEntry getEntry() {
            return entry;
        }

        public String getPrice() throws CommerceException {
            List<PriceInfo> priceInfos = entry.getPriceInfo(new PriceFilter(WeRetailConstants.PRICE_FILTER_UNIT));
            return CollectionUtils.isNotEmpty(priceInfos) ? priceInfos.get(0).getFormattedString() : null;
        }

        public ProductItem getProduct() throws CommerceException {
            return productItem;
        }

        public String getTotalPrice() throws CommerceException {
            List<PriceInfo> priceInfos = entry.getPriceInfo(new PriceFilter(WeRetailConstants.PRICE_FILTER_LINE));
            return CollectionUtils.isNotEmpty(priceInfos) ? priceInfos.get(0).getFormattedString() : null;
        }

        public Map<String, String> getVariantAxesMap() {
            return variantAxesMap;
        }

        public List<PromotionInfo> getEntryPromotions() {
            return entryPromotions;
        }

        public boolean isWrapping() {
            return Boolean.TRUE.equals(entry.getProperty("wrapping-selected", Boolean.class));
        }

        public String getWrappingLabel() {
            return entry.getProperty("wrapping-label", String.class);
        }
    }
}

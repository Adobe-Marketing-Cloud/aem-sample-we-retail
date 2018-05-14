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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
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

import static we.retail.core.WeRetailConstants.PRICE_TYPE_DISCOUNT;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_LINE;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_PRE_TAX;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_UNIT;

@Model(adaptables = SlingHttpServletRequest.class)
public class ShoppingCartModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingCartModel.class);

    @SlingObject
    protected SlingHttpServletRequest request;

    @SlingObject
    protected SlingHttpServletResponse response;

    @ScriptVariable
    private Page currentPage;

    protected CommerceSession commerceSession;
    protected List<CartEntry> entries = new ArrayList<CartEntry>();
    protected List<PromotionInfo> allPromotions;

    private List<PromotionInfo> orderPromotions = new ArrayList<PromotionInfo>();
    private Map<Integer, List<PromotionInfo>> cartEntryPromotions = new HashMap<Integer, List<PromotionInfo>>();

    @PostConstruct
    private void initModel() throws CommerceException {
        createCommerceSession();
        populatePromotions();
        populateCartEntries();
    }

    protected void createCommerceSession() {
        CommerceService commerceService = currentPage.getContentResource().adaptTo(CommerceService.class);
        if (commerceService == null) {
            LOGGER.error("Failed to obtain commerce service");
            return;
        }
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

    public List<CartEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public List<PromotionInfo> getOrderPromotions() {
        return Collections.unmodifiableList(orderPromotions);
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
            List<PriceInfo> priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_UNIT, PRICE_TYPE_DISCOUNT));
            if (CollectionUtils.isNotEmpty(priceInfos)) {
                return priceInfos.get(0).getFormattedString();
            } else {
                priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_UNIT));
                return CollectionUtils.isNotEmpty(priceInfos) ? priceInfos.get(0).getFormattedString() : null;
            }
        }

        public String getStrikeThroughPrice() throws CommerceException {
            List<PriceInfo> priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_UNIT, PRICE_TYPE_DISCOUNT));
            if (CollectionUtils.isNotEmpty(priceInfos)) {
                priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_UNIT));
                priceInfos = filterOut(priceInfos, PRICE_TYPE_DISCOUNT);

                if (CollectionUtils.isNotEmpty(priceInfos)) {
                    return priceInfos.get(0).getFormattedString();
                }
            }
            return null;
        }

        public boolean isReadOnly() {
            return BooleanUtils.toBoolean(entry.getProperty(CommerceSession.PN_READONLY, Boolean.class));
        }

        public ProductItem getProduct() throws CommerceException {
            return productItem;
        }

        public String getTotalPrice() throws CommerceException {
            List<PriceInfo> priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_LINE, PRICE_TYPE_DISCOUNT));
            if (CollectionUtils.isNotEmpty(priceInfos)) {
                return priceInfos.get(0).getFormattedString();
            } else {
                priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_LINE, PRICE_TYPE_PRE_TAX));
                return CollectionUtils.isNotEmpty(priceInfos) ? priceInfos.get(0).getFormattedString() : null;
            }
        }

        public String getStrikeThroughTotalPrice() throws CommerceException {
            List<PriceInfo> priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_LINE, PRICE_TYPE_DISCOUNT));
            if (CollectionUtils.isNotEmpty(priceInfos)) {
                priceInfos = entry.getPriceInfo(new PriceFilter(PRICE_TYPE_LINE, PRICE_TYPE_PRE_TAX));
                priceInfos = filterOut(priceInfos, PRICE_TYPE_DISCOUNT);

                if (CollectionUtils.isNotEmpty(priceInfos)) {
                    return priceInfos.get(0).getFormattedString();
                }
            }
            return null;
        }

        public Map<String, String> getVariantAxesMap() {
            return Collections.unmodifiableMap(variantAxesMap);
        }

        public List<PromotionInfo> getEntryPromotions() {
            return Collections.unmodifiableList(entryPromotions);
        }

        public boolean isWrapping() {
            return Boolean.TRUE.equals(entry.getProperty("wrapping-selected", Boolean.class));
        }

        public String getWrappingLabel() {
            return entry.getProperty("wrapping-label", String.class);
        }

        @SuppressWarnings("unchecked")
        private List<PriceInfo> filterOut(List<PriceInfo> priceInfos, String filter) {
            List<PriceInfo> result = new ArrayList<PriceInfo>();
            for (PriceInfo priceInfo : priceInfos) {
                if (priceInfo.containsKey(PriceFilter.PN_TYPES) && priceInfo.get(PriceFilter.PN_TYPES) instanceof Set) {
                    final Set<String> types = (Set<String>) priceInfo.get(PriceFilter.PN_TYPES);
                    if (!types.contains(filter)) {
                        result.add(priceInfo);
                    }
                }
            }
            return result;
        }
    }
}

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
package common.mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.PaymentMethod;
import com.adobe.cq.commerce.api.PlacedOrder;
import com.adobe.cq.commerce.api.PlacedOrderResult;
import com.adobe.cq.commerce.api.PriceInfo;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.ShippingMethod;
import com.adobe.cq.commerce.api.promotion.PromotionInfo;
import com.adobe.cq.commerce.api.promotion.Voucher;
import com.adobe.cq.commerce.api.promotion.VoucherInfo;
import com.adobe.cq.commerce.api.smartlist.SmartListManager;
import com.adobe.cq.commerce.common.DefaultJcrCartEntry;
import com.adobe.cq.commerce.common.PriceFilter;
import we.retail.core.model.Constants;

import static we.retail.core.WeRetailConstants.PRICE_TYPE_CART;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_LINE;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_POST_TAX;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_PRE_TAX;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_SHIPPING;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_TAX;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_TOTAL;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_UNIT;

public class MockCommerceSession implements CommerceSession {

    /**
     * This is used to mock the fetching of orders in the JCR.
     */
    private Map<String, PlacedOrder> placedOrders = new LinkedHashMap<String, PlacedOrder>();

    private List<PriceInfo> prices;

    private List<MockDefaultJcrCartEntry> cart;

    private Locale locale;

    public MockCommerceSession() {
        this.locale = new Locale("en", "US");
    }

    @Override
    public void addCartEntry(Product product, int quantity) throws CommerceException {
        addCartEntry(product, quantity, Collections.emptyMap());
    }

    @Override
    public void logout() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserLocale(Locale locale) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getUserLocale() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAvailableCountries() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ShippingMethod> getAvailableShippingMethods() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PaymentMethod> getAvailablePaymentMethods() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PriceInfo> getProductPriceInfo(Product product) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PriceInfo> getProductPriceInfo(Product product, Predicate filter) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProductPrice(Product product, Predicate filter) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProductPrice(Product product) throws CommerceException {
        return product.getProperty("price", String.class);
    }

    @Override
    public int getCartEntryCount() throws CommerceException {
        List<CartEntry> cart = getCartEntries();
        return cart == null ? 0 : cart.size();
    }

    @Override
    public List<CartEntry> getCartEntries() throws CommerceException {
        if (cart != null && !cart.isEmpty()) {
            return Collections.unmodifiableList(cart);
        }
        // To mock the shopping cart, we use the first registered order in the mock session 
        if (!placedOrders.isEmpty()) {
            PlacedOrder placedOrder = placedOrders.values().iterator().next();

            // We restore the product prices from the saved order
            if (placedOrder != null) {
                restoreCartEntriesPrices(placedOrder);
            }

            return placedOrder.getCartEntries();
        }
        return Collections.emptyList();
    }

    private void initializeCartPrices() throws CommerceException {
        if (prices != null) {
            return;
        }

        prices = new ArrayList<PriceInfo>();
        List<CartEntry> entries = getCartEntries();
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartEntry entry : entries) {
            subTotal = subTotal.add(entry.getPriceInfo(new PriceFilter(PRICE_TYPE_LINE)).get(0).getAmount());
        }

        setPrice(new PriceInfo(Constants.SHIPPING_TOTAL_VALUE, locale), PRICE_TYPE_SHIPPING, PRICE_TYPE_PRE_TAX);
        setPrice(new PriceInfo(subTotal, locale), PRICE_TYPE_CART, PRICE_TYPE_PRE_TAX);
        setPrice(new PriceInfo(Constants.TAX_TOTAL_VALUE, locale), PRICE_TYPE_CART, PRICE_TYPE_TAX);
        BigDecimal total = subTotal.add(Constants.SHIPPING_TOTAL_VALUE).add(Constants.TAX_TOTAL_VALUE);
        setPrice(new PriceInfo(total, locale), PRICE_TYPE_TOTAL);
    }

    private void setPrice(PriceInfo priceInfo, String... types) {
        List<String> typeList = new ArrayList<String>(Arrays.asList(types));
        priceInfo.put(PriceFilter.PN_TYPES, new HashSet<String>(typeList));
        prices.add(priceInfo);
    }

    @Override
    public List<PriceInfo> getCartPriceInfo(Predicate filter) throws CommerceException {
        initializeCartPrices();
        if (filter != null) {
            final ArrayList<PriceInfo> filteredPrices = new ArrayList<PriceInfo>();
            CollectionUtils.select(prices, filter, filteredPrices);
            return filteredPrices;
        }
        return prices;
    }

    @Override
    public String getCartPrice(Predicate filter) throws CommerceException {
        final List<PriceInfo> prices = getCartPriceInfo(filter);
        return prices.isEmpty() ? "" : prices.get(0).getFormattedString();
    }

    @Override
    public void addCartEntry(Product product, int quantity, Map<String, Object> properties) throws CommerceException {
        if (cart == null) {
            cart = new ArrayList<>();
        }
        MockDefaultJcrCartEntry entry = new MockDefaultJcrCartEntry(cart.size() + 1, product, quantity);
        cart.add(entry);
    }

    @Override
    public void modifyCartEntry(int entryNumber, int quantity) throws CommerceException {
        cart.get(entryNumber).setQuantity(quantity);
    }

    @Override
    public void modifyCartEntry(int entryNumber, Map<String, Object> delta) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteCartEntry(int entryNumber) throws CommerceException {
        if (cart != null) {
            cart.remove(entryNumber);
        }
    }

    @Override
    public void addVoucher(String code) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVoucher(String code) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VoucherInfo> getVoucherInfos() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsClientsidePromotionResolution() {
        return false;
    }

    @Override
    public void addPromotion(String path) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePromotion(String path) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PromotionInfo> getPromotions() throws CommerceException {
        return Collections.emptyList();
    }

    @Override
    public String getOrderId() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getOrderDetails(String predicate) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getOrder() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateOrderDetails(Map<String, Object> details, String predicate) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateOrder(Map<String, Object> delta) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void placeOrder(Map<String, Object> orderDetailsDelta) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PlacedOrderResult getPlacedOrders(String predicate, int pageNumber, int pageSize, String sortId) throws CommerceException {
        List<PlacedOrder> orders = new ArrayList<PlacedOrder>();
        orders.addAll(placedOrders.values());
        return new PlacedOrderResult(orders, null, null);
    }

    private void restoreCartEntriesPrices(PlacedOrder placedOrder) throws CommerceException {
        for (CartEntry cartEntry : placedOrder.getCartEntries()) {
            String price = cartEntry.getProperty("price", String.class);
            if (price != null) {
                BigDecimal unitPrice = new BigDecimal(price);
                DefaultJcrCartEntry jcrCartEntry = (DefaultJcrCartEntry) cartEntry;
                jcrCartEntry.setPrice(new PriceInfo(unitPrice, locale), PRICE_TYPE_UNIT, PRICE_TYPE_PRE_TAX);
                BigDecimal preTaxPrice = unitPrice.multiply(new BigDecimal(cartEntry.getQuantity()));
                jcrCartEntry.setPrice(new PriceInfo(preTaxPrice, locale), PRICE_TYPE_LINE, PRICE_TYPE_PRE_TAX);
                jcrCartEntry.setPrice(new PriceInfo(preTaxPrice, locale), PRICE_TYPE_LINE, PRICE_TYPE_POST_TAX);
            }
        }
    }

    @Override
    public PlacedOrder getPlacedOrder(String orderId) throws CommerceException {
        PlacedOrder placedOrder = placedOrders.get(orderId);

        // We restore the product prices from the saved order
        if (placedOrder != null) {
            restoreCartEntriesPrices(placedOrder);
        }

        return placedOrder;
    }

    @Override
    public SmartListManager getSmartListManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPriceInfo(Product product) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCartPreTaxPrice() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCartTax() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCartTotalPrice() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOrderShipping() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOrderTotalTax() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOrderTotalPrice() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Voucher> getVouchers() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateOrderDetails(Map<String, String> delta) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getOrderDetails() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void submitOrder(Map<String, String> orderDetailsDelta) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    /**
     * This is used to mock the fetching of orders in the JCR. This method permits to register an order in the mocked session, so that it
     * can be fetched again with {@link #getPlacedOrder(String)}. The insertion order is preserved.
     */
    public void registerPlacedOrder(String orderId, PlacedOrder placedOrder) {
        placedOrders.put(orderId, placedOrder);
    }

    public void clearCart() {
        cart = null;
    }

    public void clearPlacedOrders() {
        placedOrders = new LinkedHashMap<>();
    }
}

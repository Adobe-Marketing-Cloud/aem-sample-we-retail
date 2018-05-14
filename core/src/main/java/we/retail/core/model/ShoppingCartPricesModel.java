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

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.PriceInfo;
import com.adobe.cq.commerce.common.PriceFilter;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;

import static we.retail.core.WeRetailConstants.PRICE_TYPE_CART;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_PRE_TAX;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_SHIPPING;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_TAX;
import static we.retail.core.WeRetailConstants.PRICE_TYPE_TOTAL;

@Model(adaptables = SlingHttpServletRequest.class)
public class ShoppingCartPricesModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingCartPricesModel.class);
    
    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private SlingHttpServletResponse response;

    @ScriptVariable
    private Page currentPage;
    
    private CommerceSession commerceSession;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(booleanValues = false)
    private boolean showSubTotal;
    
    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(booleanValues = false)
    private boolean showShippingTotal;
    
    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(booleanValues = false)
    private boolean showShippingTax;

    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(booleanValues = false)
    private boolean showTaxTotal;
    
    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Default(booleanValues = false)
    private boolean showTotal;
    
    private boolean isEmpty;
    private String shippingTotal;
    private String shippingTax;
    private String subTotal;
    private String taxTotal;
    private String total;

    private I18n i18n;

    @PostConstruct
    private void initModel() throws CommerceException {
        CommerceService commerceService = currentPage.getContentResource().adaptTo(CommerceService.class);
        if (commerceService == null) {
            LOGGER.error("Failed to obtain commerce service");
            return;
        }
        try {
            commerceSession = commerceService.login(request, response);
        } catch (CommerceException e) {
            LOGGER.error("Failed to create commerce session", e);
        }
        
        Locale pageLocale = currentPage.getLanguage(true);
        ResourceBundle bundle = request.getResourceBundle(pageLocale);
        i18n = new I18n(bundle);

        isEmpty = commerceSession.getCartEntries().isEmpty();
        
        shippingTotal = formatShippingPrice(commerceSession.getCartPriceInfo(new PriceFilter(PRICE_TYPE_SHIPPING, PRICE_TYPE_PRE_TAX)));
        shippingTax = commerceSession.getCartPrice(new PriceFilter(PRICE_TYPE_SHIPPING, PRICE_TYPE_TAX));
        subTotal = commerceSession.getCartPrice(new PriceFilter(PRICE_TYPE_CART, PRICE_TYPE_PRE_TAX));
        taxTotal = commerceSession.getCartPrice(new PriceFilter(PRICE_TYPE_CART, PRICE_TYPE_TAX));
        total = commerceSession.getCartPrice(new PriceFilter(PRICE_TYPE_TOTAL));
    }

    private String formatShippingPrice(List<PriceInfo> prices) {
        if (prices.isEmpty()) {
            return i18n.get("Unknown");
        }
        else {
            PriceInfo priceInfo = prices.get(0);
            if (priceInfo.getAmount() != null && priceInfo.getAmount().signum() == 0) {
                return i18n.get("Free");
            }
            else {
                return priceInfo.getFormattedString();
            }
        }
    }

    public boolean getShowSubTotal() {
        return showSubTotal;
    }

    public boolean getShowShippingTotal() {
        return showShippingTotal;
    }
    
    public boolean getShowShippingTax() {
        return showShippingTax;
    }

    public boolean getShowTaxTotal() {
        return showTaxTotal;
    }
    
    public boolean getShowTotal() {
        return showTotal;
    }
    
    public String getShippingTotal() {
        return shippingTotal;
    }
    
    public String getShippingTax() {
        return shippingTax;
    }

    public String getSubTotal() {
        return subTotal;
    }
    
    public String getTaxTotal() {
        return taxTotal;
    }
    
    public String getTotal() {
        return total;
    }
    
    public boolean isEmpty() {
        return isEmpty;
    }
    
}

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

import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceQuery;
import com.adobe.cq.commerce.api.CommerceResult;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.collection.ProductCollection;
import com.adobe.cq.commerce.api.promotion.Promotion;
import com.adobe.cq.commerce.api.promotion.Voucher;
import com.day.cq.wcm.api.Page;

public class MockCommerceService extends SlingAdaptable implements CommerceService {

    private Resource resource;
    private MockCommerceSession mockCommerceSession = new MockCommerceSession();

    public MockCommerceService(Resource resource) {
        this.resource = resource;
    }

    @Override
    public void catalogRolloutHook(Page blueprint, Page catalog) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommerceSession login(SlingHttpServletRequest request, SlingHttpServletResponse response) throws CommerceException {
        return mockCommerceSession;
    }

    @Override
    public boolean isAvailable(String serviceType) {
        return false;
    }

    @Override
    public String getServer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContext(Map<String, Object> context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Product getProduct(String path) throws CommerceException {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        Resource productResource = resourceResolver.getResource(path);
        if(productResource != null) {
            return new MockProduct(productResource);
        }
        return null;
    }

    @Override
    public Promotion getPromotion(String path) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Voucher getVoucher(String path) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProductCollection getProductCollection(String path) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isActivated(Product product) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sectionRolloutHook(Page blueprint, Page section) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void productRolloutHook(Product productData, Page productPage, Product productReference) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommerceResult search(CommerceQuery query) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Promotion> getAvailablePromotions(ResourceResolver resourceResolver) throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getOrderPredicates() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getCountries() throws CommerceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getCreditCardTypes() throws CommerceException {
        throw new UnsupportedOperationException();
    }
}

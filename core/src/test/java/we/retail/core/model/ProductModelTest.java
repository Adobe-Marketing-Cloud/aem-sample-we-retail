/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductModelTest {

    @Test
    public void testAdaptsToCifBackedProductModel() throws Exception {
        ProductModel productModel = new ProductModel();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        SiteStructure siteStructure = mock(SiteStructure.class);
        Product product = createProduct();

        when(request.getRequestURI()).thenReturn("/content/we-retail/us/en/products/product-page.html/eq/running/eqrusufle.html");
        when(resource.getPath()).thenReturn("/content/we-retail/us/en/products/product-page/jcr:content/root/product");
        when(resource.getChildren()).thenReturn(Collections.<Resource>emptyList());
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/product-page");
        when(siteStructure.isProductPage(currentPage)).thenReturn(true);

        setField(productModel, "request", request);
        setField(productModel, "resource", resource);
        setField(productModel, "currentPage", currentPage);
        setField(productModel, "siteStructure", siteStructure);
        setField(productModel, "cifProduct", product);

        Method initMethod = ProductModel.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productModel);

        assertNotNull(productModel.getProductItem());
        assertNotNull(productModel.getProductItem().getTitle());
    }

    private Product createProduct() {
        Product product = mock(Product.class);
        Price price = mock(Price.class);
        AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);

        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn("$64.99");

        when(product.getFound()).thenReturn(Boolean.TRUE);
        when(product.getSku()).thenReturn("eqrusufle");
        when(product.getName()).thenReturn("Fleet Cross-Training Shoe");
        when(product.getPriceRange()).thenReturn(price);
        when(product.getAssets()).thenReturn(Collections.emptyList());
        when(product.getVariants()).thenReturn(Collections.emptyList());
        when(product.getVariantAttributes()).thenReturn(Collections.emptyList());
        when(product.getProductRetriever()).thenReturn(retriever);

        return product;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}

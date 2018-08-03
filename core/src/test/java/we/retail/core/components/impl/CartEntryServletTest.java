/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe Systems Incorporated
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
package we.retail.core.components.impl;

import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import common.AppAemContext;
import common.mock.MockCommerceService;
import common.mock.MockCommerceSession;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.sling.servlet.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.apache.sling.xss.XSSAPI;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import we.retail.core.WeRetailConstants;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@RunWith(MockitoJUnitRunner.class)
public class CartEntryServletTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    @InjectMocks
    private CartEntryServlet servlet = new CartEntryServlet();

    @Mock
    private XSSAPI xssApi;

    @Mock
    private RequestDispatcher requestDispatcher;

    private MockCommerceSession commerceSession;


    @Before
    public void setup() throws Exception {
        when(xssApi.getValidInteger(anyString(), anyInt())).then(i -> Integer.parseInt((String) i.getArguments()[0]));
        context.request().setContextPath("");
        context.request().setRequestDispatcherFactory(new MockRequestDispatcherFactory() {
            @Override
            public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
                return requestDispatcher;
            }

            @Override
            public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
                return requestDispatcher;
            }
        });

        context.currentResource("/content/we-retail");

        CommerceService commerceService = context.currentResource().adaptTo(CommerceService.class);
        commerceSession = (MockCommerceSession) commerceService.login(null, null);
    }

    @Test
    public void testAddToEmptyCart() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(WeRetailConstants.ADD_CARTENTRY_SELECTOR);

        request.setParameterMap(new HashMap<String, Object>(){{
            put("redirect", "/content.html");
            put("product-path", AppAemContext.PRODUCT_ROOT + "/eq/running/eqrusufle/size-9");
        }});

        servlet.doPost(request, response);

        assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/content.html", response.getHeader("Location"));

        assertEquals(1, commerceSession.getCartEntryCount());
    }

    @Test
    public void testAddToEmptyCartXhr() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        request.setHeader("X-Requested-With", "XMLHttpRequest");
        MockSlingHttpServletResponse response = context.response();

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(WeRetailConstants.ADD_CARTENTRY_SELECTOR);

        request.setParameterMap(new HashMap<String, Object>(){{
            put("redirect", "/content.html");
            put("product-path", AppAemContext.PRODUCT_ROOT + "/eq/running/eqrusufle/size-9");
        }});

        servlet.doPost(request, response);

        assertEquals(SC_OK, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals(1, commerceSession.getCartEntryCount());
        JSONObject parsed = new JSONObject(response.getOutputAsString());
        assertEquals("1", parsed.getString("entries"));

    }

    @Test
    public void testAddToNonEmptyCart() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        addProduct();

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(WeRetailConstants.ADD_CARTENTRY_SELECTOR);

        request.setParameterMap(new HashMap<String, Object>(){{
            put("redirect", "/content.html");
            put("product-path", AppAemContext.PRODUCT_ROOT + "/eq/running/eqrusufle/size-9");
        }});

        servlet.doPost(request, response);

        assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/content.html", response.getHeader("Location"));

        assertEquals(2, commerceSession.getCartEntryCount());
    }

    @Test
    public void testDelete() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        addProduct();

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(WeRetailConstants.DELETE_CARTENTRY_SELECTOR);

        request.setParameterMap(new HashMap<String, Object>(){{
            put("redirect", "/content.html");
            put("entryNumber", "0");
        }});

        servlet.doPost(request, response);

        assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/content.html", response.getHeader("Location"));

        assertEquals(0, commerceSession.getCartEntryCount());
    }

    @Test
    public void testDeleteFromEmptyCart() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(WeRetailConstants.DELETE_CARTENTRY_SELECTOR);

        request.setParameterMap(new HashMap<String, Object>(){{
            put("redirect", "/content.html");
            put("entryNumber", "0");
        }});

        servlet.doPost(request, response);

        // this seems like bad behavior, but I'm leaving it in
        assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/content.html", response.getHeader("Location"));

        assertEquals(0, commerceSession.getCartEntryCount());
    }

    @Test
    public void testModify() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        addProduct();

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(WeRetailConstants.MODIFY_CARTENTRY_SELECTOR);

        request.setParameterMap(new HashMap<String, Object>(){{
            put("redirect", "/content.html");
            put("entryNumber", "0");
            put("quantity", "2");
        }});

        servlet.doPost(request, response);

        assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/content.html", response.getHeader("Location"));

        assertEquals(1, commerceSession.getCartEntryCount());
        assertEquals(2, commerceSession.getCartEntries().get(0).getQuantity());
    }

    @Test
    public void testModifyWithZeroQuantity() throws Exception {
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        addProduct();

        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSelectorString(WeRetailConstants.MODIFY_CARTENTRY_SELECTOR);

        request.setParameterMap(new HashMap<String, Object>(){{
            put("redirect", "/content.html");
            put("entryNumber", "0");
            put("quantity", "0");
        }});

        servlet.doPost(request, response);

        assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
        assertEquals("/content.html", response.getHeader("Location"));

        assertEquals(0, commerceSession.getCartEntryCount());
    }

    private void addProduct() throws Exception {
        Resource productResource = context.resourceResolver().getResource(AppAemContext.PRODUCT_ROOT + "/eq/running/eqrusufle/size-13");
        Product product = productResource.adaptTo(Product.class);

        commerceSession.addCartEntry(product, 1);

    }

    @After
    public void tearDown() {
        commerceSession.clearCart();
    }

}
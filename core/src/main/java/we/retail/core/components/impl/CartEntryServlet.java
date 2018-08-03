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
package we.retail.core.components.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.xss.XSSAPI;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.day.cq.wcm.api.Page;

import we.retail.core.WeRetailConstants;
import we.retail.core.util.WeRetailHelper;


/**
 * The CartEntryServlet handles delete and modify POST request for the commerce cart.
 */
@SuppressWarnings("serial")
@Component
@Service
@Properties(value={
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Provides cart services for We.Retail products"),
        @Property(name = "sling.servlet.resourceTypes", value = "sling/servlet/default"),
        @Property(name = "sling.servlet.selectors", value = { WeRetailConstants.DELETE_CARTENTRY_SELECTOR,
                WeRetailConstants.MODIFY_CARTENTRY_SELECTOR, WeRetailConstants.ADD_CARTENTRY_SELECTOR }),
        @Property(name = "sling.servlet.extensions", value = {"html"}),
        @Property(name = "sling.servlet.methods", value = HttpConstants.METHOD_POST)
})
public class CartEntryServlet extends SlingAllMethodsServlet {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CartEntryServlet.class);

    private static final String CONTENT_WE_RETAIL_DEFAULT = "/content/we-retail/us/en/";
    private static final String CART_PATH = "/user/cart/jcr:content/root/responsivegrid/cart";
    private static final String CART_PRICES_PATH = "/user/cart/jcr:content/root/responsivegrid/shoppingcartprices";

    @SuppressWarnings("CQRules:CQBP-71")
    private static final String NAV_CART_PATH = "/apps/weretail/components/structure/navcart";

    @Reference
    private transient XSSAPI xssAPI;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // Make sure commerceService is adapted from a product resource so that we get
        // the right service implementation (hybris, Geo, etc.)
        CommerceService commerceService = request.getResource().adaptTo(CommerceService.class);
        if (commerceService == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        CommerceSession session;
        try {
            session = commerceService.login(request, response);
        } catch (CommerceException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String selectorString = request.getRequestPathInfo().getSelectorString();
        if (WeRetailConstants.DELETE_CARTENTRY_SELECTOR.equals(selectorString)) {
            doDeleteProduct(request, response, session);
        } else if (WeRetailConstants.MODIFY_CARTENTRY_SELECTOR.equals(selectorString)) {
            doModifyProduct(request, response, session);
        } else if (WeRetailConstants.ADD_CARTENTRY_SELECTOR.equals(selectorString)) {
            doAddProduct(request, response, session);
        }

        if (AuthUtil.isAjaxRequest(request)) {
            response.setContentType("application/json");
            try (JsonGenerator jsonGenerator = Json.createGenerator(response.getWriter())) {
                String shoppingCart = renderContent(request, response, CART_PATH);
                String cartPrices = renderContent(request, response, CART_PRICES_PATH);
                String navCart = renderNavCart(request, response);


                jsonGenerator.writeStartObject()
                        .write("shoppingCart", shoppingCart)
                        .write("cartPrices", cartPrices)
                        .write("navCart", navCart)
                        .write("entries", Integer.valueOf(session.getCartEntryCount()).toString())
                        .writeEnd()
                        .close();
            } catch (CommerceException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            String redirect = request.getParameter("redirect");
            if (AuthUtil.isRedirectValid(request, redirect)) {
                response.sendRedirect(redirect);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    private void doModifyProduct(SlingHttpServletRequest request, SlingHttpServletResponse response, CommerceSession session) throws IOException {
        String qty = request.getParameter("quantity");
        int quantity = StringUtils.isNumeric(qty) ? xssAPI.getValidInteger(qty, 1) : -1;
        if (quantity < 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String entryNumber = request.getParameter("entryNumber");
        int entry = xssAPI.getValidInteger(entryNumber, -1);
        try {
            if (entry < 0 || entry >= session.getCartEntries().size()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (quantity > 0) {
                session.modifyCartEntry(entry, quantity);
            } else {
                session.deleteCartEntry(entry);
            }

        } catch (CommerceException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void doDeleteProduct(SlingHttpServletRequest request, SlingHttpServletResponse response, CommerceSession session) throws IOException {
        String entryNumber = request.getParameter("entryNumber");
        int entry = xssAPI.getValidInteger(entryNumber, -1);
        try {
            if (entry < 0 || entry >= session.getCartEntries().size()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            session.deleteCartEntry(entry);
        } catch (CommerceException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void doAddProduct(SlingHttpServletRequest request, SlingHttpServletResponse response, CommerceSession session)
            throws IOException {
        String productPath = request.getParameter("product-path");
        String qty = request.getParameter("product-quantity");

        Resource productResource = request.getResourceResolver().getResource(productPath);
        if (productResource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Product product = productResource.adaptTo(Product.class);

        int quantity = 1;
        if (qty != null && qty.length() > 0) {
            quantity = xssAPI.getValidInteger(qty, 1);
            if (quantity < 0) {
                quantity = 1;
            }
        }

        try {
            session.addCartEntry(product, quantity);
        } catch (CommerceException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String renderContent(SlingHttpServletRequest request, SlingHttpServletResponse response, String contentPath)
            throws ServletException, IOException {

        // We wrap the original POST request in a GET and wrap the response in a string buffer
        GetRequestWrapper requestWrapper = new GetRequestWrapper(request);
        BufferingResponseWrapper responseWrapper = new BufferingResponseWrapper(response);

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("");

        Page currentPage = request.getResource().adaptTo(Page.class);
        Page root = WeRetailHelper.findRoot(currentPage);

        String path = CONTENT_WE_RETAIL_DEFAULT + contentPath; // Fallback if root is not found
        if (root != null) {
            path = root.getPath() + contentPath;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher(path, options);
        if (dispatcher != null) {
            dispatcher.include(requestWrapper, responseWrapper);
            return responseWrapper.toStrippedOutput();
        } else {
            throw new ServletException("Unable to obtain Request Dispatcher");
        }
    }

    private String renderNavCart(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        // We wrap the original POST request in a GET and wrap the response in a string buffer
        GetRequestWrapper requestWrapper = new GetRequestWrapper(request);
        BufferingResponseWrapper responseWrapper = new BufferingResponseWrapper(response);

        RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors("");

        SyntheticResource resource = new SyntheticResource(request.getResourceResolver(), NAV_CART_PATH, NAV_CART_PATH);
        RequestDispatcher dispatcher = request.getRequestDispatcher(resource, options);
        if (dispatcher != null) {
            dispatcher.include(requestWrapper, responseWrapper);

            return responseWrapper.toStrippedOutput();
        } else {
            throw new ServletException("Unable to obtain Request Dispatcher");
        }
    }

    private class GetRequestWrapper extends SlingHttpServletRequestWrapper {

        public GetRequestWrapper(SlingHttpServletRequest request) {
            super(request);
        }
        
        @Override
        public String getMethod() {
            return HttpConstants.METHOD_GET;
        }
    }
    
    private class BufferingResponseWrapper extends SlingHttpServletResponseWrapper {

        private StringWriter outputBuffer = new StringWriter();

        public BufferingResponseWrapper(SlingHttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(outputBuffer);
        }

        @Override
        public SlingHttpServletResponse getSlingResponse() {
            return super.getSlingResponse();
        }

        /**
         * Returns the generated html, with all html comments stripped out. This removes all cq:decoration comments.
         * 
         * @return The stripped html output.
         */
        public String toStrippedOutput() {
            return outputBuffer.toString().replaceAll("<!--(.*?)-->", "");
        }
    };
}

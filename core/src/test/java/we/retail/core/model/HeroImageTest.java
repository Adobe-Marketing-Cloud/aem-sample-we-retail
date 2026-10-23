/*
 *   Copyright 2018 Adobe Systems Incorporated
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HeroImageTest {

    @Test
    public void testNormal() throws Exception {
        HeroImage heroImage = new HeroImage();

        setField(heroImage, "request", createRequestWithImageSrc(null));
        setField(heroImage, "properties", valueMap());
        invokeInit(heroImage);

        assertEquals("we-HeroImage", heroImage.getClassList());
    }

    @Test
    public void testFullWidth() throws Exception {
        HeroImage heroImage = new HeroImage();

        setField(heroImage, "request", createRequestWithImageSrc(null));
        setField(heroImage, "properties", valueMap("useFullWidth", "true"));
        invokeInit(heroImage);

        assertEquals("we-HeroImage width-full", heroImage.getClassList());
    }

    @Test
    public void testNullImage() throws Exception {
        HeroImage heroImage = new HeroImage();

        setField(heroImage, "request", createRequestWithImageSrc(null));
        setField(heroImage, "properties", valueMap());
        invokeInit(heroImage);

        assertNull(heroImage.getImage().getSrc());
    }

    @Test
    public void testNoNullImage() throws Exception {
        HeroImage heroImage = new HeroImage();

        setField(heroImage, "request", createRequestWithImageSrc("/content/dam/we-retail/hero.jpg"));
        setField(heroImage, "properties", valueMap());
        invokeInit(heroImage);

        assertNotNull(heroImage.getImage().getSrc());
    }

    @Test
    public void testResolvesCategoryButtonLink() throws Exception {
        HeroImage heroImage = new HeroImage();
        org.apache.sling.api.SlingHttpServletRequest request = createRequestWithImageSrc(null);
        Page currentPage = mock(Page.class);
        UrlProvider urlProvider = mock(UrlProvider.class);

        when(urlProvider.formatCategoryUrl(eq(request), eq(currentPage), any()))
            .thenReturn("/content/we-retail/us/en/products/category-page.html/eq/hiking.html");

        setField(heroImage, "request", request);
        setField(heroImage, "properties", valueMap());
        setField(heroImage, "currentPage", currentPage);
        setField(heroImage, "urlProvider", urlProvider);
        setField(heroImage, "buttonLabel", "Explore hiking");
        setField(heroImage, "buttonLinkType", "category");
        setField(heroImage, "buttonCategoryId", "eq/hiking");
        setField(heroImage, "buttonCategoryIdType", "urlPath");
        invokeInit(heroImage);

        assertEquals("/content/we-retail/us/en/products/category-page.html/eq/hiking.html", heroImage.getButtonLink());
        assertTrue(heroImage.isButtonCallToAction());
    }

    @Test
    public void testResolvesProductButtonLink() throws Exception {
        HeroImage heroImage = new HeroImage();
        org.apache.sling.api.SlingHttpServletRequest request = createRequestWithImageSrc(null);
        Page currentPage = mock(Page.class);
        UrlProvider urlProvider = mock(UrlProvider.class);

        when(urlProvider.toProductUrl(request, currentPage, "meskwielt"))
            .thenReturn("/content/we-retail/us/en/products/product-page.html/me/coats/meskwielt.html");

        setField(heroImage, "request", request);
        setField(heroImage, "properties", valueMap());
        setField(heroImage, "currentPage", currentPage);
        setField(heroImage, "urlProvider", urlProvider);
        setField(heroImage, "buttonLabel", "Shop coat");
        setField(heroImage, "buttonLinkType", "product");
        setField(heroImage, "buttonProductSku", "meskwielt");
        invokeInit(heroImage);

        assertEquals("/content/we-retail/us/en/products/product-page.html/me/coats/meskwielt.html", heroImage.getButtonLink());
        assertTrue(heroImage.isButtonCallToAction());
    }

    @Test
    public void testHidesButtonWithoutResolvedLink() throws Exception {
        HeroImage heroImage = new HeroImage();

        setField(heroImage, "request", createRequestWithImageSrc(null));
        setField(heroImage, "properties", valueMap());
        setField(heroImage, "buttonLabel", "Explore hiking");
        setField(heroImage, "buttonLinkType", "category");
        invokeInit(heroImage);

        assertEquals("#", heroImage.getButtonLink());
        assertFalse(heroImage.isButtonCallToAction());
    }

    private org.apache.sling.api.SlingHttpServletRequest createRequestWithImageSrc(String src) {
        org.apache.sling.api.SlingHttpServletRequest request = mock(org.apache.sling.api.SlingHttpServletRequest.class);
        com.adobe.cq.wcm.core.components.models.Image image = mock(com.adobe.cq.wcm.core.components.models.Image.class);
        when(image.getSrc()).thenReturn(src);
        when(request.adaptTo(com.adobe.cq.wcm.core.components.models.Image.class)).thenReturn(image);
        return request;
    }

    private ValueMap valueMap() {
        return valueMap(null, null);
    }

    private ValueMap valueMap(String key, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        if (key != null) {
            values.put(key, value);
        }
        return new ValueMapDecorator(values);
    }

    private void invokeInit(HeroImage heroImage) throws Exception {
        Method initMethod = HeroImage.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(heroImage);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

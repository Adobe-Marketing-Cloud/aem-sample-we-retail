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

import com.adobe.cq.sightly.WCMBindings;
import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;

import static common.AppAemContext.TEASERS_PATH;

import static org.junit.Assert.*;

public class CategoryTeaserTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    @Test
    public void testWithLabel() {
        MockSlingHttpServletRequest request = context.request();
        Resource buttonRes = context.currentResource(TEASERS_PATH + "/with-label");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PROPERTIES, buttonRes.getValueMap());

        CategoryTeaser teaser = request.adaptTo(CategoryTeaser.class);
        assertEquals("/content/we-retail/us/en/products/men", teaser.getButtonLinkTo());
        assertEquals("All men's products", teaser.getButtonLabel());
    }

    @Test
    public void testWithoutLabel() {
        MockSlingHttpServletRequest request = context.request();
        Resource buttonRes = context.currentResource(TEASERS_PATH + "/without-label");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PROPERTIES, buttonRes.getValueMap());

        CategoryTeaser teaser = request.adaptTo(CategoryTeaser.class);
        assertEquals("/content/we-retail/us/en/products/men", teaser.getButtonLinkTo());
        assertEquals("Men", teaser.getButtonLabel());
    }
}

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
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProductGridItemTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    @Before
    public void setup() {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PAGE_MANAGER, context.pageManager());
    }

    @Test
    public void testExists() {
        context.currentResource("/content/we-retail/us/en/products/equipment/running/fleet-cross-training-shoe/jcr:content");
        ProductGridItem gridItem = context.request().adaptTo(ProductGridItem.class);
        assertNotNull(gridItem);
        assertTrue(gridItem.exists());
        assertEquals("/var/commerce/products/we-retail/eq/running/eqrusufle/image", gridItem.getImage());
        assertEquals("Fleet Cross-Training Shoe", gridItem.getName());
        assertEquals("footwear", gridItem.getDescription());
    }

    @Test
    public void testNotExists() {
        context.currentResource("/content/we-retail/us/en/about-us/jcr:content");
        ProductGridItem gridItem = context.request().adaptTo(ProductGridItem.class);
        assertNotNull(gridItem);
        assertFalse(gridItem.exists());
    }
}
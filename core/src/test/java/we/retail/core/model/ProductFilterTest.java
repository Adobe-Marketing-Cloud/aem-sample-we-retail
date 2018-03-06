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
package we.retail.core.model;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.sightly.WCMBindings;
import com.day.cq.wcm.api.designer.Style;
import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

import static common.AppAemContext.PRODUCT_FILTER_PATH;

@RunWith(MockitoJUnitRunner.class)
public class ProductFilterTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    @Mock
    private Style style;

    private ProductFilter productFilter;

    @Before
    public void setup() {
        MockSlingHttpServletRequest request = context.request();
        Resource productFilterRes = context.currentResource(PRODUCT_FILTER_PATH);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.CURRENT_STYLE, style);
        slingBindings.put(WCMBindings.PROPERTIES, productFilterRes.getValueMap());
        productFilter = request.adaptTo(ProductFilter.class);
    }

    /**
     * Test the product filter type
     */
    @Test
    public void testGetType() {
        Assert.assertEquals(productFilter.getType(), "color");
    }

}

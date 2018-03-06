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

import com.adobe.cq.sightly.WCMBindings;
import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

import static common.AppAemContext.BUTTON_PATH;

public class ButtonTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    private Button button;

    @Before
    public void setup() {
        MockSlingHttpServletRequest request = context.request();
        Resource buttonRes = context.currentResource(BUTTON_PATH);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PROPERTIES, buttonRes.getValueMap());
        button = request.adaptTo(Button.class);
    }

    /**
     * Test the button link
     */
    @Test
    public void testGetLinkTo() {
        Assert.assertEquals(button.getLinkTo(), "/content/we-retail/us/en/products/men");
    }

    /**
     * Test the button CSS class
     */
    @Test
    public void testGetCssClass() {
        Assert.assertEquals(button.getCssClass(), "myClass");
    }

}

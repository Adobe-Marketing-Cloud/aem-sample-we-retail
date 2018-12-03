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
import com.day.cq.wcm.api.designer.Cell;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Style;
import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static common.AppAemContext.HERO_IMAGES_PATH;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HeroImageTest {

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    @Test
    public void testNormal() {
        MockSlingHttpServletRequest request = context.request();
        Resource buttonRes = context.currentResource(HERO_IMAGES_PATH + "/normal");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PROPERTIES, buttonRes.getValueMap());

        HeroImage heroImage = request.adaptTo(HeroImage.class);
        assertEquals("we-HeroImage", heroImage.getClassList());
    }

    @Test
    public void testFullWidth() {
        MockSlingHttpServletRequest request = context.request();
        Resource buttonRes = context.currentResource(HERO_IMAGES_PATH + "/full-width");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PROPERTIES, buttonRes.getValueMap());

        HeroImage heroImage = request.adaptTo(HeroImage.class);
        assertEquals("we-HeroImage width-full", heroImage.getClassList());
    }

    @Test
    public void testNullImage() {
        MockSlingHttpServletRequest request = context.request();
        Resource buttonRes = context.currentResource(HERO_IMAGES_PATH + "/normal");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PROPERTIES, buttonRes.getValueMap());

        HeroImage heroImage = request.adaptTo(HeroImage.class);
        assertNull(heroImage.getImage().getSrc());
    }

    @Test
    public void testNoNullImage() {
        MockSlingHttpServletRequest request = context.request();
        Resource buttonRes = context.currentResource(HERO_IMAGES_PATH + "/withImage");
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindings.PROPERTIES, buttonRes.getValueMap());

        HeroImage heroImage = request.adaptTo(HeroImage.class);

        assertNotNull(heroImage.getImage().getSrc());
    }


    private class EmptyStyle extends ValueMapDecorator implements Style {
        private EmptyStyle() {
            super(Collections.emptyMap());
        }

        @Override
        public Cell getCell() {
            return null;
        }

        @Override
        public Design getDesign() {
            return null;
        }

        @Override
        public Resource getDefiningResource(String s) {
            return null;
        }

        @Override
        public String getDefiningPath(String s) {
            return null;
        }

        @Override
        public Style getSubStyle(String s) {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }
    }

}

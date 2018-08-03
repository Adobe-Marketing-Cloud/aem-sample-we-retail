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
package we.retail.core.model;

import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.sightly.WCMBindings;
import com.adobe.granite.security.user.UserManagementService;
import com.day.cq.wcm.api.designer.Style;
import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class ProductModelTest {

    private static final String CURRENT_RESOURCE = "/content/we-retail/us/en/products/equipment/running/fleet-cross-training-shoe/jcr:content/root/product";
    private static final String CURRENT_PAGE = "/content/we-retail/us/en/products/equipment/running/fleet-cross-training-shoe";

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    @Mock
    private Style style;

    @Mock
    private UserManagementService ums;

    private ProductModel productModel;
    private ProductItem productItem;

    @Before
    public void setUp() throws Exception {
        context.currentPage(CURRENT_PAGE);
        SlingBindings attribute = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        attribute.put("currentStyle", style);
        attribute.put(WCMBindings.CURRENT_PAGE, context.currentPage());
        context.currentResource(CURRENT_RESOURCE);
        context.registerService(UserManagementService.class, ums);

        productModel = context.request().adaptTo(ProductModel.class);
        productItem = productModel.getProductItem();
    }

    @Test
    public void testProduct() throws Exception {
        assertNotNull(productItem);
        assertNull(productItem.getVariantValueForAxis("size"));
        assertEquals("eqrusufle", productItem.getSku());
        assertEquals(CURRENT_RESOURCE, productItem.getPath());
        assertNull(productItem.getTitle());
        assertNull(productItem.getDescription());
        assertNotNull(productItem.getSummary());
        assertNotNull(productItem.getFeatures());
        assertNotNull(productItem.getPrice());
        assertEquals("/content/dam/we-retail/en/products/apparel/footwear/source/Fleet Shoe.jpg", productItem.getImageUrl());
        assertNull(productItem.getThumbnailUrl());
    }

    @Test
    public void testVariants() throws Exception {
        assertEquals(3, productItem.getVariants().size());
        ProductItem variantItem = productItem.getVariants().get(0);
        assertEquals(CURRENT_RESOURCE + "/eqrusufle-9", variantItem.getPath());
        assertNotNull(variantItem.getPagePath());
        assertEquals("eqrusufle-9", variantItem.getSku());
        assertNull(variantItem.getTitle());
        assertNull(variantItem.getDescription());
        assertNull(variantItem.getVariantValueForAxis("color"));
        assertEquals("9", variantItem.getVariantValueForAxis("size"));
        assertNull(variantItem.getPrice());
        assertNull(variantItem.getSummary());
        assertNull(variantItem.getFeatures());
        assertNull(variantItem.getImageUrl());
        assertNull(variantItem.getThumbnailUrl());
    }

}

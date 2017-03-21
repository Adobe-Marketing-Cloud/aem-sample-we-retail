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
package we.retail.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;

import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;
import we.retail.core.model.Constants;

public class WeRetailHelperTest {

    private static final String MOCK_RESOURCE_TITLE = "mockResourceTitle";
    private static final String ORDER_DETAILS_TITLE = "Order Details";

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    private Page page;

    @Before
    public void setUp() throws Exception {
        page = context.currentPage(Constants.TEST_ORDER_PAGE);
        context.currentResource(Constants.TEST_ORDER_RESOURCE);
    }

    @Test
    public void testMethods() throws Exception {
        Resource mockResource = mock(Resource.class);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(NameConstants.PN_TITLE, MOCK_RESOURCE_TITLE);
        ValueMap vm = new ValueMapDecorator(map);
        when(mockResource.adaptTo(ValueMap.class)).thenReturn(vm);

        assertEquals(MOCK_RESOURCE_TITLE, WeRetailHelper.getTitle(mockResource, page));
        assertEquals(ORDER_DETAILS_TITLE, WeRetailHelper.getPageTitle(page));
        assertEquals(ORDER_DETAILS_TITLE, WeRetailHelper.getTitle(page));

        assertNull(WeRetailHelper.getTitle(null, page));
        assertNull(WeRetailHelper.getPageTitle(null));
        assertNull(WeRetailHelper.getTitle(null));

        // If the resource does not have a title, the page title is returned
        Map<String, Object> map2 = new HashMap<String, Object>();
        ValueMap vm2 = new ValueMapDecorator(map2);
        when(mockResource.adaptTo(ValueMap.class)).thenReturn(vm2);
        assertEquals(ORDER_DETAILS_TITLE, WeRetailHelper.getTitle(mockResource, page));
    }
}

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CatalogBreadcrumbTest {

    private CatalogBreadcrumb catalogBreadcrumb;
    private SlingHttpServletRequest request;
    private Page currentPage;
    private Style currentStyle;
    private SiteStructure siteStructure;
    private Breadcrumb delegate;
    private Breadcrumb commerceBreadcrumb;
    private ModelFactory modelFactory;
    private Map<String, Object> properties;

    @Before
    public void setUp() throws Exception {
        catalogBreadcrumb = new CatalogBreadcrumb();
        request = mock(SlingHttpServletRequest.class);
        currentPage = mock(Page.class);
        currentStyle = mock(Style.class);
        siteStructure = mock(SiteStructure.class);
        delegate = mock(Breadcrumb.class);
        commerceBreadcrumb = mock(Breadcrumb.class);
        modelFactory = mock(ModelFactory.class);
        properties = new HashMap<String, Object>();
        properties.put("sling:resourceType", CatalogBreadcrumb.RESOURCE_TYPE);
        properties.put("editable", Boolean.FALSE);

        Resource resource = mock(Resource.class);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(properties));
        when(currentStyle.get(Breadcrumb.PN_HIDE_CURRENT, false)).thenReturn(true);
        when(modelFactory.getModelFromWrappedRequest(eq(request), any(Resource.class), eq(Breadcrumb.class)))
            .thenReturn(commerceBreadcrumb);

        setField("request", request);
        setField("resource", resource);
        setField("currentPage", currentPage);
        setField("properties", new ValueMapDecorator(properties));
        setField("currentStyle", currentStyle);
        setField("siteStructure", siteStructure);
        setField("delegate", delegate);
        setField("modelFactory", modelFactory);
    }

    @Test
    public void testHidesCurrentProductOnProductPage() throws Exception {
        Collection<NavigationItem> commerceItems = items(item("Equipment", false), item("Snow Sports", false),
            item("Sleek Insulated Coat", true));
        when(siteStructure.isProductPage(currentPage)).thenReturn(true);
        when(commerceBreadcrumb.getItems()).thenReturn(commerceItems);

        assertEquals(Arrays.asList("Equipment", "Snow Sports"), titles(catalogBreadcrumb.getItems()));

        ArgumentCaptor<Resource> commerceResource = ArgumentCaptor.forClass(Resource.class);
        verify(modelFactory).getModelFromWrappedRequest(eq(request), commerceResource.capture(), eq(Breadcrumb.class));
        assertEquals(CatalogBreadcrumb.CIF_BREADCRUMB_RESOURCE_TYPE, commerceResource.getValue().getResourceType());
        assertEquals(Boolean.FALSE, commerceResource.getValue().getValueMap().get(Breadcrumb.PN_HIDE_CURRENT, Boolean.class));
        assertEquals(Boolean.TRUE, commerceResource.getValue().getValueMap().get(Breadcrumb.PN_SHOW_HIDDEN, Boolean.class));
        assertEquals(Boolean.FALSE, commerceResource.getValue().getValueMap().get("editable", Boolean.class));
        verify(delegate, never()).getItems();
    }

    @Test
    public void testHidesCurrentCategoryOnCategoryPage() throws Exception {
        Collection<NavigationItem> commerceItems = items(item("Men", false), item("Shirts", true));
        when(siteStructure.isCategoryPage(currentPage)).thenReturn(true);
        when(commerceBreadcrumb.getItems()).thenReturn(commerceItems);

        assertEquals(Arrays.asList("Men"), titles(catalogBreadcrumb.getItems()));
    }

    @Test
    public void testTopLevelCategoryPageHasNoItems() throws Exception {
        Collection<NavigationItem> commerceItems = items(item("Men", true));
        when(siteStructure.isCategoryPage(currentPage)).thenReturn(true);
        when(commerceBreadcrumb.getItems()).thenReturn(commerceItems);

        assertTrue(catalogBreadcrumb.getItems().isEmpty());
    }

    @Test
    public void testKeepsCurrentItemWhenHideCurrentIsDisabled() throws Exception {
        Collection<NavigationItem> commerceItems = items(item("Men", false), item("Shirts", false),
            item("Eton Short-Sleeve Shirt", true));
        properties.put(Breadcrumb.PN_HIDE_CURRENT, false);
        when(siteStructure.isProductPage(currentPage)).thenReturn(true);
        when(commerceBreadcrumb.getItems()).thenReturn(commerceItems);

        assertEquals(Arrays.asList("Men", "Shirts", "Eton Short-Sleeve Shirt"), titles(catalogBreadcrumb.getItems()));
    }

    @Test
    public void testContentPageUsesCommerceBreadcrumbAsConfigured() throws Exception {
        Collection<NavigationItem> contentItems = items(item("English", false), item("Experience", false));
        ComponentData data = mock(ComponentData.class);
        when(delegate.getItems()).thenReturn(contentItems);
        when(delegate.getId()).thenReturn("breadcrumb-1");
        when(delegate.getData()).thenReturn(data);

        assertSame(contentItems, catalogBreadcrumb.getItems());
        assertEquals("breadcrumb-1", catalogBreadcrumb.getId());
        assertSame(data, catalogBreadcrumb.getData());
        verify(modelFactory, never()).getModelFromWrappedRequest(any(SlingHttpServletRequest.class), any(Resource.class),
            eq(Breadcrumb.class));
    }

    private NavigationItem item(String title, boolean active) {
        NavigationItem item = mock(NavigationItem.class);
        when(item.getTitle()).thenReturn(title);
        when(item.isActive()).thenReturn(active);
        return item;
    }

    private Collection<NavigationItem> items(NavigationItem... items) {
        return new ArrayList<NavigationItem>(Arrays.asList(items));
    }

    private List<String> titles(Collection<NavigationItem> items) {
        List<String> titles = new ArrayList<String>();
        for (NavigationItem item : items) {
            titles.add(item.getTitle());
        }
        return titles;
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = CatalogBreadcrumb.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(catalogBreadcrumb, value);
    }
}

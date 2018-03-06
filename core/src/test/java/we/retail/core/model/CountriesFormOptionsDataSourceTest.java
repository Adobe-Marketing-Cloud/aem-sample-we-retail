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

import java.util.Iterator;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.sightly.WCMBindings;
import com.adobe.granite.ui.components.ds.DataSource;
import com.day.cq.wcm.api.Page;
import common.AppAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountriesFormOptionsDataSourceTest {

    private static final String DATASOURCE_PATH = "/apps/countriesList";

    @Rule
    public final AemContext context = AppAemContext.newAemContext();

    @Mock
    private Page mockPage;

    @Mock
    private ResourceBundleProvider mockResourceBundleProvider;


    private CountriesFormOptionsDataSource underTest;

    @Before
    public void setUp() throws Exception {
        Locale deLocale = new Locale("de", "CH");
        ResourceBundle resourceBundle = new PropertyResourceBundle(this.getClass().getResourceAsStream("/de_CH.properties"));
        context.registerService(ResourceBundleProvider.class, mockResourceBundleProvider);
        when(mockResourceBundleProvider.getResourceBundle(any(), any())).thenReturn(resourceBundle);
        context.load().json("/countries-from-options-data-source.json", DATASOURCE_PATH);
        underTest = new CountriesFormOptionsDataSource();
        SlingBindings bindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        bindings.put(WCMBindings.CURRENT_PAGE, mockPage);
        when(mockPage.getLanguage(true)).thenReturn(deLocale);
    }

    @Test
    public void testGetCountriesList() {
        context.currentResource(DATASOURCE_PATH);
        underTest.doGet(context.request(), context.response());
        DataSource dataSource = (DataSource) context.request().getAttribute(DataSource.class.getName());
        Iterator<Resource> iterator = dataSource.iterator();
        int length = 0;
        Resource frResource = null;
        while (iterator.hasNext()) {
            Resource resource = iterator.next();
            if(resource.getValueMap().get("value", "").equals("FR")) {
                 frResource = resource;
            }
            length++;
        }
        assertEquals(31, length);
        assertNotNull(frResource);
        assertEquals("Frankreich", frResource.getValueMap().get("text", ""));
    }
}
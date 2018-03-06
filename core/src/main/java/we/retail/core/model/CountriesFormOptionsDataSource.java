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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.sightly.WCMBindings;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableMap;

@Component(
        service = {Servlet.class},
        property = {
                "sling.servlet.resourceTypes=" + CountriesFormOptionsDataSource.RESOURCE_TYPE,
                "sling.servlet.methods=GET",
                "sling.servlet.extensions=html"
        }
)
public class CountriesFormOptionsDataSource extends SlingSafeMethodsServlet {

    final static String RESOURCE_TYPE = "weretail/components/form/options/datasource/countriesdatasource";

    private final static String COUNTRY_OPTIONS_HEADER = "Country";
    private final static String PN_TEXT = "text";
    private final static String PN_VALUE = "value";
    private final static Map<String, String> COUNTRY_MAP = ImmutableMap.<String, String>builder()
            .put("AR", "Argentina")
            .put("AU", "Australia")
            .put("AT", "Austria")
            .put("BS", "Bahamas")
            .put("BH", "Bahrain")
            .put("BR", "Brazil")
            .put("CA", "Canada")
            .put("CL", "Chile")
            .put("CN", "China")
            .put("CO", "Colombia")
            .put("EG", "Egypt")
            .put("FR", "France")
            .put("DE", "Germany")
            .put("GI", "Gibraltar")
            .put("HK", "Hong Kong")
            .put("IE", "Ireland")
            .put("IT", "Italy")
            .put("JP", "Japan")
            .put("LU", "Luxembourg")
            .put("MY", "Malaysia")
            .put("MX", "Mexico")
            .put("MC", "Monaco")
            .put("RU", "Russia")
            .put("SG", "Singapore")
            .put("ES", "Spain")
            .put("CH", "Switzerland")
            .put("US", "United States of America")
            .put("AE", "United Arab Emirates")
            .put("GB", "United Kingdom")
            .put("UY", "Uruguay")
            .build();

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        List<Resource> countriesList = getCountriesList(request);
        SimpleDataSource countriesDataSource = new SimpleDataSource(countriesList.iterator());
        request.setAttribute(DataSource.class.getName(), countriesDataSource);
    }

    private List<Resource> getCountriesList(SlingHttpServletRequest request) {
        List<Resource> countries = new ArrayList<>();
        SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        if (bindings != null) {
            Page currentPage = (Page) bindings.get(WCMBindings.CURRENT_PAGE);
            if (currentPage != null) {
                Locale pageLocale = currentPage.getLanguage(true);
                ResourceBundle bundle = request.getResourceBundle(pageLocale);
                if (bundle != null) {
                    I18n i18n = new I18n(bundle);

                    ResourceResolver resourceResolver = request.getResourceResolver();
                    for (String key : COUNTRY_MAP.keySet()) {
                        countries.add(getCountryResource(resourceResolver, i18n, key, COUNTRY_MAP.get(key)));
                    }

                    // Sort based on translated display text:
                    countries.sort((o1, o2) -> {
                        ValueMap v1 = o1.adaptTo(ValueMap.class);
                        ValueMap v2 = o2.adaptTo(ValueMap.class);
                        if (v1 != null && v2 != null) {
                            return v1.get(PN_TEXT, StringUtils.EMPTY).compareTo(v2.get(PN_TEXT, StringUtils.EMPTY));
                        } else {
                            return 0;
                        }
                    });
                    // add the header of the country options
                    countries.add(0, getCountryOptionHeader(resourceResolver, i18n));
                }
            }
        }
        return countries;
    }

    private Resource getCountryResource(ResourceResolver resolver, I18n i18n, String countryCode, String countryName) {
        ValueMap vm = new ValueMapDecorator(new HashMap<>());
        vm.put(PN_VALUE, countryCode);
        vm.put(PN_TEXT, i18n.get(countryName));
        return new ValueMapResource(resolver, "", "", vm);
    }

    private Resource getCountryOptionHeader(ResourceResolver resolver, I18n i18n) {
        ValueMap vm = new ValueMapDecorator(new HashMap<>());
        vm.put("value", "");
        vm.put("text", i18n.get(COUNTRY_OPTIONS_HEADER));
        vm.put("selected", true);
        vm.put("disabled", true);
        return new ValueMapResource(resolver, "", "", vm);
    }
}

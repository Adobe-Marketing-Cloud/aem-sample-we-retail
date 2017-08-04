/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2016 Adobe Systems Incorporated
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

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

    private I18n i18n;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        Page currentPage = (Page) bindings.get(WCMBindings.CURRENT_PAGE);
        final Locale pageLocale = currentPage.getLanguage(true);
        final ResourceBundle bundle = request.getResourceBundle(pageLocale);
        i18n = new I18n(bundle);
        SimpleDataSource countriesDataSource = new SimpleDataSource(buildCountriesList(request.getResourceResolver()).iterator());
        request.setAttribute(DataSource.class.getName(), countriesDataSource);
    }

    private List<Resource> buildCountriesList(ResourceResolver resolver) {
        List<Resource> countries = new ArrayList<Resource>();
        addCountry(resolver, countries, "AR", "Argentina");
        addCountry(resolver, countries, "AU", "Australia");
        addCountry(resolver, countries, "AT", "Austria");
        addCountry(resolver, countries, "BS", "Bahamas");
        addCountry(resolver, countries, "BH", "Bahrain");
        addCountry(resolver, countries, "BR", "Brazil");
        addCountry(resolver, countries, "CA", "Canada");
        addCountry(resolver, countries, "CL", "Chile");
        addCountry(resolver, countries, "CN", "China");
        addCountry(resolver, countries, "CO", "Colombia");
        addCountry(resolver, countries, "EG", "Egypt");
        addCountry(resolver, countries, "FR", "France");
        addCountry(resolver, countries, "DE", "Germany");
        addCountry(resolver, countries, "GI", "Gibraltar");
        addCountry(resolver, countries, "HK", "Hong Kong");
        addCountry(resolver, countries, "IE", "Ireland");
        addCountry(resolver, countries, "IT", "Italy");
        addCountry(resolver, countries, "JP", "Japan");
        addCountry(resolver, countries, "LU", "Luxembourg");
        addCountry(resolver, countries, "MY", "Malaysia");
        addCountry(resolver, countries, "MX", "Mexico");
        addCountry(resolver, countries, "MC", "Monaco");
        addCountry(resolver, countries, "RU", "Russia");
        addCountry(resolver, countries, "SG", "Singapore");
        addCountry(resolver, countries, "ES", "Spain");
        addCountry(resolver, countries, "CH", "Switzerland");
        addCountry(resolver, countries, "US", "United States of America");
        addCountry(resolver, countries, "AE", "United Arab Emirates");
        addCountry(resolver, countries, "GB", "United Kingdom");
        addCountry(resolver, countries, "UY", "Uruguay");
        addCountry(resolver, countries, "VE", "Venezuela");

        // Sort based on translated display text:
        Collections.sort(countries, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return o1.adaptTo(ValueMap.class).get("text", "").compareTo(o2.adaptTo(ValueMap.class).get("text", ""));
            }
        });

        // add the header of the country options
        addCountryOptionHeader(resolver, countries);

        return countries;
    }

    private void addCountry(ResourceResolver resolver, List<Resource> countries, String countryCode, String countryName) {
        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
        vm.put("value", countryCode);
        vm.put("text", i18n.get(countryName));
        ValueMapResource countryRes = new ValueMapResource(resolver, "", "", vm);
        countries.add(countryRes);
    }

    private void addCountryOptionHeader(ResourceResolver resolver, List<Resource> countries) {
        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
        vm.put("value", "");
        vm.put("text", i18n.get(COUNTRY_OPTIONS_HEADER));
        vm.put("selected", true);
        vm.put("disabled", true);
        ValueMapResource countryRes = new ValueMapResource(resolver, "", "", vm);
        countries.add(0, countryRes);
    }

}

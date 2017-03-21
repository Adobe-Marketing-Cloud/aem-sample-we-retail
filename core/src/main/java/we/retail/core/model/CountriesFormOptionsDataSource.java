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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.adobe.cq.wcm.core.components.models.form.DataSourceModel;

@Model(adaptables = SlingHttpServletRequest.class,
        adapters = DataSourceModel.class,
        resourceType = CountriesFormOptionsDataSource.RESOURCE_TYPE)
public class CountriesFormOptionsDataSource extends DataSourceModel {

    protected final static String RESOURCE_TYPE = "weretail/components/form/options/datasource/countriesdatasource";
    protected final static String COUNTRY_OPTIONS_HEADER = "Country";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @SlingObject
    private ResourceResolver resolver;

    private I18n i18n;

    @PostConstruct
    private void initModel() {
        final Locale pageLocale = currentPage.getLanguage(true);
        final ResourceBundle bundle = request.getResourceBundle(pageLocale);
        i18n = new I18n(bundle);

        SimpleDataSource countriesDataSource = new SimpleDataSource(buildCountriesList().iterator());
        initDataSource(request, countriesDataSource);
    }

    private List<Resource> buildCountriesList() {
        List<Resource> countries = new ArrayList<Resource>();
        addCountry(countries, "AR", "Argentina");
        addCountry(countries, "AU", "Australia");
        addCountry(countries, "AT", "Austria");
        addCountry(countries, "BS", "Bahamas");
        addCountry(countries, "BH", "Bahrain");
        addCountry(countries, "BR", "Brazil");
        addCountry(countries, "CA", "Canada");
        addCountry(countries, "CL", "Chile");
        addCountry(countries, "CN", "China");
        addCountry(countries, "CO", "Colombia");
        addCountry(countries, "EG", "Egypt");
        addCountry(countries, "FR", "France");
        addCountry(countries, "DE", "Germany");
        addCountry(countries, "GI", "Gibraltar");
        addCountry(countries, "HK", "Hong Kong");
        addCountry(countries, "IE", "Ireland");
        addCountry(countries, "IT", "Italy");
        addCountry(countries, "JP", "Japan");
        addCountry(countries, "LU", "Luxembourg");
        addCountry(countries, "MY", "Malaysia");
        addCountry(countries, "MX", "Mexico");
        addCountry(countries, "MC", "Monaco");
        addCountry(countries, "RU", "Russia");
        addCountry(countries, "SG", "Singapore");
        addCountry(countries, "ES", "Spain");
        addCountry(countries, "CH", "Switzerland");
        addCountry(countries, "US", "United States of America");
        addCountry(countries, "AE", "United Arab Emirates");
        addCountry(countries, "GB", "United Kingdom");
        addCountry(countries, "UY", "Uruguay");
        addCountry(countries, "VE", "Venezuela");

        // Sort based on translated display text:
        Collections.sort(countries, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return o1.adaptTo(ValueMap.class).get("text", "").compareTo(o2.adaptTo(ValueMap.class).get("text", ""));
            }
        });

        // add the header of the country options
        addCountryOptionHeader(countries);

        return countries;
    }

    private void addCountry(List<Resource> countries, String countryCode, String countryName) {
        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
        vm.put("value", countryCode);
        vm.put("text", i18n.get(countryName));
        ValueMapResource countryRes = new ValueMapResource(resolver, "", "", vm);
        countries.add(countryRes);
    }

    private void addCountryOptionHeader(List<Resource> countries) {
        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
        vm.put("value", "");
        vm.put("text", i18n.get(COUNTRY_OPTIONS_HEADER));
        vm.put("selected", true);
        vm.put("disabled", true);
        ValueMapResource countryRes = new ValueMapResource(resolver, "", "", vm);
        countries.add(0, countryRes);
    }

}

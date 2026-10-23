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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

/**
 * We.Retail breadcrumb that applies {@code hideCurrent} to the category or product that the commerce breadcrumb appends
 * on CIF category and product pages.
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Breadcrumb.class,
    resourceType = CatalogBreadcrumb.RESOURCE_TYPE)
public class CatalogBreadcrumb implements Breadcrumb {

    static final String RESOURCE_TYPE = "weretail/components/content/breadcrumb";
    static final String CIF_BREADCRUMB_RESOURCE_TYPE = "core/cif/components/structure/breadcrumb/v1/breadcrumb";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Style currentStyle;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private SiteStructure siteStructure;

    @Self
    @Via(type = ResourceSuperType.class)
    private Breadcrumb delegate;

    @OSGiService
    private ModelFactory modelFactory;

    private Collection<NavigationItem> items;

    @Override
    public Collection<NavigationItem> getItems() {
        if (items == null) {
            items = isCatalogPage() ? getCatalogItems() : delegate.getItems();
        }
        return items;
    }

    private boolean isCatalogPage() {
        return siteStructure != null && currentPage != null
            && (siteStructure.isProductPage(currentPage) || siteStructure.isCategoryPage(currentPage));
    }

    private Collection<NavigationItem> getCatalogItems() {
        // The commerce breadcrumb only expands the category or product page when that hidden page stays in the page trail.
        ValueMapResourceWrapper catalogResource = new ValueMapResourceWrapper(resource, CIF_BREADCRUMB_RESOURCE_TYPE);
        ValueMap catalogProperties = catalogResource.getValueMap();
        for (Map.Entry<String, Object> property : resource.getValueMap().entrySet()) {
            catalogProperties.putIfAbsent(property.getKey(), property.getValue());
        }
        catalogProperties.put(PN_HIDE_CURRENT, false);
        catalogProperties.put(PN_SHOW_HIDDEN, true);

        Breadcrumb catalogBreadcrumb = modelFactory.getModelFromWrappedRequest(request, catalogResource, Breadcrumb.class);
        if (catalogBreadcrumb == null) {
            return Collections.emptyList();
        }

        List<NavigationItem> catalogItems = new ArrayList<NavigationItem>(catalogBreadcrumb.getItems());
        if (isHideCurrent()) {
            catalogItems.removeIf(NavigationItem::isActive);
        }
        return Collections.unmodifiableList(catalogItems);
    }

    private boolean isHideCurrent() {
        boolean styleHideCurrent = currentStyle != null && currentStyle.get(PN_HIDE_CURRENT, false);
        return properties.get(PN_HIDE_CURRENT, styleHideCurrent);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public ComponentData getData() {
        return delegate.getData();
    }

    @Override
    public String getAppliedCssClasses() {
        return delegate.getAppliedCssClasses();
    }

    @Override
    public String getExportedType() {
        return delegate.getExportedType();
    }
}

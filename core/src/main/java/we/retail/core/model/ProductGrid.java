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

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.CommerceHelper;
import com.adobe.cq.wcm.core.components.models.List;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(adaptables = SlingHttpServletRequest.class, adapters = List.class, resourceType = "weretail/components/content/productgrid")
public class ProductGrid implements List {

    @Self @Via(type = ResourceSuperType.class)
    private List delegate;

    @ScriptVariable
    private PageManager pageManager;

    @Override
    public Collection<ListItem> getListItems() {
        return delegate.getListItems().stream().map(listItem -> {
            Page page = pageManager.getPage(listItem.getPath());
            if (page != null) {
                Product product = CommerceHelper.findCurrentProduct(page);
                if (product != null) {
                    return listItem;
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public boolean linkItems() {
        return delegate.linkItems();
    }

    @Override
    public boolean showDescription() {
        return delegate.showDescription();
    }

    @Override
    public boolean showModificationDate() {
        return delegate.showModificationDate();
    }

    @Override
    public String getDateFormatString() {
        return delegate.getDateFormatString();
    }

    @Override
    public String getExportedType() {
        return delegate.getExportedType();
    }
}

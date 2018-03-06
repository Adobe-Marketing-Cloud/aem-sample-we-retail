/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package we.retail.core.productrelationships;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.ProductRelationship;
import com.adobe.cq.commerce.api.ProductRelationshipsProvider;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import we.retail.core.WeRetailConstants;

/**
 * Abstract base classe for We.Retail <code>ProductRelationshipsProvider</code>.
 */
public abstract class AbstractRelationshipsProvider implements ProductRelationshipsProvider {

    private final String relationshipType;
    private final String relationshipTitle;

    public AbstractRelationshipsProvider(String relationshipType, String relationshipTitle) {
        this.relationshipType = relationshipType;
        this.relationshipTitle = relationshipTitle;
    }

    @Override
    public Map<String, String> getRelationshipTypes() {
        Map<String, String> types = new HashMap<String, String>(0);
        types.put(relationshipType, relationshipTitle);
        return types;
    }

    @Override
    public List<ProductRelationship> getRelationships(SlingHttpServletRequest request, CommerceSession session,
                                                      Page currentPage, Product currentProduct)
            throws CommerceException {
        if (session == null) {
            return null;
        }

        if (currentPage != null) {
            InheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(currentPage.getContentResource());
            String commerceProvider = properties.getInherited(CommerceConstants.PN_COMMERCE_PROVIDER, String.class);
            if (commerceProvider != null && !commerceProvider.equals(WeRetailConstants.WE_RETAIL_COMMERCEPROVIDER)) {
                return null;
            }
        }

        return calculateRelationships(request, session, currentPage, currentProduct);
    }

    protected abstract List<ProductRelationship> calculateRelationships(SlingHttpServletRequest request,
                                                                        CommerceSession session, Page currentPage,
                                                                        Product currentProduct)
            throws CommerceException;
}

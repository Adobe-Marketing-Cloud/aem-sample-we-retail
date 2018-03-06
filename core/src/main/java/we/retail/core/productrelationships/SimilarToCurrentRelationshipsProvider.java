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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.ProductRelationship;
import com.adobe.cq.commerce.api.ProductRelationshipsProvider;
import com.day.cq.wcm.api.Page;
import we.retail.core.util.WeRetailHelper;

/**
 * <code>SimilarToCurrentRelatedProductsProvider</code> provides a list of relationships to products
 * having at least one tag in common with the current product.  The list is sorted on number of matched
 * tags.
 *
 * NB: this is an example relationship provider which trades off performance for simplicity.  A
 * production system would require a much more performant implementation.
 */
@Component(label = "We.Retail Similar-to-Current Recommendations Provider",
        description = "Example ProductRelationshipsProvider which recommends products similar to the current product")
@Service
@Properties(value = {
        @Property(name = ProductRelationshipsProvider.RELATIONSHIP_TYPE_PN, value = SimilarToCurrentRelationshipsProvider.RELATIONSHIP_TYPE, propertyPrivate = true)
})
public class SimilarToCurrentRelationshipsProvider extends AbstractRelationshipsProvider {

    public static final String RELATIONSHIP_TYPE = "we-retail.similar-to-current";
    // i18n.get("Similar to current");
    public static final String RELATIONSHIP_TITLE = "Similar to current";

    public SimilarToCurrentRelationshipsProvider() {
        super(RELATIONSHIP_TYPE, RELATIONSHIP_TITLE);
    }

    @Override
    protected List<ProductRelationship> calculateRelationships(SlingHttpServletRequest request, CommerceSession session,
                                                               Page currentPage, Product currentProduct)
            throws CommerceException {
        if (currentProduct == null) {
            return null;
        }

        // Add current product to context
        List<Product> contextProducts = new ArrayList<Product>();
        contextProducts.add(currentProduct);

        // Walk content-pages to find similar products
        ResourceResolver resolver = request.getResourceResolver();
        SimilarProductsCollector collector = new SimilarProductsCollector(resolver, session, RELATIONSHIP_TYPE,
                RELATIONSHIP_TITLE, contextProducts);
        final Page root = WeRetailHelper.findRoot(currentPage);
        if (root != null) {
            collector.walk(root.getContentResource().getParent());
        }
        return collector.getRelationships();
    }
}

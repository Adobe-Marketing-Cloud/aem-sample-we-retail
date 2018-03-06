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
 * <code>SimilarToCartRelationshipsProvider</code> provides a list of relationships to products
 * having at least one tag in common with a product in the cart.  The relationships are sorted
 * on the number of matched tags.
 *
 * NB: this is an example relationship provider which trades off performance for simplicity.  A
 * production system would require a much more performant implementation.
 */
@Component(label = "We.Retail Similar-to-Cart Recommendations Provider",
        description = "Example ProductRelationshipsProvider which recommends products similar to the products in the cart")
@Service
@Properties(value = {
        @Property(name = ProductRelationshipsProvider.RELATIONSHIP_TYPE_PN, value = SimilarToCartRelationshipsProvider.RELATIONSHIP_TYPE, propertyPrivate = true)
})
public class SimilarToCartRelationshipsProvider extends AbstractRelationshipsProvider {

    public static final String RELATIONSHIP_TYPE = "we-retail.similar-to-cart";
    // i18n.get("Similar to cart");
    public static final String RELATIONSHIP_TITLE = "Similar to cart";

    public SimilarToCartRelationshipsProvider() {
        super(RELATIONSHIP_TYPE, RELATIONSHIP_TITLE);
    }

    @Override
    protected List<ProductRelationship> calculateRelationships(SlingHttpServletRequest request, CommerceSession session,
                                                               Page currentPage, Product currentProduct)
            throws CommerceException {
        // Add all products of the current cart to context
        final List<Product> contextProducts = new ArrayList<Product>();
        final List<CommerceSession.CartEntry> cartEntries = session.getCartEntries();
        for (CommerceSession.CartEntry entry : cartEntries) {
            contextProducts.add(entry.getProduct());
        }

        // Walk content-pages to find similar products
        ResourceResolver resolver = request.getResourceResolver();
        SimilarProductsCollector collector = new SimilarProductsCollector(resolver, session, RELATIONSHIP_TYPE,
                RELATIONSHIP_TITLE,
                contextProducts);
        final Page root = WeRetailHelper.findRoot(currentPage);
        if (root != null && root.getContentResource() != null) {
            collector.walk(root.getContentResource().getParent());
        }
        return collector.getRelationships();
    }
}


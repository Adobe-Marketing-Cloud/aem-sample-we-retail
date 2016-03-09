package we.retail.core.productrelationships;


import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.ProductRelationship;
import com.adobe.cq.commerce.api.ProductRelationshipsProvider;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>SimilarToCurrentRelatedProductsProvider</code> provides a list of relationships to products
 * having at least one tag in common with the current product.  The list is sorted on number of matched
 * tags.
 *
 * NB: this is an example relationship provider which trades off performance for simplicity.  A
 * production system would require a much more performant implementation.
 */
@Component(metatype = true,
        label = "we.Retail Similar-to-Current Recommendations Provider",
        description = "Example ProductRelationshipsProvider which recommends products similar to the current product")
@Service
@Properties(value = {
        @Property(name = "service.description", value = "Example ProductRelationshipsProvider which recommends products similar to the current product"),
        @Property(name = ProductRelationshipsProvider.RELATIONSHIP_TYPE_PN, value = SimilarToCurrentRelationshipsProvider.RELATIONSHIP_TYPE, propertyPrivate = true)
})
public class SimilarToCurrentRelationshipsProvider implements ProductRelationshipsProvider {

    public static final String RELATIONSHIP_TYPE = "info.we-retail.similar-to-current";
    public static final String RELATIONSHIP_TITLE = "Similar to current";

    private boolean enabled;

    @Property(boolValue = true, label = "Enable", description = "Provide recommendations")
    public final static String ENABLED = RELATIONSHIP_TYPE + ".enabled";

    @SuppressWarnings ("unused")
    @Activate
    private void activate(ComponentContext context) throws IOException {
        enabled = PropertiesUtil.toBoolean(context.getProperties().get(ENABLED), true);
    }

    @SuppressWarnings ("unused")
    @Deactivate
    private void deactivate() throws IOException {
    }

    @Override
    public Map<String, String> getRelationshipTypes() {
        Map<String, String> types = new HashMap<String, String>(0);
        types.put(RELATIONSHIP_TYPE, RELATIONSHIP_TITLE);
        return types;
    }

    /**
     * @return a list of products whose tags match the tags of products already in the cart
     */
    @Override
    public List<ProductRelationship> getRelationships(SlingHttpServletRequest request, CommerceSession session, Page currentPage,
                                                      Product currentProduct) throws CommerceException {
        if (!enabled) {
            return null;
        }

        //
        // Don't provide relationships to non-we-retail pages:
        //
        if (currentPage != null) {
            InheritanceValueMap properties = new HierarchyNodeInheritanceValueMap(currentPage.getContentResource());
            String commerceProvider = properties.getInherited(CommerceConstants.PN_COMMERCE_PROVIDER, String.class);
            if (commerceProvider != null && !commerceProvider.equals("we-retail")) {
                return null;
            }
        }

        if (currentProduct == null) {
            return null;
        }

        //
        // Add current product to context:
        //
        List<Product> contextProducts = new ArrayList<Product>();
        contextProducts.add(currentProduct);

        //
        // Walk content-pages to find similar products:
        //
        ResourceResolver resolver = request.getResourceResolver();
        SimilarProductsCollector collector = new SimilarProductsCollector(resolver, session, RELATIONSHIP_TYPE, RELATIONSHIP_TITLE,
                contextProducts);
        collector.walk(resolver.getResource("/content/we-retail/language-masters/en/products"));
        return collector.getRelationships();
    }
}

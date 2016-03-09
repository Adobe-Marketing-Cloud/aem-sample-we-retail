package we.retail.core;

import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceServiceFactory;
import com.adobe.cq.commerce.common.AbstractJcrCommerceServiceFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;

/**
 * we.retail implementation for the {@link CommerceServiceFactory} interface.
 */
@Component
@Service
@Properties(value = {
    @Property(name = "service.description", value = "Factory for reference implementation commerce service"),
    @Property(name = "commerceProvider", value = "we-retail", propertyPrivate = true)
})
public class WeRetailCommerceServiceFactory  extends AbstractJcrCommerceServiceFactory implements CommerceServiceFactory {
    /**
     * Create a new <code>GeoCommerceServiceImpl</code>.
     */
    public CommerceService getCommerceService(Resource res) {
        return new WeRetailCommerceServiceImpl(getServiceContext(), res);
    }
}

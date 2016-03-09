package we.retail.core;

import com.adobe.cq.commerce.common.AbstractJcrProduct;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import we.retail.core.util.WeRetailHelper;

/**
 * Created by Daniel on 09/03/16.
 */
public class WeRetailProductImpl extends AbstractJcrProduct {
    public static final String PN_IDENTIFIER = "identifier";
    public static final String PN_PRICE = "price";

    protected final ResourceResolver resourceResolver;
    protected final PageManager pageManager;
    protected final Page productPage;
    protected String brand = null;

    public WeRetailProductImpl(Resource resource) {
        super(resource);

        resourceResolver = resource.getResourceResolver();
        pageManager = resourceResolver.adaptTo(PageManager.class);
        productPage = pageManager.getContainingPage(resource);
    }

    public String getSKU() {
        String sku = getProperty(PN_IDENTIFIER, String.class);
        // Geometrixx products don't have unique ids for size, so append the size to the sku:
        String size = getProperty("size", String.class);
        if (size != null && size.length() > 0) {
            sku += "-" + size;
        }
        return sku;
    }

    @Override
    public <T> T getProperty(String name, Class<T> type) {
        if (name.equals("brand")) {
            return (T) getBrand();
        }

        return super.getProperty(name, type);
    }

    @Override
    public <T> T getProperty(String name, String selectorString, Class<T> type) {
        if (name.equals("brand")) {
            return (T) getBrand();
        }

        return super.getProperty(name, selectorString, type);
    }

    public String getBrand() {
        // A null value is considered as non-initialized
        if (brand == null) {
            // Get value from root page title
            if (productPage != null)
                brand = WeRetailHelper.getPageTitle(productPage.getAbsoluteParent(2));
            // Make sure that the value is not null, to avoid initializing it again
            if (WeRetailHelper.isEmpty(brand))
                brand = "";
        }
        return brand;
    }
}

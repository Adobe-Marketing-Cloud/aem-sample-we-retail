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
package we.retail.core;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceServiceFactory;
import com.adobe.cq.commerce.common.AbstractJcrCommerceServiceFactory;
import org.osgi.framework.Constants;

/**
 * we.retail implementation for the {@link CommerceServiceFactory} interface.
 */
@Component
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Factory for reference implementation commerce service"),
    @Property(name = "commerceProvider", value = WeRetailConstants.WE_RETAIL_COMMERCEPROVIDER, propertyPrivate = true)
})
public class WeRetailCommerceServiceFactory  extends AbstractJcrCommerceServiceFactory implements CommerceServiceFactory {
    /**
     * Create a new <code>GeoCommerceServiceImpl</code>.
     */
    public CommerceService getCommerceService(Resource res) {
        return new WeRetailCommerceServiceImpl(getServiceContext(), res);
    }
}

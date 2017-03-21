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
package apps.weretail.components.structure.productfilter;

import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.wcm.api.designer.Style;

public class ProductFilter extends WCMUsePojo {

    private static final String PROP_TYPE = "type";
    private static final String PROP_TYPE_DEFAULT = "color";
    private String type;
    private ValueMap properties;
    private Style style;

    @Override
    public void activate() throws Exception {
        properties = getProperties();
        style = getCurrentStyle();
        readConfiguration();
    }

    private void readConfiguration() {
        type = properties.get(PROP_TYPE, style.get(PROP_TYPE, PROP_TYPE_DEFAULT));
    }

    public String getType() {
        return type;
    }
}

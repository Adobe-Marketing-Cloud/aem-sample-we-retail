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
package apps.weretail.components.content.carousel;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.commons.jcr.JcrConstants;

public class Carousel extends WCMUsePojo {

    private static final String TYPE_DEFAULT = "default";
    private static final String PN_TYPE = "displayAs";
    private String id;
    private String type;
    private Resource resource;

    @Override
    public void activate() throws Exception {
        this.resource = getResource();
        this.id = getGeneratedId();
        ValueMap properties = getProperties();
        this.type = properties.get(PN_TYPE, TYPE_DEFAULT);
    }

    private String getGeneratedId() {
        String path = resource.getPath();
        String inJcrContent = JcrConstants.JCR_CONTENT + "/";
        int root = path.indexOf(inJcrContent);
        if (root >= 0) {
            path = path.substring(root + inJcrContent.length());
        }
        return path.replace("/", "_");
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}

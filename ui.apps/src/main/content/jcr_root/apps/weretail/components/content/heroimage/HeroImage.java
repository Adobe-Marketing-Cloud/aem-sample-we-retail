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
package apps.weretail.components.content.heroimage;

import java.lang.String;
import java.util.Calendar;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.sightly.WCMUsePojo;

public class HeroImage extends WCMUsePojo {

    public static final Logger log = LoggerFactory.getLogger(HeroImage.class);

    public static final String PROP_FULL_WIDTH = "useFullWidth";
    public static final String PROP_KEEP_RATIO = "keepRatio";

    private String classList;
    private Image image;

    @Override
    public void activate() throws Exception {
        classList = getClassList();
        image = getImage();
    }

    public String getClassList() {
        if (classList != null) {
            return classList;
        }
        ValueMap properties = getProperties();
        classList = "we-HeroImage";
        if ("true".equals(properties.get(PROP_FULL_WIDTH, ""))) {
            classList += " width-full";
        }
        if ("true".equals(properties.get(PROP_KEEP_RATIO, ""))) {
            classList += " ratio-16by9";
        }
        return classList;
    }

    public Image getImage() {
        if (image != null) {
            return image;
        }
        String escapedResourcePath = Text.escapePath(getResource().getPath());
        long lastModifiedDate = getLastModifiedDate(getProperties());
        String src = getRequest().getContextPath() + escapedResourcePath + ".img.jpeg" +
                (!getWcmMode().isDisabled() && lastModifiedDate > 0 ? "/" + lastModifiedDate + ".jpeg" : "");
        image = new Image(src);
        return image;
    }

    public class Image {
        private String src;

        public Image(String src) {
            this.src = src;
        }

        public String getSrc() {
            return src;
        }
    }

    private long getLastModifiedDate(ValueMap properties) {
        long lastMod = 0L;
        if (properties.containsKey(JcrConstants.JCR_LASTMODIFIED)) {
            lastMod = properties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class).getTimeInMillis();
        } else if (properties.containsKey(JcrConstants.JCR_CREATED)) {
            lastMod = properties.get(JcrConstants.JCR_CREATED, Calendar.class).getTimeInMillis();
        }
        return lastMod;
    }

}

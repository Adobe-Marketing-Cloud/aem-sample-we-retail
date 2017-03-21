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

import com.adobe.cq.sightly.SightlyWCMMode;
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

    private Resource resource;
    private String classList;
    private Image image;
    private SightlyWCMMode wcmMode;

    @Override
    public void activate() throws Exception {
        resource = getResource();
        wcmMode = getWcmMode();
        classList = getClassList();
        image = getImage();
        log.debug("resource: {}", resource.getPath());
        log.debug("classList: {}", classList);
        log.debug("image.src: {}", image.getSrc());
        log.debug("wcm mode: {}", wcmMode.toString());
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
        String escapedResourcePath = Text.escapePath(resource.getPath());
        String src = getRequest().getContextPath() + escapedResourcePath + ".img.jpeg";
        // cache killer for edit mode to refresh image after drag-and-drop
        if(wcmMode.isEdit()) {
            src = src + "?" + System.currentTimeMillis();
        }
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

}
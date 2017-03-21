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
package apps.weretail.components.content.button;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.sightly.WCMUsePojo;

public class Button extends WCMUsePojo {

    public static final Logger log = LoggerFactory.getLogger(Button.class);

    public static final String PROP_LINK_TO = "linkTo";
    public static final String CSS_CLASS = "cssClass";

    private String linkTo;
    private String cssClass;

    @Override
    public void activate() throws Exception {
        Resource resource = getResource();
        ValueMap properties = getProperties();
        linkTo = properties.get(PROP_LINK_TO, "#");
        cssClass = properties.get(CSS_CLASS, "");
        if (StringUtils.isNotEmpty(linkTo) && !"#".equals(linkTo)) {
            linkTo = linkTo + ".html";
        }
        log.debug("resource: {}", resource.getPath());
        log.debug("linkTo: {}", linkTo);
    }

    public String getLinkTo() {
        return linkTo;
    }

    public String getCssClass() {
        return cssClass;
    }
}
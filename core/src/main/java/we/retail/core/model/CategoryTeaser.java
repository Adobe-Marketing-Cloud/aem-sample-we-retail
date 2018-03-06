/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package we.retail.core.model;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class)
public class CategoryTeaser {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryTeaser.class);

    private static final String PN_BUTTON_LINK_TO = "buttonLinkTo";
    private static final String PN_BUTTON_LABEL = "buttonLabel";

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private ValueMap properties;

    private String buttonLinkTo;
    private String buttonLabel;

    @PostConstruct
    private void initModel() {
        buttonLinkTo = properties.get(PN_BUTTON_LINK_TO, "");
        buttonLabel = properties.get(PN_BUTTON_LABEL, "");
        if (StringUtils.isNotEmpty(buttonLinkTo)) {
            // if button label is not set, try to get it from target page's title
            if (StringUtils.isEmpty(buttonLabel)) {
                Resource linkResource = resourceResolver.getResource(buttonLinkTo);
                if (linkResource != null) {
                    Page targetPage = linkResource.adaptTo(Page.class);
                    if (targetPage != null) {
                        buttonLabel = targetPage.getTitle();
                    }
                }
            }
        }
        LOGGER.debug("resource: {}", resource.getPath());
        LOGGER.debug("buttonLinkTo: {}", buttonLinkTo);
        LOGGER.debug("buttonLabel: {}", buttonLabel);
    }

    public String getButtonLinkTo() {
        return buttonLinkTo;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }
}

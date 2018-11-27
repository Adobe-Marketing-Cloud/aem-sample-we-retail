/*******************************************************************************
 * Copyright 2016 Adobe Systems Incorporated
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package we.retail.core.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class UrlHelper {

    private static final Logger log = LoggerFactory.getLogger(UrlHelper.class);
    private static final String RT_REDIRECT = "weretail/components/structure/page";
    private static final String PN_REDIRECT_TARGET = "cq:redirectTarget";

    /**
     * Check if the current page is a redirect page
     * @param page to check
     * @return true if current page is of type redirect, otherwise false
     */
    public static boolean isRedirectPage(Page page) {
        boolean isRedirect = false;
        Resource contentResource = page.getContentResource();
        if (contentResource != null) {
            isRedirect = contentResource.isResourceType(RT_REDIRECT);
        } else {
            log.error("Can't get content resource of page {}", page.getPath());
        }
        return isRedirect;
    }

    /**
     * Resolve the page to the redirect page
     * 
     * @param page
     *            to resolve
     * @param pageManager
     *            the page manager
     * @return the redirect target or the given page
     */
    public static Page resolveRedirectPage(Page page, PageManager pageManager) {
        Page redirectTarget = page;
        if (isRedirectPage(page)) {
            Resource contentResource = page.getContentResource();
            ValueMap valueMap = contentResource.getValueMap();
            String redirectPagePath = valueMap.get(PN_REDIRECT_TARGET, StringUtils.EMPTY);
            Page resolvedPage = pageManager.getPage(redirectPagePath);
            if (resolvedPage != null) {
                redirectTarget = resolvedPage;
            }
        }
        return redirectTarget;
    }
}

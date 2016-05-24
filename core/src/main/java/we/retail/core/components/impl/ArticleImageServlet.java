/*
 *   Copyright 2016 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package we.retail.core.components.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.RenditionPicker;
import com.day.cq.wcm.foundation.WCMRenditionPicker;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;

@SlingServlet(resourceTypes = "cq:Page", selectors = "article-image", extensions = "jpeg")
public class ArticleImageServlet extends SlingSafeMethodsServlet {

    private static final RenditionPicker RENDITION_PICKER = new WCMRenditionPicker();

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        Resource heroImageResource = request.getResource().getChild(JcrConstants.JCR_CONTENT + "/root/hero_image");
        if (heroImageResource != null) {
            String heroFileReference = heroImageResource.getValueMap().get("fileReference", String.class);

            if (heroFileReference != null) {
                Resource heroResource = request.getResourceResolver().getResource(heroFileReference);
                if (heroResource != null) {
                    Asset asset = heroResource.adaptTo(Asset.class);
                    if (asset != null) {
                        Rendition rendition = RENDITION_PICKER.getRendition(asset);
                        if (rendition != null) {
                            response.setContentType(rendition.getMimeType());
                            IOUtils.copy(rendition.getStream(), response.getOutputStream());
                            return;
                        }
                    }
                }
            }
        }

        // if we get to this point, there either is no hero image or the hero image can't be found,
        // so we redirect to the page thumbnail
        response.sendRedirect(request.getResource().getPath() + ".thumb.319.319.png");
    }
}

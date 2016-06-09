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

import com.day.cq.commons.ImageHelper;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.RenditionPicker;
import com.day.cq.wcm.commons.AbstractImageServlet;
import com.day.cq.wcm.foundation.WCMRenditionPicker;
import com.day.image.Layer;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;
import java.awt.Dimension;
import java.io.IOException;

@SlingServlet(resourceTypes = "cq:Page", selectors = "article-image", extensions = "jpeg")
public class ArticleImageServlet extends AbstractImageServlet  {

    private static final RenditionPicker RENDITION_PICKER = new WCMRenditionPicker();
    private static final int MAX_HEIGHT = 768;
    private static final int MAX_WIDTH = 768;
    private static final double QUALITY = 0.75d;

    @Override
    protected Layer createLayer(ImageContext imageContext) throws RepositoryException, IOException {
        Resource heroImageResource = imageContext.resource.getChild(JcrConstants.JCR_CONTENT + "/root/hero_image");
        if (heroImageResource != null) {
            String heroFileReference = heroImageResource.getValueMap().get("fileReference", String.class);

            if (heroFileReference != null) {
                Resource heroResource = imageContext.resolver.getResource(heroFileReference);
                if (heroResource != null) {
                    Asset asset = heroResource.adaptTo(Asset.class);
                    if (asset != null) {
                        Rendition rendition = RENDITION_PICKER.getRendition(asset);
                        if (rendition != null) {
                            Layer layer = new Layer(rendition.getStream());
                            if (layer.getHeight() > MAX_HEIGHT || layer.getWidth() > MAX_WIDTH) {
                                Layer resized = ImageHelper.resize(layer, new Dimension(), new Dimension(0, 0), new Dimension(MAX_WIDTH, MAX_HEIGHT));
                                if (resized != null) {
                                    layer = resized;
                                }
                            }
                            return layer;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected double getImageQuality() {
        return QUALITY;
    }

    @Override
    protected void writeLayer(SlingHttpServletRequest request, SlingHttpServletResponse response, ImageContext context, Layer layer, double quality) throws IOException, RepositoryException {
        if (layer == null) {
            // if we get to this point, there either is no hero image or the hero image can't be found,
            // so we redirect to the page thumbnail
            response.sendRedirect(request.getResource().getPath() + ".thumb.319.319.png");
        } else {
            super.writeLayer(request, response, context, layer, quality);
        }
    }
}

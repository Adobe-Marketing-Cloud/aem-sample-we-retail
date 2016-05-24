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

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.RenditionPicker;
import com.day.cq.wcm.commons.AbstractImageServlet;
import com.day.cq.wcm.foundation.WCMRenditionPicker;
import com.day.image.Layer;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;
import java.io.IOException;

@SlingServlet(resourceTypes = { "we-retail/components/content/category-teaser" }, selectors = "img", extensions = { "jpeg", "jpg", "png" })
public class CategoryTeaserImageServlet extends AbstractImageServlet {

    private static final RenditionPicker RENDITION_PICKER = new WCMRenditionPicker();

    @Override
    protected Layer createLayer(ImageContext imageContext) throws RepositoryException, IOException {
        String imageReference = imageContext.properties.get("fileReference", String.class);
        if (imageReference == null) {
            return null;
        }
        Resource image = imageContext.resolver.getResource(imageReference);
        if (image == null) {
            return null;
        }
        Asset asset = image.adaptTo(Asset.class);
        if (asset == null) {
            return null;
        }
        Rendition rendition = RENDITION_PICKER.getRendition(asset);
        return new Layer(rendition.getStream());

    }
}

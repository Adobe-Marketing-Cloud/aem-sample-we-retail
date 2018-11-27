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
package common.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.VariantFilter;
import com.day.cq.commons.ImageResource;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class MockProduct extends ResourceWrapper implements Product {

    public static final String PRODUCT_DATA = "productData";
    private final Resource resource;

    public MockProduct(@Nonnull Resource resource) {
        super(resource);
        this.resource = resource;
    }

    @Override
    public boolean axisIsVariant(String axis) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPagePath() {
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(resource);
        return (page != null ? page.getPath() : null);
    }

    @Override
    public String getSKU() {
        String sku = getProperty("identifier", String.class);
        String size = getProperty("size", String.class);
        if (size != null && size.length() > 0) {
            sku += "-" + size;
        }
        return sku;
    }

    @Override
    public String getTitle() {
        return getValueMap().get("jcr:title", String.class);
    }

    @Override
    public String getTitle(String selectorString) {
        return getTitle();
    }

    @Override
    public String getDescription() {
        return getValueMap().get("jcr:description", String.class);
    }

    @Override
    public String getDescription(String selectorString) {
        return getDescription();
    }

    @Override
    public String getThumbnailUrl() {
        return null;
    }

    @Override
    public String getThumbnailUrl(int width) {
        return null;
    }

    @Override
    public String getThumbnailUrl(String selectorString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getAsset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Resource> getAssets() {
        return null;
    }

    @Override
    public ImageResource getImage() {
        Resource imageResource = resource.getResourceResolver().getResource(getValueMap().get("productData", String.class)).getChild("image");
        if (imageResource == null) {
            return null;
        }
        return new ImageResource(imageResource);
    }

    @Override
    public List<ImageResource> getImages() {
        ImageResource imageResource = getImage();
        if (imageResource == null) {
            return null;
        }
        return Collections.singletonList(imageResource);
    }

    @Override
    public <T> T getProperty(String name, Class<T> type) {
        if(getValueMap().containsKey(name)) {
            return getValueMap().get(name, type);
        } else if(getValueMap().containsKey(PRODUCT_DATA)){
            Resource productResource = this.resource.getResourceResolver().getResource(getValueMap().get(PRODUCT_DATA, String.class));
            if(productResource != null && productResource.getValueMap().containsKey(name)) {
                return productResource.getValueMap().get(name, type);
            }
        }
        return null;
    }

    @Override
    public <T> T getProperty(String name, String selectorString, Class<T> type) {
        return getProperty(name, type);
    }

    @Override
    public Iterator<String> getVariantAxes() {
        return null;
    }

    @Override
    public Iterator<Product> getVariants() throws CommerceException {
        final List<Product> variants = new ArrayList<Product>();
        for (Resource child : resource.getChildren()) {
            if (StringUtils.equals(child.getValueMap().get("cq:commerceType", String.class), "variant")) {
                variants.add(new MockProduct(child));
            }
        }
        return variants.iterator();
    }

    @Override
    public Iterator<Product> getVariants(VariantFilter filter) throws CommerceException {
        return null;
    }

    @Override
    public Product getBaseProduct() throws CommerceException {
        if (StringUtils.equals(getValueMap().get("cq:commerceType", String.class), "variant")) {
            return new MockProduct(resource.getParent());
        }
        return null;
    }

    @Override
    public Product getPIMProduct() throws CommerceException {
        return new MockProduct(resource.getResourceResolver().getResource(getValueMap().get(PRODUCT_DATA, String.class)));
    }

    @Override
    public String getImageUrl() {
        return null;
    }

    @Override
    public String getImagePath() {
        return null;
    }

    @Override
    public ImageResource getThumbnail() {
        return null;
    }
}

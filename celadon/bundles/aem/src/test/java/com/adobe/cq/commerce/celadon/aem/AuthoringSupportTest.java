/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.celadon.aem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthoringSupportTest {

    @Test
    public void derivesExtensionFromMimeType() {
        assertEquals("jpeg", AuthoringSupport.extensionForMimeType("image/jpeg", "http://h/a"));
        assertEquals("png", AuthoringSupport.extensionForMimeType("image/png", "http://h/a"));
        assertEquals("webp", AuthoringSupport.extensionForMimeType("image/webp", "http://h/a"));
        assertEquals("gif", AuthoringSupport.extensionForMimeType("image/gif", "http://h/a"));
    }

    @Test
    public void fallsBackToUrlExtensionWhenMimeMissing() {
        assertEquals("gif", AuthoringSupport.extensionForMimeType(null, "http://h/path/a.gif"));
        assertEquals("bin", AuthoringSupport.extensionForMimeType(null, "http://h/path/noext"));
    }

    @Test
    public void coLocatesAssetWithProductFolder() {
        assertEquals("/content/dam/celadon/geotest/equipment/winter/eqwntb_img.jpeg",
                AuthoringSupport.assetPath("/content/dam/celadon/geotest/equipment/winter", "eqwntb", "jpeg"));
    }

    @Test
    public void sanitizesSkuInAssetName() {
        assertEquals("/content/dam/celadon/geotest/eq/men-shirts-sku1_img.png",
                AuthoringSupport.assetPath("/content/dam/celadon/geotest/eq", "men/shirts sku1", "png"));
    }

    @Test
    public void recognizesLocalDamPaths() {
        assertTrue(AuthoringSupport.isLocalDamPath("/content/dam/geometrixx-outdoors/x.jpg"));
        assertFalse(AuthoringSupport.isLocalDamPath("http://localhost:4504/content/dam/x.jpg"));
        assertFalse(AuthoringSupport.isLocalDamPath("https://example.com/x.jpg"));
        assertFalse(AuthoringSupport.isLocalDamPath(null));
    }

    @Test
    public void generatesVariantSkuWhenAbsent() {
        assertEquals("eqwntb-sz-9", AuthoringSupport.variantSku("eqwntb", null, "sz-9"));
        assertEquals("eqwntb-sz-9", AuthoringSupport.variantSku("eqwntb", "  ", "sz-9"));
    }

    @Test
    public void keepsProvidedVariantSku() {
        assertEquals("CUSTOM-1", AuthoringSupport.variantSku("eqwntb", "CUSTOM-1", "sz-9"));
    }

    @Test
    public void encodesSpacesInSourceUrl() {
        assertEquals("http://localhost:4504/content/dam/x/Jola%20Blue.jpg",
                AuthoringSupport.encodedUri("http://localhost:4504/content/dam/x/Jola Blue.jpg").toString());
    }

    @Test
    public void leavesAlreadyValidUrlUnchanged() {
        assertEquals("http://h/a/Tobermory.jpg",
                AuthoringSupport.encodedUri("http://h/a/Tobermory.jpg").toString());
    }
}

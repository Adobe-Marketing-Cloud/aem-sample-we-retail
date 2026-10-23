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
package com.adobe.cq.commerce.celadon.aem.attribute.manifest;

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

/**
 * Reads and writes the {@code celadon:ready} flag on the catalog root's
 * {@code jcr:content} node. The flag indicates whether the catalog has a
 * usable manifest and product model.
 */
public final class ReadyFlag {

    public static final String PROPERTY = "celadonReady";

    private ReadyFlag() {}

    public static boolean isReady(ResourceResolver resolver, String catalog) {
        Resource content = resolver.getResource("/content/dam/celadon/" + catalog + "/jcr:content");
        if (content == null) {
            return false;
        }
        return content.getValueMap().get(PROPERTY, false);
    }

    public static void set(ResourceResolver resolver, String catalog, boolean ready) throws PersistenceException {
        String catalogPath = "/content/dam/celadon/" + catalog;
        String contentPath = catalogPath + "/jcr:content";
        Resource content = resolver.getResource(contentPath);
        if (content == null) {
            Resource catalogRoot = resolver.getResource(catalogPath);
            if (catalogRoot == null) {
                catalogRoot = ResourceUtil.getOrCreateResource(
                        resolver, catalogPath, "sling:Folder", "sling:Folder", false);
            }
            Map<String, Object> props = new HashMap<>();
            props.put("jcr:primaryType", "nt:unstructured");
            content = resolver.create(catalogRoot, "jcr:content", props);
        }
        ModifiableValueMap mv = content.adaptTo(ModifiableValueMap.class);
        if (mv == null) {
            throw new PersistenceException("Cannot adapt " + contentPath + " to ModifiableValueMap");
        }
        mv.put(PROPERTY, ready);
        resolver.commit();
    }
}

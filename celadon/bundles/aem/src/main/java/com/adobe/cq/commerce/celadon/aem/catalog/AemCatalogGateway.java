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
package com.adobe.cq.commerce.celadon.aem.catalog;

import com.adobe.cq.commerce.celadon.core.api.CatalogGateway;
import com.adobe.cq.commerce.celadon.core.api.FetcherContext;
import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.FragmentData;
import com.adobe.cq.dam.cfm.VariationDef;
import com.day.cq.dam.api.Asset;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

public final class AemCatalogGateway implements CatalogGateway {
    private final ResourceResolver resourceResolver;
    private final FetcherContext context;
    private final boolean closeResourceResolver;

    public AemCatalogGateway(ResourceResolver resourceResolver, FetcherContext context) {
        this(resourceResolver, context, false);
    }

    public AemCatalogGateway(ResourceResolver resourceResolver, FetcherContext context, boolean closeResourceResolver) {
        this.resourceResolver = Objects.requireNonNull(resourceResolver, "resourceResolver");
        this.context = Objects.requireNonNull(context, "context");
        this.closeResourceResolver = closeResourceResolver;
    }

    @Override
    public Map<String, Object> getListing(String relativePath) {
        Resource folder = resourceResolver.getResource(folderPath(relativePath));
        if (folder == null) {
            return Map.of("entities", List.of());
        }
        List<Map<String, Object>> entities = new ArrayList<>();
        for (Resource child : folder.getChildren()) {
            if ("jcr:content".equals(child.getName())) {
                continue;
            }
            Asset asset = child.adaptTo(Asset.class);
            if (asset != null) {
                entities.add(assetEntity(child, asset));
                continue;
            }
            if (isFolder(child)) {
                entities.add(folderEntity(child));
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("properties", Map.of("name", folder.getName()));
        response.put("entities", entities);
        return response;
    }

    @Override
    public Map<String, Object> getFolderJcrContent(String categoryPath) {
        Resource jcrContent = resourceResolver.getResource(folderPath(categoryPath) + "/jcr:content");
        if (jcrContent == null) {
            return Map.of();
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        String title = jcrContent.getValueMap().get("jcr:title", "");
        if (title != null && !title.isBlank()) {
            properties.put("jcr:title", title);
        }
        String thumbnailPath = resolveFolderThumbnailPath(jcrContent);
        if (!thumbnailPath.isBlank()) {
            properties.put("folderThumbnailPath", thumbnailPath);
        }
        return properties;
    }

    private String resolveFolderThumbnailPath(Resource jcrContent) {
        for (Resource child : jcrContent.getChildren()) {
            String name = child.getName();
            if (name.startsWith("manualThumbnail.")) {
                return child.getPath();
            }
        }
        Resource folderThumbnail = jcrContent.getChild("folderThumbnail");
        return folderThumbnail == null ? "" : folderThumbnail.getPath();
    }

    @Override
    public Map<String, Object> getProductJcrContent(String categoryPath, String productName) {
        Resource jcrContent = resourceResolver.getResource(productPath(categoryPath, productName) + "/jcr:content");
        if (jcrContent == null) {
            return Map.of();
        }
        List<Map<String, Object>> optionDefinitions = resolveOptionDefinitions(jcrContent);
        if (optionDefinitions.isEmpty()) {
            return Map.of();
        }
        return Map.of("configurableOptionDefinitions", optionDefinitions);
    }

    private List<Map<String, Object>> resolveOptionDefinitions(Resource jcrContent) {
        Resource master = jcrContent.getChild("data/master");
        if (master == null) {
            return List.of();
        }
        List<String> referencedPaths = collectFragmentReferences(master.getValueMap().get("configurableOptions"));
        if (referencedPaths.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (String path : referencedPaths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            Resource definitionResource = resourceResolver.getResource(path);
            if (definitionResource == null) {
                continue;
            }
            Map<String, Object> definition = readOptionDefinition(definitionResource);
            if (!definition.isEmpty()) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    private List<String> collectFragmentReferences(Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        if (value instanceof String string) {
            if (string.startsWith("/content/")) {
                paths.add(string);
            }
        } else if (value instanceof String[] array) {
            for (String entry : array) {
                if (entry != null && entry.startsWith("/content/")) {
                    paths.add(entry);
                }
            }
        } else if (value instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry != null) {
                    String text = entry.toString();
                    if (text.startsWith("/content/")) {
                        paths.add(text);
                    }
                }
            }
        }
        return paths;
    }

    private Map<String, Object> readOptionDefinition(Resource definitionResource) {
        ContentFragment fragment = definitionResource.adaptTo(ContentFragment.class);
        if (fragment == null) {
            return Map.of();
        }
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("label", elementValue(fragment, "label"));
        definition.put("attributeCode", elementValue(fragment, "attributeCode"));
        definition.put("productField", elementValue(fragment, "productField"));
        definition.put("swatchType", elementValue(fragment, "swatchType"));
        definition.put("values", elementMultiValue(fragment, "values"));
        return definition;
    }

    private String elementValue(ContentFragment fragment, String elementName) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) {
            return "";
        }
        FragmentData data = element.getValue();
        if (data == null) {
            return "";
        }
        Object value = data.getValue();
        if (value == null) {
            return "";
        }
        if (value instanceof Object[] array && array.length > 0) {
            Object first = array[0];
            return first == null ? "" : first.toString();
        }
        return value.toString();
    }

    private List<String> elementMultiValue(ContentFragment fragment, String elementName) {
        ContentElement element = fragment.getElement(elementName);
        if (element == null) {
            return List.of();
        }
        FragmentData data = element.getValue();
        if (data == null) {
            return List.of();
        }
        Object value = data.getValue();
        if (value == null) {
            return List.of();
        }
        if (value instanceof Object[] array) {
            List<String> result = new ArrayList<>(array.length);
            for (Object entry : array) {
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<String> result = new ArrayList<>(collection.size());
            for (Object entry : collection) {
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        return List.of(value.toString());
    }

    @Override
    public boolean isCatalogReady() {
        String contentPath = "/content/dam/celadon/" + FetcherContext.normalizeBasePath(context.basePath()) + "/jcr:content";
        Resource content = resourceResolver.getResource(contentPath);
        if (content == null) {
            return false;
        }
        return content.getValueMap().get("celadonReady", false);
    }

    @Override
    public String toAssetUrl(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        // Host-less, root-relative path so image URLs resolve against whatever
        // host/port serves the storefront (author, publish, custom port).
        return imagePath.startsWith("/") ? imagePath : "/" + imagePath;
    }

    @Override
    public void close() {
        if (closeResourceResolver) {
            resourceResolver.close();
        }
    }

    private Map<String, Object> folderEntity(Resource folder) {
        return entity("assets/folder", Map.of("name", folder.getName()));
    }

    private Map<String, Object> assetEntity(Resource resource, Asset asset) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", asset.getName());
        properties.put("path", asset.getPath());

        ContentFragment fragment = resource.adaptTo(ContentFragment.class);
        if (fragment != null) {
            properties.put("contentFragment", true);
            properties.put("elements", serializeElements(fragment));
        }

        ValueMap metadata = resource.getChild("jcr:content") == null
                ? ValueMap.EMPTY
                : resource.getChild("jcr:content").getValueMap();
        Object lastModified = metadata.get("jcr:lastModified");
        if (lastModified != null) {
            properties.put("lastModified", serializeValue(lastModified));
        }

        return entity("assets/asset", properties);
    }

    private Map<String, Object> entity(String className, Map<String, Object> properties) {
        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("class", List.of(className));
        entity.put("properties", properties);
        return entity;
    }

    private Map<String, Object> serializeElements(ContentFragment fragment) {
        List<String> variationOrder = listVariationOrder(fragment);
        Map<String, Object> elements = new LinkedHashMap<>();
        Iterator<ContentElement> iterator = fragment.getElements();
        while (iterator.hasNext()) {
            ContentElement element = iterator.next();
            Map<String, Object> elementPayload = new LinkedHashMap<>();
            elementPayload.put("value", valueOf(element.getValue()));
            Map<String, Object> variations = serializeVariations(element);
            if (!variations.isEmpty()) {
                elementPayload.put("variations", variations);
                if ("name".equals(element.getName()) && !variationOrder.isEmpty()) {
                    elementPayload.put("variationsOrder", variationOrder);
                }
            }
            elements.put(element.getName(), elementPayload);
        }
        return elements;
    }

    private Map<String, Object> serializeVariations(ContentElement element) {
        Map<String, Object> variations = new LinkedHashMap<>();
        Iterator<ContentVariation> iterator = element.getVariations();
        while (iterator.hasNext()) {
            ContentVariation variation = iterator.next();
            variations.put(variation.getName(), Map.of("value", valueOf(variation.getValue())));
        }
        return variations;
    }

    private List<String> listVariationOrder(ContentFragment fragment) {
        List<String> variationOrder = new ArrayList<>();
        Iterator<VariationDef> iterator = fragment.listAllVariations();
        while (iterator.hasNext()) {
            VariationDef variation = iterator.next();
            if (variation.getName() != null && !variation.getName().isBlank()) {
                variationOrder.add(variation.getName());
            }
        }
        return variationOrder;
    }

    private Object valueOf(FragmentData fragmentData) {
        if (fragmentData == null) {
            return "";
        }
        return serializeValue(fragmentData.getValue());
    }

    private Object serializeValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof java.util.Calendar calendar) {
            return calendar.toInstant().toString();
        }
        if (value instanceof Collection<?> collection) {
            List<Object> converted = new ArrayList<>();
            for (Object entry : collection) {
                converted.add(serializeValue(entry));
            }
            return converted;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> converted = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                converted.add(serializeValue(Array.get(value, index)));
            }
            return converted;
        }
        return value.toString();
    }

    private boolean isFolder(Resource resource) {
        String primaryType = resource.getValueMap().get("jcr:primaryType", "");
        return "sling:Folder".equals(primaryType) || "sling:OrderedFolder".equals(primaryType);
    }

    private String folderPath(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        StringBuilder path = new StringBuilder("/content/dam/celadon/")
                .append(FetcherContext.normalizeBasePath(context.basePath()));
        if (!normalized.isBlank()) {
            path.append('/').append(normalized);
        }
        return path.toString();
    }

    private String productPath(String categoryPath, String productName) {
        String normalizedCategory = normalizeRelativePath(categoryPath);
        String normalizedProduct = normalizeRelativePath(productName);
        if (normalizedProduct.isBlank()) {
            return folderPath(normalizedCategory);
        }
        StringBuilder path = new StringBuilder(folderPath(normalizedCategory));
        if (path.charAt(path.length() - 1) != '/') {
            path.append('/');
        }
        path.append(normalizedProduct);
        return path.toString();
    }

    private String normalizeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

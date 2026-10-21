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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public final class AemContentFragmentSupport {
    private static final String CONTENT_FRAGMENT_CLASS = "com.adobe.cq.dam.cfm.ContentFragment";
    private static final String FRAGMENT_TEMPLATE_CLASS = "com.adobe.cq.dam.cfm.FragmentTemplate";
    private static final String CONTENT_ELEMENT_CLASS = "com.adobe.cq.dam.cfm.ContentElement";
    private static final String CONTENT_VARIATION_CLASS = "com.adobe.cq.dam.cfm.ContentVariation";
    private static final String FRAGMENT_DATA_CLASS = "com.adobe.cq.dam.cfm.FragmentData";
    private static final String VARIATION_TEMPLATE_CLASS = "com.adobe.cq.dam.cfm.VariationTemplate";
    private static final String ASSET_MANAGER_CLASS = "com.day.cq.dam.api.AssetManager";

    private AemContentFragmentSupport() {
    }

    public static Resource ensureFragment(ResourceResolver resolver,
                                   Resource modelResource,
                                   Resource parent,
                                   String nodeName,
                                   String title) throws Exception {
        Resource existing = parent.getChild(nodeName);
        if (existing != null && adapt(existing, CONTENT_FRAGMENT_CLASS) == null) {
            resolver.delete(existing);
            resolver.commit();
            existing = null;
        }

        if (existing == null) {
            Object template = adapt(modelResource, FRAGMENT_TEMPLATE_CLASS);
            if (template == null) {
                throw new IllegalStateException("Product model does not adapt to FragmentTemplate");
            }
            invoke(template, "createFragment", parent, nodeName, title);
            resolver.commit();
            existing = parent.getChild(nodeName);
        }

        if (existing == null || adapt(existing, CONTENT_FRAGMENT_CLASS) == null) {
            throw new IllegalStateException("Failed to create content fragment at " + parent.getPath() + "/" + nodeName);
        }
        setFragmentTitle(existing, title);
        return existing;
    }

    static void setFragmentTitle(Resource fragmentResource, String title) throws PersistenceException {
        Resource jcrContent = fragmentResource.getChild("jcr:content");
        if (jcrContent != null) {
            ModifiableValueMap values = jcrContent.adaptTo(ModifiableValueMap.class);
            if (values != null) {
                values.put("jcr:title", title);
            }
        }
    }

    public static void writeText(Resource fragmentResource, String elementName, String value, String mimeType) throws Exception {
        Object fragment = requireFragment(fragmentResource);
        Object element = invoke(fragment, "getElement", elementName);
        if (element == null) {
            // The CF model has no field for this attribute (e.g. an attribute the
            // model generator skipped). Skip rather than fail the whole import.
            return;
        }
        invoke(element, "setContent", value == null ? "" : value, mimeType);
    }

    public static void writeTyped(Resource fragmentResource, String elementName, Object value) throws Exception {
        Object fragment = requireFragment(fragmentResource);
        Object element = invoke(fragment, "getElement", elementName);
        if (element == null) {
            return;
        }
        writeTypedValue(element, value);
    }

    static void writeVariationText(Resource fragmentResource,
                                   String variationName,
                                   String variationTitle,
                                   String elementName,
                                   String value,
                                   String mimeType) throws Exception {
        Object variation = ensureVariation(fragmentResource, variationName, variationTitle, elementName);
        if (variation != null) {
            invoke(variation, "setContent", value == null ? "" : value, mimeType);
        }
    }

    static void writeVariationTyped(Resource fragmentResource,
                                    String variationName,
                                    String variationTitle,
                                    String elementName,
                                    Object value) throws Exception {
        Object variation = ensureVariation(fragmentResource, variationName, variationTitle, elementName);
        if (variation != null) {
            writeTypedValue(variation, value);
        }
    }

    static void createOrUpdateAsset(ResourceResolver resolver,
                                    String assetPath,
                                    byte[] data,
                                    String mimeType) throws Exception {
        Resource existing = resolver.getResource(assetPath);
        if (existing != null) {
            resolver.delete(existing);
            resolver.commit();
        }
        Class<?> assetManagerClass = classForName(ASSET_MANAGER_CLASS);
        Object assetManager = resolver.adaptTo(assetManagerClass);
        if (assetManager == null) {
            throw new IllegalStateException("AssetManager unavailable");
        }
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            invoke(assetManager, "createAsset", assetPath, inputStream, mimeType, true);
        }
    }

    /**
     * Removes every variation of the fragment that is not named in {@code keep} and returns the
     * names of the removed variations.
     */
    static List<String> removeVariationsExcept(Resource fragmentResource, Set<String> keep) throws Exception {
        Object fragment = requireFragment(fragmentResource);
        List<String> stale = new ArrayList<>();
        Iterator<?> variations = (Iterator<?>) invoke(fragment, "listAllVariations");
        while (variations.hasNext()) {
            String name = (String) invoke(variations.next(), "getName");
            if (!keep.contains(name)) {
                stale.add(name);
            }
        }
        for (String name : stale) {
            invoke(fragment, "removeVariation", name);
        }
        return stale;
    }

    private static Object ensureVariation(Resource fragmentResource,
                                          String variationName,
                                          String variationTitle,
                                          String elementName) throws Exception {
        Object fragment = requireFragment(fragmentResource);
        Object element = invoke(fragment, "getElement", elementName);
        if (element == null) {
            return null;
        }
        Object variation = invoke(element, "getVariation", variationName);
        if (variation != null) {
            return variation;
        }
        Object variationTemplate = null;
        try {
            variationTemplate = invoke(fragment, "createVariation", variationName, variationTitle, "Variant: " + variationTitle);
        } catch (Exception ignored) {
        }
        if (variationTemplate == null) {
            return invoke(element, "getVariation", variationName);
        }
        return invoke(element, "createVariation", variationTemplate);
    }

    private static void writeTypedValue(Object target, Object value) throws Exception {
        Object fragmentData = invoke(target, "getValue");
        Method getValue = fragmentData.getClass().getMethod("getValue");
        Object current = getValue.invoke(fragmentData);
        Object compatible = compatibleValue(current, value);
        invoke(fragmentData, "setValue", compatible);
        invoke(target, "setValue", fragmentData);
    }

    private static Object compatibleValue(Object current, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            List<String> strings = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    strings.add(entry.toString());
                }
            }
            if (current instanceof List<?>) {
                return strings;
            }
            return strings.toArray(String[]::new);
        }
        if (current instanceof Double || value instanceof Double) {
            return value instanceof Number number ? number.doubleValue() : Double.parseDouble(value.toString());
        }
        return value;
    }

    private static Object requireFragment(Resource resource) throws Exception {
        Object fragment = adapt(resource, CONTENT_FRAGMENT_CLASS);
        if (fragment == null) {
            throw new IllegalStateException("Resource does not adapt to ContentFragment: " + resource.getPath());
        }
        return fragment;
    }

    @SuppressWarnings("unchecked")
    private static Object adapt(Resource resource, String className) throws Exception {
        Class<?> clazz = classForName(className);
        return resource.adaptTo((Class<Object>) clazz);
    }

    private static Class<?> classForName(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Method method = findMethod(target.getClass(), methodName, args.length);
        if (method == null) {
            throw new NoSuchMethodException(methodName + " on " + target.getClass());
        }
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private static Method findMethod(Class<?> type, String name, int argCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        return null;
    }
}

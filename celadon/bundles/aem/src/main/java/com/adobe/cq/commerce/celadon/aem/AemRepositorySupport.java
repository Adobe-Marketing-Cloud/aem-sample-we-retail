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
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public final class AemRepositorySupport {

    static final String CELADON_ROOT_TITLE = "Celadon Product Catalogs";

    public static final String MANIFEST_FOLDER_TITLE = "Attribute Manifest";

    private AemRepositorySupport() {
    }

    public static Resource ensureOrderedFolder(ResourceResolver resolver, String absolutePath, String title) throws PersistenceException {
        Resource folder = ensurePath(resolver, absolutePath, "sling:OrderedFolder");
        Resource jcrContent = ensureChild(resolver, folder, "jcr:content", Map.of("jcr:primaryType", "nt:unstructured"));
        ModifiableValueMap properties = jcrContent.adaptTo(ModifiableValueMap.class);
        if (properties != null) {
            properties.put("jcr:title", title);
        }
        return folder;
    }

    static void writeFolderManualThumbnail(ResourceResolver resolver,
                                           Resource folder,
                                           byte[] data,
                                           String mimeType,
                                           String extension) throws PersistenceException {
        Resource jcrContent = ensureChild(resolver, folder, "jcr:content", Map.of("jcr:primaryType", "nt:unstructured"));
        removeExistingThumbnails(resolver, jcrContent);
        String fileName = "manualThumbnail." + (extension == null || extension.isBlank() ? "bin" : extension);
        Node jcrContentNode = jcrContent.adaptTo(Node.class);
        if (jcrContentNode == null) {
            throw new PersistenceException("Cannot adapt folder " + folder.getPath() + " to JCR node");
        }
        try {
            Node file = jcrContentNode.hasNode(fileName)
                    ? jcrContentNode.getNode(fileName)
                    : jcrContentNode.addNode(fileName, "nt:file");
            Node resourceNode = file.hasNode("jcr:content")
                    ? file.getNode("jcr:content")
                    : file.addNode("jcr:content", "nt:resource");
            Session session = resolver.adaptTo(Session.class);
            ValueFactory valueFactory = session == null ? null : session.getValueFactory();
            if (valueFactory == null) {
                throw new PersistenceException("JCR ValueFactory unavailable for " + folder.getPath());
            }
            Binary binary = valueFactory.createBinary(new ByteArrayInputStream(data));
            resourceNode.setProperty("jcr:data", binary);
            resourceNode.setProperty("jcr:mimeType", mimeType == null || mimeType.isBlank() ? "application/octet-stream" : mimeType);
            resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
            binary.dispose();
        } catch (RepositoryException e) {
            throw new PersistenceException("Failed to write manualThumbnail under " + folder.getPath(), e);
        }
    }

    private static void removeExistingThumbnails(ResourceResolver resolver, Resource jcrContent) throws PersistenceException {
        for (Resource child : jcrContent.getChildren()) {
            String name = child.getName();
            if (name.startsWith("manualThumbnail.") || "folderThumbnail".equals(name)) {
                resolver.delete(child);
            }
        }
    }

    static Resource ensureOrderedFolderPreservingTitle(ResourceResolver resolver,
                                                       String absolutePath,
                                                       String fallbackTitle) throws PersistenceException {
        Resource existing = resolver.getResource(absolutePath);
        if (existing != null) {
            ensureChild(resolver, existing, "jcr:content", Map.of("jcr:primaryType", "nt:unstructured"));
            return existing;
        }
        return ensureOrderedFolder(resolver, absolutePath, fallbackTitle);
    }

    static Resource ensurePath(ResourceResolver resolver, String absolutePath, String primaryType) throws PersistenceException {
        Resource current = resolver.getResource("/");
        String[] segments = absolutePath.split("/");
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            Resource child = current.getChild(segment);
            if (child == null) {
                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("jcr:primaryType", primaryType);
                child = resolver.create(current, segment, properties);
            }
            current = child;
        }
        return current;
    }

    static Resource ensureChild(ResourceResolver resolver,
                                Resource parent,
                                String name,
                                Map<String, Object> properties) throws PersistenceException {
        Resource child = parent.getChild(name);
        if (child == null) {
            child = resolver.create(parent, name, properties);
        } else {
            ModifiableValueMap valueMap = child.adaptTo(ModifiableValueMap.class);
            if (valueMap != null) {
                valueMap.putAll(properties);
            }
        }
        return child;
    }

    /**
     * Ensures the Celadon Option Definition Content Fragment model exists at
     * {@code /conf/<catalog>/settings/dam/cfm/models/celadon-option-definition}.
     * The model is idempotent — recreated only if not already present.
     */
    public static Resource ensureOptionDefinitionModel(ResourceResolver resolver,
                                                       String catalog) throws PersistenceException {
        String settingsRoot = "/conf/" + catalog + "/settings";
        String modelsRoot = settingsRoot + "/dam/cfm/models";
        String modelPath = modelsRoot + "/celadon-option-definition";

        ensureModelFolders(resolver, catalog, settingsRoot, modelsRoot);
        return buildOptionDefinitionModel(resolver, modelPath);
    }

    private static Resource buildOptionDefinitionModel(ResourceResolver resolver,
                                                       String modelPath) throws PersistenceException {
        Resource model = ensurePath(resolver, modelPath, "cq:Template");
        put(model, Map.of(
                "allowedPaths", new String[]{"/content/dam(/.*)?"},
                "ranking", 100L
        ));

        Resource jcrContent = ensureChild(resolver, model, "jcr:content", map(
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Celadon Option Definition",
                "jcr:description", "Configurable option definition reusable across products",
                "sling:resourceType", "dam/cfm/models/console/components/data/entity/default",
                "sling:resourceSuperType", "dam/cfm/models/console/components/data/entity",
                "cq:templateType", "/libs/settings/dam/cfm/model-types/fragment",
                "status", "enabled",
                "cq:scaffolding", modelPath + "/jcr:content/model"
        ));
        ensureChild(resolver, jcrContent, "metadata", Map.of("jcr:primaryType", "nt:unstructured"));

        Resource modelRoot = ensureChild(resolver, jcrContent, "model", map(
                "jcr:primaryType", "cq:PageContent",
                "sling:resourceType", "wcm/scaffolding/components/scaffolding",
                "dataTypesConfig", "/mnt/overlay/settings/dam/cfm/models/formbuilderconfig/datatypes",
                "cq:targetPath", "/content/entities",
                "maxGeneratedOrder", 5L
        ));
        Resource dialog = ensureChild(resolver, modelRoot, "cq:dialog", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "cq/gui/components/authoring/dialog"
        ));
        Resource dialogContent = ensureChild(resolver, dialog, "content", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/fixedcolumns"
        ));
        Resource items = ensureChild(resolver, dialogContent, "items", map(
                "jcr:primaryType", "nt:unstructured",
                "maxGeneratedOrder", 5L
        ));

        ensureField(resolver, items, "label", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "label",
                "fieldLabel", "Label",
                "cfm-element", "Label",
                "valueType", "string",
                "listOrder", "1",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "attributeCode", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "attributeCode",
                "fieldLabel", "Attribute Code",
                "cfm-element", "Attribute Code",
                "valueType", "string",
                "listOrder", "2",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "productField", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "productField",
                "fieldLabel", "Product Field",
                "cfm-element", "Product Field",
                "valueType", "string",
                "listOrder", "3",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "swatchType", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "swatchType",
                "fieldLabel", "Swatch Type",
                "cfm-element", "Swatch Type",
                "valueType", "string",
                "listOrder", "4",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        Resource values = ensureField(resolver, items, "values", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/multifield",
                "metaType", "text-single",
                "name", "values",
                "fieldLabel", "Values",
                "valueType", "string[]",
                "listOrder", "5",
                "maxlength", "255",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureChild(resolver, values, "granite:data", Map.of("jcr:primaryType", "nt:unstructured"));
        Resource valuesField = ensureChild(resolver, values, "field", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "name", "values",
                "fieldLabel", "Value",
                "maxlength", "255",
                "emptyText", "valueIndex;Label;SwatchValue",
                "renderReadOnly", "false"
        ));
        ensureChild(resolver, valuesField, "granite:data", Map.of("jcr:primaryType", "nt:unstructured"));
        return model;
    }

    /**
     * Ensures the folder skeleton {@code /conf/<catalog>/settings/dam/cfm/models}
     * exists. Called by both the option-definition and attribute model writers.
     */
    private static void ensureModelFolders(ResourceResolver resolver,
                                           String catalog,
                                           String settingsRoot,
                                           String modelsRoot) throws PersistenceException {
        if (resolver.getResource(modelsRoot) != null) {
            return;
        }
        ensureOrderedFolder(resolver, "/conf", "conf");
        ensureOrderedFolder(resolver, "/conf/" + catalog, catalog);
        ensureOrderedFolder(resolver, settingsRoot, "settings");
        ensureOrderedFolder(resolver, settingsRoot + "/dam", "dam");
        ensureOrderedFolder(resolver, settingsRoot + "/dam/cfm", "cfm");
        ensureOrderedFolder(resolver, modelsRoot, "models");
    }

    /**
     * Returns the per-catalog Option Definition model resource, or {@code null}
     * if it has not been created yet.
     */
    public static Resource optionDefinitionModel(ResourceResolver resolver, String catalog) {
        return resolver.getResource("/conf/" + catalog + "/settings/dam/cfm/models/celadon-option-definition");
    }

    public static String ensureAttributeModel(ResourceResolver resolver, String catalog) throws PersistenceException {
        String settingsRoot = "/conf/" + catalog + "/settings";
        String modelsRoot = settingsRoot + "/dam/cfm/models";
        String modelPath = modelsRoot + "/celadon-attribute";

        if (resolver.getResource(modelPath) != null) {
            return modelPath;
        }

        ensureModelFolders(resolver, catalog, settingsRoot, modelsRoot);

        Resource model = ensurePath(resolver, modelPath, "cq:Template");
        put(model, Map.of(
                "allowedPaths", new String[]{"/content/dam(/.*)?"},
                "ranking", 100L
        ));

        Resource jcrContent = ensureChild(resolver, model, "jcr:content", map(
                "jcr:primaryType", "cq:PageContent",
                "jcr:title", "Celadon Attribute",
                "jcr:description", "Per-attribute manifest entry",
                "sling:resourceType", "dam/cfm/models/console/components/data/entity/default",
                "sling:resourceSuperType", "dam/cfm/models/console/components/data/entity",
                "cq:templateType", "/libs/settings/dam/cfm/model-types/fragment",
                "status", "enabled",
                "cq:scaffolding", modelPath + "/jcr:content/model"
        ));
        ensureChild(resolver, jcrContent, "metadata", Map.of("jcr:primaryType", "nt:unstructured"));

        Resource modelRoot = ensureChild(resolver, jcrContent, "model", map(
                "jcr:primaryType", "cq:PageContent",
                "sling:resourceType", "wcm/scaffolding/components/scaffolding",
                "dataTypesConfig", "/mnt/overlay/settings/dam/cfm/models/formbuilderconfig/datatypes",
                "cq:targetPath", "/content/entities",
                "maxGeneratedOrder", 9L
        ));
        Resource dialog = ensureChild(resolver, modelRoot, "cq:dialog", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "cq/gui/components/authoring/dialog"
        ));
        Resource dialogContent = ensureChild(resolver, dialog, "content", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/fixedcolumns"
        ));
        Resource items = ensureChild(resolver, dialogContent, "items", map(
                "jcr:primaryType", "nt:unstructured",
                "maxGeneratedOrder", 9L
        ));

        ensureField(resolver, items, "code", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "code",
                "fieldLabel", "Code",
                "cfm-element", "Code",
                "valueType", "string",
                "listOrder", "1",
                "required", "true",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "label", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "label",
                "fieldLabel", "Label",
                "cfm-element", "Label",
                "valueType", "string",
                "listOrder", "2",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "type", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "type",
                "fieldLabel", "Type",
                "cfm-element", "Type",
                "valueType", "string",
                "listOrder", "3",
                "required", "true",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "scope", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "scope",
                "fieldLabel", "Scope",
                "cfm-element", "Scope",
                "valueType", "string",
                "listOrder", "4",
                "required", "true",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "filterable", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/checkbox",
                "metaType", "boolean",
                "name", "filterable",
                "fieldLabel", "Filterable",
                "text", "Filterable",
                "cfm-element", "Filterable",
                "valueType", "boolean",
                "listOrder", "5",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "aggregatable", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/checkbox",
                "metaType", "boolean",
                "name", "aggregatable",
                "fieldLabel", "Aggregatable",
                "text", "Aggregatable",
                "cfm-element", "Aggregatable",
                "valueType", "boolean",
                "listOrder", "6",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "ordering", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/numberfield",
                "metaType", "number",
                "name", "ordering",
                "fieldLabel", "Ordering",
                "cfm-element", "Ordering",
                "valueType", "long",
                "listOrder", "7",
                "typeHint", "long",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "optionDefinition", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "dam/cfm/models/editor/components/fragmentreference",
                "metaType", "fragment-reference",
                "name", "optionDefinition",
                "fieldLabel", "Option Definition",
                "cfm-element", "Option Definition",
                "valueType", "string/content-fragment",
                "listOrder", "8",
                "filter", "hierarchy",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));
        ensureField(resolver, items, "sourceHint", map(
                "jcr:primaryType", "nt:unstructured",
                "sling:resourceType", "granite/ui/components/coral/foundation/form/textfield",
                "metaType", "text-single",
                "name", "sourceHint",
                "fieldLabel", "Source Hint",
                "cfm-element", "Source Hint",
                "valueType", "string",
                "listOrder", "9",
                "renderReadOnly", "false",
                "showEmptyInReadOnly", "true"
        ));

        resolver.commit();
        return modelPath;
    }

    static Resource ensureField(ResourceResolver resolver,
                                Resource parent,
                                String name,
                                Map<String, Object> properties) throws PersistenceException {
        return ensureChild(resolver, parent, name, properties);
    }

    static void put(Resource resource, Map<String, Object> properties) {
        ModifiableValueMap values = resource.adaptTo(ModifiableValueMap.class);
        if (values != null) {
            values.putAll(properties);
        }
    }

    static String escapeNodeName(String value) {
        if (value == null || value.isBlank()) {
            return "unnamed";
        }
        return value.trim().toLowerCase()
                .replace(" ", "-")
                .replace("/", "-")
                .replace(":", "-")
                .replace("[", "")
                .replace("]", "")
                .replace("*", "")
                .replace("|", "")
                .replace("'", "");
    }

    static Map<String, Object> map(Object... values) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index].toString(), values[index + 1]);
        }
        return result;
    }
}

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

import com.adobe.cq.commerce.celadon.aem.AemContentFragmentSupport;
import com.adobe.cq.commerce.celadon.aem.AemRepositorySupport;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Writes per-attribute manifest Content Fragments under
 * {@code /content/dam/celadon/<catalog>/_manifest}.
 *
 * <p>Fragments are created through AEM's Content Fragment API
 * ({@code FragmentTemplate.createFragment}) so they are fully valid assets —
 * with metadata, renditions, the {@code data/cq:model} link and a proper
 * {@code master} variation node. Hand-building the JCR produced assets the
 * Assets console and Content Fragment editor rejected as malformed.</p>
 *
 * <p>The writer is idempotent: it deletes the existing {@code _manifest} folder
 * before recreating it from the supplied {@link AttributeManifest}. The
 * {@code celadon-attribute} CF model must already exist under
 * {@code /conf/<catalog>/settings/dam/cfm/models} (the importers call
 * {@code AemRepositorySupport.ensureAttributeModel} first).</p>
 */
public final class ManifestWriter {

    public void write(ResourceResolver resolver, AttributeManifest manifest) throws PersistenceException {
        String catalogPath = "/content/dam/celadon/" + manifest.catalog();
        String manifestPath = catalogPath + "/_manifest";

        Resource existing = resolver.getResource(manifestPath);
        if (existing != null) {
            resolver.delete(existing);
        }

        Resource manifestFolder = AemRepositorySupport.ensureOrderedFolder(
                resolver, manifestPath, AemRepositorySupport.MANIFEST_FOLDER_TITLE);

        String modelPath = "/conf/" + manifest.catalog() + "/settings/dam/cfm/models/celadon-attribute";
        Resource model = resolver.getResource(modelPath);
        if (model == null) {
            throw new PersistenceException("Attribute model not found at " + modelPath);
        }

        for (AttributeEntry e : manifest.entries()) {
            writeEntry(resolver, model, manifestFolder, e);
        }
        resolver.commit();
    }

    private void writeEntry(ResourceResolver resolver, Resource model, Resource manifestFolder,
                            AttributeEntry e) throws PersistenceException {
        try {
            String title = e.label() == null || e.label().isBlank() ? e.code() : e.label();
            Resource fragment = AemContentFragmentSupport.ensureFragment(
                    resolver, model, manifestFolder, e.code(), title);

            AemContentFragmentSupport.writeText(fragment, "code", e.code(), "text/plain");
            AemContentFragmentSupport.writeText(fragment, "label", e.label() == null ? "" : e.label(), "text/plain");
            AemContentFragmentSupport.writeText(fragment, "type", e.type().name(), "text/plain");
            AemContentFragmentSupport.writeText(fragment, "scope", e.scope().name(), "text/plain");
            AemContentFragmentSupport.writeTyped(fragment, "filterable", e.filterable());
            AemContentFragmentSupport.writeTyped(fragment, "aggregatable", e.aggregatable());
            AemContentFragmentSupport.writeTyped(fragment, "ordering", (long) e.ordering());
            if (e.optionDefinitionPath() != null) {
                AemContentFragmentSupport.writeText(fragment, "optionDefinition", e.optionDefinitionPath(), "text/plain");
            }
            if (e.sourceHint() != null) {
                AemContentFragmentSupport.writeText(fragment, "sourceHint", e.sourceHint(), "text/plain");
            }
        } catch (PersistenceException pe) {
            throw pe;
        } catch (Exception ex) {
            throw new PersistenceException("Failed to write manifest fragment for attribute '" + e.code() + "'", ex);
        }
    }
}

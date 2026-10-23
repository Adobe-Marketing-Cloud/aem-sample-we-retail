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

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifestReader;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

/**
 * Reads per-catalog attribute manifests from {@code /content/dam/celadon/<catalog>/_manifest}.
 *
 * <p>Reads are cached per-catalog by lazy-load-once: first successful read for a
 * given catalog populates the cache and subsequent reads return the same instance.</p>
 */
@Component(service = AttributeManifestReader.class, immediate = true)
public final class ManifestReaderImpl implements AttributeManifestReader {

    private final Map<String, AttributeManifest> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<AttributeManifest> read(ResourceResolver resolver, String catalog) {
        AttributeManifest cached = cache.get(catalog);
        if (cached != null) {
            return Optional.of(cached);
        }

        Resource folder = resolver.getResource("/content/dam/celadon/" + catalog + "/_manifest");
        if (folder == null) {
            return Optional.empty();
        }

        List<AttributeEntry> entries = new ArrayList<>();
        for (Resource child : folder.getChildren()) {
            Resource master = child.getChild("jcr:content/data/master");
            if (master == null) {
                continue;
            }
            ValueMap vm = master.getValueMap();
            entries.add(new AttributeEntry(
                    vm.get("code", ""),
                    vm.get("label", ""),
                    NormalizedType.valueOf(vm.get("type", "STRING")),
                    AttributeScope.valueOf(vm.get("scope", "PRODUCT")),
                    vm.get("filterable", false),
                    vm.get("aggregatable", false),
                    vm.get("ordering", 0),
                    vm.get("optionDefinition", String.class),
                    vm.get("sourceHint", String.class)
            ));
        }
        entries.sort(Comparator.comparingInt(AttributeEntry::ordering));
        AttributeManifest m = new AttributeManifest(catalog, entries);
        cache.put(catalog, m);
        return Optional.of(m);
    }
}

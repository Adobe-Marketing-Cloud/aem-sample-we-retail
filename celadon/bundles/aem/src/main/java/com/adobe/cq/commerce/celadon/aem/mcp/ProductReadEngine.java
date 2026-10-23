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
package com.adobe.cq.commerce.celadon.aem.mcp;

import com.adobe.cq.commerce.celadon.aem.attribute.manifest.ManifestReaderImpl;
import com.adobe.cq.commerce.celadon.aem.catalog.AemCatalogGateway;
import com.adobe.cq.commerce.celadon.core.api.CeladonGraphqlEngine;
import com.adobe.cq.commerce.celadon.core.api.FetcherContext;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

/**
 * Runs read-only GraphQL queries against a catalog by reusing the production
 * {@link CeladonGraphqlEngine} on the AEM-direct backend, driven by the caller's
 * own {@link ResourceResolver} (run-as-user). This reuses the exact filtering /
 * snapshot logic the storefront uses rather than reimplementing it.
 */
@Component(service = ProductReadEngine.class)
public class ProductReadEngine {

    /** Execute a GraphQL query for the given catalog; returns the raw result map. */
    public Map<String, Object> execute(ResourceResolver resolver, String catalog,
                                       String query, Map<String, Object> variables) {
        AttributeManifest manifest = new ManifestReaderImpl().read(resolver, catalog)
                .orElse(AttributeManifest.empty(catalog));
        FetcherContext context = new FetcherContext(catalog);
        CeladonGraphqlEngine engine = new CeladonGraphqlEngine(context, manifest);
        // closeResourceResolver=false: the request resolver is owned by the caller.
        AemCatalogGateway gateway = new AemCatalogGateway(resolver, context, false);
        return engine.execute(query, null, variables == null ? Map.of() : variables, gateway);
    }
}

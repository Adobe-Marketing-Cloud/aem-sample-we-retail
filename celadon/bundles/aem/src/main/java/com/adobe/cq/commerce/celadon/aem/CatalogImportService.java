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

import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * In-JVM entry point for the full (wipe-and-rebuild) catalog import, implemented
 * by {@link CeladonCatalogServlet}. Lets the MCP {@code reimport_catalog} tool
 * reuse the exact import pipeline the HTTP endpoint runs.
 */
public interface CatalogImportService {
    void importCatalog(ResourceResolver resolver, String endpoint, String root, Map<String, String> headers)
            throws Exception;
}

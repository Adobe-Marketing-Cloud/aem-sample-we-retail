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

import com.adobe.cq.commerce.celadon.aem.attribute.manifest.ManifestReaderImpl;
import com.adobe.cq.commerce.celadon.aem.catalog.AemCatalogGateway;
import com.adobe.cq.commerce.celadon.core.api.CeladonGraphqlEngine;
import com.adobe.cq.commerce.celadon.core.api.FetcherContext;
import com.adobe.cq.commerce.celadon.core.api.JsonSupport;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.osgi.service.metatype.annotations.Designate;

@Component(
        service = {Servlet.class, ResourceChangeListener.class},
        property = {
                "sling.auth.requirements=-/apps/celadon/graphql",
                "sling.auth.requirements=-/apps/celadon/graphql/*",
                // Evict the per-catalog engine whenever catalog content changes
                // (author edits in the Assets console, importer writes, replication)
                // so live edits are reflected without a bundle restart.
                ResourceChangeListener.PATHS + "=/content/dam/celadon",
                ResourceChangeListener.CHANGES + "=ADDED",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED"
        }
)
@Designate(ocd = CeladonGraphqlServletConfiguration.class)
@HttpWhiteboardServletPattern({"/apps/celadon/graphql", "/apps/celadon/graphql/*"})
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=org.osgi.service.http)")
public class CeladonGraphqlServlet extends HttpServlet implements ResourceChangeListener {
    /** Subservice name mapped to the {@code celadon-catalog-reader} system user. */
    private static final String SUBSERVICE_CATALOG_READER = "catalog-reader";

    /** JCR root under which every Celadon catalog folder lives. */
    private static final String CATALOG_ROOT = "/content/dam/celadon";

    /** Catalog folder names must be a single, traversal-free path segment. */
    private static final Pattern CATALOG_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /** One engine per catalog, each owning its own schema + snapshot cache. */
    final Map<String, CeladonGraphqlEngine> engines = new ConcurrentHashMap<>();

    /** Catalog served at the bare {@code /apps/celadon/graphql} endpoint (no segment). */
    volatile String defaultCatalog;

    @Activate
    protected void activate(CeladonGraphqlServletConfiguration configuration) {
        this.defaultCatalog = FetcherContext.normalizeBasePath(configuration.defaultCatalog());
        // Pre-warm the default catalog so the bare endpoint is ready immediately.
        if (!defaultCatalog.isBlank()) {
            engineFor(defaultCatalog);
        }
    }

    /** Builds (or returns the cached) engine for {@code catalog}. */
    private CeladonGraphqlEngine engineFor(String catalog) {
        return engines.computeIfAbsent(catalog, c ->
                new CeladonGraphqlEngine(new FetcherContext(c), loadManifest(c)));
    }

    private AttributeManifest loadManifest(String catalog) {
        // catalogs live under /content/dam/celadon/<catalog>; a fresh reader each
        // call means a rebuilt engine always re-reads the current manifest.
        if (catalog == null || catalog.isBlank()) {
            return AttributeManifest.empty(catalog);
        }
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(serviceAuthInfo())) {
            return new ManifestReaderImpl().read(resolver, catalog)
                    .orElse(AttributeManifest.empty(catalog));
        } catch (LoginException e) {
            return AttributeManifest.empty(catalog);
        }
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        // Evict only the engine(s) whose catalog content changed; the next query
        // for that catalog rebuilds its schema + snapshot from current JCR. A change
        // that can't be attributed to a single catalog clears the whole cache.
        for (ResourceChange change : changes) {
            String catalog = catalogFromPath(change.getPath());
            if (catalog == null) {
                engines.clear();
                return;
            }
            engines.remove(catalog);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response);
    }

    private void execute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String catalog = catalogFromRequest(request);
        if (catalog == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown catalog");
            return;
        }

        CeladonGraphqlEngine engine = engineFor(catalog);
        RequestPayload payload = resolvePayload(request);
        Map<String, Object> variables = new LinkedHashMap<>(payload.variables());
        Map<String, Object> result = engine.execute(
                payload.query(),
                payload.operationName().isBlank() ? null : payload.operationName(),
                variables,
                new AemCatalogGateway(openCatalogResolver(), new FetcherContext(catalog), true)
        );

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        response.getWriter().write(JsonSupport.toJson(result));
    }

    private RequestPayload resolvePayload(HttpServletRequest request) throws IOException {
        String parameterQuery = trimToEmpty(request.getParameter("query"));
        if (!parameterQuery.isBlank()) {
            return new RequestPayload(
                    parameterQuery,
                    trimToEmpty(request.getParameter("operationName")),
                    parseVariables(request.getParameter("variables"))
            );
        }

        if ("GET".equalsIgnoreCase(request.getMethod()) || isFormRequest(request)) {
            return RequestPayload.empty();
        }

        String body = readBody(request);
        if (body.isBlank()) {
            return RequestPayload.empty();
        }

        Map<String, Object> json = parseJsonBody(body);
        if (!json.isEmpty()) {
            return new RequestPayload(
                    trimToEmpty(json.get("query")),
                    trimToEmpty(json.get("operationName")),
                    parseVariables(json.get("variables"))
            );
        }

        return new RequestPayload(body, "", Map.of());
    }

    private boolean isFormRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("application/x-www-form-urlencoded")
                || contentType.startsWith("multipart/form-data");
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> parseJsonBody(String body) {
        return JsonSupport.parseMap(body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseVariables(Object rawVariables) {
        if (rawVariables instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (rawVariables instanceof String string && !string.isBlank()) {
            return JsonSupport.parseMap(string);
        }
        return Map.of();
    }

    private String trimToEmpty(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private ResourceResolver openCatalogResolver() {
        try {
            return resourceResolverFactory.getServiceResourceResolver(serviceAuthInfo());
        } catch (LoginException e) {
            throw new IllegalStateException("Failed to open catalog resource resolver for direct AEM backend", e);
        }
    }

    private Map<String, Object> serviceAuthInfo() {
        return Map.of(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_CATALOG_READER);
    }

    /**
     * Resolves the target catalog for a request from the path beyond
     * {@code /apps/celadon/graphql} (the servlet pathInfo). See
     * {@link #catalogForPathInfo(String)} for the routing rules.
     */
    private String catalogFromRequest(HttpServletRequest request) {
        return catalogForPathInfo(request.getPathInfo());
    }

    /**
     * Pure routing logic behind {@link #catalogFromRequest}: maps a servlet
     * pathInfo to a catalog name. A blank or {@code "/"} pathInfo falls back to
     * the configured default catalog; otherwise the first path segment names the
     * catalog. Returns {@code null} when the segment is malformed or no default
     * is set.
     */
    String catalogForPathInfo(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            String fallback = defaultCatalog;
            return fallback == null || fallback.isBlank() ? null : fallback;
        }
        String segment = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slash = segment.indexOf('/');
        if (slash >= 0) {
            segment = segment.substring(0, slash);
        }
        return CATALOG_SEGMENT.matcher(segment).matches() ? segment : null;
    }

    /**
     * Extracts the catalog name from a JCR change path under {@link #CATALOG_ROOT};
     * returns {@code null} when the change is the root itself (i.e. not attributable
     * to a single catalog), forcing a full cache clear.
     */
    String catalogFromPath(String path) {
        if (path == null || !path.startsWith(CATALOG_ROOT + "/")) {
            return null;
        }
        String rest = path.substring(CATALOG_ROOT.length() + 1);
        int slash = rest.indexOf('/');
        String catalog = slash >= 0 ? rest.substring(0, slash) : rest;
        return catalog.isBlank() ? null : catalog;
    }

    private record RequestPayload(String query, String operationName, Map<String, Object> variables) {
        private static RequestPayload empty() {
            return new RequestPayload("", "", Map.of());
        }
    }
}

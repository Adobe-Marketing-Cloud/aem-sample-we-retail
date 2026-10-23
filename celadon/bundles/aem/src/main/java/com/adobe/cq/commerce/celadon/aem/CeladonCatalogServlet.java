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

import com.adobe.cq.commerce.celadon.aem.attribute.manifest.ManifestWriter;
import com.adobe.cq.commerce.celadon.aem.attribute.model.MagentoImportQueryBuilder;
import com.adobe.cq.commerce.celadon.aem.attribute.model.ProductModelGenerator;
import com.adobe.cq.commerce.celadon.aem.attribute.source.DiscoveryHints;
import com.adobe.cq.commerce.celadon.aem.attribute.source.MagentoIntrospector;
import com.adobe.cq.commerce.celadon.core.api.JsonSupport;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import com.adobe.cq.commerce.celadon.aem.authoring.CatalogAuthoringService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        service = {javax.servlet.Servlet.class, CatalogImportService.class},
        property = {
                "sling.servlet.paths=/apps/celadon/catalog",
                "sling.servlet.methods=POST"
        }
)
public class CeladonCatalogServlet extends SlingAllMethodsServlet implements CatalogImportService {
    private static final Logger LOG = Logger.getLogger(CeladonCatalogServlet.class.getName());
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final ManifestWriter manifestWriter = new ManifestWriter();
    private final ProductModelGenerator productModelGenerator = new ProductModelGenerator();

    @Reference
    private CatalogAuthoringService authoringService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setStatus(SlingHttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().write("POST only");
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        try {
            Map<String, String> parameters = resolveParameters(request);
            String endpoint = parameters.get("endpoint");
            String root = parameters.get("root");
            if (isBlank(endpoint) || isBlank(root)) {
                response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Missing required parameters: endpoint, root");
                return;
            }

            importCatalog(request.getResourceResolver(), endpoint, root, resolveHeaders(parameters));
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getWriter().write("Import completed successfully");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Catalog import failed", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Import failed: " + e.getMessage());
        }
    }

    @Override
    public void importCatalog(ResourceResolver resolver,
                              String endpoint,
                              String root,
                              Map<String, String> headers) throws Exception {
        Resource catalogRoot = AemRepositorySupport.ensureOrderedFolder(resolver, "/content/dam/celadon", AemRepositorySupport.CELADON_ROOT_TITLE);
        Resource targetRoot = AemRepositorySupport.ensureOrderedFolder(resolver, catalogRoot.getPath() + "/" + AemRepositorySupport.escapeNodeName(root), root);
        resolver.commit();

        // Step 1: clear ready flag for the duration of the import.
        String catalog = AemRepositorySupport.escapeNodeName(root);
        authoringService.setCatalogReady(resolver, catalog, false);
        LOG.info(() -> "celadon: catalog " + catalog + " marked not-ready for import");

        // Step 2: introspect attributes from the Magento source.
        DiscoveryHints hints = DiscoveryHints.read(targetRoot);
        MagentoIntrospector.IntrospectionResult introspection = introspectAttributes(endpoint, headers, hints, catalog);
        AttributeManifest manifest = introspection.manifest();
        LOG.info(() -> "celadon: introspected " + manifest.entries().size() + " attributes for catalog " + catalog);

        // Step 3 + 4: ensure attribute model, write manifest, regenerate product model.
        AemRepositorySupport.ensureAttributeModel(resolver, catalog);
        manifestWriter.write(resolver, manifest);
        productModelGenerator.regenerate(resolver, catalog, manifest);
        AemRepositorySupport.ensureOptionDefinitionModel(resolver, catalog);
        LOG.info(() -> "celadon: manifest and product model regenerated for catalog " + catalog);

        Resource modelResource = Objects.requireNonNull(
                resolver.getResource("/conf/" + catalog + "/settings/dam/cfm/models/product"),
                "Product CF model not available under /conf");
        Resource optionDefinitionModel = Objects.requireNonNull(
                AemRepositorySupport.optionDefinitionModel(resolver, catalog),
                "Option Definition model not available under /conf");
        resolver.commit();

        // Step 5: drop existing products before reimporting.
        Resource productsRoot = resolver.getResource(targetRoot.getPath() + "/products");
        if (productsRoot != null) {
            resolver.delete(productsRoot);
            resolver.commit();
            LOG.info(() -> "celadon: dropped existing products under " + targetRoot.getPath() + "/products");
        }

        OptionDefinitionWriter optionWriter = new OptionDefinitionWriter(resolver, targetRoot, optionDefinitionModel);

        List<Map<String, Object>> categories = listOfMaps(postGraphql(endpoint, categoryTreeQuery(8), Map.of(), headers, null)
                .getOrDefault("data.categoryList", List.of()));
        for (Map<String, Object> category : categories) {
            processCategory(resolver, targetRoot.getPath(), category, "", headers);
        }
        resolver.commit();

        String productsQuery = MagentoImportQueryBuilder.fromManifest(manifest);
        String fallbackQuery = MagentoImportQueryBuilder.categoryFallbackFromManifest(manifest);
        int currentPage = 1;
        int fetched = 0;
        int totalCount = Integer.MAX_VALUE;
        while (fetched < totalCount) {
            Map<String, Object> productsResponse = postGraphql(endpoint, productsQuery, AemRepositorySupport.map("currentPage", currentPage), headers, "");
            Map<String, Object> products = map(productsResponse.get("data.products"));
            totalCount = intValue(products.get("total_count"), 0);
            List<Map<String, Object>> items = listOfMaps(products.get("items"));
            if (currentPage == 1 && totalCount == 0 && items.isEmpty()) {
                String rootUid = firstNonBlankCategoryUid(endpoint, headers);
                if (!rootUid.isBlank()) {
                    items = listOfMaps(map(postGraphql(endpoint, fallbackQuery, AemRepositorySupport.map("categoryUid", rootUid, "currentPage", currentPage), headers, null)
                            .get("data.products")).get("items"));
                }
            }
            if (items.isEmpty()) {
                break;
            }
            for (Map<String, Object> product : items) {
                try {
                    importProduct(resolver, modelResource, optionWriter, targetRoot.getPath(), product, headers, manifest);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Skipping product import failure: " + product.get("sku"), e);
                }
            }
            resolver.commit();
            fetched += items.size();
            currentPage++;
        }

        // Step 7: write option definitions for manifest SELECT/MULTISELECT entries.
        writeManifestOptionDefinitions(optionWriter, manifest, introspection);
        resolver.commit();

        // Step 8: mark the catalog ready.
        authoringService.setCatalogReady(resolver, catalog, true);
        LOG.info(() -> "celadon: catalog " + catalog + " marked ready");
    }

    private void writeManifestOptionDefinitions(OptionDefinitionWriter optionWriter,
                                                AttributeManifest manifest,
                                                MagentoIntrospector.IntrospectionResult introspection) throws Exception {
        for (AttributeEntry entry : manifest.entries()) {
            if (entry.type() != NormalizedType.SELECT && entry.type() != NormalizedType.MULTISELECT) {
                continue;
            }
            List<MagentoIntrospector.OptionValue> values = introspection.options().get(entry.code());
            if (values == null || values.isEmpty()) {
                continue;
            }
            optionWriter.ensureOptionFromManifest(entry, values);
        }
    }

    private MagentoIntrospector.IntrospectionResult introspectAttributes(String endpoint,
                                                                          Map<String, String> headers,
                                                                          DiscoveryHints hints,
                                                                          String catalog) throws Exception {
        // Magento returns null for `customAttributeMetadata(attributes: [])`; pass concrete codes so we get data.
        // The list below covers the canonical Venia + accessory/fashion attribute set; codes the source doesn't
        // know about are silently ignored, so the same list works across stores.
        String introspectionQuery = "query CeladonIntrospect { customAttributeMetadata(attributes: ["
                + "{attribute_code:\"fashion_color\",entity_type:\"4\"},"
                + "{attribute_code:\"fashion_size\",entity_type:\"4\"},"
                + "{attribute_code:\"fashion_material\",entity_type:\"4\"},"
                + "{attribute_code:\"fashion_style\",entity_type:\"4\"},"
                + "{attribute_code:\"format\",entity_type:\"4\"},"
                + "{attribute_code:\"has_video\",entity_type:\"4\"},"
                + "{attribute_code:\"accessory_brand\",entity_type:\"4\"},"
                + "{attribute_code:\"accessory_gemstone_addon\",entity_type:\"4\"},"
                + "{attribute_code:\"accessory_recyclable_material\",entity_type:\"4\"}"
                + "]) { items { attribute_code attribute_type input_type } } }";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        JsonSupport.toJson(Map.of("query", introspectionQuery))));
        headers.forEach(builder::header);
        HttpResponse<String> response = sendWithRedirects(builder.build(), 0);
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Introspection failed: HTTP " + response.statusCode());
        }
        return MagentoIntrospector.parseWithOptions(catalog, response.body(), hints);
    }

    private void processCategory(ResourceResolver resolver,
                                 String parentPath,
                                 Map<String, Object> category,
                                 String parentUrlPath,
                                 Map<String, String> headers) throws PersistenceException {
        // FIX: Use url_key first (single segment) instead of url_path (full path from root)
        // to avoid creating duplicate nested folder structures.
        String folderPath = normalizeCategoryPath(stringValue(category.get("url_key")));
        if (folderPath.isBlank()) {
            // Fallback: extract the last segment from url_path if url_key is missing
            String urlPath = normalizeCategoryPath(stringValue(category.get("url_path")));
            folderPath = urlPath.isBlank() ? "" : pathLeaf(urlPath);
        }
        if (folderPath.isBlank()) {
            folderPath = normalizeCategoryPath(stringValue(category.get("name")));
        }
        if (folderPath.isBlank()) {
            folderPath = normalizeCategoryPath(stringValue(category.get("uid")));
        }
        if (folderPath.isBlank() || "default-category".equalsIgnoreCase(folderPath)) {
            for (Map<String, Object> child : listOfMaps(category.get("children"))) {
                processCategory(resolver, parentPath, child, parentUrlPath, headers);
            }
            return;
        }
        String currentPath = parentPath + "/" + folderPath;
        Resource folder = AemRepositorySupport.ensureOrderedFolder(resolver, currentPath, stringValueOrFallback(category.get("name"), folderPath));
        importCategoryImage(resolver, folder, stringValue(category.get("image")), headers);
        for (Map<String, Object> child : listOfMaps(category.get("children"))) {
            processCategory(resolver, currentPath, child, folderPath, headers);
        }
    }

    private void importCategoryImage(ResourceResolver resolver,
                                     Resource folder,
                                     String imageUrl,
                                     Map<String, String> headers) {
        if (imageUrl == null || imageUrl.isBlank() || isPlaceholder(imageUrl)) {
            return;
        }
        try {
            DownloadedAsset asset = downloadAsset(resolveSourceUrl(imageUrl, headers), headers);
            AemRepositorySupport.writeFolderManualThumbnail(
                    resolver,
                    folder,
                    asset.bytes(),
                    asset.mimeType(),
                    asset.extension()
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Skipping category image for " + folder.getPath() + ": " + imageUrl, e);
        }
    }

    private void importProduct(ResourceResolver resolver,
                               Resource modelResource,
                               OptionDefinitionWriter optionWriter,
                               String catalogRootPath,
                               Map<String, Object> product,
                               Map<String, String> headers,
                               AttributeManifest manifest) throws Exception {
        String categoryPath = canonicalCategoryPath(product);
        Resource parentFolder = AemRepositorySupport.ensureOrderedFolderPreservingTitle(
                resolver,
                categoryPath.isBlank() ? catalogRootPath : catalogRootPath + "/" + categoryPath,
                categoryPath.isBlank() ? pathLeaf(catalogRootPath) : pathLeaf(categoryPath)
        );
        String nodeName = AemRepositorySupport.escapeNodeName(productNodeName(product));
        Resource fragmentResource = AemContentFragmentSupport.ensureFragment(
                resolver,
                modelResource,
                parentFolder,
                nodeName,
                stringValue(product.get("name"))
        );

        // Manifest-driven write of every product-scoped attribute (universal + custom).
        writeManifestAttributes(fragmentResource, product, manifest);

        // Image is the only field that needs the asset-import pipeline; keep it out of the manifest loop.
        List<String> imagePaths = importImages(
                resolver,
                parentFolder.getPath(),
                nodeName,
                listOfMaps(product.get("media_gallery")),
                map(product.get("image")),
                map(product.get("thumbnail")),
                headers
        );
        AemContentFragmentSupport.writeTyped(fragmentResource, "image", imagePaths);

        // Multi-category membership: keep the deepest path as primary (the JCR home
        // folder above), and persist the other branches. Ancestors of the primary are
        // implied by location and are NOT stored.
        List<String> additionalCategories = CategoryMembership.deriveAdditionalCategories(
                assignedCategoryPaths(product), categoryPath);
        AemContentFragmentSupport.writeTyped(fragmentResource, "additionalCategories", additionalCategories);

        List<Map<String, Object>> variants = listOfMaps(product.get("variants"));
        if (!variants.isEmpty()) {
            List<Map<String, Object>> configurableOptions = listOfMaps(product.get("configurable_options"));
            List<String> optionPaths = new ArrayList<>();
            for (Map<String, Object> option : configurableOptions) {
                OptionDefinitionWriter.OptionInput input = OptionDefinitionWriter.fromMagento(option);
                if (input.isEmpty()) {
                    continue;
                }
                optionPaths.add(optionWriter.ensureOption(input));
            }
            optionWriter.writeProductReferences(fragmentResource, optionPaths);
            Set<String> seenVariationNames = new LinkedHashSet<>();
            for (Map<String, Object> variant : variants) {
                List<Map<String, Object>> attributes = listOfMaps(variant.get("attributes"));
                String variationName = variationName(attributes);
                if (!seenVariationNames.add(variationName)) {
                    LOG.warning(() -> "Duplicate generated variation " + variationName + " for product " + product.get("sku"));
                    continue;
                }
                Map<String, Object> variantProduct = map(variant.get("product"));

                // Manifest-driven write of every variant-scoped attribute.
                writeManifestVariantAttributes(fragmentResource, variationName, variantProduct, manifest);

                List<String> variationImages = importImages(
                        resolver,
                        parentFolder.getPath(),
                        nodeName + "_" + variationName,
                        listOfMaps(variantProduct.get("media_gallery")),
                        map(variantProduct.get("image")),
                        map(variantProduct.get("thumbnail")),
                        headers
                );
                AemContentFragmentSupport.writeVariationTyped(fragmentResource, variationName, variationName, "image", variationImages);
            }
        }
    }

    /** Codes the importer writes outside the manifest loop (image needs asset download). */
    private static final Set<String> SCAFFOLD_CODES = Set.of("image");

    private void writeManifestAttributes(Resource fragmentResource,
                                         Map<String, Object> product,
                                         AttributeManifest manifest) throws Exception {
        for (AttributeEntry entry : manifest.entries()) {
            if (!entry.scope().appliesToProduct()) continue;
            if (SCAFFOLD_CODES.contains(entry.code())) continue;
            Object raw = readMagentoAttribute(product, entry);
            if (raw == null) continue;
            writeManifestAttributeValue(fragmentResource, entry, raw, null);
        }
    }

    private void writeManifestVariantAttributes(Resource fragmentResource,
                                                String variationName,
                                                Map<String, Object> variantProduct,
                                                AttributeManifest manifest) throws Exception {
        for (AttributeEntry entry : manifest.entries()) {
            if (!entry.scope().appliesToVariant()) continue;
            if (SCAFFOLD_CODES.contains(entry.code())) continue;
            Object raw = readMagentoAttribute(variantProduct, entry);
            if (raw == null) continue;
            writeManifestAttributeValue(fragmentResource, entry, raw, variationName);
        }
    }

    private Object readMagentoAttribute(Map<String, Object> source, AttributeEntry entry) {
        String code = entry.code();
        if (entry.type() == NormalizedType.PRICE) {
            Map<String, Object> priceRange = map(source.get("price_range"));
            Map<String, Object> minimumPrice = map(priceRange.get("minimum_price"));
            Map<String, Object> regularPrice = map(minimumPrice.get("regular_price"));
            if (!regularPrice.isEmpty()) return regularPrice.get("value");
            Map<String, Object> finalPrice = map(minimumPrice.get("final_price"));
            if (!finalPrice.isEmpty()) return finalPrice.get("value");
            return source.get(code);
        }
        Object raw = source.get(code);
        // Magento exposes some TEXT fields (description, short_description, ...) as { html: "..." } objects.
        if (raw instanceof Map<?, ?> nested && nested.containsKey("html")) {
            return ((Map<?, ?>) nested).get("html");
        }
        return raw;
    }

    private void writeManifestAttributeValue(Resource fragmentResource,
                                             AttributeEntry entry,
                                             Object raw,
                                             String variationName) throws Exception {
        switch (entry.type()) {
            case STRING, SELECT, DATE, IMAGE_URL -> {
                String value = stringValue(raw);
                if (variationName == null) {
                    AemContentFragmentSupport.writeText(fragmentResource, entry.code(), value, "text/plain");
                } else {
                    AemContentFragmentSupport.writeVariationText(fragmentResource, variationName, variationName,
                            entry.code(), value, "text/plain");
                }
            }
            case TEXT -> {
                String value = stringValue(raw);
                if (variationName == null) {
                    AemContentFragmentSupport.writeText(fragmentResource, entry.code(), value, "text/html");
                } else {
                    AemContentFragmentSupport.writeVariationText(fragmentResource, variationName, variationName,
                            entry.code(), value, "text/html");
                }
            }
            case INT -> {
                long value = (long) doubleValue(raw, 0.0d);
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragmentResource, entry.code(), value);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragmentResource, variationName, variationName,
                            entry.code(), value);
                }
            }
            case FLOAT, PRICE -> {
                double value = doubleValue(raw, 0.0d);
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragmentResource, entry.code(), value);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragmentResource, variationName, variationName,
                            entry.code(), value);
                }
            }
            case BOOLEAN -> {
                boolean value = raw instanceof Boolean b ? b : Boolean.parseBoolean(stringValue(raw));
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragmentResource, entry.code(), value);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragmentResource, variationName, variationName,
                            entry.code(), value);
                }
            }
            case MULTISELECT -> {
                List<String> values = new ArrayList<>();
                if (raw instanceof List<?> list) {
                    for (Object o : list) {
                        if (o != null) values.add(o.toString());
                    }
                } else if (raw != null) {
                    values.add(raw.toString());
                }
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragmentResource, entry.code(), values);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragmentResource, variationName, variationName,
                            entry.code(), values);
                }
            }
        }
    }

    private List<String> importImages(ResourceResolver resolver,
                                      String parentFolderPath,
                                      String prefix,
                                      List<Map<String, Object>> mediaGallery,
                                      Map<String, Object> image,
                                      Map<String, Object> thumbnail,
                                      Map<String, String> headers) throws Exception {
        List<String> sources = new ArrayList<>();
        for (Map<String, Object> galleryItem : mediaGallery) {
            String url = stringValue(galleryItem.get("url"));
            if (!isPlaceholder(url) && !url.isBlank()) {
                sources.add(url);
            }
        }
        if (sources.isEmpty()) {
            String imageUrl = stringValue(image.get("url"));
            if (!isPlaceholder(imageUrl) && !imageUrl.isBlank()) {
                sources.add(imageUrl);
            }
        }
        if (sources.isEmpty()) {
            String thumbnailUrl = stringValue(thumbnail.get("url"));
            if (!isPlaceholder(thumbnailUrl) && !thumbnailUrl.isBlank()) {
                sources.add(thumbnailUrl);
            }
        }

        List<String> assetPaths = new ArrayList<>();
        int index = 1;
        for (String source : sources) {
            DownloadedAsset asset = downloadAsset(resolveSourceUrl(source, headers), headers);
            String assetPath = parentFolderPath + "/" + prefix + "_img_" + index++ + "." + asset.extension();
            AemContentFragmentSupport.createOrUpdateAsset(resolver, assetPath, asset.bytes(), asset.mimeType());
            assetPaths.add(assetPath);
        }
        return assetPaths;
    }

    private String firstNonBlankCategoryUid(String endpoint, Map<String, String> headers) throws Exception {
        Map<String, Object> response = postGraphql(endpoint, "{ categoryList { uid } }", Map.of(), headers, null);
        for (Map<String, Object> category : listOfMaps(map(response.get("data")).get("categoryList"))) {
            String uid = stringValue(category.get("uid"));
            if (!uid.isBlank()) {
                return uid;
            }
        }
        return "";
    }

    private Map<String, Object> postGraphql(String endpoint,
                                            String query,
                                            Map<String, Object> variables,
                                            Map<String, String> headers,
                                            String searchOverride) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("variables", variables);
        if (searchOverride != null && variables instanceof Map<?, ?> map) {
            ((Map<String, Object>) payload.get("variables")).putIfAbsent("search", searchOverride);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonSupport.toJson(payload)));
        headers.forEach(builder::header);
        HttpResponse<String> response = sendWithRedirects(builder.build(), 0);
        Map<String, Object> parsed = JsonSupport.parseMap(response.body());
        if (parsed == null) {
            return Map.of();
        }
        Map<String, Object> data = map(parsed.get("data"));
        Map<String, Object> flattened = new LinkedHashMap<>();
        flattened.put("data", data);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            flattened.put("data." + entry.getKey(), entry.getValue());
        }
        return flattened;
    }

    private HttpResponse<String> sendWithRedirects(HttpRequest request, int redirects) throws Exception {
        if (redirects > 5) {
            throw new IllegalStateException("Too many redirects");
        }
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            String location = response.headers().firstValue("Location").orElse(null);
            if (location == null) {
                return response;
            }
            URI redirect = request.uri().resolve(location);
            HttpRequest redirected = HttpRequest.newBuilder(redirect)
                    .header("Content-Type", "application/json")
                    .POST(request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                    .build();
            return sendWithRedirects(redirected, redirects + 1);
        }
        return response;
    }

    private Map<String, String> resolveParameters(SlingHttpServletRequest request) throws IOException {
        Map<String, String> resolved = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0 && !isBlank(values[0])) {
                resolved.put(key, values[0]);
            }
        });

        if (!isFormRequest(request)) {
            String body = readBody(request);
            if (!body.isBlank()) {
                Map<String, Object> json = parseJson(body);
                if (!json.isEmpty()) {
                    json.forEach((key, value) -> mergeParameter(resolved, key, value));
                    map(json.get("headers")).forEach((key, value) -> mergeParameter(resolved, "headers." + key, value));
                    map(json.get("graphqlHeaders")).forEach((key, value) -> mergeParameter(resolved, "headers." + key, value));
                } else {
                    for (String line : body.split("\\R")) {
                        int separator = line.indexOf('=');
                        if (separator > 0) {
                            resolved.putIfAbsent(line.substring(0, separator), line.substring(separator + 1));
                        }
                    }
                }
            }
        }
        return resolved;
    }

    private void mergeParameter(Map<String, String> resolved, String key, Object value) {
        if (value != null && !resolved.containsKey(key)) {
            resolved.put(key, value.toString());
        }
    }

    private Map<String, String> resolveHeaders(Map<String, String> parameters) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("header.")) {
                headers.put(key.substring("header.".length()), entry.getValue());
            } else if (key.startsWith("headers.")) {
                headers.put(key.substring("headers.".length()), entry.getValue());
            } else if (key.matches("[a-z0-9-]+") && !"endpoint".equals(key) && !"root".equals(key) && !"category".equals(key)) {
                headers.put(key, entry.getValue());
            }
        }
        return headers;
    }

    private boolean isFormRequest(SlingHttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && (contentType.startsWith("application/x-www-form-urlencoded")
                || contentType.startsWith("multipart/form-data"));
    }

    private String readBody(SlingHttpServletRequest request) throws IOException {
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

    private Map<String, Object> parseJson(String body) {
        return JsonSupport.parseMap(body);
    }

    private String categoryTreeQuery(int depth) {
        String fields = "uid name description url_key url_path image";
        String children = fields;
        for (int index = 0; index < depth; index++) {
            children += " children { " + fields;
        }
        children += " }".repeat(depth);
        return "{ categoryList { " + children + " } }";
    }


    String canonicalCategoryPath(Map<String, Object> product) {
        String deepest = "";
        for (Map<String, Object> category : listOfMaps(product.get("categories"))) {
            String path = categoryPathFor(category);
            if (segmentCount(path) > segmentCount(deepest)) {
                deepest = path;
            }
        }
        return deepest;
    }

    Set<String> assignedCategoryPaths(Map<String, Object> product) {
        Set<String> paths = new LinkedHashSet<>();
        for (Map<String, Object> category : listOfMaps(product.get("categories"))) {
            String path = categoryPathFor(category);
            if (!path.isBlank()) {
                paths.add(path);
            }
        }
        return paths;
    }

    String categoryPathFor(Map<String, Object> category) {
        String path = normalizeCategoryPath(stringValue(category.get("url_path")));
        if (path.isBlank()) {
            List<String> segments = new ArrayList<>();
            for (Map<String, Object> breadcrumb : listOfMaps(category.get("breadcrumbs"))) {
                String breadcrumbPath = normalizeCategoryPath(stringValue(breadcrumb.get("category_url_path")));
                if (!breadcrumbPath.isBlank()) {
                    segments.add(pathLeaf(breadcrumbPath));
                }
            }
            String last = normalizeCategoryPath(stringValue(category.get("url_key")));
            if (last.isBlank()) {
                last = normalizeCategoryPath(stringValue(category.get("uid")));
            }
            if (!last.isBlank()) {
                segments.add(last);
            }
            path = String.join("/", segments);
        }
        return path;
    }

    private int segmentCount(String path) {
        if (path == null || path.isBlank()) {
            return 0;
        }
        return path.split("/").length;
    }

    private String normalizeCategoryPath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replace("+", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String productNodeName(Map<String, Object> product) {
        String urlKey = stringValue(product.get("url_key"));
        if (!urlKey.isBlank()) {
            return urlKey;
        }
        String sku = stringValue(product.get("sku"));
        if (!sku.isBlank()) {
            return pathLeaf(sku);
        }
        return stringValue(product.get("name"));
    }

    private String pathLeaf(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path;
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private String variationName(List<Map<String, Object>> attributes) {
        if (attributes.isEmpty()) {
            return "var-default";
        }
        List<String> parts = new ArrayList<>();
        parts.add("var");
        for (Map<String, Object> attribute : attributes) {
            parts.add(AemRepositorySupport.escapeNodeName(stringValue(attribute.get("code"))));
            parts.add(String.valueOf(intValue(attribute.get("value_index"), 0)));
        }
        return String.join("-", parts);
    }

    private String resolveSourceUrl(String rawUrl, Map<String, String> headers) {
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl;
        }
        String template = headers.get("asset-url-template");
        if (template == null || template.isBlank()) {
            return rawUrl;
        }
        return template.replace("{{asset-id}}", rawUrl)
                .replace("{asset-id}", rawUrl)
                .replace("%7B%7Basset-id%7D%7D", rawUrl)
                .replace("%7Basset-id%7D", rawUrl);
    }

    private boolean isPlaceholder(String url) {
        return url.contains("/catalog/images/product/placeholder/");
    }

    private DownloadedAsset downloadAsset(String sourceUrl, Map<String, String> headers) throws Exception {
        URLConnection connection = new URL(sourceUrl).openConnection();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            inputStream.transferTo(output);
            String mimeType = connection.getContentType();
            String extension = extensionForMimeType(mimeType, sourceUrl);
            return new DownloadedAsset(output.toByteArray(), mimeType == null ? "application/octet-stream" : mimeType, extension);
        }
    }

    private String extensionForMimeType(String mimeType, String sourceUrl) {
        if (mimeType == null) {
            int dot = sourceUrl.lastIndexOf('.');
            return dot >= 0 ? sourceUrl.substring(dot + 1) : "bin";
        }
        if (mimeType.contains("jpeg")) {
            return "jpeg";
        }
        if (mimeType.contains("png")) {
            return "png";
        }
        if (mimeType.contains("gif")) {
            return "gif";
        }
        if (mimeType.contains("webp")) {
            return "webp";
        }
        return "bin";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object value) {
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return List.of();
    }

    private Map<String, Object> map(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(key, value);
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        // JSON numbers arrive as Double, so integral attribute values (e.g. Magento
        // select option indices like 41) would stringify as "41.0". Render whole
        // numbers without the trailing ".0" so select/option values stay clean.
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (!Double.isInfinite(d) && !Double.isNaN(d) && d == Math.rint(d)) {
                return Long.toString((long) d);
            }
        }
        return value.toString();
    }

    private String stringValueOrFallback(Object value, String fallback) {
        String string = stringValue(value);
        return string.isBlank() ? fallback : string;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double doubleValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private record DownloadedAsset(byte[] bytes, String mimeType, String extension) {
    }
}

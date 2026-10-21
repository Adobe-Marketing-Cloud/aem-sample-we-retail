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
import com.adobe.cq.commerce.celadon.aem.attribute.manifest.ReadyFlag;
import com.adobe.cq.commerce.celadon.aem.attribute.model.ProductModelGenerator;
import com.adobe.cq.commerce.celadon.aem.attribute.source.DiscoveryHints;
import com.adobe.cq.commerce.celadon.aem.attribute.source.LegacyDiscovery;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;

@Component(
        service = javax.servlet.Servlet.class,
        property = {
                "sling.servlet.paths=/bin/celadon/legacy-import",
                "sling.servlet.methods=POST"
        }
)
public class CeladonLegacyImportServlet extends SlingAllMethodsServlet {
    private static final Logger LOG = Logger.getLogger(CeladonLegacyImportServlet.class.getName());
    private final ManifestWriter manifestWriter = new ManifestWriter();
    private final ProductModelGenerator productModelGenerator = new ProductModelGenerator();

    /** Codes the importer writes outside the manifest loop (image needs asset download). */
    private static final java.util.Set<String> SCAFFOLD_CODES = java.util.Set.of("image");

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setStatus(SlingHttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().write("POST only");
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String sourceUrl = request.getParameter("sourceUrl");
        String targetCatalog = request.getParameter("targetCatalog");
        String sourceAuthorization = firstNonBlank(
                request.getParameter("sourceAuthorization"),
                request.getHeader("X-Celadon-Source-Authorization")
        );
        LegacyTagCategories.CategoryMode categoryMode =
                LegacyTagCategories.CategoryMode.fromParam(request.getParameter("categoryMode"));
        String catalogBlueprintPath = request.getParameter("catalogBlueprintPath");
        // Optional (BLUEPRINT only): authored product-page tree carrying editorial slugs via
        // jcr:content/cq:productMaster. When present, harvested slugs replace SKU-based CF node names.
        String productPageTreePath = request.getParameter("productPageTreePath");
        // Optional: also remove products the source no longer has (duplicates are always removed).
        boolean prune = Boolean.parseBoolean(request.getParameter("prune"));
        if (isBlank(sourceUrl) || isBlank(targetCatalog)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required parameters: sourceUrl, targetCatalog");
            return;
        }
        if (categoryMode == LegacyTagCategories.CategoryMode.BLUEPRINT && isBlank(catalogBlueprintPath)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing required parameter for BLUEPRINT mode: catalogBlueprintPath");
            return;
        }
        try {
            importLegacyCatalog(request.getResourceResolver(), sourceUrl, targetCatalog, sourceAuthorization,
                    categoryMode, catalogBlueprintPath, productPageTreePath, prune);
            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getWriter().write("Import completed successfully");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Legacy import failed", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Import failed: " + e.getMessage());
        }
    }

    private void importLegacyCatalog(ResourceResolver resolver,
                                     String sourceUrl,
                                     String targetCatalog,
                                     String sourceAuthorization,
                                     LegacyTagCategories.CategoryMode mode,
                                     String catalogBlueprintPath,
                                     String productPageTreePath,
                                     boolean prune) throws Exception {
        Resource catalogRoot = AemRepositorySupport.ensureOrderedFolder(resolver, "/content/dam/celadon", AemRepositorySupport.CELADON_ROOT_TITLE);
        Resource targetRoot = AemRepositorySupport.ensureOrderedFolder(catalogRoot.getResourceResolver(),
                catalogRoot.getPath() + "/" + AemRepositorySupport.escapeNodeName(targetCatalog),
                targetCatalog);
        resolver.commit();

        // Step 1: clear ready flag.
        String catalog = AemRepositorySupport.escapeNodeName(targetCatalog);
        ReadyFlag.set(resolver, catalog, false);
        LOG.info(() -> "celadon: legacy import - catalog " + catalog + " marked not-ready");
        LOG.info(() -> "celadon: legacy import - category mode " + mode);

        // Step 2: pass-1 discovery via .infinity.json.
        DiscoveryHints hints = DiscoveryHints.read(targetRoot);
        LegacyDiscovery.DiscoveryResult discovery = LegacyDiscovery.scan(catalog, sourceUrl, sourceAuthorization, hints);
        AttributeManifest manifest = discovery.manifest();
        for (String code : discovery.conflicts()) {
            LOG.warning(() -> "celadon: legacy discovery JCR-type conflict for attribute " + code + " in catalog " + catalog);
        }
        LOG.info(() -> "celadon: legacy discovery yielded " + manifest.entries().size() + " attributes for catalog " + catalog);

        // Step 3 + 4: ensure attribute model, write manifest, regenerate product model.
        AemRepositorySupport.ensureAttributeModel(resolver, catalog);
        manifestWriter.write(resolver, manifest);
        productModelGenerator.regenerate(resolver, catalog, manifest);
        AemRepositorySupport.ensureOptionDefinitionModel(resolver, catalog);
        LOG.info(() -> "celadon: legacy - manifest + product model regenerated for catalog " + catalog);

        Resource modelResource = Objects.requireNonNull(
                resolver.getResource("/conf/" + catalog + "/settings/dam/cfm/models/product"),
                "Product CF model not available under /conf");
        Resource optionDefinitionModel = Objects.requireNonNull(
                AemRepositorySupport.optionDefinitionModel(resolver, catalog),
                "Option Definition model not available under /conf");
        resolver.commit();

        // Step 5: drop existing products under this catalog before reimporting.
        Resource productsRoot = resolver.getResource(targetRoot.getPath() + "/products");
        if (productsRoot != null) {
            resolver.delete(productsRoot);
            resolver.commit();
            LOG.info(() -> "celadon: legacy - dropped existing products under " + targetRoot.getPath() + "/products");
        }

        OptionDefinitionWriter optionWriter = new OptionDefinitionWriter(resolver, targetRoot, optionDefinitionModel);

        Map<String, Object> source = fetchJson(sourceUrl + ".infinity.json", sourceAuthorization);

        // Step 5b: folders-first pre-pass — materialise every derived category before products write
        // their additionalCategories (both authoring and the read path require referenced folders).
        LegacyBlueprintCategories blueprint = null;
        LegacyProductSlugs productSlugs = null;
        if (mode == LegacyTagCategories.CategoryMode.BLUEPRINT) {
            String blueprintUrl = taxonomyHost(sourceUrl) + catalogBlueprintPath + ".infinity.json";
            Map<String, Object> blueprintJson = fetchJson(blueprintUrl, sourceAuthorization);
            blueprint = LegacyBlueprintCategories.parse(blueprintJson);
            // Harvest the authored editorial page tree once: the same QueryBuilder result yields both the
            // product slugs (identifier -> slug, so CF node names become human page slugs rather than SKU
            // codes) and the editorial section titles (path -> title). Editorial titles override the
            // blueprint's because the live page tree reflects the storefront's actual human-maintained
            // labels (e.g. "Men"/"Women") rather than the blueprint's merchandising form ("Men's"/"Women's"),
            // which may lag. Harvested before folder creation so the override applies as folders are written.
            LegacyPageTitles pageTitles = LegacyPageTitles.empty();
            if (!isBlank(productPageTreePath)) {
                Map<String, Object> pageTreeJson = fetchJson(pageTreeQueryUrl(sourceUrl, productPageTreePath), sourceAuthorization);
                productSlugs = LegacyProductSlugs.parse(pageTreeJson);
                pageTitles = LegacyPageTitles.parse(pageTreeJson, productPageTreePath);
                int slugCount = productSlugs.size();
                int titleCount = pageTitles.size();
                LOG.info(() -> "celadon: legacy - harvested " + slugCount + " authored product slugs and "
                        + titleCount + " editorial section titles from " + productPageTreePath);
            }
            ensureBlueprintFolders(resolver, targetRoot, blueprint, pageTitles);
            resolver.commit();
            int sectionCount = blueprint.sections().size();
            LOG.info(() -> "celadon: legacy - pre-pass ensured " + sectionCount
                    + " blueprint sections from " + catalogBlueprintPath);
        } else if (mode != LegacyTagCategories.CategoryMode.FOLDER) {
            Map<String, String> titleCache = new HashMap<>();
            Set<String> rawTagIds = collectTagIds(source);
            ensureTagFolders(resolver, targetRoot, rawTagIds, titleCache, sourceUrl, sourceAuthorization);
            resolver.commit();
            LOG.info(() -> "celadon: legacy - pre-pass ensured " + rawTagIds.size() + " tag-derived category branches");
        }

        // Catalog-wide guard so every product CF node name is unique within the catalog, regardless of
        // mode (the slug path can collide; SKU-keyed names are already unique so this is a safety net).
        Set<String> usedProductNodeNames = new LinkedHashSet<>();
        Map<String, String> importedProducts = new LinkedHashMap<>();
        processLegacyNode(resolver, modelResource, optionWriter, targetRoot, targetRoot, "", source, sourceUrl, sourceAuthorization, manifest, mode, blueprint, productSlugs, usedProductNodeNames, importedProducts);
        resolver.commit();
        removeStaleProducts(resolver, targetRoot, modelResource.getPath(), importedProducts, prune);
        resolver.commit();

        // Step 7: write option definitions for manifest SELECT/MULTISELECT entries.
        writeManifestOptionDefinitions(optionWriter, manifest, discovery);
        resolver.commit();

        // Step 8: mark the catalog ready.
        ReadyFlag.set(resolver, catalog, true);
        LOG.info(() -> "celadon: legacy - catalog " + catalog + " marked ready");
    }

    /**
     * Removes the product fragments this import did not write, with their images: always when
     * the import wrote the same SKU elsewhere (the product moved or was renamed), and for SKUs the
     * source no longer has only when {@code prune} is set.
     */
    private void removeStaleProducts(ResourceResolver resolver,
                                     Resource targetRoot,
                                     String productModelPath,
                                     Map<String, String> importedProducts,
                                     boolean prune) throws Exception {
        Set<String> importedSkus = new HashSet<>(importedProducts.values());
        List<Resource> productFragments = new ArrayList<>();
        collectProductFragments(targetRoot, productModelPath, true, productFragments);
        int removed = 0;
        List<String> kept = new ArrayList<>();
        for (Resource fragment : productFragments) {
            if (importedProducts.containsKey(fragment.getPath())) {
                continue;
            }
            Resource master = fragment.getChild("jcr:content/data/master");
            String sku = master != null ? master.getValueMap().get("sku", "") : "";
            if (!LegacyProductStructure.isStaleProduct(sku, importedSkus, prune)) {
                kept.add(fragment.getPath());
                continue;
            }
            Resource parent = fragment.getParent();
            List<Resource> images = new ArrayList<>();
            for (Resource sibling : parent.getChildren()) {
                if (LegacyProductStructure.isProductImage(sibling.getName(), fragment.getName())) {
                    images.add(sibling);
                }
            }
            String path = fragment.getPath();
            for (Resource image : images) {
                resolver.delete(image);
            }
            resolver.delete(fragment);
            removed++;
            LOG.info(() -> "celadon: legacy import - removed stale product " + path + " (sku " + sku + ")");
        }
        int removedCount = removed;
        LOG.info(() -> "celadon: legacy import - removed " + removedCount + " stale product(s); kept " + kept.size()
                + " product(s) the source does not have" + (kept.isEmpty() ? "" : " (import with prune=true to remove them): " + kept));
    }

    private static void collectProductFragments(Resource node, String productModelPath, boolean isRoot, List<Resource> fragments) {
        for (Resource child : node.getChildren()) {
            String name = child.getName();
            if (name.startsWith("jcr:") || (isRoot && (name.equals("_manifest") || name.equals("_options")))) {
                continue;
            }
            Resource data = child.getChild("jcr:content/data");
            if (data != null) {
                if (productModelPath.equals(data.getValueMap().get("cq:model", String.class))) {
                    fragments.add(child);
                }
                continue;
            }
            collectProductFragments(child, productModelPath, false, fragments);
        }
    }

    private void writeManifestOptionDefinitions(OptionDefinitionWriter optionWriter,
                                                AttributeManifest manifest,
                                                LegacyDiscovery.DiscoveryResult discovery) throws Exception {
        for (AttributeEntry entry : manifest.entries()) {
            if (entry.type() != NormalizedType.SELECT && entry.type() != NormalizedType.MULTISELECT) {
                continue;
            }
            Set<String> values = discovery.options().get(entry.code());
            if (values == null || values.isEmpty()) {
                continue;
            }
            optionWriter.ensureOptionFromManifestValues(entry, values);
        }
    }

    private void processLegacyNode(ResourceResolver resolver,
                                   Resource modelResource,
                                   OptionDefinitionWriter optionWriter,
                                   Resource parent,
                                   Resource targetRoot,
                                   String sourceFolderPath,
                                   Map<String, Object> node,
                                   String sourceUrl,
                                   String sourceAuthorization,
                                   AttributeManifest manifest,
                                   LegacyTagCategories.CategoryMode mode,
                                   LegacyBlueprintCategories blueprint,
                                   LegacyProductSlugs productSlugs,
                                   Set<String> usedProductNodeNames,
                                   Map<String, String> importedProducts) throws Exception {
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = entry.getKey();
            if (skipLegacyKey(key)) {
                continue;
            }
            Map<String, Object> child = map(entry.getValue());
            if (child.isEmpty()) {
                continue;
            }
            String commerceType = stringValue(child.get("cq:commerceType"));
            if ("product".equals(commerceType)) {
                importLegacyProduct(resolver, modelResource, optionWriter, parent, targetRoot, sourceFolderPath, key, child, sourceUrl, sourceAuthorization, manifest, mode, blueprint, productSlugs, usedProductNodeNames, importedProducts);
            } else if ("variant".equals(commerceType)) {
                continue;
            } else if (mode == LegacyTagCategories.CategoryMode.BLUEPRINT) {
                // BLUEPRINT mode: the authored pre-pass is the sole source of category folders. Descend
                // the source tree only to locate products; never materialise the source folder, so the
                // served tree is exactly the blueprint. The source folder path is still tracked so the
                // resolver can favor the product's original editorial home when multiple sections match.
                // Products matching no section fall back to the catalog root via the blank folderPrimary.
                String childSource = appendSegment(sourceFolderPath, AemRepositorySupport.escapeNodeName(key));
                processLegacyNode(resolver, modelResource, optionWriter, parent, targetRoot, childSource, child, sourceUrl, sourceAuthorization, manifest, mode, blueprint, productSlugs, usedProductNodeNames, importedProducts);
            } else {
                String segment = AemRepositorySupport.escapeNodeName(key);
                Resource category = AemRepositorySupport.ensureOrderedFolder(
                        resolver,
                        parent.getPath() + "/" + segment,
                        firstNonBlank(stringValue(child.get("jcr:title")), key)
                );
                String childSource = appendSegment(sourceFolderPath, segment);
                processLegacyNode(resolver, modelResource, optionWriter, category, targetRoot, childSource, child, sourceUrl, sourceAuthorization, manifest, mode, blueprint, productSlugs, usedProductNodeNames, importedProducts);
            }
        }
    }

    private void importLegacyProduct(ResourceResolver resolver,
                                     Resource modelResource,
                                     OptionDefinitionWriter optionWriter,
                                     Resource categoryResource,
                                     Resource targetRoot,
                                     String sourceFolderPath,
                                     String productKey,
                                     Map<String, Object> product,
                                     String sourceUrl,
                                     String sourceAuthorization,
                                     AttributeManifest manifest,
                                     LegacyTagCategories.CategoryMode mode,
                                     LegacyBlueprintCategories blueprint,
                                     LegacyProductSlugs productSlugs,
                                     Set<String> usedProductNodeNames,
                                     Map<String, String> importedProducts) throws Exception {
        String productSku = legacyBaseSku(product, productKey);
        String title = firstNonBlank(stringValue(product.get("jcr:title")), productKey);

        // Resolve category placement for the selected mode. FOLDER yields the folder parent and an empty
        // additional set, keeping this path byte-identical to before tag support. BLUEPRINT defers to the
        // authored blueprint resolver; all other modes use the data-only LegacyTagCategories resolver.
        // Computed before the slug because BLUEPRINT slug selection aligns candidate pages to the primary.
        String folderPrimary = relativeToCatalog(targetRoot, categoryResource);
        LegacyTagCategories.CategoryResolution resolution =
                (mode == LegacyTagCategories.CategoryMode.BLUEPRINT && blueprint != null)
                        ? blueprint.resolve(folderPrimary, sourceFolderPath, product)
                        : LegacyTagCategories.resolve(mode, folderPrimary, product);

        // BLUEPRINT with a harvested slug map names the CF after the authored editorial slug; every other
        // path keeps the SKU-derived node name. The catalog-wide guard then enforces uniqueness for all
        // modes (slug collisions get the SKU appended, then a numeric suffix as a last resort). When a
        // product is referenced by several authored pages with differing slugs, the canonical page is
        // chosen by primary-category alignment, then page-title match, then source order.
        String slug = (mode == LegacyTagCategories.CategoryMode.BLUEPRINT && productSlugs != null)
                ? productSlugs.slugFor(productKey, resolution.primary(), stringValue(product.get("jcr:title")),
                        AemRepositorySupport::escapeNodeName)
                : null;
        String baseName = (slug != null && !slug.isBlank())
                ? AemRepositorySupport.escapeNodeName(slug)
                : AemRepositorySupport.escapeNodeName(productKey);
        String nodeName = ensureUniqueNodeName(baseName, productSku, usedProductNodeNames);
        // TAG and BLUEPRINT place the CF under the resolved primary section; FOLDER/HYBRID keep it
        // under its physical folder parent. In BLUEPRINT mode categoryResource is the catalog root
        // (no source folders are created), so a product matching no section lands at the root.
        Resource cfParent = categoryResource;
        if ((mode == LegacyTagCategories.CategoryMode.TAG || mode == LegacyTagCategories.CategoryMode.BLUEPRINT)
                && !resolution.primary().isBlank()) {
            Resource primaryParent = resolver.getResource(targetRoot.getPath() + "/" + resolution.primary());
            if (primaryParent != null) {
                cfParent = primaryParent;
            }
        }
        Resource fragment = AemContentFragmentSupport.ensureFragment(resolver, modelResource, cfParent, nodeName, title);
        importedProducts.put(fragment.getPath(), productSku);
        // Surface the resolved sku into the source map so the manifest writer below picks it up.
        product.putIfAbsent("sku", productSku);

        // Manifest-driven write of every product-scoped attribute (universal + custom).
        writeManifestAttributesLegacy(fragment, product, null, manifest);

        // Persist tag-derived membership (dual write: CF element + master node). Skipped when empty so
        // FOLDER mode introduces no new writes.
        if (!resolution.additionalCategories().isEmpty()) {
            writeLegacyAdditionalCategories(fragment, resolution.additionalCategories());
        }

        // Image is the only field that needs the asset-import pipeline; keep it out of the manifest loop.
        List<String> imagePaths = importImages(resolver, product, cfParent.getPath() + "/" + nodeName,
                sourceUrl, sourceAuthorization);
        AemContentFragmentSupport.writeTyped(fragment, "image", imagePaths);

        // Only the axes the master declares are configurable; a product declaring none imports as a
        // simple product because its variant children are not selectable in the classic storefront.
        List<String> variantAxes = LegacyProductStructure.declaredVariantAxes(product);
        List<Map<String, Object>> leafVariants = new ArrayList<>();
        if (!variantAxes.isEmpty()) {
            collectLeafVariants(product, AemRepositorySupport.map(
                    "_resolvedSkuBase", productSku,
                    "_resolvedSkuBaseOwn", true
            ), leafVariants);
        }
        List<String> optionPaths = buildManifestOptionReferences(optionWriter, leafVariants,
                LegacyProductStructure.configurableAxisEntries(manifest, variantAxes));
        optionWriter.writeProductReferences(fragment, optionPaths);
        Set<String> seenVariations = new LinkedHashSet<>();
        Set<String> seenVariantSkus = new LinkedHashSet<>();
        seenVariantSkus.add(productSku);
        for (Map<String, Object> variant : leafVariants) {
            String variationName = legacyVariantId(variant, manifest);
            if (!seenVariations.add(variationName)) {
                LOG.warning(() -> "Duplicate legacy variation " + variationName + " for product " + productKey);
                continue;
            }
            String variantSku = uniqueLegacyVariantSku(product, variant, productSku, variationName, seenVariantSkus);
            // Surface the resolved variant sku so the manifest writer picks it up.
            variant.put("sku", variantSku);

            // Manifest-driven write of every variant-scoped attribute.
            writeManifestAttributesLegacy(fragment, variant, variationName, manifest);

            List<String> variantImagePaths = importImages(resolver, variant,
                    cfParent.getPath() + "/" + nodeName + "_" + variationName, sourceUrl, sourceAuthorization);
            if (!variantImagePaths.isEmpty()) {
                AemContentFragmentSupport.writeVariationTyped(fragment, variationName, variationName, "image", variantImagePaths);
            }
        }

        // Variations from an earlier import that the source no longer has must not survive a re-import.
        List<String> staleVariations = AemContentFragmentSupport.removeVariationsExcept(fragment, seenVariations);
        if (!staleVariations.isEmpty()) {
            List<Resource> staleImages = new ArrayList<>();
            for (Resource child : cfParent.getChildren()) {
                if (LegacyProductStructure.isVariationImage(child.getName(), nodeName, staleVariations)) {
                    staleImages.add(child);
                }
            }
            for (Resource image : staleImages) {
                resolver.delete(image);
            }
            LOG.info(() -> "celadon: legacy import - removed stale variations " + staleVariations + " from product " + productKey);
        }
    }

    /**
     * Downloads every source image of a product or variant node and stores them as
     * {@code <assetPathPrefix>_img_<n>} assets, returning the asset paths in source order.
     */
    private List<String> importImages(ResourceResolver resolver,
                                      Map<String, Object> node,
                                      String assetPathPrefix,
                                      String sourceUrl,
                                      String sourceAuthorization) throws Exception {
        List<String> assetPaths = new ArrayList<>();
        for (String reference : LegacyProductStructure.imageReferences(node)) {
            String imageUrl = resolveSourceImageUrl(sourceUrl, reference);
            if (imageUrl.isBlank()) {
                continue;
            }
            DownloadedAsset asset = download(imageUrl, sourceAuthorization);
            String assetPath = assetPathPrefix + "_img_" + assetPaths.size() + "." + asset.extension();
            AemContentFragmentSupport.createOrUpdateAsset(resolver, assetPath, asset.bytes(), asset.mimeType());
            assetPaths.add(assetPath);
        }
        return assetPaths;
    }

    /** Catalog-relative path of {@code node} under {@code targetRoot} (empty when it is the root itself). */
    private static String relativeToCatalog(Resource targetRoot, Resource node) {
        String rootPath = targetRoot.getPath();
        String nodePath = node.getPath();
        if (nodePath.equals(rootPath)) {
            return "";
        }
        return nodePath.substring(rootPath.length() + 1);
    }

    /** Joins {@code segment} onto a catalog-relative folder path, tolerating a blank (root) base. */
    private static String appendSegment(String base, String segment) {
        if (base == null || base.isBlank()) {
            return segment;
        }
        return base + "/" + segment;
    }

    /**
     * Persist {@code additionalCategories} through the CF element (production / integration) AND directly onto
     * the master data node the storefront read path consumes. The CF element write is a no-op under sling-mock
     * and when the scaffold element is absent, so the master-node write keeps the stored property correct.
     */
    private void writeLegacyAdditionalCategories(Resource fragment, List<String> additional) throws Exception {
        try {
            AemContentFragmentSupport.writeTyped(fragment, "additionalCategories", additional);
        } catch (Exception e) {
            LOG.log(Level.WARNING, e,
                    () -> "writeTyped(additionalCategories) failed for " + fragment.getPath()
                            + "; relying on direct master-node write");
        }
        Resource master = fragment.getChild("jcr:content/data/master");
        if (master != null) {
            ModifiableValueMap mvm = master.adaptTo(ModifiableValueMap.class);
            if (mvm != null) {
                mvm.put("additionalCategories", additional.toArray(new String[0]));
            }
        }
    }

    /**
     * Recursively collects every distinct raw (namespaced) {@code cq:tags} id across all product
     * nodes. Raw ids are retained (rather than stripped paths) so the taxonomy title lookup in
     * {@link #ensureTagFolders} can locate {@code /content/cq:tags/<ns>/<seg...>}. Variant nodes are
     * skipped — only master-product tags drive category structure.
     */
    private Set<String> collectTagIds(Map<String, Object> node) {
        Set<String> ids = new LinkedHashSet<>();
        collectTagIds(node, ids);
        return ids;
    }

    private void collectTagIds(Map<String, Object> node, Set<String> ids) {
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = entry.getKey();
            if (skipLegacyKey(key)) {
                continue;
            }
            Map<String, Object> child = map(entry.getValue());
            if (child.isEmpty()) {
                continue;
            }
            String commerceType = stringValue(child.get("cq:commerceType"));
            if ("product".equals(commerceType)) {
                for (Object value : asIterable(child.get("cq:tags"))) {
                    String id = value == null ? "" : value.toString().trim();
                    if (!id.isBlank() && id.indexOf(':') > 0) {
                        ids.add(id);
                    }
                }
            } else if ("variant".equals(commerceType)) {
                continue;
            } else {
                collectTagIds(child, ids);
            }
        }
    }

    /**
     * Materialises the authored blueprint structure as folders under {@code targetRoot}. Sections are
     * pre-ordered (parents before children), so ensuring each section's full catalog-relative path in
     * turn applies the correct {@code jcr:title} to every intermediate node. The editorial page-tree
     * title (keyed by the same catalog-relative path) overrides the blueprint title when present, so
     * folders reflect the live storefront labels; the blueprint title is the fallback. Idempotent;
     * caller commits.
     */
    private void ensureBlueprintFolders(ResourceResolver resolver,
                                        Resource targetRoot,
                                        LegacyBlueprintCategories blueprint,
                                        LegacyPageTitles pageTitles) throws PersistenceException {
        for (LegacyBlueprintCategories.Section section : blueprint.sections()) {
            if (section.path().isBlank()) {
                continue;
            }
            String editorialTitle = pageTitles.titleFor(section.path());
            String title = (editorialTitle != null && !editorialTitle.isBlank())
                    ? editorialTitle
                    : section.title();
            AemRepositorySupport.ensureOrderedFolder(resolver,
                    targetRoot.getPath() + "/" + section.path(),
                    title);
        }
    }

    /**
     * For each raw tag id, ensures the catalog-relative folder branch ancestor-or-self under
     * {@code targetRoot}. Each segment is escaped via {@link AemRepositorySupport#escapeNodeName}
     * (so the folder paths are byte-identical to {@link LegacyTagCategories#parseTagPath}) while the
     * un-escaped segment feeds the taxonomy title lookup. Idempotent: shared ancestors are re-ensured
     * safely. Caller commits.
     */
    private void ensureTagFolders(ResourceResolver resolver,
                                  Resource targetRoot,
                                  Set<String> rawTagIds,
                                  Map<String, String> titleCache,
                                  String sourceUrl,
                                  String sourceAuthorization) throws PersistenceException {
        for (String rawId : rawTagIds) {
            int colon = rawId.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String namespace = rawId.substring(0, colon).trim();
            String remainder = rawId.substring(colon + 1).trim();
            if (namespace.isBlank() || remainder.isBlank()) {
                continue;
            }
            StringBuilder stripped = new StringBuilder();
            StringBuilder taxonomy = new StringBuilder(namespace);
            for (String segment : remainder.split("/")) {
                if (segment == null || segment.isBlank()) {
                    continue;
                }
                if (stripped.length() > 0) {
                    stripped.append('/');
                }
                stripped.append(AemRepositorySupport.escapeNodeName(segment));
                taxonomy.append('/').append(segment);
                String title = resolveTagTitle(taxonomy.toString(), segment, titleCache, sourceUrl, sourceAuthorization);
                AemRepositorySupport.ensureOrderedFolder(resolver,
                        targetRoot.getPath() + "/" + stripped,
                        title);
            }
        }
    }

    /**
     * Best-effort folder title for a single taxonomy node. Reads
     * {@code <host>/content/cq:tags/<taxonomyRelPath>.json} once and uses its {@code jcr:title};
     * caches per distinct taxonomy path and falls back to a title-cased segment. Never throws —
     * any network/parse failure degrades to the deterministic title-cased fallback.
     */
    private String resolveTagTitle(String taxonomyRelPath,
                                   String segment,
                                   Map<String, String> titleCache,
                                   String sourceUrl,
                                   String sourceAuthorization) {
        String cached = titleCache.get(taxonomyRelPath);
        if (cached != null) {
            return cached;
        }
        String title = titleCase(segment);
        try {
            String host = taxonomyHost(sourceUrl);
            if (!host.isBlank()) {
                Map<String, Object> node = fetchJson(host + "/content/cq:tags/" + taxonomyRelPath + ".json", sourceAuthorization);
                String fetched = stringValue(node.get("jcr:title"));
                if (!fetched.isBlank()) {
                    title = fetched;
                }
            }
        } catch (Exception e) {
            LOG.fine(() -> "celadon: tag title lookup failed for " + taxonomyRelPath + "; using fallback");
        }
        titleCache.put(taxonomyRelPath, title);
        return title;
    }

    private String taxonomyHost(String sourceUrl) {
        try {
            URI base = URI.create(sourceUrl);
            if (base.getScheme() == null || base.getHost() == null) {
                return "";
            }
            return base.getScheme() + "://" + base.getHost() + (base.getPort() > 0 ? ":" + base.getPort() : "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Build the source QueryBuilder URL that returns every authored page under the given tree with its
     * {@code jcr:path}, {@code jcr:content/cq:productMaster} (the commerce identifier, present only on
     * product pages) and {@code jcr:content/jcr:title} (the editorial display title). One unbounded call
     * backs both {@link LegacyProductSlugs} (product pages -> slug) and {@link LegacyPageTitles} (section
     * pages -> editorial title); the {@code cq:productMaster} presence partitions the two.
     */
    private String pageTreeQueryUrl(String sourceUrl, String productPageTreePath) {
        return taxonomyHost(sourceUrl) + "/bin/querybuilder.json"
                + "?path=" + productPageTreePath
                + "&type=cq:Page"
                + "&p.limit=-1"
                + "&p.hits=selective"
                + "&p.properties=jcr:path%20jcr:content/cq:productMaster%20jcr:content/jcr:title";
    }

    private String titleCase(String segment) {
        String normalized = segment.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return segment;
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == ' ') {
                builder.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                builder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private Iterable<?> asIterable(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Iterable<?> iterable) {
            return iterable;
        }
        if (raw instanceof Object[] array) {
            return List.of(array);
        }
        return List.of(raw);
    }

    /**
     * For each configurable axis entry that has at least one value among the leaf variants, ensure the
     * corresponding option definition CF exists and return the set of paths in manifest order.
     */
    private List<String> buildManifestOptionReferences(OptionDefinitionWriter optionWriter,
                                                       List<Map<String, Object>> leafVariants,
                                                       List<AttributeEntry> axisEntries) throws Exception {
        List<String> paths = new ArrayList<>();
        for (AttributeEntry entry : axisEntries) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (Map<String, Object> variant : leafVariants) {
                String value = stringValue(variant.get(entry.code()));
                if (!value.isBlank()) values.add(value);
            }
            if (values.isEmpty()) continue;
            paths.add(optionWriter.ensureOptionFromManifestValues(entry, values));
        }
        return paths;
    }

    /**
     * Writes manifest entries beyond the hardcoded universal set onto a fragment.
     * If {@code variationName} is {@code null}, writes go to the master element;
     * otherwise they go to the named variation.
     */
    private void writeManifestAttributesLegacy(Resource fragment,
                                               Map<String, Object> source,
                                               String variationName,
                                               AttributeManifest manifest) throws Exception {
        for (AttributeEntry entry : manifest.entries()) {
            boolean scopeOk = variationName == null ? entry.scope().appliesToProduct() : entry.scope().appliesToVariant();
            if (!scopeOk) continue;
            if (SCAFFOLD_CODES.contains(entry.code())) continue;
            String legacyKey = legacyAttributeKey(entry.code(), source);
            if (legacyKey == null || !source.containsKey(legacyKey)) continue;
            Object raw = source.get(legacyKey);
            if (raw == null) continue;
            writeManifestAttributeValueLegacy(fragment, entry, raw, variationName);
        }
    }

    /**
     * Maps a manifest attribute code to its legacy JCR field name where the two
     * disagree on the wire. Legacy stores titles under {@code jcr:title} and the
     * standard product description under {@code jcr:description}; catalogs that
     * keep the long-form description under {@code summary} use
     * {@code jcr:description} as the short description instead.
     */
    static String legacyAttributeKey(String code, Map<String, Object> source) {
        return switch (code) {
            case "name" -> source.containsKey("jcr:title") ? "jcr:title" : "name";
            case "description" -> source.containsKey("summary") ? "summary"
                    : source.containsKey("jcr:description") ? "jcr:description" : "description";
            case "short_description" -> source.containsKey("summary") && source.containsKey("jcr:description")
                    ? "jcr:description" : "short_description";
            default -> code;
        };
    }

    private void writeManifestAttributeValueLegacy(Resource fragment,
                                                   AttributeEntry entry,
                                                   Object raw,
                                                   String variationName) throws Exception {
        switch (entry.type()) {
            case STRING, SELECT, DATE, IMAGE_URL -> {
                String value = stringValue(raw);
                if (variationName == null) {
                    AemContentFragmentSupport.writeText(fragment, entry.code(), value, "text/plain");
                } else {
                    AemContentFragmentSupport.writeVariationText(fragment, variationName, variationName,
                            entry.code(), value, "text/plain");
                }
            }
            case TEXT -> {
                String value = stringValue(raw);
                if (variationName == null) {
                    AemContentFragmentSupport.writeText(fragment, entry.code(), value, "text/html");
                } else {
                    AemContentFragmentSupport.writeVariationText(fragment, variationName, variationName,
                            entry.code(), value, "text/html");
                }
            }
            case INT -> {
                long value = (long) doubleValue(raw, 0.0d);
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragment, entry.code(), value);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragment, variationName, variationName,
                            entry.code(), value);
                }
            }
            case FLOAT, PRICE -> {
                double value = doubleValue(raw, 0.0d);
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragment, entry.code(), value);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragment, variationName, variationName,
                            entry.code(), value);
                }
            }
            case BOOLEAN -> {
                boolean value = raw instanceof Boolean b ? b : Boolean.parseBoolean(stringValue(raw));
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragment, entry.code(), value);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragment, variationName, variationName,
                            entry.code(), value);
                }
            }
            case MULTISELECT -> {
                List<String> values = new ArrayList<>();
                if (raw instanceof List<?> list) {
                    for (Object o : list) if (o != null) values.add(o.toString());
                } else {
                    values.add(stringValue(raw));
                }
                if (variationName == null) {
                    AemContentFragmentSupport.writeTyped(fragment, entry.code(), values);
                } else {
                    AemContentFragmentSupport.writeVariationTyped(fragment, variationName, variationName,
                            entry.code(), values);
                }
            }
        }
    }

    /**
     * Manifest-driven variant identifier. Produces variant IDs from the
     * variant-scoped (or BOTH-scoped) SELECT/STRING attributes present in the
     * manifest, falling back to the variant's own tree key when no values
     * are available.
     */
    private static String legacyVariantId(Map<String, Object> leaf, AttributeManifest manifest) {
        StringBuilder builder = new StringBuilder();
        for (AttributeEntry entry : manifest.entries()) {
            if (entry.scope() != com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope.VARIANT
                    && entry.scope() != com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope.BOTH) continue;
            if (entry.type() != NormalizedType.SELECT && entry.type() != NormalizedType.STRING) continue;
            if ("sku".equals(entry.code()) || "name".equals(entry.code())) continue;
            Object raw = leaf.get(entry.code());
            if (raw == null) continue;
            String value = raw.toString();
            if (value.isBlank()) continue;
            if (builder.length() == 0) {
                builder.append("var");
            }
            builder.append('-').append(entry.code()).append('-').append(sanitizeId(value));
        }
        if (builder.length() > 0) return builder.toString();
        String variantKey = leaf.get("_variantKey") == null ? "" : leaf.get("_variantKey").toString();
        return variantKey.isBlank() ? "var-default" : "var-" + sanitizeId(variantKey);
    }

    private static String sanitizeId(String value) {
        return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private void collectLeafVariants(Map<String, Object> node,
                                     Map<String, Object> inherited,
                                     List<Map<String, Object>> leaves) {
        boolean hasChildVariant = false;
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            Map<String, Object> child = map(entry.getValue());
            if (!"variant".equals(stringValue(child.get("cq:commerceType")))) {
                continue;
            }
            hasChildVariant = true;
            Map<String, Object> merged = new LinkedHashMap<>(inherited);
            merged.putAll(child);
            merged.put("_parentResolvedSkuBase", stringValue(inherited.get("_resolvedSkuBase")));
            String ownSkuBase = firstNonBlank(
                    stringValue(child.get("sku")),
                    stringValue(child.get("identifier"))
            );
            merged.put("_resolvedSkuBase", ownSkuBase.isBlank()
                    ? stringValue(inherited.get("_resolvedSkuBase"))
                    : ownSkuBase);
            merged.put("_resolvedSkuBaseOwn", !ownSkuBase.isBlank());
            merged.put("_variantKey", entry.getKey());
            collectLeafVariants(child, merged, leaves);
        }
        if (!hasChildVariant && inherited.containsKey("_variantKey")) {
            leaves.add(new LinkedHashMap<>(inherited));
        }
    }

    private boolean skipLegacyKey(String key) {
        return key.startsWith("jcr:")
                || key.startsWith("cq:")
                || "sling:resourceType".equals(key)
                || "textIsRich".equals(key)
                || "inventory".equals(key)
                || "margin".equals(key)
                || "rating".equals(key)
                || "reviews".equals(key)
                || "features".equals(key)
                || "identifier".equals(key)
                || "price".equals(key)
                || "image".equals(key);
    }

    private String legacyBaseSku(Map<String, Object> product, String productKey) {
        return firstNonBlank(
                stringValue(product.get("sku")),
                stringValue(product.get("identifier")),
                productKey
        );
    }

    /**
     * Reserve a catalog-unique CF node name. The slug/SKU base is taken when free; on collision the SKU
     * is appended (skipped when the base already is/ends with it), then a numeric suffix is appended
     * until the name is unused. The chosen name is recorded so later products cannot reuse it.
     */
    private static String ensureUniqueNodeName(String baseName, String sku, Set<String> usedProductNodeNames) {
        String candidate = baseName;
        if (usedProductNodeNames.contains(candidate)) {
            String skuSuffix = AemRepositorySupport.escapeNodeName(sku);
            if (!skuSuffix.isBlank() && !candidate.equals(skuSuffix) && !candidate.endsWith("-" + skuSuffix)) {
                candidate = baseName + "-" + skuSuffix;
            }
            String deduped = candidate;
            int n = 2;
            while (usedProductNodeNames.contains(deduped)) {
                deduped = candidate + "-" + n++;
            }
            candidate = deduped;
        }
        usedProductNodeNames.add(candidate);
        return candidate;
    }

    private String uniqueLegacyVariantSku(Map<String, Object> product,
                                          Map<String, Object> variant,
                                          String productSku,
                                          String variationName,
                                          Set<String> seenVariantSkus) {
        String candidate = legacyVariantSku(product, variant, productSku, variationName);
        if (seenVariantSkus.add(candidate)) {
            return candidate;
        }
        String fallback = candidate + "-" + sanitizeVariationValue(variationName);
        seenVariantSkus.add(fallback);
        LOG.warning(() -> "Duplicate legacy variant sku " + candidate + " for product "
                + firstNonBlank(stringValue(product.get("identifier")), stringValue(product.get("jcr:title")), productSku)
                + "; using fallback " + fallback);
        return fallback;
    }

    private String legacyVariantSku(Map<String, Object> product,
                                    Map<String, Object> variant,
                                    String productSku,
                                    String variationName) {
        String resolvedBase = firstNonBlank(
                stringValue(variant.get("_resolvedSkuBase")),
                stringValue(product.get("identifier")),
                stringValue(product.get("sku")),
                productSku
        );
        boolean ownResolvedBase = booleanValue(variant.get("_resolvedSkuBaseOwn"));
        String parentResolvedBase = stringValue(variant.get("_parentResolvedSkuBase"));
        String size = sanitizeVariationValue(stringValue(variant.get("size")));
        if (!resolvedBase.isBlank() && !size.isBlank()) {
            if (!ownResolvedBase || resolvedBase.equals(parentResolvedBase)) {
                return resolvedBase + "-" + size;
            }
        }
        if (!resolvedBase.isBlank()) {
            return resolvedBase;
        }
        String variantKey = sanitizeVariationValue(stringValue(variant.get("_variantKey")));
        if (!variantKey.isBlank()) {
            return variantKey;
        }
        return "legacy-" + sanitizeVariationValue(variationName);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private Map<String, Object> fetchJson(String url, String authorization) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        if (!isBlank(authorization)) {
            connection.setRequestProperty("Authorization", authorization);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            Map<String, Object> parsed = JsonSupport.parseMap(reader);
            return parsed == null ? Map.of() : parsed;
        }
    }

    private String resolveSourceImageUrl(String sourceUrl, String sourcePath) {
        if (isBlank(sourcePath)) {
            return "";
        }
        String normalized = sourcePath.trim().replace(" ", "%20");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        URI base = URI.create(sourceUrl);
        if (normalized.startsWith("/")) {
            return base.getScheme() + "://" + base.getHost() + (base.getPort() > 0 ? ":" + base.getPort() : "") + normalized;
        }
        return base.resolve(normalized).toString();
    }

    private DownloadedAsset download(String sourceUrl, String authorization) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(sourceUrl).toURL().openConnection();
        if (!isBlank(authorization)) {
            connection.setRequestProperty("Authorization", authorization);
        }
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            String mimeType = connection.getContentType();
            return new DownloadedAsset(outputStream.toByteArray(), mimeType == null ? "application/octet-stream" : mimeType,
                    extensionForMimeType(mimeType, sourceUrl));
        }
    }

    private String extensionForMimeType(String mimeType, String sourceUrl) {
        if (mimeType != null && mimeType.contains("jpeg")) {
            return "jpeg";
        }
        if (mimeType != null && mimeType.contains("png")) {
            return "png";
        }
        int dot = sourceUrl.lastIndexOf('.');
        return dot >= 0 ? sourceUrl.substring(dot + 1) : "bin";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String sanitizeVariationValue(String value) {
        return value.replaceAll("[^a-zA-Z0-9]", "");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record DownloadedAsset(byte[] bytes, String mimeType, String extension) {
    }
}

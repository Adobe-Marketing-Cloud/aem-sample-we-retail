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
import com.adobe.cq.commerce.celadon.aem.attribute.manifest.ManifestWriter;
import com.adobe.cq.commerce.celadon.aem.attribute.manifest.ReadyFlag;
import com.adobe.cq.commerce.celadon.aem.attribute.model.ProductModelGenerator;
import com.adobe.cq.commerce.celadon.aem.authoring.AuthoringException;
import com.adobe.cq.commerce.celadon.aem.authoring.CatalogAuthoringService;
import com.adobe.cq.commerce.celadon.aem.authoring.CatalogInfo;
import com.adobe.cq.commerce.celadon.aem.authoring.CategoryInfo;
import com.adobe.cq.commerce.celadon.aem.authoring.ProductInput;
import com.adobe.cq.commerce.celadon.aem.authoring.VariantInput;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.Session;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Default {@link CatalogAuthoringService}. Lives in the {@code ...celadon.aem}
 * package (not a sub-package) so it can call the package-private repository and
 * Content Fragment helpers (e.g. {@code AemRepositorySupport.ensureOrderedFolder},
 * {@code AemContentFragmentSupport.writeVariationTyped}, {@code OptionDefinitionWriter}).
 */
@Component(service = CatalogAuthoringService.class)
public class CatalogAuthoringServiceImpl implements CatalogAuthoringService {

    private static final Logger LOG = Logger.getLogger(CatalogAuthoringServiceImpl.class.getName());

    static final String CELADON_ROOT = "/content/dam/celadon";
    static final String GRAPHQL_SERVLET_PID = "com.adobe.cq.commerce.celadon.aem.CeladonGraphqlServlet";
    static final String DEFAULT_CATALOG = "we-retail";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY)
    private volatile ConfigurationAdmin configurationAdmin;

    private static String catalogRoot(String catalog) {
        return CELADON_ROOT + "/" + catalog;
    }

    @Override
    public String defaultCatalog() {
        if (configurationAdmin == null) {
            return DEFAULT_CATALOG;
        }
        try {
            Configuration cfg = configurationAdmin.getConfiguration(GRAPHQL_SERVLET_PID, null);
            Dictionary<String, Object> props = cfg.getProperties();
            Object defaultCatalog = props == null ? null : props.get("defaultCatalog");
            if (defaultCatalog == null) {
                return DEFAULT_CATALOG;
            }
            String path = defaultCatalog.toString();
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        } catch (Exception e) {
            return DEFAULT_CATALOG;
        }
    }

    @Override
    public List<String> listCatalogs(ResourceResolver resolver) {
        Resource root = resolver.getResource(CELADON_ROOT);
        List<String> names = new ArrayList<>();
        if (root != null) {
            for (Resource child : root.getChildren()) {
                if (!child.getName().startsWith("jcr:")) {
                    names.add(child.getName());
                }
            }
        }
        return names;
    }

    @Override
    public boolean isCatalogReady(ResourceResolver resolver, String catalog) {
        return ReadyFlag.isReady(resolver, catalog);
    }

    @Override
    public CatalogInfo getCatalogInfo(ResourceResolver resolver, String catalog) {
        Resource root = resolver.getResource(catalogRoot(catalog));
        int[] counts = new int[2]; // [products, categories]
        if (root != null) {
            countTree(root, counts, true);
        }
        boolean isDefault = catalog.equals(defaultCatalog());
        return new CatalogInfo(catalog, isCatalogReady(resolver, catalog), counts[0], counts[1], isDefault);
    }

    private void countTree(Resource node, int[] counts, boolean isRoot) {
        for (Resource child : node.getChildren()) {
            String name = child.getName();
            if (name.startsWith("jcr:") || (isRoot && (name.equals("_manifest") || name.equals("_options")))) {
                continue;
            }
            if (isContentFragment(child)) {
                counts[0]++;
            } else if (isCategoryFolder(child)) {
                counts[1]++;
                countTree(child, counts, false);
            }
            // else: a plain dam:Asset (e.g. a co-located product image) or nt:file — neither
            // a product nor a category. Skip it and do not recurse into its content subtree.
        }
    }

    /**
     * Rewrite every product CF's additionalCategories under {@code node}: any entry equal to
     * {@code from} or prefixed by {@code from + "/"} has its {@code from} prefix replaced by {@code to}
     * (catalog-relative paths). Mirrors countTree's traversal and skips.
     */
    private void rewriteMembershipReferences(Resource node, String from, String to, boolean isRoot) throws Exception {
        for (Resource child : node.getChildren()) {
            String name = child.getName();
            if (name.startsWith("jcr:") || (isRoot && (name.equals("_manifest") || name.equals("_options")))) {
                continue;
            }
            if (isContentFragment(child)) {
                rewriteOneProductMembership(child, from, to);
            } else if (isCategoryFolder(child)) {
                rewriteMembershipReferences(child, from, to, false);
            }
        }
    }

    /** Apply the prefix rewrite to a single product CF's stored additionalCategories (writes only if something changed). */
    private void rewriteOneProductMembership(Resource fragment, String from, String to) throws Exception {
        Collection<String> current = readAdditional(fragment);
        List<String> rewritten = new ArrayList<>();
        boolean changed = false;
        for (String entry : current) {
            if (entry.equals(from)) {
                rewritten.add(to);
                changed = true;
            } else if (entry.startsWith(from + "/")) {
                rewritten.add(to + entry.substring(from.length()));   // entry.substring keeps the leading "/..."
                changed = true;
            } else {
                rewritten.add(entry);
            }
        }
        if (changed) {
            writeAdditional(fragment, rewritten);
        }
    }

    @Override
    public List<CategoryInfo> listCategories(ResourceResolver resolver, String catalog) {
        List<CategoryInfo> out = new ArrayList<>();
        Resource root = resolver.getResource(catalogRoot(catalog));
        if (root != null) {
            collectCategories(root, "", out, true);
        }
        return out;
    }

    private void collectCategories(Resource node, String basePath, List<CategoryInfo> out, boolean isRoot) {
        for (Resource child : node.getChildren()) {
            String name = child.getName();
            if (name.startsWith("jcr:") || (isRoot && (name.equals("_manifest") || name.equals("_options")))) {
                continue;
            }
            if (isCategoryFolder(child)) {
                String path = basePath.isEmpty() ? name : basePath + "/" + name;
                out.add(new CategoryInfo(path, categoryTitle(child), directProductCount(child)));
                collectCategories(child, path, out, false);
            }
        }
    }

    private static String categoryTitle(Resource folder) {
        Resource content = folder.getChild("jcr:content");
        return content != null ? content.getValueMap().get("jcr:title", folder.getName()) : folder.getName();
    }

    private static int directProductCount(Resource folder) {
        int n = 0;
        for (Resource child : folder.getChildren()) {
            if (isContentFragment(child)) {
                n++;
            }
        }
        return n;
    }

    private static boolean isContentFragment(Resource resource) {
        Resource data = resource.getChild("jcr:content/data");
        return data != null;
    }

    /**
     * A category is a folder node. Mirrors {@code AemCatalogGateway.isFolder} so the counts
     * stay consistent with the storefront read path: only {@code sling:OrderedFolder} (what
     * authoring writes) and {@code sling:Folder} (accepted for imported content) are categories.
     */
    private static boolean isCategoryFolder(Resource resource) {
        String type = resource.getValueMap().get("jcr:primaryType", String.class);
        return "sling:OrderedFolder".equals(type) || "sling:Folder".equals(type);
    }

    @Override
    public List<AttributeEntry> listAttributes(ResourceResolver resolver, String catalog) {
        return loadManifest(resolver, catalog).entries();
    }

    @Override
    public String createCatalog(ResourceResolver resolver, String catalog) throws AuthoringException {
        requireCatalogName(catalog);
        try {
            AemRepositorySupport.ensureOrderedFolder(resolver, CELADON_ROOT, AemRepositorySupport.CELADON_ROOT_TITLE);
            AemRepositorySupport.ensureOrderedFolder(resolver, catalogRoot(catalog), catalog);
            AemRepositorySupport.ensureAttributeModel(resolver, catalog);
            AemRepositorySupport.ensureOptionDefinitionModel(resolver, catalog);
            resolver.commit();
            // Seed the baseline manifest (sku/name/description/price/image) so a new catalog
            // matches real catalogs out of the box. Only when no manifest exists yet, so
            // re-running createCatalog never clobbers a customized manifest. Best-effort:
            // seeding writes Content Fragments and needs the live CF runtime, so when that is
            // unavailable (e.g. sling-mock unit tests) the catalog folders + models are still
            // created and the baseline can be added later via define_attribute.
            if (loadManifest(resolver, catalog).entries().isEmpty()) {
                try {
                    defineAttributes(resolver, catalog, baselineManifestEntries());
                } catch (AuthoringException seedingFailed) {
                    resolver.revert();
                }
            }
            return catalogRoot(catalog);
        } catch (Exception e) {
            throw new AuthoringException("createCatalog failed for '" + catalog + "': " + e.getMessage(), e);
        }
    }

    /** The baseline attributes every catalog needs to render in a CIF/Venia storefront. */
    static List<AttributeEntry> baselineManifestEntries() {
        List<AttributeEntry> entries = new ArrayList<>();
        entries.add(AttributeEntry.of("sku", "SKU", NormalizedType.STRING, AttributeScope.BOTH, true, false, 0));
        entries.add(AttributeEntry.of("name", "Name", NormalizedType.STRING, AttributeScope.PRODUCT, true, false, 1));
        entries.add(AttributeEntry.of("description", "Description", NormalizedType.TEXT, AttributeScope.PRODUCT, false, false, 2));
        entries.add(AttributeEntry.of("price", "Price", NormalizedType.PRICE, AttributeScope.PRODUCT, true, true, 3));
        entries.add(AttributeEntry.of("image", "Image", NormalizedType.IMAGE_URL, AttributeScope.PRODUCT, false, false, 4));
        return entries;
    }

    @Override
    public void defineAttributes(ResourceResolver resolver, String catalog, List<AttributeEntry> entries)
            throws AuthoringException {
        try {
            AttributeManifest manifest = new AttributeManifest(catalog, List.copyOf(entries));
            new ManifestWriter().write(resolver, manifest);
            new ProductModelGenerator().regenerate(resolver, catalog, manifest);
            resolver.commit();
        } catch (Exception e) {
            throw new AuthoringException("defineAttributes failed for '" + catalog + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void defineAttribute(ResourceResolver resolver, String catalog, AttributeEntry entry)
            throws AuthoringException {
        List<AttributeEntry> current = loadManifest(resolver, catalog).entries();
        List<AttributeEntry> merged = new ArrayList<>();
        for (AttributeEntry e : current) {
            if (!e.code().equals(entry.code())) {
                merged.add(e);
            }
        }
        merged.add(entry);
        defineAttributes(resolver, catalog, merged);
    }

    @Override
    public void removeAttribute(ResourceResolver resolver, String catalog, String code) throws AuthoringException {
        List<AttributeEntry> current = loadManifest(resolver, catalog).entries();
        List<AttributeEntry> merged = new ArrayList<>();
        boolean found = false;
        for (AttributeEntry e : current) {
            if (e.code().equals(code)) {
                found = true;
            } else {
                merged.add(e);
            }
        }
        if (!found) {
            throw new AuthoringException("attribute '" + code + "' is not in the '" + catalog + "' manifest");
        }
        defineAttributes(resolver, catalog, merged);
    }

    @Override
    public void regenerateProductModel(ResourceResolver resolver, String catalog) throws AuthoringException {
        try {
            AttributeManifest manifest = loadManifest(resolver, catalog);
            new ProductModelGenerator().regenerate(resolver, catalog, manifest);
            resolver.commit();
        } catch (Exception e) {
            throw new AuthoringException("regenerateProductModel failed for '" + catalog + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void ensureCategory(ResourceResolver resolver, String catalog, String relativePath, String title)
            throws AuthoringException {
        try {
            String acc = catalogRoot(catalog);
            String[] segments = relativePath.split("/");
            int lastIdx = -1;
            for (int i = 0; i < segments.length; i++) {
                if (!segments[i].isBlank()) {
                    lastIdx = i;
                }
            }
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].isBlank()) {
                    continue;
                }
                acc = acc + "/" + segments[i];
                String segTitle;
                if (i == lastIdx && title != null) {
                    segTitle = title; // leaf: apply the requested title
                } else {
                    // ancestor: preserve any existing title; default to the segment name when new
                    Resource jcrContent = resolver.getResource(acc + "/jcr:content");
                    String existing = jcrContent == null ? null
                            : jcrContent.getValueMap().get("jcr:title", String.class);
                    segTitle = (existing != null && !existing.isBlank()) ? existing : segments[i];
                }
                AemRepositorySupport.ensureOrderedFolder(resolver, acc, segTitle);
            }
            resolver.commit();
        } catch (Exception e) {
            throw new AuthoringException("ensureCategory failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void moveCategory(ResourceResolver resolver, String catalog, String fromRelativePath, String toRelativePath)
            throws AuthoringException {
        String fromPath = catalogRoot(catalog) + "/" + fromRelativePath;
        String toPath = catalogRoot(catalog) + "/" + toRelativePath;
        try {
            Resource from = resolver.getResource(fromPath);
            if (from == null) {
                throw new AuthoringException("source category '" + fromRelativePath + "' does not exist");
            }
            // ensure the target's parent exists
            int lastSlash = toPath.lastIndexOf('/');
            String parentPath = toPath.substring(0, lastSlash);
            if (resolver.getResource(parentPath) == null) {
                throw new AuthoringException("target parent '" + parentPath + "' does not exist");
            }
            session(resolver).move(fromPath, toPath);

            // Referential integrity: rewrite additionalCategories references into the moved subtree,
            // in the SAME transaction as the move (before commit) so it is atomic. Products physically
            // under the moved folder follow the JCR subtree move automatically; this fixes the stored
            // references on products elsewhere in the catalog.
            Resource catalogRootResource = resolver.getResource(catalogRoot(catalog));
            if (catalogRootResource != null) {
                rewriteMembershipReferences(catalogRootResource, fromRelativePath, toRelativePath, true);
            }
            resolver.commit();
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("moveCategory failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void upsertProduct(ResourceResolver resolver, String catalog, String categoryRelativePath,
                              ProductInput product) throws AuthoringException {
        try {
            AttributeManifest manifest = loadManifest(resolver, catalog);
            validateMasterScope(manifest, catalog, product.attributeValues().keySet());

            Resource categoryFolder = resolver.getResource(catalogRoot(catalog) + "/" + categoryRelativePath);
            if (categoryFolder == null) {
                throw new AuthoringException("category '" + categoryRelativePath + "' does not exist");
            }
            Resource productModel = resolver.getResource(
                    "/conf/" + catalog + "/settings/dam/cfm/models/product");
            if (productModel == null) {
                throw new AuthoringException("product model missing; run defineAttributes/defineAttribute first");
            }
            String nodeName = AemRepositorySupport.escapeNodeName(product.sku());
            String title = product.name() == null || product.name().isBlank() ? product.sku() : product.name();
            Resource fragment = AemContentFragmentSupport.ensureFragment(
                    resolver, productModel, categoryFolder, nodeName, title);
            AemContentFragmentSupport.setFragmentTitle(fragment, title);
            // best-effort standard fields (no-op if the model has no such field)
            AemContentFragmentSupport.writeText(fragment, "sku", product.sku(), "text/plain");
            if (product.name() != null) {
                AemContentFragmentSupport.writeText(fragment, "name", product.name(), "text/plain");
            }
            if (product.description() != null) {
                AemContentFragmentSupport.writeText(fragment, "description", product.description(), "text/html");
            }
            for (Map.Entry<String, Object> e : product.attributeValues().entrySet()) {
                AemContentFragmentSupport.writeTyped(fragment, e.getKey(), e.getValue());
            }
            resolver.commit();
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("upsertProduct failed for sku '" + product.sku() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void setProductAttributes(ResourceResolver resolver, String catalog, String sku, Map<String, Object> values)
            throws AuthoringException {
        try {
            AttributeManifest manifest = loadManifest(resolver, catalog);
            validateMasterScope(manifest, catalog, values.keySet());
            Resource fragment = requireProduct(resolver, catalog, sku);
            for (Map.Entry<String, Object> e : values.entrySet()) {
                AemContentFragmentSupport.writeTyped(fragment, e.getKey(), e.getValue());
            }
            resolver.commit();
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("setProductAttributes failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void setProductImage(ResourceResolver resolver, String catalog, String sku, String imageRef)
            throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            if (AuthoringSupport.isLocalDamPath(imageRef) && resolver.getResource(imageRef) == null) {
                throw new AuthoringException("asset not found in DAM: " + imageRef);
            }
            AemContentFragmentSupport.writeTyped(fragment, "image", List.of(imageRef));
            resolver.commit();
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("setProductImage failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String importProductImage(ResourceResolver resolver, String catalog, String sku, String variantId,
                                     String source, Map<String, String> headers) throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            // Local DAM asset: reference in place, no copy.
            if (AuthoringSupport.isLocalDamPath(source)) {
                if (resolver.getResource(source) == null) {
                    throw new AuthoringException("asset not found in DAM: " + source);
                }
                writeImageRef(fragment, variantId, source);
                resolver.commit();
                return source;
            }
            // External URL: download and create the asset co-located with the product.
            DownloadedBytes downloaded = download(source, headers == null ? Map.of() : headers);
            String ext = AuthoringSupport.extensionForMimeType(downloaded.mimeType(), source);
            String namePart = (variantId == null || variantId.isBlank()) ? sku : sku + "-" + variantId;
            String assetPath = AuthoringSupport.assetPath(fragment.getParent().getPath(), namePart, ext);
            AemContentFragmentSupport.createOrUpdateAsset(resolver, assetPath, downloaded.bytes(), downloaded.mimeType());
            writeImageRef(fragment, variantId, assetPath);
            resolver.commit();
            return assetPath;
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("importProductImage failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    /** Write an image reference onto the master fragment, or a named variation when variantId is given. */
    private static void writeImageRef(Resource fragment, String variantId, String imageRef) throws Exception {
        if (variantId == null || variantId.isBlank()) {
            AemContentFragmentSupport.writeTyped(fragment, "image", List.of(imageRef));
        } else {
            AemContentFragmentSupport.writeVariationTyped(fragment, variantId, variantId, "image", List.of(imageRef));
        }
    }

    private static DownloadedBytes download(String url, Map<String, String> headers) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest.Builder builder = HttpRequest.newBuilder(AuthoringSupport.encodedUri(url)).GET();
        headers.forEach(builder::header);
        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new AuthoringException("download failed: HTTP " + response.statusCode() + " for " + url);
        }
        String mime = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        int semi = mime.indexOf(';');
        if (semi > 0) {
            mime = mime.substring(0, semi).trim();
        }
        return new DownloadedBytes(response.body(), mime);
    }

    private record DownloadedBytes(byte[] bytes, String mimeType) {}

    @Override
    public void setProductVariants(ResourceResolver resolver, String catalog, String sku, List<VariantInput> variants)
            throws AuthoringException {
        AttributeManifest manifest = loadManifest(resolver, catalog);
        Resource fragment;
        try {
            fragment = requireProduct(resolver, catalog, sku);
        } catch (AuthoringException ae) {
            throw ae;
        }
        Resource master = fragment.getChild("jcr:content/data/master");
        ValueMap masterValues = master == null ? ValueMap.EMPTY : master.getValueMap();
        List<String> failures = new ArrayList<>();
        for (VariantInput variant : variants) {
            try {
                Map<String, Object> combined = new LinkedHashMap<>();
                combined.putAll(variant.axisValues());
                combined.putAll(variant.overrides());
                for (Map.Entry<String, Object> e : combined.entrySet()) {
                    AttributeEntry entry = manifest.entryFor(e.getKey()).orElseThrow(() ->
                            new AuthoringException("attribute '" + e.getKey() + "' is not in the '" + catalog + "' manifest"));
                    if (!entry.scope().appliesToVariant()) {
                        throw new AuthoringException("attribute '" + e.getKey()
                                + "' is PRODUCT-scoped and cannot be set per variant");
                    }
                    AemContentFragmentSupport.writeVariationTyped(
                            fragment, variant.variantId(), variant.variantId(), e.getKey(), e.getValue());
                }
                // Generate a stable variant sku when none was supplied.
                String variantSku = AuthoringSupport.variantSku(sku, variant.sku(), variant.variantId());
                AemContentFragmentSupport.writeVariationTyped(
                        fragment, variant.variantId(), variant.variantId(), "sku", variantSku);
                if (variant.imageRef() != null) {
                    AemContentFragmentSupport.writeVariationTyped(
                            fragment, variant.variantId(), variant.variantId(), "image", List.of(variant.imageRef()));
                }
                // Fully materialize: copy master values for every manifest attribute the
                // variant has not set itself (axis/override/sku/image take precedence).
                materializeMasterValues(fragment, variant.variantId(), manifest, masterValues);
            } catch (Exception e) {
                failures.add(variant.variantId() + ": " + e.getMessage());
            }
        }
        try {
            resolver.commit();
        } catch (Exception e) {
            throw new AuthoringException("setProductVariants commit failed: " + e.getMessage(), e);
        }
        if (!failures.isEmpty()) {
            throw new AuthoringException("some variants failed: " + String.join("; ", failures));
        }
    }

    @Override
    public String defineOption(ResourceResolver resolver, String catalog, String attributeCode, String productField,
                               String swatchType, List<String> valueLabels) throws AuthoringException {
        try {
            Resource catalogRoot = resolver.getResource(catalogRoot(catalog));
            if (catalogRoot == null) {
                throw new AuthoringException("catalog '" + catalog + "' does not exist; run createCatalog first");
            }
            Resource optionModel = AemRepositorySupport.optionDefinitionModel(resolver, catalog);
            if (optionModel == null) {
                optionModel = AemRepositorySupport.ensureOptionDefinitionModel(resolver, catalog);
            }
            List<String> encoded = new ArrayList<>();
            int index = 1;
            for (String label : valueLabels) {
                encoded.add(index + ";" + (label == null ? "" : label.replace(';', ',')));
                index++;
            }
            OptionDefinitionWriter writer = new OptionDefinitionWriter(resolver, catalogRoot, optionModel);
            OptionDefinitionWriter.OptionInput input = new OptionDefinitionWriter.OptionInput(
                    attributeCode,
                    attributeCode,
                    productField == null || productField.isBlank() ? attributeCode : productField,
                    swatchType == null ? "" : swatchType,
                    encoded);
            String path = writer.ensureOption(input);
            resolver.commit();
            return path;
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("defineOption failed for '" + attributeCode + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void defineVariantAxes(ResourceResolver resolver, String catalog, String sku,
                                  List<String> optionDefinitionPaths) throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            AemContentFragmentSupport.writeTyped(fragment, "configurableOptions",
                    optionDefinitionPaths == null ? List.of() : optionDefinitionPaths);
            resolver.commit();
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("defineVariantAxes failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void moveProduct(ResourceResolver resolver, String catalog, String sku, String targetCategoryRelativePath)
            throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            String targetFolder = catalogRoot(catalog) + "/" + targetCategoryRelativePath;
            if (resolver.getResource(targetFolder) == null) {
                throw new AuthoringException("target category '" + targetCategoryRelativePath + "' does not exist");
            }
            String dest = targetFolder + "/" + fragment.getName();
            session(resolver).move(fragment.getPath(), dest);
            resolver.commit();
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("moveProduct failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String addProductToCategory(ResourceResolver resolver, String catalog, String sku,
                                       String categoryRelativePath) throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            String targetFolder = catalogRoot(catalog) + "/" + categoryRelativePath;
            if (resolver.getResource(targetFolder) == null) {
                throw new AuthoringException("category '" + categoryRelativePath + "' does not exist");
            }
            String primary = primaryCategoryOf(catalog, fragment);
            if (CategoryMembership.isAncestorOrSelf(categoryRelativePath, primary)) {
                return "'" + sku + "' already belongs to '" + categoryRelativePath
                        + "' (implied by its primary category '" + primary + "'); no change";
            }
            Collection<String> assigned = readAdditional(fragment);
            assigned.add(categoryRelativePath);
            List<String> reconciled = CategoryMembership.deriveAdditionalCategories(assigned, primary);
            writeAdditional(fragment, reconciled);
            resolver.commit();
            return "added '" + sku + "' to category '" + categoryRelativePath + "'";
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("addProductToCategory failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String removeProductFromCategory(ResourceResolver resolver, String catalog, String sku,
                                            String categoryRelativePath) throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            String primary = primaryCategoryOf(catalog, fragment);
            if (categoryRelativePath.equals(primary)) {
                throw new AuthoringException("cannot remove the primary category '" + categoryRelativePath
                        + "' of '" + sku + "'; use set_primary_category or move_product");
            }
            Collection<String> assigned = readAdditional(fragment);
            if (!assigned.remove(categoryRelativePath)) {
                return "'" + sku + "' has no stored membership '" + categoryRelativePath
                        + "' (membership there, if any, is implied by a deeper stored path); no change";
            }
            List<String> reconciled = CategoryMembership.deriveAdditionalCategories(assigned, primary);
            writeAdditional(fragment, reconciled);
            resolver.commit();
            return "removed '" + sku + "' from category '" + categoryRelativePath + "'";
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("removeProductFromCategory failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String setPrimaryCategory(ResourceResolver resolver, String catalog, String sku,
                                     String targetCategoryRelativePath) throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            String oldPrimary = primaryCategoryOf(catalog, fragment);
            if (targetCategoryRelativePath.equals(oldPrimary)) {
                return "'" + sku + "' is already primary in '" + targetCategoryRelativePath + "'; no change";
            }
            String targetFolder = catalogRoot(catalog) + "/" + targetCategoryRelativePath;
            if (resolver.getResource(targetFolder) == null) {
                throw new AuthoringException("target category '" + targetCategoryRelativePath + "' does not exist");
            }
            // Capture the old membership BEFORE the move (read off the old location).
            Collection<String> assigned = readAdditional(fragment);
            assigned.add(oldPrimary);

            // Re-home the CF (same approach as moveProduct).
            String dest = targetFolder + "/" + fragment.getName();
            session(resolver).move(fragment.getPath(), dest);

            // Re-resolve at the new path and reconcile membership against the NEW primary.
            Resource moved = resolver.getResource(dest);
            List<String> reconciled = CategoryMembership.deriveAdditionalCategories(assigned, targetCategoryRelativePath);
            writeAdditional(moved, reconciled);

            resolver.commit();   // single atomic commit covering the move + the reconcile
            return "set primary category of '" + sku + "' to '" + targetCategoryRelativePath + "'";
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("setPrimaryCategory failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteProduct(ResourceResolver resolver, String catalog, String sku) throws AuthoringException {
        try {
            Resource fragment = requireProduct(resolver, catalog, sku);
            resolver.delete(fragment);
            resolver.commit();
        } catch (AuthoringException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AuthoringException("deleteProduct failed for sku '" + sku + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void setCatalogReady(ResourceResolver resolver, String catalog, boolean ready) throws AuthoringException {
        try {
            ReadyFlag.set(resolver, catalog, ready);
        } catch (Exception e) {
            throw new AuthoringException("setCatalogReady failed: " + e.getMessage(), e);
        }
    }

    // --- helpers ---

    AttributeManifest loadManifest(ResourceResolver resolver, String catalog) {
        return new ManifestReaderImpl().read(resolver, catalog)
                .orElseGet(() -> AttributeManifest.empty(catalog));
    }

    private static void validateMasterScope(AttributeManifest manifest, String catalog, Iterable<String> codes)
            throws AuthoringException {
        for (String code : codes) {
            AttributeEntry entry = manifest.entryFor(code).orElseThrow(() ->
                    new AuthoringException("attribute '" + code + "' is not in the '" + catalog + "' manifest"));
            if (!entry.scope().appliesToProduct()) {
                throw new AuthoringException("attribute '" + code
                        + "' is VARIANT-scoped; use set_product_variants instead");
            }
        }
    }

    /** Locate a product Content Fragment by SKU (matches the slugged node name). */
    Resource findProductBySku(ResourceResolver resolver, String catalog, String sku) {
        Resource root = resolver.getResource(catalogRoot(catalog));
        if (root == null) {
            return null;
        }
        String nodeName = AemRepositorySupport.escapeNodeName(sku);
        return findByNodeName(root, nodeName, true);
    }

    private Resource findByNodeName(Resource node, String nodeName, boolean isRoot) {
        for (Resource child : node.getChildren()) {
            String name = child.getName();
            if (name.startsWith("jcr:") || (isRoot && (name.equals("_manifest") || name.equals("_options")))) {
                continue;
            }
            if (name.equals(nodeName)) {
                return child;
            }
            Resource found = findByNodeName(child, nodeName, false);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Copy master attribute values onto a variation for every manifest code it has not set itself. */
    private static void materializeMasterValues(Resource fragment, String variantId,
                                                AttributeManifest manifest, ValueMap masterValues) {
        Resource variation = fragment.getChild("jcr:content/data/" + variantId);
        if (variation == null) {
            return;
        }
        ModifiableValueMap variationValues = variation.adaptTo(ModifiableValueMap.class);
        if (variationValues == null) {
            return;
        }
        for (AttributeEntry entry : manifest.entries()) {
            String code = entry.code();
            if (!masterValues.containsKey(code) || variationValues.containsKey(code)) {
                continue;
            }
            variationValues.put(code, masterValues.get(code));
        }
    }

    private Resource requireProduct(ResourceResolver resolver, String catalog, String sku) throws AuthoringException {
        Resource fragment = findProductBySku(resolver, catalog, sku);
        if (fragment == null) {
            throw new AuthoringException("product '" + sku + "' not found in catalog '" + catalog + "'");
        }
        return fragment;
    }

    /** Catalog-relative primary category of a product CF = its parent folder, relative to the catalog root. */
    private String primaryCategoryOf(String catalog, Resource fragment) {
        String root = catalogRoot(catalog);
        String parent = fragment.getParent().getPath();      // .../<catalog>/<category-path>
        if (parent.equals(root)) {
            return "";                                        // product sits directly under the catalog root
        }
        return parent.substring(root.length() + 1);          // strip "<root>/"
    }

    /** Read the product's currently stored additionalCategories off its master data node. */
    private Collection<String> readAdditional(Resource fragment) {
        Resource master = fragment.getChild("jcr:content/data/master");
        ValueMap masterValues = master == null ? ValueMap.EMPTY : master.getValueMap();
        String[] current = masterValues.get("additionalCategories", String[].class);
        return current == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(current));
    }

    /**
     * Persist the reconciled membership: through the CF element (production / integration) AND
     * directly onto the master node property the read path consumes (so it is consistent and
     * observable — writeTyped is a no-op under sling-mock and when the scaffold element is absent).
     */
    private void writeAdditional(Resource fragment, List<String> reconciled) throws Exception {
        try {
            AemContentFragmentSupport.writeTyped(fragment, "additionalCategories", reconciled);
        } catch (Exception e) {
            // The CF element write can fail when the AEM Content Fragment runtime is unavailable
            // (e.g. sling-mock unit tests) or the scaffold element is absent. Non-fatal: the direct
            // master-node write below keeps the property the read path consumes correct. Log it so a
            // genuine production failure is visible rather than silently swallowed.
            LOG.log(Level.WARNING, e,
                    () -> "writeTyped(additionalCategories) failed for " + fragment.getPath()
                            + "; relying on direct master-node write");
        }
        Resource master = fragment.getChild("jcr:content/data/master");
        if (master != null) {
            ModifiableValueMap mvm = master.adaptTo(ModifiableValueMap.class);
            if (mvm != null) {
                mvm.put("additionalCategories", reconciled.toArray(new String[0]));
            }
        }
    }

    private static Session session(ResourceResolver resolver) throws AuthoringException {
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            throw new AuthoringException("cannot obtain JCR session for move");
        }
        return session;
    }

    private static void requireCatalogName(String catalog) throws AuthoringException {
        if (catalog == null || catalog.isBlank() || catalog.contains("/")) {
            throw new AuthoringException("invalid catalog name: '" + catalog + "'");
        }
    }
}

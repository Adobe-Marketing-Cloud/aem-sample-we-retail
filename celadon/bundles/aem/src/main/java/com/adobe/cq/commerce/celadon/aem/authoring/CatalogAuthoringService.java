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
package com.adobe.cq.commerce.celadon.aem.authoring;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Transport-agnostic catalog authoring API. Every method takes the caller's
 * {@link ResourceResolver} (run-as-user) and a catalog name (e.g. "we-retail", stored
 * under {@code /content/dam/celadon/<catalog>}). Implementations commit on success.
 *
 * <p>Operations that create Content Fragments require a live AEM Content Fragment
 * runtime (the {@code FragmentTemplate} API); they are exercised by integration
 * tests against a real instance, not by sling-mock unit tests.</p>
 */
public interface CatalogAuthoringService {

    /** Resolve the catalog name to use when a tool omits the {@code catalog} arg. */
    String defaultCatalog();

    /** List catalog names under {@code /content/dam/celadon}. */
    List<String> listCatalogs(ResourceResolver resolver);

    /** True if the catalog root's jcr:content carries {@code celadonReady=true}. */
    boolean isCatalogReady(ResourceResolver resolver, String catalog);

    /** Summary of a catalog's state (counts + ready + default marker). */
    CatalogInfo getCatalogInfo(ResourceResolver resolver, String catalog);

    /** The catalog's attribute manifest entries (empty if none). */
    List<AttributeEntry> listAttributes(ResourceResolver resolver, String catalog);

    /**
     * List the catalog's category folders as a flat, depth-first (path-ordered) tree.
     * Each entry carries the catalog-relative path, the folder title, and the number of
     * products living directly in that category. Image assets and product Content Fragments
     * are not categories and are excluded.
     */
    List<CategoryInfo> listCategories(ResourceResolver resolver, String catalog);

    /**
     * Bootstrap a new (or existing) catalog: root folders + the celadon-attribute
     * and celadon-option-definition CF models. Idempotent. Returns the catalog
     * root path. Prerequisite for {@link #defineAttributes}/{@link #defineAttribute}.
     */
    String createCatalog(ResourceResolver resolver, String catalog) throws AuthoringException;

    /**
     * Replace the catalog's attribute manifest with the given entries (full rewrite
     * of the _manifest folder) and regenerate the product CF model. Requires the
     * attribute model to exist (run {@link #createCatalog} first).
     */
    void defineAttributes(ResourceResolver resolver, String catalog, List<AttributeEntry> entries)
            throws AuthoringException;

    /** Add or replace a single attribute, then rewrite the manifest + product model. */
    void defineAttribute(ResourceResolver resolver, String catalog, AttributeEntry entry)
            throws AuthoringException;

    /** Remove an attribute from the manifest by code, then rewrite the manifest + product model. */
    void removeAttribute(ResourceResolver resolver, String catalog, String code) throws AuthoringException;

    /** Rebuild the product CF model from the catalog's current manifest. */
    void regenerateProductModel(ResourceResolver resolver, String catalog) throws AuthoringException;

    /** Ensure a category folder at the given catalog-relative path. */
    void ensureCategory(ResourceResolver resolver, String catalog, String relativePath, String title)
            throws AuthoringException;

    /** Move a category folder (and everything under it) to a new catalog-relative path. */
    void moveCategory(ResourceResolver resolver, String catalog, String fromRelativePath, String toRelativePath)
            throws AuthoringException;

    /**
     * Create or update a product Content Fragment at the given category-relative
     * path. Writes master-scope values only; rejects attribute codes that are not
     * in the manifest or are VARIANT-only.
     */
    void upsertProduct(ResourceResolver resolver, String catalog, String categoryRelativePath,
                       ProductInput product) throws AuthoringException;

    /** Patch master-scope attribute values on an existing product (by SKU). */
    void setProductAttributes(ResourceResolver resolver, String catalog, String sku, Map<String, Object> values)
            throws AuthoringException;

    /** Set the image content-reference on an existing product (by SKU). */
    void setProductImage(ResourceResolver resolver, String catalog, String sku, String imageRef)
            throws AuthoringException;

    /**
     * Set a product's image from a source. If {@code source} is a local {@code /content}
     * asset path, it is referenced in place. Otherwise it is treated as an external URL,
     * downloaded into the DAM co-located with the product, and the new asset path is set.
     * When {@code variantId} is non-null, the image is set on that named variation instead
     * of the master. Returns the asset path that was set.
     */
    String importProductImage(ResourceResolver resolver, String catalog, String sku, String variantId,
                              String source, Map<String, String> headers) throws AuthoringException;

    /** Create/update named CF variations on a product, honoring variant scope. */
    void setProductVariants(ResourceResolver resolver, String catalog, String sku, List<VariantInput> variants)
            throws AuthoringException;

    /**
     * Create or reuse an option-definition CF for a configurable attribute and
     * return its path. {@code valueLabels} are encoded as {@code index;label}.
     */
    String defineOption(ResourceResolver resolver, String catalog, String attributeCode, String productField,
                        String swatchType, List<String> valueLabels) throws AuthoringException;

    /** Wire a product's configurableOptions to the given option-definition paths. */
    void defineVariantAxes(ResourceResolver resolver, String catalog, String sku, List<String> optionDefinitionPaths)
            throws AuthoringException;

    /** Move a product (by SKU) to a different category (JCR re-path). */
    void moveProduct(ResourceResolver resolver, String catalog, String sku, String targetCategoryRelativePath)
            throws AuthoringException;

    /**
     * Add a category membership to a product. No-op (with a message) if the target equals or is an
     * ancestor of the product's primary category. Otherwise applies the antichain rule and writes.
     */
    String addProductToCategory(ResourceResolver resolver, String catalog, String sku,
                                String categoryRelativePath) throws AuthoringException;

    /**
     * Remove a category membership from a product. Rejects removing the product's PRIMARY category
     * (use setPrimaryCategory/moveProduct). Removing a path that is only an implied ancestor (not in
     * the stored set) is a no-op with a message. Otherwise strips the entry and re-derives.
     */
    String removeProductFromCategory(ResourceResolver resolver, String catalog, String sku,
                                     String categoryRelativePath) throws AuthoringException;

    /**
     * Re-home a product to a new primary category (same JCR move as moveProduct) and reconcile
     * membership in the same commit: the old primary becomes an additional membership unless it is
     * an ancestor of the new primary; the new primary and its ancestors are dropped.
     */
    String setPrimaryCategory(ResourceResolver resolver, String catalog, String sku,
                              String targetCategoryRelativePath) throws AuthoringException;

    /** Delete a product Content Fragment (by SKU). */
    void deleteProduct(ResourceResolver resolver, String catalog, String sku) throws AuthoringException;

    /** Set the catalog ready flag (storefront visibility gate). */
    void setCatalogReady(ResourceResolver resolver, String catalog, boolean ready) throws AuthoringException;
}

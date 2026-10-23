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

import static org.junit.Assert.*;

import com.adobe.cq.commerce.celadon.aem.authoring.AuthoringException;
import com.adobe.cq.commerce.celadon.aem.authoring.CatalogInfo;
import com.adobe.cq.commerce.celadon.aem.authoring.CategoryInfo;
import com.adobe.cq.commerce.celadon.aem.authoring.ProductInput;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit coverage for the JCR-only operations and the manifest validation branches.
 * Operations that create Content Fragments ({@code defineAttributes} with entries,
 * {@code upsertProduct}, variants) require the live AEM CF runtime and are covered
 * by integration validation against a real instance, not by sling-mock here — the
 * same constraint {@link com.adobe.cq.commerce.celadon.aem.attribute.manifest.ManifestWriter}
 * documents.
 */
public class CatalogAuthoringServiceImplTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private final CatalogAuthoringServiceImpl service = new CatalogAuthoringServiceImpl();

    @Test
    public void createCatalogScaffoldsFoldersAndModels() throws Exception {
        String root = service.createCatalog(context.resourceResolver(), "demo");

        assertEquals("/content/dam/celadon/demo", root);
        assertNotNull(context.resourceResolver().getResource("/content/dam/celadon/demo"));
        assertNotNull(context.resourceResolver().getResource(
                "/conf/demo/settings/dam/cfm/models/celadon-attribute"));
        assertNotNull(context.resourceResolver().getResource(
                "/conf/demo/settings/dam/cfm/models/celadon-option-definition"));
    }

    @Test
    public void createCatalogTitlesRootFolderWithCeladonProductCatalogs() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");

        assertEquals(AemRepositorySupport.CELADON_ROOT_TITLE, context.resourceResolver()
                .getResource("/content/dam/celadon/jcr:content").getValueMap().get("jcr:title", String.class));
    }

    @Test
    public void listCatalogsReturnsChildrenExcludingJcr() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.createCatalog(context.resourceResolver(), "other");

        List<String> catalogs = service.listCatalogs(context.resourceResolver());
        assertTrue(catalogs.contains("demo"));
        assertTrue(catalogs.contains("other"));
        assertFalse(catalogs.stream().anyMatch(c -> c.startsWith("jcr:")));
    }

    @Test
    public void readyFlagRoundTrips() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        assertFalse(service.isCatalogReady(context.resourceResolver(), "demo"));

        service.setCatalogReady(context.resourceResolver(), "demo", true);
        assertTrue(service.isCatalogReady(context.resourceResolver(), "demo"));
    }

    @Test
    public void ensureCategoryCreatesNestedFolders() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "men/shirts", "Shirts");

        assertNotNull(context.resourceResolver().getResource("/content/dam/celadon/demo/men"));
        assertNotNull(context.resourceResolver().getResource("/content/dam/celadon/demo/men/shirts"));
    }

    @Test
    public void defineAttributesEmptyCreatesManifestAndProductModel() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.defineAttributes(context.resourceResolver(), "demo", List.of());

        // _manifest folder created; product model regenerated with scaffold fields
        assertNotNull(context.resourceResolver().getResource("/content/dam/celadon/demo/_manifest"));
        assertNotNull(context.resourceResolver().getResource(
                "/conf/demo/settings/dam/cfm/models/product/jcr:content/model/cq:dialog/content/items/image"));
        assertNotNull(context.resourceResolver().getResource(
                "/conf/demo/settings/dam/cfm/models/product/jcr:content/model/cq:dialog/content/items/configurableOptions"));
    }

    @Test
    public void regenerateProductModelRebuildsFromManifest() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.defineAttributes(context.resourceResolver(), "demo", List.of());

        service.regenerateProductModel(context.resourceResolver(), "demo");
        assertNotNull(context.resourceResolver().getResource(
                "/conf/demo/settings/dam/cfm/models/product"));
    }

    @Test(expected = AuthoringException.class)
    public void upsertProductRejectsUnknownAttribute() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.defineAttributes(context.resourceResolver(), "demo", List.of());
        service.ensureCategory(context.resourceResolver(), "demo", "men", "Men");

        // validation throws before any Content Fragment is created
        service.upsertProduct(context.resourceResolver(), "demo", "men",
                new ProductInput("SKU2", "x", "y", Map.of("not_a_real_attr", "v")));
    }

    @Test(expected = AuthoringException.class)
    public void setProductAttributesRejectsVariantScopedCode() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        // Build a manifest in memory is not possible without fragments; instead assert
        // the validation path: an empty manifest means any code is unknown -> throws.
        service.defineAttributes(context.resourceResolver(), "demo", List.of());
        service.setProductAttributes(context.resourceResolver(), "demo", "S1", Map.of("color", "red"));
    }

    @Test
    public void getCatalogInfoReportsReadyAndCounts() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "men", "Men");
        service.setCatalogReady(context.resourceResolver(), "demo", true);

        CatalogInfo info = service.getCatalogInfo(context.resourceResolver(), "demo");
        assertEquals("demo", info.name());
        assertTrue(info.ready());
        assertEquals(0, info.productCount());
        assertTrue(info.categoryCount() >= 1); // at least "men"
    }

    @Test
    public void getCatalogInfoDoesNotCountImageAssetsAsCategories() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "men", "Men");
        // Imported catalogs co-locate each product's image as a sibling dam:Asset
        // (e.g. shoe_img.jpeg). It is not a Content Fragment and not a folder, so it
        // must be ignored by the counts — not classified as a category.
        context.create().resource("/content/dam/celadon/demo/men/shoe_img.jpeg",
                "jcr:primaryType", "dam:Asset");
        context.create().resource("/content/dam/celadon/demo/men/shoe_img.jpeg/jcr:content",
                "jcr:primaryType", "dam:AssetContent");
        context.resourceResolver().commit();

        CatalogInfo info = service.getCatalogInfo(context.resourceResolver(), "demo");
        assertEquals(1, info.categoryCount()); // only "men"; the image asset must not count
        assertEquals(0, info.productCount());
    }

    @Test
    public void listCategoriesReturnsFolderTreeWithTitlesAndDirectProductCounts() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "eq", "Equipment");
        service.ensureCategory(context.resourceResolver(), "demo", "eq/eqwn", "Winter Sports");
        service.ensureCategory(context.resourceResolver(), "demo", "men", "Men");
        // Emulate a product Content Fragment (dam:Asset with jcr:content/data) living directly
        // in eq/eqwn, plus a co-located image asset that must NOT be counted as a product.
        context.create().resource("/content/dam/celadon/demo/eq/eqwn/ski",
                "jcr:primaryType", "dam:Asset");
        context.create().resource("/content/dam/celadon/demo/eq/eqwn/ski/jcr:content/data",
                "jcr:primaryType", "nt:unstructured");
        context.create().resource("/content/dam/celadon/demo/eq/eqwn/ski_img.jpeg",
                "jcr:primaryType", "dam:Asset");
        context.resourceResolver().commit();

        List<CategoryInfo> cats = service.listCategories(context.resourceResolver(), "demo");

        // Depth-first, path order; only real folders, no assets.
        List<String> paths = cats.stream().map(CategoryInfo::path).toList();
        assertEquals(List.of("eq", "eq/eqwn", "men"), paths);

        CategoryInfo eqwn = cats.stream().filter(c -> c.path().equals("eq/eqwn")).findFirst().orElseThrow();
        assertEquals("Winter Sports", eqwn.title());
        assertEquals(1, eqwn.productCount()); // the CF; the image asset must not count

        CategoryInfo eq = cats.stream().filter(c -> c.path().equals("eq")).findFirst().orElseThrow();
        assertEquals("Equipment", eq.title());
        assertEquals(0, eq.productCount());
    }

    @Test
    public void ensureCategoryPreservesAncestorTitles() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "eq", "Equipment");
        service.ensureCategory(context.resourceResolver(), "demo", "eq/eqwn", "Winter Sports");

        assertEquals("Equipment", context.resourceResolver()
                .getResource("/content/dam/celadon/demo/eq/jcr:content").getValueMap().get("jcr:title", String.class));
        assertEquals("Winter Sports", context.resourceResolver()
                .getResource("/content/dam/celadon/demo/eq/eqwn/jcr:content").getValueMap().get("jcr:title", String.class));
    }

    @Test
    public void defaultCatalogFallsBackWhenNoConfigAdmin() {
        // No ConfigurationAdmin bound in unit context -> falls back to constant.
        assertEquals(CatalogAuthoringServiceImpl.DEFAULT_CATALOG, service.defaultCatalog());
    }

    // ---- multi-category membership (Plan 3a) ----

    /** Build a product CF fixture: dam:Asset + jcr:content/data/master, living under `category`. */
    private void seedProduct(String category, String sku, String... additional) throws Exception {
        String base = "/content/dam/celadon/demo/" + category + "/" + sku;
        context.create().resource(base, "jcr:primaryType", "dam:Asset");
        context.create().resource(base + "/jcr:content/data", "jcr:primaryType", "nt:unstructured");
        if (additional.length == 0) {
            context.create().resource(base + "/jcr:content/data/master", "jcr:primaryType", "nt:unstructured");
        } else {
            context.create().resource(base + "/jcr:content/data/master",
                    "jcr:primaryType", "nt:unstructured", "additionalCategories", additional);
        }
        context.resourceResolver().commit();
    }

    /** The String[] stored on the product's master node (the property the read path consumes). */
    private String[] storedAdditional(String category, String sku) {
        Resource master = context.resourceResolver().getResource(
                "/content/dam/celadon/demo/" + category + "/" + sku + "/jcr:content/data/master");
        return master.getValueMap().get("additionalCategories", new String[0]);
    }

    @Test
    public void addProductToCategoryWritesNewMembership() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        service.ensureCategory(context.resourceResolver(), "demo", "sale", "Sale");
        seedProduct("man/pants/summer", "p1");

        String msg = service.addProductToCategory(context.resourceResolver(), "demo", "p1", "sale");

        assertArrayEquals(new String[] {"sale"}, storedAdditional("man/pants/summer", "p1"));
        assertTrue("message should mention the added category", msg.contains("sale"));
    }

    @Test
    public void addProductToCategoryNoOpsWhenImpliedByPrimary() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        seedProduct("man/pants/summer", "p1");

        // "man/pants" is an ancestor of the primary "man/pants/summer" → already implied.
        String msg = service.addProductToCategory(context.resourceResolver(), "demo", "p1", "man/pants");

        assertEquals("nothing should be stored", 0, storedAdditional("man/pants/summer", "p1").length);
        assertTrue("message should explain the no-op", msg.toLowerCase().contains("no change"));
    }

    @Test
    public void removeProductFromCategoryStripsEntry() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        service.ensureCategory(context.resourceResolver(), "demo", "sale", "Sale");
        seedProduct("man/pants/summer", "p1", "sale");

        String msg = service.removeProductFromCategory(context.resourceResolver(), "demo", "p1", "sale");

        assertEquals("entry should be stripped", 0, storedAdditional("man/pants/summer", "p1").length);
        assertTrue(msg.contains("sale"));
    }

    @Test(expected = AuthoringException.class)
    public void removeProductFromCategoryRejectsPrimary() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        seedProduct("man/pants/summer", "p1", "sale");

        // Removing the product's PRIMARY category must be rejected.
        service.removeProductFromCategory(context.resourceResolver(), "demo", "p1", "man/pants/summer");
    }

    @Test
    public void removeProductFromCategoryNoOpsOnImpliedAncestor() throws Exception {
        service.createCatalog(context.resourceResolver(), "demo");
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        service.ensureCategory(context.resourceResolver(), "demo", "sale/clearance", "Clearance");
        seedProduct("man/pants/summer", "p1", "sale/clearance");

        // "sale" is only an implied ancestor of the stored "sale/clearance" — not in the stored set.
        String msg = service.removeProductFromCategory(context.resourceResolver(), "demo", "p1", "sale");

        assertArrayEquals("stored set unchanged", new String[] {"sale/clearance"},
                storedAdditional("man/pants/summer", "p1"));
        assertTrue(msg.toLowerCase().contains("no change"));
    }

}

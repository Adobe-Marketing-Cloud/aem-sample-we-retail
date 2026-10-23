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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that exercise a real JCR node move (via {@code javax.jcr.Session#move}).
 * Uses {@link ResourceResolverType#JCR_MOCK} so that {@code resolver.adaptTo(Session.class)}
 * returns a real mock JCR session — the code path exercised by {@code moveCategory} and
 * {@code setPrimaryCategory} after the removal of the sling-mock fallback.
 *
 * <p>We skip {@code createCatalog} (whose best-effort seeding path calls {@code resolver.revert()},
 * which JCR_MOCK does not support) and instead seed just the catalog root folder directly via
 * {@code ensureCategory} — which is all the move operations need.
 */
public class CatalogAuthoringMoveTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final CatalogAuthoringServiceImpl service = new CatalogAuthoringServiceImpl();

    /** Seed the catalog root folder so ensureCategory can build beneath it. */
    private void seedCatalogRoot() throws Exception {
        context.create().resource("/content/dam/celadon/demo",
                "jcr:primaryType", "sling:OrderedFolder");
        context.resourceResolver().commit();
    }

    /**
     * Build a product CF fixture: nt:unstructured + jcr:content/data/master, living under {@code category}.
     * Uses nt:unstructured instead of dam:Asset because the DAM nodetype is not registered in the
     * bare JCR_MOCK repository; the authoring logic only relies on node name matching and the
     * presence of a jcr:content/data child (isContentFragment).
     */
    private void seedProduct(String category, String sku, String... additional) throws Exception {
        String base = "/content/dam/celadon/demo/" + category + "/" + sku;
        context.create().resource(base, "jcr:primaryType", "nt:unstructured");
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
    public void setPrimaryCategoryRehomesAndReconciles() throws Exception {
        seedCatalogRoot();
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        service.ensureCategory(context.resourceResolver(), "demo", "sale", "Sale");
        // Start: primary man/pants/summer, additional {sale}.
        seedProduct("man/pants/summer", "p1", "sale");

        String msg = service.setPrimaryCategory(context.resourceResolver(), "demo", "p1", "sale");

        // CF re-homed under the new primary "sale".
        assertNotNull("product re-homed under new primary",
                context.resourceResolver().getResource("/content/dam/celadon/demo/sale/p1"));
        assertNull("product no longer under old primary",
                context.resourceResolver().getResource("/content/dam/celadon/demo/man/pants/summer/p1"));
        // Reconciled: new primary "sale" dropped; old primary "man/pants/summer" becomes additional.
        assertArrayEquals(new String[] {"man/pants/summer"}, storedAdditional("sale", "p1"));
        assertTrue(msg.contains("sale"));
    }

    @Test
    public void moveCategoryRewritesAdditionalReferences() throws Exception {
        seedCatalogRoot();
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        service.ensureCategory(context.resourceResolver(), "demo", "sale/clearance", "Clearance");
        service.ensureCategory(context.resourceResolver(), "demo", "promotions", "Promotions");
        // Product lives under man/pants/summer; additionally a member of sale/clearance.
        seedProduct("man/pants/summer", "p1", "sale/clearance");

        service.moveCategory(context.resourceResolver(), "demo", "sale", "promotions/sale");

        // The stored reference into the moved subtree is rewritten with the new prefix.
        assertArrayEquals(new String[] {"promotions/sale/clearance"},
                storedAdditional("man/pants/summer", "p1"));
    }

    @Test
    public void moveCategoryLeavesUnrelatedReferencesUntouched() throws Exception {
        seedCatalogRoot();
        service.ensureCategory(context.resourceResolver(), "demo", "man/pants/summer", "Summer");
        service.ensureCategory(context.resourceResolver(), "demo", "saleroom", "Saleroom");
        service.ensureCategory(context.resourceResolver(), "demo", "promotions", "Promotions");
        // moveCategory requires the source folder to exist; create "sale" so the move is valid.
        service.ensureCategory(context.resourceResolver(), "demo", "sale", "Sale");
        // "saleroom" must NOT match a move of "sale" (prefix match requires a "/" boundary).
        seedProduct("man/pants/summer", "p1", "saleroom");

        service.moveCategory(context.resourceResolver(), "demo", "sale", "promotions/sale");

        assertArrayEquals(new String[] {"saleroom"}, storedAdditional("man/pants/summer", "p1"));
    }
}

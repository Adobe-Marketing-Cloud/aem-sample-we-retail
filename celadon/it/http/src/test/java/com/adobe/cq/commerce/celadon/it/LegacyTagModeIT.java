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
package com.adobe.cq.commerce.celadon.it;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.ValidatableResponse;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

/**
 * Live-instance coverage for the legacy importer's category modes (FOLDER / TAG / HYBRID / BLUEPRINT).
 *
 * <p>Each test imports a legacy source into its own fresh catalog (so existing catalogs are never
 * overwritten) and verifies placement via direct JCR {@code .json} reads — bypassing the singleton
 * GraphQL servlet, whose served catalog is configured independently.
 *
 * <p>Gated by {@code -Dceladon.it.enabled=true}; requires a writable AEM at {@code celadon.it.baseUrl}
 * with the legacy sources reachable from that instance.
 */
public class LegacyTagModeIT extends GraphqlITBase {

    // Base source URLs — the servlet appends ".infinity.json" itself, so these must NOT include it.
    private static final String GEOMETRIXX_SOURCE = System.getProperty(
            "celadon.it.legacy.geometrixxUrl",
            "http://localhost:4504/etc/commerce/products/geometrixx-outdoors");
    private static final String WE_RETAIL_SOURCE = System.getProperty(
            "celadon.it.legacy.weRetailUrl",
            "http://localhost:4504/var/commerce/products/we-retail");
    // Authored catalog blueprint for geometrixx-outdoors — drives BLUEPRINT-mode placement.
    private static final String GEOMETRIXX_BLUEPRINT = System.getProperty(
            "celadon.it.legacy.geometrixxBlueprint",
            "/content/catalogs/geometrixx-outdoors/en/base-catalog");
    // base64(admin:admin) — credentials the target AEM uses to read the source instance.
    private static final String SOURCE_AUTH = System.getProperty(
            "celadon.it.legacy.sourceAuthorization", "Basic YWRtaW46YWRtaW4=");

    private static final String IT_USER = System.getProperty("celadon.it.user", "admin");
    private static final String IT_PASSWORD = System.getProperty("celadon.it.password", "admin");
    private static final String DAM_ROOT = "/content/dam/celadon/";

    /** FOLDER mode: products stay at their folder home, no tag folders, no additionalCategories. */
    @Test
    public void testFolderMode() {
        String catalog = "it-geometrixx-folder";
        importLegacy(catalog, GEOMETRIXX_SOURCE, "folder");

        // eqwntb keeps its folder-derived home.
        assertStatus(DAM_ROOT + catalog + "/eq/eqwn/eqwntb/jcr:content/data/master.json", 200);
        // Pre-pass never runs in FOLDER mode, so no tag-derived branch exists.
        assertStatus(DAM_ROOT + catalog + "/activity/skiing.json", 404);
        // Empty membership is not written at all.
        master(catalog, "eq/eqwn/eqwntb").body("additionalCategories", nullValue());
    }

    /** TAG mode: product re-homed under its primary (deepest) tag; non-primary tags become additional. */
    @Test
    public void testTagMode() {
        String catalog = "it-geometrixx-tag";
        importLegacy(catalog, GEOMETRIXX_SOURCE, "tag");

        // Re-homed under the deepest tag (activity/skiing wins over apparel/footwear, season/winter).
        assertStatus(DAM_ROOT + catalog + "/activity/skiing/eqwntb/jcr:content/data/master.json", 200);
        // The original folder home no longer exists.
        assertStatus(DAM_ROOT + catalog + "/eq/eqwn/eqwntb.json", 404);
        // Minimal, sorted additional set with the primary dropped.
        master(catalog, "activity/skiing/eqwntb")
                .body("additionalCategories", contains("apparel/footwear", "season/winter"));
    }

    /** HYBRID mode: product stays at its folder home; every tag path is stored as additional. */
    @Test
    public void testHybridMode() {
        String catalog = "it-we-retail-hybrid";
        importLegacy(catalog, WE_RETAIL_SOURCE, "hybrid");

        // spixa_runners keeps its folder home eq/running.
        assertStatus(DAM_ROOT + catalog + "/eq/running/spixa_runners/jcr:content/data/master.json", 200);
        // Tag folders are materialised by the pre-pass.
        assertStatus(DAM_ROOT + catalog + "/activity/running.json", 200);
        assertStatus(DAM_ROOT + catalog + "/apparel/footwear.json", 200);
        // Both tag paths are stored (folder primary dropped), sorted.
        master(catalog, "eq/running/spixa_runners")
                .body("additionalCategories", contains("activity/running", "apparel/footwear"));
    }

    /**
     * BLUEPRINT mode: products are re-homed under the authored blueprint section whose
     * {@code matchTags} they satisfy (AND logic). {@code eqwntb} (activity/skiing, season/winter,
     * apparel/footwear) matches only {@code equipment/skiing}.
     */
    @Test
    public void testBlueprintMode() {
        String catalog = "it-geometrixx-blueprint";
        importLegacy(catalog, GEOMETRIXX_SOURCE, "blueprint", GEOMETRIXX_BLUEPRINT);

        // Re-homed under the matched blueprint section (activity/skiing -> equipment/skiing).
        assertStatus(DAM_ROOT + catalog + "/equipment/skiing/eqwntb/jcr:content/data/master.json", 200);
        // The original folder home no longer exists.
        assertStatus(DAM_ROOT + catalog + "/eq/eqwn/eqwntb.json", 404);
        // BLUEPRINT mode never materialises source folders: the cryptic source skeleton is absent and
        // only the authored blueprint structure is served.
        assertStatus(DAM_ROOT + catalog + "/eq.json", 404);
        // The pre-pass materialises navigation containers even when they hold no product directly.
        assertStatus(DAM_ROOT + catalog + "/seasonal.json", 200);
        assertStatus(DAM_ROOT + catalog + "/men/coats.json", 200);
        // Single blueprint match -> no additionalCategories written.
        master(catalog, "equipment/skiing/eqwntb").body("additionalCategories", nullValue());
    }

    /** POST the legacy-import job and assert it succeeds. */
    private void importLegacy(String catalog, String sourceUrl, String mode) {
        importLegacy(catalog, sourceUrl, mode, null);
    }

    /** POST the legacy-import job (optionally with a blueprint path) and assert it succeeds. */
    private void importLegacy(String catalog, String sourceUrl, String mode, String blueprintPath) {
        var request = given()
                .basePath("")
                .auth().preemptive().basic(IT_USER, IT_PASSWORD)
                .config(importConfig())
                .formParam("targetCatalog", catalog)
                .formParam("sourceUrl", sourceUrl)
                .formParam("categoryMode", mode)
                .formParam("sourceAuthorization", SOURCE_AUTH);
        if (blueprintPath != null) {
            request.formParam("catalogBlueprintPath", blueprintPath);
        }
        request
                .post("/bin/celadon/legacy-import")
                .then()
                .statusCode(200);
    }

    /** Read a product's master data node for property assertions. */
    private ValidatableResponse master(String catalog, String homePath) {
        return given()
                .basePath("")
                .auth().preemptive().basic(IT_USER, IT_PASSWORD)
                .get(DAM_ROOT + catalog + "/" + homePath + "/jcr:content/data/master.json")
                .then()
                .statusCode(200);
    }

    private void assertStatus(String path, int expected) {
        int actual = given()
                .basePath("")
                .auth().preemptive().basic(IT_USER, IT_PASSWORD)
                .get(path)
                .then()
                .extract().statusCode();
        assertEquals("unexpected status for " + path, expected, actual);
    }

    /** Generous socket timeout — a full legacy import (with image downloads) can run for minutes. */
    private RestAssuredConfig importConfig() {
        return RestAssuredConfig.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.protocol.expect-continue", false)
                        .setParam("http.socket.timeout", 600000)
                        .setParam("http.connection.timeout", 60000));
    }
}

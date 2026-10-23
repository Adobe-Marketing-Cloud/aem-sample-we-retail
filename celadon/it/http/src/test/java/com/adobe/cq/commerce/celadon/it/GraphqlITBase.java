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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assume;
import org.junit.BeforeClass;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public abstract class GraphqlITBase {
    protected static final String PRODUCT_MODEL_PATH_VENIA = "/conf/venia/settings/dam/cfm/models/product";
    protected static final String PRODUCT_MODEL_PATH_WE_RETAIL = "/conf/we-retail/settings/dam/cfm/models/product";
    protected static final String SKU_CANDACE_DRESS = "VD02";
    protected static final String SKU_CANDACE_PATH = "venia-dresses/candace-dress";
    protected static final String URL_KEY_CANDACE = "candace-dress";
    protected static final String CAT_VENIA_DRESSES = "venia-dresses";
    protected static final int CAT_ID_SAMPLE = stableId("venia-dresses");
    protected static final String SEARCH_TERM = "dress";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @BeforeClass
    public static void configureRestAssured() {
        Assume.assumeTrue(Boolean.getBoolean("celadon.it.enabled"));
        RestAssured.baseURI = System.getProperty("celadon.it.baseUrl", "http://localhost:4502");
        RestAssured.basePath = System.getProperty("celadon.it.basePath", "/apps/celadon/graphql");
        RestAssured.authentication = RestAssured.preemptive().basic(
                System.getProperty("celadon.it.user", "admin"),
                System.getProperty("celadon.it.password", "admin")
        );
    }

    protected ValidatableResponse postQuery(String query) {
        return postQuery(query, Map.of());
    }

    protected ValidatableResponse postQuery(String query, Map<String, Object> variables) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", compactQuery(query));
        payload.put("variables", variables);
        return given()
                .config(restConfig())
                .contentType("application/json")
                .body(GSON.toJson(payload))
                .post()
                .then()
                .statusCode(200)
                .body("errors", nullValue())
                .body("data", notNullValue());
    }

    protected ValidatableResponse postQueryAllowErrors(String query, Map<String, Object> variables) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", compactQuery(query));
        payload.put("variables", variables);
        return given()
                .config(restConfig())
                .contentType("application/json")
                .body(GSON.toJson(payload))
                .post()
                .then()
                .statusCode(200)
                .body("data", notNullValue());
    }

    protected String compactQuery(String query) {
        return query.replaceAll("\\s+", " ").trim();
    }

    protected RestAssuredConfig restConfig() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig().setParam("http.protocol.expect-continue", false));
    }

    protected String existingProductModelPath() {
        Response response = given()
                .basePath("")
                .auth().preemptive().basic(
                        System.getProperty("celadon.it.user", "admin"),
                        System.getProperty("celadon.it.password", "admin")
                )
                .get(PRODUCT_MODEL_PATH_WE_RETAIL + "/jcr:content.json");
        return response.statusCode() == 200 ? PRODUCT_MODEL_PATH_WE_RETAIL : PRODUCT_MODEL_PATH_VENIA;
    }

    private static int stableId(String key) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int candidate = ((bytes[0] & 0xff) << 24)
                    | ((bytes[1] & 0xff) << 16)
                    | ((bytes[2] & 0xff) << 8)
                    | (bytes[3] & 0xff);
            candidate = candidate & 0x7fffffff;
            return candidate == 0 ? 1 : candidate;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

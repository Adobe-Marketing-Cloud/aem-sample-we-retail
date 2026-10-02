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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * GraphQL-endpoint fidelity coverage for the We-Retail catalog imported in BLUEPRINT mode.
 *
 * <p>Unlike {@link LegacyTagModeIT} (which reads raw JCR to verify placement), these tests hit the
 * live Celadon GraphQL servlet at {@code celadon.it.basePath} and assert the storefront-facing shape
 * of the import: the authored category distribution, multi-axis variant flattening, multi-category
 * membership, and editorial (page-tree) slugs.
 *
 * <p><b>Precondition:</b> the served catalog must be the We-Retail blueprint import
 * (e.g. {@code celadon/it-we-retail-blueprint}): 60 products across the Women / Men / Equipment tree.
 * These are exact assertions on purpose — they pin the fidelity of the blueprint re-homing and the
 * {@code cq:productVariantAxes} variant fix, so a regression in either surfaces here.
 *
 * <p>Gated by {@code -Dceladon.it.enabled=true}.
 */
public class WeRetailBlueprintFidelityIT extends GraphqlITBase {

    // Editorial url_key harvested from the product-page tree (NOT the SKU) for the Sonja jacket.
    private static final String SONJA_URL_KEY = "sonja-insulated-jacket";
    private static final String SONJA_SKU = "wootwisot";
    // Multi-category product: belongs to both equipment/hiking and women/pants via blueprint matchTags.
    private static final String HIKING_PANTS_SKU = "331050524";
    private static final String HIKING_PANTS_URL_KEY = "hiking-pants";
    // Declares no cq:productVariantAxes and references its image through assets/* instead of image.
    private static final String HIKING_POLES_SKU = "170227049";

    /**
     * Golden ground truth for the complete blueprint catalog (60 products), captured from the live
     * endpoint. Each row is {@code sku|type|url_key|sortedCategoryMembership}, where type is
     * {@code C}onfigurable or {@code S}imple and membership is the comma-joined sorted set of
     * {@code categories.url_path}. This pins, for EVERY product, its editorial slug, type and full
     * (multi-)category placement — a regression in blueprint re-homing or slug harvest fails here.
     */
    private static final String[] CATALOG = {
            "116138647|S|faba-running-pants|equipment,equipment/running,women,women/pants",
            "170227049|S|hiking-poles|equipment,equipment/hiking",
            "30941863|S|basa-mp3-player|equipment,equipment/running",
            "331050524|S|hiking-pants|equipment,equipment/hiking,women,women/pants",
            "84391210|S|spixa-runners|equipment,equipment/running",
            "90019930|S|gloves|women,women/gloves",
            "eqbisublp|S|blast-mini-pump|equipment,equipment/biking",
            "eqbisucol|S|compact-chain-tool|equipment,equipment/biking",
            "eqbisucos|C|comfort-gel-gloves|equipment,equipment/biking",
            "eqbisumas|C|marin-mountain-bike-shoes|equipment,equipment/biking",
            "eqbisumxs|C|mx121-sport-pedals|equipment,equipment/biking",
            "eqbisuset|C|sequoia-bike-helmet|equipment,equipment/biking",
            "eqrusubpe|S|bpa-free-water-bottle|equipment,equipment/running",
            "eqrusufle|C|fleet-cross-training-shoe|equipment,equipment/running",
            "eqrusugor|S|gomobile-fitness-monitorsmart-music-player|equipment,equipment/running",
            "eqsmcz|C|cuzco|equipment,equipment/hiking",
            "eqsusubed|S|bear-8_8_-longboard|equipment,equipment/surfing",
            "eqsusuchd|S|chasing-tail-surfboard|equipment,equipment/surfing",
            "eqsusuely|S|el-greco-8_-shorty|equipment,equipment/surfing",
            "eqsusuevd|S|even-keel-paddleboard|equipment,equipment/surfing",
            "eqsusurud|S|rusty-parrot-shortboad|equipment,equipment/surfing",
            "eqsusuthd|S|the-stretch-longboard|equipment,equipment/surfing",
            "eqsusutho|S|the-neutrino|equipment,equipment/surfing",
            "eqswsuaqr|C|aqua-force-gps-fitness-monitor|equipment,equipment/water-sports",
            "eqswsucal|C|cabana-stripe-towel|equipment,equipment/water-sports",
            "eqswsumak|C|manta-ray-snorkel-and-dive-mask|equipment,equipment/water-sports",
            "eqswsuprs|C|pro-series-swim-goggles|equipment,equipment/water-sports",
            "eqswsupus|C|pufferfish-snorkeling-fins|equipment,equipment/water-sports",
            "eqswsuses|C|seastar-flip-flops|equipment,equipment/water-sports",
            "eqwrsnbd|C|flying-snowboard|equipment,equipment/snow-sports",
            "mehisubus|C|buffalo-plaid-shorts|equipment,equipment/hiking,men,men/shorts",
            "mehisucos|C|corona-shorts|equipment,equipment/hiking,men,men/shorts",
            "mehisulat|C|laguna-short-sleeve-shirt|equipment,equipment/hiking,men,men/shirts",
            "mehisusls|C|slot-canyon-active-shorts|equipment,equipment/hiking,men,men/shorts",
            "mehisusts|C|stretch-fatigue-shorts|equipment,equipment/hiking,men,men/shorts",
            "mehisutrs|C|trail-model-pants|equipment,equipment/hiking,men,men/pants",
            "mehiwiext|C|expedition-tech-long-sleeved-shirt|equipment,equipment/hiking,men,men/shirts",
            "meotsuamt|C|amsterdam-short-sleeve-travel-shirt|men,men/shirts",
            "meotsuann|C|analog-dark-wash-jean|men,men/pants",
            "meotsuett|C|eton-short-sleeve-shirt|men,men/shirts",
            "meotsutrs|C|tribeca-cargo-pants|men,men/pants",
            "meotwibrt|C|brooklyn-coat|men,men/coats",
            "meotwicls|C|classic-leather-gloves|men,men/gloves",
            "meotwidot|C|double-breasted-raincoat|men,men/coats",
            "meotwipot|C|portland-hooded-jacket|men,men/coats",
            "meotwislt|C|slopeside-coat|men,men/coats",
            "meotwisus|C|sussex-rain-boots|men,men/footwear",
            "meotwizuf|C|zurich-wool-scarf|men,men/scarfs",
            "meskwielt|C|el-gordo-down-jacket|equipment,equipment/snow-sports,men,men/coats",
            "meskwimis|C|midweight-fleece-gloves|equipment,equipment/snow-sports,men,men/gloves",
            "mesusupis|C|pipeline-board-shorts|equipment,equipment/surfing,men,men/shorts",
            "wohisucat|C|candide-trail-short|equipment,equipment/hiking,women,women/shorts",
            "wohisudes|C|desert-sky-shorts|equipment,equipment/hiking,women,women/shorts",
            "wohisufls|C|fleet-fox-running-shorts|equipment,equipment/hiking,women,women/shorts",
            "wohisurit|C|rios-t-shirt|equipment,equipment/hiking,women,women/shirts",
            "wootsudet|C|devi-sleeveless-shirt|women,women/shirts",
            "wootwisot|C|sonja-insulated-jacket|women,women/coats",
            "woskwislt|C|sleek-insulated-coat|equipment,equipment/snow-sports,women,women/coats",
            "woswsubas|C|bahamas-shorts|equipment,equipment/water-sports,women,women/shorts",
            "woswsusoc|C|soleil-tunic|equipment,equipment/water-sports,women,women/shirts",
    };

    /**
     * Golden axes + flattened variant count for the 43 configurable products, as
     * {@code sku|sortedAxes|variantCount}. Pins the {@code cq:productVariantAxes} flattening for
     * every configurable — single-axis (size/color) and multi-axis (color,size) alike.
     */
    private static final String[] CONFIGURABLES = {
            "eqbisucos|size|3", "eqbisumas|size|6", "eqbisumxs|size|1",
            "eqbisuset|size|4", "eqrusufle|size|3", "eqsmcz|color|2", "eqswsuaqr|size|1",
            "eqswsucal|size|1", "eqswsumak|size|1", "eqswsuprs|color,size|3", "eqswsupus|color,size|9",
            "eqswsuses|color,size|24", "eqwrsnbd|size|1", "mehisubus|size|4", "mehisucos|size|5",
            "mehisulat|size|5", "mehisusls|size|6", "mehisusts|size|4", "mehisutrs|size|5",
            "mehiwiext|size|5", "meotsuamt|size|6", "meotsuann|size|9", "meotsuett|size|6",
            "meotsutrs|size|6", "meotwibrt|size|4", "meotwicls|size|3", "meotwidot|size|4",
            "meotwipot|size|4", "meotwislt|size|5", "meotwisus|size|6", "meotwizuf|size|1",
            "meskwielt|color,size|15", "meskwimis|size|1", "mesusupis|color,size|15", "wohisucat|size|6",
            "wohisudes|size|6", "wohisufls|size|5", "wohisurit|size|6", "wootsudet|size|6",
            "wootwisot|color,size|10", "woskwislt|size|5", "woswsubas|size|5", "woswsusoc|color,size|10",
    };

    /**
     * The blueprint re-homes products into the authored Women / Men / Equipment tree. Aggregate
     * product counts (including descendants and multi-category memberships) match the classic
     * We-Retail distribution.
     */
    @Test
    public void shouldExposeAuthoredCategoryDistribution() {
        postQuery(Queries.WR_CATEGORY_TREE)
                .body("data.categoryList[0].children_count", is("3"))
                .body("data.categoryList[0].children.find { it.url_path == 'women' }.product_count", equalTo(12))
                .body("data.categoryList[0].children.find { it.url_path == 'men' }.product_count", equalTo(21))
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }.product_count", equalTo(46));
    }

    /**
     * Navigation fidelity: the GraphQL endpoint serves categories in the authored blueprint
     * sequence with the editorial section titles, not alphabetically or by SKU. Order is conveyed
     * by the {@code children} array order (the {@code position} field is uniformly 0). This pins
     * both the blueprint section ordering and the page-tree title harvest end-to-end.
     */
    @Test
    public void shouldExposeAuthoredNavigationOrderAndTitles() {
        postQuery(Queries.WR_CATEGORY_TREE)
                // Top-level sections in authored order, by title and by url_path.
                .body("data.categoryList[0].children.name", contains("Women", "Men", "Equipment"))
                .body("data.categoryList[0].children.url_path", contains("women", "men", "equipment"))
                // Equipment leaves in authored order, by title.
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }.children.name",
                        contains("Surfing", "Running", "Water Sports", "Hiking", "Biking", "Snow Sports"))
                // Equipment leaves in authored order, by url_path (editorial slugs).
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }.children.url_path",
                        contains("equipment/surfing", "equipment/running", "equipment/water-sports",
                                "equipment/hiking", "equipment/biking", "equipment/snow-sports"));
    }

    /** Equipment leaves carry the exact authored counts that sum to the Equipment aggregate (46). */
    @Test
    public void shouldExposeEquipmentLeafCounts() {
        postQuery(Queries.WR_CATEGORY_TREE)
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }"
                        + ".children.find { it.url_path == 'equipment/hiking' }.product_count", equalTo(14))
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }"
                        + ".children.find { it.url_path == 'equipment/surfing' }.product_count", equalTo(8))
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }"
                        + ".children.find { it.url_path == 'equipment/water-sports' }.product_count", equalTo(8))
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }"
                        + ".children.find { it.url_path == 'equipment/running' }.product_count", equalTo(6))
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }"
                        + ".children.find { it.url_path == 'equipment/biking' }.product_count", equalTo(6))
                .body("data.categoryList[0].children.find { it.url_path == 'equipment' }"
                        + ".children.find { it.url_path == 'equipment/snow-sports' }.product_count", equalTo(4));
    }

    /**
     * The two-level JCR master -> color-group -> size-leaf structure is flattened into a single
     * ConfigurableProduct exposing BOTH the color and size axes, with all 10 combinations as
     * variants. This is the {@code cq:productVariantAxes} fix surfaced through GraphQL; the axes keep
     * their declared order, color before size.
     */
    @Test
    public void shouldFlattenMultiAxisVariants() {
        postQuery(Queries.WR_PRODUCT_BY_URL_KEY_VARIANTS, Map.of("urlKey", SONJA_URL_KEY))
                .body("data.products.items[0].__typename", equalTo("ConfigurableProduct"))
                .body("data.products.items[0].configurable_options.attribute_code", contains("color", "size"))
                .body("data.products.items[0].variants.size()", equalTo(10));
    }

    /**
     * Editorial slugs from the product-page tree are used as {@code url_key} instead of the SKU, so
     * the configurable product resolves by its human-friendly key while keeping the legacy SKU.
     */
    @Test
    public void shouldUseEditorialSlugAsUrlKey() {
        postQuery(Queries.WR_PRODUCT_BY_URL_KEY_VARIANTS, Map.of("urlKey", SONJA_URL_KEY))
                .body("data.products.items.size()", equalTo(1))
                .body("data.products.items[0].url_key", equalTo(SONJA_URL_KEY))
                .body("data.products.items[0].sku", equalTo(SONJA_SKU));
    }

    /**
     * A product whose tags satisfy multiple authored blueprint sections is stored as one canonical
     * CF with many memberships. The Hiking Pants live under both Equipment/Hiking and Women/Pants.
     */
    @Test
    public void shouldExposeMultiCategoryMembership() {
        postQuery(Queries.WR_PRODUCT_BY_SKU_CATEGORIES, Map.of("sku", HIKING_PANTS_SKU))
                .body("data.products.items[0].url_key", equalTo(HIKING_PANTS_URL_KEY))
                .body("data.products.items[0].categories.url_path",
                        hasItems("equipment", "equipment/hiking", "women", "women/pants"));
    }

    /**
     * A master that declares no {@code cq:productVariantAxes} is a simple product even though it has
     * a variant child, and an image referenced through {@code assets/*} is imported like an
     * {@code image} child. The Hiking Poles carry both legacy shapes.
     */
    @Test
    public void shouldImportUndeclaredAxesProductAsSimpleWithAssetsImage() {
        postQuery(Queries.PRODUCTS_BY_SKU_EQ, Map.of("sku", HIKING_POLES_SKU))
                .body("data.products.items[0].__typename", equalTo("SimpleProduct"))
                .body("data.products.items[0].image.url", endsWith("/equipment/hiking/hiking-poles_img_0.jpeg"));
    }

    /**
     * Whole-catalog fidelity: pins EVERY product. The full set of {@code sku|type|url_key|categories}
     * served by the endpoint must equal the golden {@link #CATALOG} captured from the import. Map
     * equality yields a precise diff (missing/extra SKU, wrong type, slug drift, membership change).
     */
    @Test
    public void shouldExposeAllProductsWithFidelity() {
        List<Map<String, Object>> items = postQuery(Queries.WR_ALL_PRODUCTS)
                .body("data.products.total_count", equalTo(CATALOG.length))
                .extract().path("data.products.items");

        Map<String, String> actual = new TreeMap<>();
        for (Map<String, Object> item : items) {
            String type = "ConfigurableProduct".equals(item.get("__typename")) ? "C" : "S";
            actual.put((String) item.get("sku"), type + "|" + item.get("url_key") + "|" + sortedCategories(item));
        }

        Map<String, String> expected = new TreeMap<>();
        for (String row : CATALOG) {
            String[] f = row.split("\\|", 4);
            expected.put(f[0], f[1] + "|" + f[2] + "|" + f[3]);
        }
        assertEquals(expected, actual);
    }

    /**
     * Whole-catalog variant fidelity: for EVERY configurable product the served axes (sorted) and the
     * flattened variant count must equal the golden {@link #CONFIGURABLES}. Pins the
     * {@code cq:productVariantAxes} flattening across single- and multi-axis products at once.
     */
    @Test
    public void shouldExposeAllConfigurableAxesAndVariantCounts() {
        List<Map<String, Object>> items = postQuery(Queries.WR_ALL_PRODUCTS)
                .extract().path("data.products.items");

        Map<String, String> actual = new TreeMap<>();
        for (Map<String, Object> item : items) {
            if (!"ConfigurableProduct".equals(item.get("__typename"))) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) item.get("configurable_options");
            @SuppressWarnings("unchecked")
            List<Object> variants = (List<Object>) item.get("variants");
            String axes = options.stream()
                    .map(o -> (String) o.get("attribute_code"))
                    .sorted()
                    .collect(Collectors.joining(","));
            actual.put((String) item.get("sku"), axes + "|" + variants.size());
        }

        Map<String, String> expected = new TreeMap<>();
        for (String row : CONFIGURABLES) {
            String[] f = row.split("\\|", 3);
            expected.put(f[0], f[1] + "|" + f[2]);
        }
        assertEquals(expected, actual);
    }

    /** Comma-joined sorted set of a product's {@code categories.url_path}. */
    @SuppressWarnings("unchecked")
    private static String sortedCategories(Map<String, Object> item) {
        List<Map<String, Object>> categories = (List<Map<String, Object>>) item.get("categories");
        return categories.stream()
                .map(c -> (String) c.get("url_path"))
                .sorted()
                .collect(Collectors.joining(","));
    }
}

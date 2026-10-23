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
package com.adobe.cq.commerce.celadon.core.api;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ProductsDataFetcherTest {
    private static final String CANDACE_SKU = "candace-dress-sku";
    private static final String CANDACE_VARIANT_SKU = "candace-dress-lilac-l";
    private static final String MEHISUCOS_SKU = "mehisucos";

    @Test
    public void shouldResolveConfigurableProductWithVariantsAndCategories() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                """
                        {
                          products(filter:{ url_key:{ eq:"candace-dress" } }) {
                            items {
                              __typename
                              sku
                              url_key
                              url_path
                              description { html }
                              short_description { html }
                              image { url }
                              media_gallery { url }
                              categories { uid name breadcrumbs { category_uid } }
                              price_range {
                                minimum_price { final_price { value currency } }
                                maximum_price { final_price { value currency } }
                              }
                              ... on ConfigurableProduct {
                                configurable_options { label attribute_code values { value_index label uid swatch_data { __typename value } } }
                                variants {
                                  attributes { label code value_index uid }
                                  product {
                                    sku
                                    name
                                    short_description { html }
                                    image { url }
                                    color
                                    price_range { minimum_price { final_price { value } } }
                                  }
                                }
                              }
                            }
                          }
                        }
                        """,
                null,
                null
        );

        Map<String, Object> product = firstProduct(response);
        Assert.assertEquals("ConfigurableProduct", product.get("__typename"));
        Assert.assertEquals(CANDACE_SKU, product.get("sku"));
        Assert.assertEquals("candace-dress", product.get("url_key"));
        // url_path is null (Magento parity); CIF derives the PDP URL from url_rewrites + context.
        Assert.assertNull(product.get("url_path"));
        Assert.assertEquals("Flowing dress for spring.", castMap(product.get("description")).get("html"));
        Assert.assertEquals("Elegant dress", castMap(product.get("short_description")).get("html"));
        Assert.assertFalse(castMap(product.get("image")).get("url").toString().contains("["));
        Assert.assertEquals(49.99, ((Number) castMap(castMap(castMap(product.get("price_range")).get("minimum_price")).get("final_price")).get("value")).doubleValue(), 0.001);
        Assert.assertEquals(69.99, ((Number) castMap(castMap(castMap(product.get("price_range")).get("maximum_price")).get("final_price")).get("value")).doubleValue(), 0.001);

        List<Map<String, Object>> categories = castList(product.get("categories"));
        Assert.assertEquals("venia-dresses", categories.getFirst().get("uid"));
        Assert.assertEquals("Dresses", categories.getFirst().get("name"));
        Assert.assertEquals(0, castList(categories.getFirst().get("breadcrumbs")).size());

        List<Map<String, Object>> options = castList(product.get("configurable_options"));
        Assert.assertEquals("Fashion Color", options.getFirst().get("label"));
        Assert.assertEquals("Lilac", castList(options.getFirst().get("values")).getFirst().get("label"));

        List<Map<String, Object>> variants = castList(product.get("variants"));
        Assert.assertEquals(2, variants.size());
        Assert.assertEquals("Lilac", castList(variants.getFirst().get("attributes")).getFirst().get("label"));
        Map<String, Object> variantProduct = castMap(variants.getFirst().get("product"));
        Assert.assertEquals(CANDACE_VARIANT_SKU, variantProduct.get("sku"));
        Assert.assertEquals("Elegant dress", castMap(variantProduct.get("short_description")).get("html"));
        Assert.assertEquals(92, ((Number) variantProduct.get("color")).intValue());
        Assert.assertFalse(castMap(variantProduct.get("image")).get("url").toString().isBlank());
    }

    @Test
    public void shouldResolveConfigurableProductFromOptionDefinitionFragments() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                """
                        {
                          products(filter:{ url_key:{ eq:"candace-dress" } }) {
                            items {
                              sku
                              ... on ConfigurableProduct {
                                configurable_options {
                                  label
                                  attribute_code
                                  values { value_index label uid swatch_data { __typename value } }
                                }
                                variants {
                                  attributes { code value_index uid label }
                                }
                              }
                            }
                          }
                        }
                        """,
                null,
                null
        );

        Map<String, Object> product = firstProduct(response);
        List<Map<String, Object>> options = castList(product.get("configurable_options"));
        Assert.assertEquals(2, options.size());
        Map<String, Object> colorOption = options.getFirst();
        Assert.assertEquals("Fashion Color", colorOption.get("label"));
        Assert.assertEquals("fashion_color", colorOption.get("attribute_code"));

        List<Map<String, Object>> colorValues = castList(colorOption.get("values"));
        Assert.assertEquals(2, colorValues.size());
        Assert.assertEquals("Lilac", colorValues.getFirst().get("label"));
        Assert.assertEquals(92, ((Number) colorValues.getFirst().get("value_index")).intValue());
        Assert.assertEquals("celadon-fashion_color-92", colorValues.getFirst().get("uid"));
        Map<String, Object> swatch = castMap(colorValues.getFirst().get("swatch_data"));
        Assert.assertEquals("ColorSwatchData", swatch.get("__typename"));
        Assert.assertEquals("#fee1d2", swatch.get("value"));

        List<Map<String, Object>> sizeValues = castList(options.get(1).get("values"));
        Assert.assertEquals(1, sizeValues.size());
        Assert.assertNull(sizeValues.getFirst().get("swatch_data"));

        List<Map<String, Object>> variants = castList(product.get("variants"));
        Assert.assertEquals(2, variants.size());
        List<Map<String, Object>> firstVariantAttributes = castList(variants.getFirst().get("attributes"));
        Assert.assertEquals("Lilac", firstVariantAttributes.getFirst().get("label"));
        Assert.assertEquals("celadon-fashion_color-92", firstVariantAttributes.getFirst().get("uid"));
    }

    @Test
    public void shouldResolveLegacyLabelBasedVariantAttributesFromConfigurableOptions() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engineWithLegacyLabelVariants();

        Map<String, Object> response = engine.execute(
                """
                        {
                          products(filter:{ sku:{ eq:"mehisucos" } }) {
                            items {
                              sku
                              ... on ConfigurableProduct {
                                configurable_options {
                                  attribute_code
                                  values { uid label value_index }
                                }
                                variants {
                                  attributes { code uid label value_index }
                                  product { sku }
                                }
                              }
                            }
                          }
                        }
                        """,
                null,
                null
        );

        Map<String, Object> product = firstProduct(response);
        Assert.assertEquals(MEHISUCOS_SKU, product.get("sku"));

        List<Map<String, Object>> options = castList(product.get("configurable_options"));
        Assert.assertEquals("size", options.getFirst().get("attribute_code"));
        List<Map<String, Object>> optionValues = castList(options.getFirst().get("values"));
        Assert.assertEquals(1, ((Number) optionValues.getFirst().get("value_index")).intValue());
        Assert.assertEquals("celadon-size-1", optionValues.getFirst().get("uid"));

        List<Map<String, Object>> variants = castList(product.get("variants"));
        Assert.assertEquals(5, variants.size());

        List<Map<String, Object>> firstVariantAttributes = castList(variants.getFirst().get("attributes"));
        Assert.assertEquals("size", firstVariantAttributes.getFirst().get("code"));
        Assert.assertEquals("XS", firstVariantAttributes.getFirst().get("label"));
        Assert.assertEquals(1, ((Number) firstVariantAttributes.getFirst().get("value_index")).intValue());
        Assert.assertEquals("celadon-size-1", firstVariantAttributes.getFirst().get("uid"));
        Assert.assertEquals("mehisucos-xs", castMap(variants.getFirst().get("product")).get("sku"));

        List<Map<String, Object>> lastVariantAttributes = castList(variants.getLast().get("attributes"));
        Assert.assertEquals("XL", lastVariantAttributes.getFirst().get("label"));
        Assert.assertEquals(5, ((Number) lastVariantAttributes.getFirst().get("value_index")).intValue());
        Assert.assertEquals("celadon-size-5", lastVariantAttributes.getFirst().get("uid"));
        Assert.assertEquals("mehisucos-xl", castMap(variants.getLast().get("product")).get("sku"));
    }

    @Test
    public void shouldResolveVariantAttributesFromCfFieldsIgnoringVariationIdNaming() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engineWithLegacyLabelVariants();

        Map<String, Object> response = engine.execute(
                """
                        {
                          products(filter:{ url_key:{ eq:"authored-dress" } }) {
                            items {
                              sku
                              ... on ConfigurableProduct {
                                variants {
                                  attributes { code value_index uid label }
                                  product { sku color image { url } }
                                }
                              }
                            }
                          }
                        }
                        """,
                null,
                null
        );

        Map<String, Object> product = firstProduct(response);
        Assert.assertEquals("authored-dress-sku", product.get("sku"));

        List<Map<String, Object>> variants = castList(product.get("variants"));
        Assert.assertEquals(2, variants.size());

        List<Map<String, Object>> firstAttributes = castList(variants.getFirst().get("attributes"));
        Assert.assertEquals("fashion_color", firstAttributes.getFirst().get("code"));
        Assert.assertEquals("Peach", firstAttributes.getFirst().get("label"));
        Assert.assertEquals(93, ((Number) firstAttributes.getFirst().get("value_index")).intValue());
        Assert.assertEquals("celadon-fashion_color-93", firstAttributes.getFirst().get("uid"));
        Assert.assertEquals("fashion_size", firstAttributes.get(1).get("code"));
        Assert.assertEquals("L", firstAttributes.get(1).get("label"));
        Assert.assertEquals(137, ((Number) firstAttributes.get(1).get("value_index")).intValue());

        Map<String, Object> firstVariantProduct = castMap(variants.getFirst().get("product"));
        Assert.assertEquals("authored-dress-peach-l", firstVariantProduct.get("sku"));
        Assert.assertEquals(93, ((Number) firstVariantProduct.get("color")).intValue());
        Assert.assertTrue(
                castMap(firstVariantProduct.get("image")).get("url").toString().endsWith("/authored-dress_peach.jpeg")
        );

        List<Map<String, Object>> secondAttributes = castList(variants.get(1).get("attributes"));
        Assert.assertEquals("Lilac", secondAttributes.getFirst().get("label"));
        Assert.assertEquals(92, ((Number) secondAttributes.getFirst().get("value_index")).intValue());
        Assert.assertEquals("authored-dress-lilac-l", castMap(variants.get(1).get("product")).get("sku"));
    }

    @Test
    public void shouldSupportSearchSortingAndPaginationContracts() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                """
                        {
                          products(search:"dress", pageSize:1, currentPage:1, sort:{ price:DESC }) {
                            total_count
                            sort_fields { default options { value } }
                            page_info { current_page page_size total_pages }
                            items { sku name }
                          }
                        }
                        """,
                null,
                null
        );

        Map<String, Object> products = castMap(castMap(response.get("data")).get("products"));
        Assert.assertEquals(2, ((Number) products.get("total_count")).intValue());
        Assert.assertEquals(1, ((Number) castMap(products.get("page_info")).get("current_page")).intValue());
        Assert.assertEquals(1, ((Number) castMap(products.get("page_info")).get("page_size")).intValue());
        Assert.assertEquals(2, ((Number) castMap(products.get("page_info")).get("total_pages")).intValue());
        Assert.assertEquals("relevance", castMap(products.get("sort_fields")).get("default"));
        Assert.assertEquals(CANDACE_SKU, castList(products.get("items")).getFirst().get("sku"));
    }

    @Test
    public void shouldResolveStoredSkuAndRetainPathLookupFallback() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> byStoredSku = engine.execute(
                "{ products(filter:{ sku:{ eq:\"candace-dress-sku\" } }) { items { sku url_path } } }",
                null,
                null
        );
        Map<String, Object> storedMatch = firstProduct(byStoredSku);
        Assert.assertEquals(CANDACE_SKU, storedMatch.get("sku"));
        Assert.assertNull(storedMatch.get("url_path"));

        Map<String, Object> byLegacyPath = engine.execute(
                "{ products(filter:{ sku:{ eq:\"venia-dresses/candace-dress\" } }) { items { sku url_path } } }",
                null,
                null
        );
        Map<String, Object> fallbackMatch = firstProduct(byLegacyPath);
        Assert.assertEquals(CANDACE_SKU, fallbackMatch.get("sku"));
        Assert.assertNull(fallbackMatch.get("url_path"));
    }

    @Test
    public void shouldSupportBrowseAllAndRejectOutOfRangePages() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> browse = engine.execute(
                "{ products(filter:{}) { total_count page_info { total_pages } sort_fields { default } items { sku } } }",
                null,
                null
        );
        Map<String, Object> products = castMap(castMap(browse.get("data")).get("products"));
        Assert.assertEquals(2, ((Number) products.get("total_count")).intValue());
        Assert.assertEquals("position", castMap(products.get("sort_fields")).get("default"));

        Map<String, Object> invalidPage = engine.execute(
                "{ products(filter:{}, pageSize:1, currentPage:99) { total_count } }",
                null,
                null
        );
        Assert.assertTrue(((List<?>) invalidPage.get("errors")).size() >= 1);
    }

    @Test
    public void shouldResolveCategoryListAndCategoriesContracts() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> categoryList = engine.execute(
                "{ categoryList { uid name url_path children_count product_count children { uid } } }",
                null,
                null
        );
        List<Map<String, Object>> list = castList(castMap(categoryList.get("data")).get("categoryList"));
        Assert.assertEquals("/", list.getFirst().get("uid"));
        Assert.assertEquals(2, Integer.parseInt(list.getFirst().get("children_count").toString()));
        Assert.assertEquals(2, Integer.parseInt(list.getFirst().get("product_count").toString()));

        Map<String, Object> categories = engine.execute(
                "{ categories(filters:{ category_uid:{ eq:\"venia-dresses\" } }) { total_count items { uid products { total_count } } } }",
                null,
                null
        );
        Map<String, Object> payload = castMap(castMap(categories.get("data")).get("categories"));
        Assert.assertEquals(1, Integer.parseInt(payload.get("total_count").toString()));
        Assert.assertEquals(2, Integer.parseInt(castMap(castList(payload.get("items")).getFirst().get("products")).get("total_count").toString()));
    }

    @Test
    public void shouldCountAdditionalCategoryMembersInProductCount() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        // angelina-tank-dress lives under venia-dresses but also has
        // additionalCategories=[venia-tops]; it must be counted under venia-tops.
        Map<String, Object> response = engine.execute(
                "{ categoryList { uid name product_count children { uid product_count } } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Map<String, Object> root = roots.getFirst();

        // Root still counts each product once even though angelina is reachable via two paths.
        Assert.assertEquals(2, Integer.parseInt(root.get("product_count").toString()));

        Map<String, Object> dresses = null;
        Map<String, Object> tops = null;
        for (Map<String, Object> child : castList(root.get("children"))) {
            if ("venia-dresses".equals(child.get("uid"))) {
                dresses = child;
            } else if ("venia-tops".equals(child.get("uid"))) {
                tops = child;
            }
        }
        Assert.assertNotNull("venia-dresses category present", dresses);
        Assert.assertNotNull("venia-tops category present", tops);
        // venia-dresses unchanged (both products physically live there).
        Assert.assertEquals(2, Integer.parseInt(dresses.get("product_count").toString()));
        // venia-tops now has the cross-listed angelina-tank-dress.
        Assert.assertEquals(1, Integer.parseInt(tops.get("product_count").toString()));
    }

    @Test
    public void shouldListMultiCategoryProductUnderEachCategory() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        // angelina-tank-dress (primary venia-dresses, additional venia-tops) must
        // appear when filtering products by EITHER category.
        Map<String, Object> underTops = engine.execute(
                "{ products(filter:{ category_uid:{ eq:\"venia-tops\" } }) { total_count items { sku url_path } } }",
                null,
                null
        );
        Map<String, Object> topsProducts = castMap(castMap(underTops.get("data")).get("products"));
        Assert.assertEquals(1, ((Number) topsProducts.get("total_count")).intValue());
        Map<String, Object> item = castList(topsProducts.get("items")).getFirst();
        Assert.assertEquals("angelina-tank-dress-sku", item.get("sku"));
        // url_path is null regardless of entry category; CIF scores url_rewrites by context.
        Assert.assertNull(item.get("url_path"));

        // It is still listed under its primary category too.
        Map<String, Object> underDresses = engine.execute(
                "{ products(filter:{ category_uid:{ eq:\"venia-dresses\" } }) { total_count items { sku } } }",
                null,
                null
        );
        Map<String, Object> dressesProducts = castMap(castMap(underDresses.get("data")).get("products"));
        Assert.assertEquals(2, ((Number) dressesProducts.get("total_count")).intValue());
    }

    @Test
    public void shouldReportEveryCategoryChainOnMultiCategoryProduct() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ products(filter:{ sku:{ eq:\"angelina-tank-dress-sku\" } }) { items { sku url_path categories { uid name } } } }",
                null,
                null
        );
        Map<String, Object> product = firstProduct(response);
        Assert.assertEquals("angelina-tank-dress-sku", product.get("sku"));
        // url_path is null (Magento parity); category context is preserved via url_rewrites.
        Assert.assertNull(product.get("url_path"));

        // categories reports BOTH branches: venia-dresses (primary) and venia-tops (additional).
        List<Map<String, Object>> categories = castList(product.get("categories"));
        List<String> uids = categories.stream().map(c -> c.get("uid").toString()).toList();
        Assert.assertEquals(List.of("venia-dresses", "venia-tops"), uids);
    }

    @Test
    public void shouldEmitUrlRewritePerCategoryMembershipWithSuffix() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        // angelina-tank-dress (primary venia-dresses, additional venia-tops) must expose one
        // suffixed url_rewrite per membership plus the bare leaf, mirroring Magento so CIF can
        // hydrate the PDP from either category context.
        Map<String, Object> response = engine.execute(
                "{ products(filter:{ sku:{ eq:\"angelina-tank-dress-sku\" } }) { items { sku url_rewrites { url } } } }",
                null,
                null
        );
        Map<String, Object> product = firstProduct(response);
        List<String> rewrites = castList(product.get("url_rewrites")).stream()
                .map(r -> r.get("url").toString())
                .toList();
        Assert.assertEquals(List.of(
                "angelina-tank-dress.html",
                "venia-dresses/angelina-tank-dress.html",
                "venia-tops/angelina-tank-dress.html"
        ), rewrites);
    }

    @Test
    public void shouldResolveProductViaAdditionalCategorySuffixedUrlKey() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        // CIF addresses the PDP by the suffixed additional-category url_rewrite; resolution
        // must find the product even though url_path is null.
        Map<String, Object> response = engine.execute(
                "{ products(filter:{ url_key:{ eq:\"venia-tops/angelina-tank-dress.html\" } }) { items { sku url_path } } }",
                null,
                null
        );
        Map<String, Object> product = firstProduct(response);
        Assert.assertEquals("angelina-tank-dress-sku", product.get("sku"));
        Assert.assertNull(product.get("url_path"));
    }

    @Test
    public void shouldNotResolveProductUnderNonMembershipCategoryPath() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        // candace-dress lives only under venia-dresses. Addressing it through venia-tops (a category
        // it does not belong to) must NOT resolve, matching Magento which 404s the wrong-category URL
        // instead of falling back to a bare leaf match.
        Map<String, Object> wrongCategory = engine.execute(
                "{ products(filter:{ url_key:{ eq:\"venia-tops/candace-dress.html\" } }) { total_count items { sku } } }",
                null,
                null
        );
        Map<String, Object> wrongCategoryProducts = castMap(castMap(wrongCategory.get("data")).get("products"));
        Assert.assertEquals(0, ((Number) wrongCategoryProducts.get("total_count")).intValue());
        Assert.assertTrue(castList(wrongCategoryProducts.get("items")).isEmpty());

        // The product still resolves through its real primary membership path (suffixed).
        Map<String, Object> primaryPath = engine.execute(
                "{ products(filter:{ url_key:{ eq:\"venia-dresses/candace-dress.html\" } }) { items { sku url_path } } }",
                null,
                null
        );
        Map<String, Object> match = firstProduct(primaryPath);
        Assert.assertEquals(CANDACE_SKU, match.get("sku"));
        Assert.assertNull(match.get("url_path"));
    }

    private static Map<String, Object> firstProduct(Map<String, Object> response) {
        return castList(castMap(castMap(response.get("data")).get("products")).get("items")).getFirst();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}

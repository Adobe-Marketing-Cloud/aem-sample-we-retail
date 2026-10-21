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

final class Queries {
    static final String INTROSPECTION_FILTER_INPUT = "{ __type(name:\"ProductAttributeFilterInput\") { name inputFields { name type { name kind } } } }";
    static final String CUSTOM_ATTRIBUTE_METADATA_ALL = "{ customAttributeMetadata(attributes:[]) { items { attribute_code attribute_type input_type } } }";
    static final String CUSTOM_ATTRIBUTE_METADATA_FILTERED = "{ customAttributeMetadata(attributes:[{attribute_code:\"sku\",entity_type:\"catalog_product\"}]) { items { attribute_code attribute_type input_type } } }";
    static final String CATEGORIES_BY_UID = "query($uid:String!){ categories(filters:{ category_uid:{ eq:$uid } }) { total_count items { uid url_path url_key } } }";
    static final String CATEGORY_LIST_ROOT = "{ categoryList { uid name url_path url_key children_count product_count } }";
    static final String CATEGORY_LIST_BY_UID_EQ = "query($uid:String!){ categoryList(filters:{ category_uid:{ eq:$uid } }) { uid name url_key url_path product_count children { uid } products(pageSize:3,currentPage:1){ total_count items { sku thumbnail { url } price_range { minimum_price { final_price { value currency } } } } } } }";
    static final String PRODUCTS_SEARCH = "query($search:String!,$page:Int!,$pageSize:Int!){ products(search:$search,currentPage:$page,pageSize:$pageSize){ sort_fields { default options { value label } } total_count page_info { current_page page_size total_pages } items { __typename sku name url_key url_path thumbnail { url } description { html } } } }";
    static final String PRODUCTS_BY_SKU_EQ = "query($sku:String!){ products(filter:{ sku:{ eq:$sku } }) { items { __typename sku name url_key url_path image { url } ... on ConfigurableProduct { variants { product { sku name image { url } price_range { minimum_price { final_price { value } } } } } } } } }";
    static final String PRODUCTS_BY_SKU_IN = "query($skus:[String]){ products(filter:{ sku:{ in:$skus } }) { items { __typename sku name ... on ConfigurableProduct { variants { product { sku name } } } } } }";
    static final String PRODUCTS_BY_URL_KEY = "query($urlKey:String!){ products(filter:{ url_key:{ eq:$urlKey } }) { items { __typename sku url_key url_path name categories { uid name url_path } ... on ConfigurableProduct { configurable_options { label attribute_code values { value_index label uid } } variants { attributes { label code value_index uid } product { sku name image { url } color price_range { minimum_price { final_price { value } } } } } } } } }";
    static final String PRODUCTS_BY_EMPTY_FILTER = "{ products(filter:{}) { sort_fields { default options { label value } } total_count items { sku } } }";
    static final String PRODUCTS_BY_SKU_EMPTY = "{ products(filter:{ sku:{} }) { items { sku } } }";
    static final String STORE_CONFIG_ROOT = "{ storeConfig { root_category_uid base_currency_code store_code configurable_thumbnail_source } }";
    static final String STOREFRONT_CONTEXT = "{ dataServicesStorefrontInstanceContext { catalog_extension_version environment environment_id store_code store_id store_name store_url store_view_code store_view_id store_view_name website_code website_id website_name } }";
    static final String VARIANT_COLOR_TYPE = "query($urlKey:String!){ products(filter:{ url_key:{ eq:$urlKey } }) { items { ... on ConfigurableProduct { variants { product { color } } } } } }";
    static final String CIF_PDP_COMPATIBILITY = "query($urlKey:String!){ products(filter:{ url_key:{ eq:$urlKey } }) { items { __typename sku url_key name description { html } image { url } thumbnail { url } media_gallery { url } ... on ConfigurableProduct { configurable_options { label attribute_code values { value_index label uid swatch_data { __typename value } } } variants { attributes { label code value_index uid } product { sku name image { url } thumbnail { url } media_gallery { url } price_range { minimum_price { final_price { value currency } } } } } } } } }";

    // --- We-Retail BLUEPRINT-import fidelity (served catalog = it-we-retail-blueprint) ---
    // Two-level category tree with aggregate product_count at each node — drives the blueprint
    // re-homing distribution (Women / Men / Equipment and their leaves).
    static final String WR_CATEGORY_TREE = "{ categoryList { children_count children { name url_path product_count children { name url_path product_count } } } }";
    // Configurable product by editorial url_key with both variant axes and flattened variants.
    static final String WR_PRODUCT_BY_URL_KEY_VARIANTS = "query($urlKey:String!){ products(filter:{ url_key:{ eq:$urlKey } }) { items { __typename sku url_key name ... on ConfigurableProduct { configurable_options { attribute_code label } variants { product { sku } } } } } }";
    // Product by SKU exposing its full (multi-)category membership — the blueprint matchTags case.
    static final String WR_PRODUCT_BY_SKU_CATEGORIES = "query($sku:String!){ products(filter:{ sku:{ eq:$sku } }) { items { sku url_key name categories { name url_path } } } }";
    // Product by url_key exposing both descriptions — the legacy summary / jcr:description split.
    static final String WR_PRODUCT_BY_URL_KEY_DESCRIPTIONS = "query($urlKey:String!){ products(filter:{ url_key:{ eq:$urlKey } }) { items { sku description { html } short_description { html } } } }";
    // Whole-catalog probe: every product with its type, editorial url_key, full category membership
    // and (for configurables) variant axes + flattened variants — drives all-product verification.
    static final String WR_ALL_PRODUCTS = "{ products(filter:{}, currentPage:1, pageSize:100){ total_count items { __typename sku url_key categories { url_path } ... on ConfigurableProduct { configurable_options { attribute_code } variants { product { sku } } } } } }";

    private Queries() {
    }
}

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
package com.adobe.cq.commerce.celadon.aem.attribute.model;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import java.util.Set;

/**
 * Builds Magento GraphQL queries for the importer from an {@link AttributeManifest}.
 *
 * <p>Universal fields (sku, name, description, image, price_range) are always
 * selected. Custom attributes in the manifest are appended as top-level scalar
 * selections. Variant-scoped (or both-scoped) attributes are additionally
 * selected on each {@code variant.product} inside the
 * {@code ConfigurableProduct} fragment.</p>
 *
 * <p>The shape mirrors the scaffold the {@code CeladonCatalogServlet} importer
 * consumes: {@code categories}, {@code media_gallery}, {@code image/thumbnail},
 * {@code configurable_options}, {@code variants.attributes/product}, etc.</p>
 */
public final class MagentoImportQueryBuilder {

    private static final Set<String> UNIVERSAL_FIELDS = Set.of("sku", "name", "description", "image");

    private MagentoImportQueryBuilder() {}

    /**
     * Builds the primary products query used for catalog import.
     */
    public static String fromManifest(AttributeManifest manifest) {
        StringBuilder b = new StringBuilder();
        b.append("query CeladonImport($currentPage:Int,$search:String) {\n");
        b.append("  products(search:$search, pageSize:100, currentPage:$currentPage) {\n");
        b.append("    total_count\n");
        b.append("    items {\n");
        appendProductFields(b, manifest, "      ");
        b.append("    }\n");
        b.append("  }\n");
        b.append("}\n");
        return b.toString();
    }

    /**
     * Builds the category-scoped fallback products query used when the primary
     * call returns zero items.
     */
    public static String categoryFallbackFromManifest(AttributeManifest manifest) {
        StringBuilder b = new StringBuilder();
        b.append("query CeladonImportFallback($categoryUid:String!,$currentPage:Int!,$search:String) {\n");
        b.append("  products(filter:{category_uid:{eq:$categoryUid}}, pageSize:100, currentPage:$currentPage, search:$search) {\n");
        b.append("    items {\n");
        appendProductFields(b, manifest, "      ");
        b.append("    }\n");
        b.append("  }\n");
        b.append("}\n");
        return b.toString();
    }

    private static void appendProductFields(StringBuilder b, AttributeManifest manifest, String indent) {
        b.append(indent).append("__typename sku url_key name\n");
        b.append(indent).append("description { html }\n");
        b.append(indent).append("image { label url } thumbnail { label url }\n");
        b.append(indent).append("price_range {\n");
        b.append(indent).append("  minimum_price { regular_price { value currency } final_price { value currency } }\n");
        b.append(indent).append("  maximum_price { final_price { value currency } }\n");
        b.append(indent).append("}\n");
        b.append(indent).append("media_gallery { __typename disabled url label position }\n");
        b.append(indent).append("categories { __typename uid name url_key url_path breadcrumbs { category_url_path } }\n");
        for (AttributeEntry e : manifest.entries()) {
            if (UNIVERSAL_FIELDS.contains(e.code()) || "price".equals(e.code())) {
                continue;
            }
            b.append(indent).append(e.code()).append("\n");
        }
        b.append(indent).append("... on ConfigurableProduct {\n");
        b.append(indent).append("  configurable_options { label attribute_code values { value_index label default_label uid swatch_data { __typename value } } }\n");
        b.append(indent).append("  variants {\n");
        b.append(indent).append("    attributes { code value_index uid label }\n");
        b.append(indent).append("    product {\n");
        b.append(indent).append("      sku name description { html } image { label url } thumbnail { label url }\n");
        b.append(indent).append("      url_key\n");
        b.append(indent).append("      price_range { minimum_price { regular_price { value currency } final_price { value currency } } }\n");
        b.append(indent).append("      media_gallery { __typename disabled url label position }\n");
        b.append(indent).append("      categories { __typename uid name image }\n");
        for (AttributeEntry e : manifest.entries()) {
            if (!e.scope().appliesToVariant()
                    || UNIVERSAL_FIELDS.contains(e.code())
                    || "price".equals(e.code())) {
                continue;
            }
            b.append(indent).append("      ").append(e.code()).append("\n");
        }
        b.append(indent).append("    }\n");
        b.append(indent).append("  }\n");
        b.append(indent).append("}\n");
    }
}

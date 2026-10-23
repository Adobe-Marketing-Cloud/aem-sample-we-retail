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

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import com.google.gson.GsonBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TestCatalogFixtures {
    private TestCatalogFixtures() {
    }

    static CeladonGraphqlEngine engine() {
        FetcherContext context = new FetcherContext("celadon/venia");
        return new CeladonGraphqlEngine(context, new GsonBuilder().serializeNulls().create(), new FixtureCatalogGateway(context));
    }

    static CeladonGraphqlEngine engineWithLegacyLabelVariants() {
        FetcherContext context = new FetcherContext("celadon/venia");
        return new CeladonGraphqlEngine(context, new GsonBuilder().serializeNulls().create(), new FixtureCatalogGateway(context, true));
    }

    /**
     * Engine whose manifest declares non-reserved custom attributes ({@code material} STRING,
     * {@code activities} MULTISELECT). The simple product fixture carries matching CF elements,
     * so these attributes are selectable on the product output types.
     */
    static CeladonGraphqlEngine engineWithCustomAttributes() {
        FetcherContext context = new FetcherContext("celadon/venia");
        AttributeManifest manifest = new AttributeManifest("venia", List.of(
                AttributeEntry.of("material", "Material", NormalizedType.STRING,
                        AttributeScope.PRODUCT, false, false, 10),
                AttributeEntry.of("activities", "Activities", NormalizedType.MULTISELECT,
                        AttributeScope.PRODUCT, false, false, 20)));
        return new CeladonGraphqlEngine(context, new GsonBuilder().serializeNulls().create(),
                CatalogGatewayFactory.fixed(new FixtureCatalogGateway(context)), manifest);
    }

    /**
     * Engine whose manifest exercises every output coercion path: numeric ({@code weight} INT,
     * {@code rating} FLOAT, {@code msrp} PRICE), {@code featured} BOOLEAN, {@code tags}
     * MULTISELECT (from a comma string), {@code material} STRING (master + per-variation), and
     * {@code activities} MULTISELECT (from a list). It also declares {@code name} INT, whose code
     * collides with a reserved base field, to verify the data-level reserved-field guard.
     */
    static CeladonGraphqlEngine engineWithTypedCustomAttributes() {
        FetcherContext context = new FetcherContext("celadon/venia");
        AttributeManifest manifest = new AttributeManifest("venia", List.of(
                AttributeEntry.of("material", "Material", NormalizedType.STRING,
                        AttributeScope.BOTH, false, false, 10),
                AttributeEntry.of("activities", "Activities", NormalizedType.MULTISELECT,
                        AttributeScope.PRODUCT, false, false, 20),
                AttributeEntry.of("tags", "Tags", NormalizedType.MULTISELECT,
                        AttributeScope.PRODUCT, false, false, 30),
                AttributeEntry.of("units_per_pack", "Units Per Pack", NormalizedType.INT,
                        AttributeScope.PRODUCT, false, false, 40),
                AttributeEntry.of("rating", "Rating", NormalizedType.FLOAT,
                        AttributeScope.PRODUCT, false, false, 50),
                AttributeEntry.of("msrp", "MSRP", NormalizedType.PRICE,
                        AttributeScope.PRODUCT, false, false, 60),
                AttributeEntry.of("featured", "Featured", NormalizedType.BOOLEAN,
                        AttributeScope.PRODUCT, false, false, 70),
                AttributeEntry.of("name", "Name", NormalizedType.INT,
                        AttributeScope.PRODUCT, false, false, 80)));
        return new CeladonGraphqlEngine(context, new GsonBuilder().serializeNulls().create(),
                CatalogGatewayFactory.fixed(new FixtureCatalogGateway(context)), manifest);
    }

    static final class FixtureCatalogGateway implements CatalogGateway {
        private final FetcherContext context;
        private final boolean includeLegacyLabelVariants;

        FixtureCatalogGateway(FetcherContext context) {
            this(context, false);
        }

        FixtureCatalogGateway(FetcherContext context, boolean includeLegacyLabelVariants) {
            this.context = context;
            this.includeLegacyLabelVariants = includeLegacyLabelVariants;
        }

        @Override
        public Map<String, Object> getListing(String relativePath) {
            String path = normalizeRelativePath(relativePath);
            return switch (path) {
                case "" -> rootListing();
                case "venia-dresses" -> dressesListing();
                case "venia-tops" -> map("properties", map("name", "venia-tops"), "entities", List.of());
                default -> Map.of("entities", List.of());
            };
        }

        @Override
        public Map<String, Object> getFolderJcrContent(String categoryPath) {
            String path = normalizeRelativePath(categoryPath);
            return switch (path) {
                case "venia-dresses" -> map(
                        "jcr:title", "Dresses",
                        "folderThumbnailPath", "/content/dam/celadon/venia/venia-dresses/jcr:content/folderThumbnail"
                );
                case "venia-tops" -> map(
                        "jcr:title", "Tops",
                        "folderThumbnailPath", "/content/dam/celadon/venia/venia-tops/jcr:content/manualThumbnail.jpg"
                );
                default -> Map.of();
            };
        }

        @Override
        public Map<String, Object> getProductJcrContent(String categoryPath, String productName) {
            if ("venia-dresses".equals(normalizeRelativePath(categoryPath))) {
                if ("candace-dress".equals(productName)) {
                    return map(
                            "configurableOptionDefinitions", List.of(
                                    map(
                                            "label", "Fashion Color",
                                            "attributeCode", "fashion_color",
                                            "productField", "color",
                                            "swatchType", "color",
                                            "values", List.of(
                                                    "92;Lilac;#fee1d2",
                                                    "93;Peach;#ffd6b3"
                                            )
                                    ),
                                    map(
                                            "label", "Fashion Size",
                                            "attributeCode", "fashion_size",
                                            "productField", "size",
                                            "swatchType", "",
                                            "values", List.of("137;L")
                                    )
                            )
                    );
                }
                if ("mehisucos".equals(productName) && includeLegacyLabelVariants) {
                    return map(
                            "configurableOptionDefinitions", List.of(
                                    map(
                                            "label", "Size",
                                            "attributeCode", "size",
                                            "productField", "size",
                                            "swatchType", "",
                                            "values", List.of("1;XS", "2;S", "3;M", "4;L", "5;XL")
                                    )
                            )
                    );
                }
                if ("authored-dress".equals(productName) && includeLegacyLabelVariants) {
                    return map(
                            "configurableOptionDefinitions", List.of(
                                    map(
                                            "label", "Fashion Color",
                                            "attributeCode", "fashion_color",
                                            "productField", "color",
                                            "swatchType", "color",
                                            "values", List.of(
                                                    "92;Lilac;#fee1d2",
                                                    "93;Peach;#ffd6b3"
                                            )
                                    ),
                                    map(
                                            "label", "Fashion Size",
                                            "attributeCode", "fashion_size",
                                            "productField", "size",
                                            "swatchType", "",
                                            "values", List.of("137;L")
                                    )
                            )
                    );
                }
            }
            return Map.of();
        }

        @Override
        public String toAssetUrl(String imagePath) {
            if (imagePath == null || imagePath.isBlank()) {
                return "";
            }
            return imagePath.startsWith("/") ? imagePath : "/" + imagePath;
        }

        private Map<String, Object> rootListing() {
            return map(
                    "properties", map("name", "venia"),
                    "entities", List.of(
                            map(
                                    "class", List.of("assets/folder"),
                                    "properties", map("name", "_options")
                            ),
                            map(
                                    "class", List.of("assets/folder"),
                                    "properties", map("name", "venia-dresses")
                            ),
                            map(
                                    "class", List.of("assets/folder"),
                                    "properties", map("name", "venia-tops")
                            )
                    )
            );
        }

        private Map<String, Object> dressesListing() {
            List<Map<String, Object>> entities = new java.util.ArrayList<>(List.of(
                    configurableProduct(),
                    simpleProduct(),
                    imageAsset("candace-dress_var-fashion_color-92-fashion_size-137_img_1.jpeg",
                            "/content/dam/celadon/venia/venia-dresses/candace-dress_var-fashion_color-92-fashion_size-137_img_1.jpeg"),
                    imageAsset("candace-dress_var-fashion_color-93-fashion_size-137_img_1.jpeg",
                            "/content/dam/celadon/venia/venia-dresses/candace-dress_var-fashion_color-93-fashion_size-137_img_1.jpeg")
            ));
            if (includeLegacyLabelVariants) {
                entities.add(legacyLabelConfigurableProduct());
                entities.add(authoredVariantProduct());
            }
            return map(
                    "properties", map("name", "venia-dresses"),
                    "entities", entities
            );
        }

        private Map<String, Object> authoredVariantProduct() {
            return map(
                    "class", List.of("assets/asset"),
                    "properties", map(
                            "name", "authored-dress",
                            "contentFragment", true,
                            "elements", map(
                                    "name", map(
                                            "value", "Authored Dress",
                                            "variationsOrder", List.of(
                                                    "summer-peach",
                                                    "summer-lilac"
                                            ),
                                            "variations", map(
                                                    "summer-peach", map("value", "Authored Dress Peach"),
                                                    "summer-lilac", map("value", "Authored Dress Lilac")
                                            )
                                    ),
                                    "description", map("value", "Authored dress with arbitrary variant names."),
                                    "sku", map(
                                            "value", "authored-dress-sku",
                                            "variations", map(
                                                    "summer-peach", map("value", "authored-dress-peach-l"),
                                                    "summer-lilac", map("value", "authored-dress-lilac-l")
                                            )
                                    ),
                                    "color", map(
                                            "value", "",
                                            "variations", map(
                                                    "summer-peach", map("value", "Peach"),
                                                    "summer-lilac", map("value", "Lilac")
                                            )
                                    ),
                                    "size", map(
                                            "value", "",
                                            "variations", map(
                                                    "summer-peach", map("value", "L"),
                                                    "summer-lilac", map("value", "L")
                                            )
                                    ),
                                    "price", map(
                                            "value", 45.0,
                                            "variations", map(
                                                    "summer-peach", map("value", 45.0),
                                                    "summer-lilac", map("value", 45.0)
                                            )
                                    ),
                                    "image", map(
                                            "value", List.of("/content/dam/celadon/venia/venia-dresses/authored-dress_img_1.jpeg"),
                                            "variations", map(
                                                    "summer-peach", map("value", "/content/dam/celadon/venia/venia-dresses/authored-dress_peach.jpeg"),
                                                    "summer-lilac", map("value", "/content/dam/celadon/venia/venia-dresses/authored-dress_lilac.jpeg")
                                            )
                                    )
                            )
                    )
            );
        }

        private Map<String, Object> configurableProduct() {
            return map(
                    "class", List.of("assets/asset"),
                    "properties", map(
                            "name", "candace-dress",
                            "contentFragment", true,
                            "elements", map(
                                    "name", map(
                                            "value", "Candace Dress",
                                            "variationsOrder", List.of(
                                                    "var-fashion_color-92-fashion_size-137",
                                                    "var-fashion_color-93-fashion_size-137"
                                            ),
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", "Candace Dress Lilac"),
                                                    "var-fashion_color-93-fashion_size-137", map("value", "Candace Dress Peach")
                                            )
                                    ),
                                    "description", map(
                                            "value", "Flowing dress for spring.",
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", "Lilac flowing dress."),
                                                    "var-fashion_color-93-fashion_size-137", map("value", "Peach flowing dress.")
                                            )
                                    ),
                                    "sku", map(
                                            "value", "candace-dress-sku",
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", "candace-dress-lilac-l"),
                                                    "var-fashion_color-93-fashion_size-137", map("value", "candace-dress-peach-l")
                                            )
                                    ),
                                    "color", map(
                                            "value", "",
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", "Lilac"),
                                                    "var-fashion_color-93-fashion_size-137", map("value", "Peach")
                                            )
                                    ),
                                    "size", map(
                                            "value", "",
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", "L"),
                                                    "var-fashion_color-93-fashion_size-137", map("value", "L")
                                            )
                                    ),
                                    "short_description", map("value", "Elegant dress"),
                                    "price", map(
                                            "value", 49.99,
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", 59.99),
                                                    "var-fashion_color-93-fashion_size-137", map("value", "69.99")
                                            )
                                    ),
                                    "image", map(
                                            "value", List.of(
                                                    "/content/dam/celadon/venia/venia-dresses/candace-dress_img_1.jpeg,/content/dam/celadon/venia/venia-dresses/candace-dress_img_2.jpeg"
                                            ),
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", "/content/dam/celadon/venia/venia-dresses/candace-dress_lilac.jpeg")
                                            )
                                    ),
                                    "material", map(
                                            "value", "Polyester",
                                            "variations", map(
                                                    "var-fashion_color-92-fashion_size-137", map("value", "Silk")
                                            )
                                    )
                            )
                    )
            );
        }

        private Map<String, Object> simpleProduct() {
            return map(
                    "class", List.of("assets/asset"),
                    "properties", map(
                            "name", "angelina-tank-dress",
                            "contentFragment", true,
                            "elements", map(
                                    "name", map("value", "Angelina Tank Dress"),
                                    "sku", map("value", "angelina-tank-dress-sku"),
                                    "description", map("value", "Tank dress with floral pattern."),
                                    "price", map("value", 35.0),
                                    "image", map("value", List.of("/content/dam/celadon/venia/venia-dresses/angelina-tank-dress_img_1.jpeg")),
                                    "material", map("value", "Cotton"),
                                    "activities", map("value", List.of("Hiking", "Camping")),
                                    "tags", map("value", "Summer, Floral , New"),
                                    "units_per_pack", map("value", "3"),
                                    "rating", map("value", "4.5"),
                                    "msrp", map("value", 42.0),
                                    "featured", map("value", "true"),
                                    "additionalCategories", map("value", List.of("venia-tops"))
                            )
                    )
            );
        }

        private Map<String, Object> legacyLabelConfigurableProduct() {
            return map(
                    "class", List.of("assets/asset"),
                    "properties", map(
                            "name", "mehisucos",
                            "contentFragment", true,
                            "elements", map(
                                    "name", map(
                                            "value", "Mehisucos",
                                            "variationsOrder", List.of(
                                                    "var-size-XS",
                                                    "var-size-S",
                                                    "var-size-M",
                                                    "var-size-L",
                                                    "var-size-XL"
                                            ),
                                            "variations", map(
                                                    "var-size-XS", map("value", "Mehisucos XS"),
                                                    "var-size-S", map("value", "Mehisucos S"),
                                                    "var-size-M", map("value", "Mehisucos M"),
                                                    "var-size-L", map("value", "Mehisucos L"),
                                                    "var-size-XL", map("value", "Mehisucos XL")
                                            )
                                    ),
                                    "description", map("value", "Legacy label-based size variants."),
                                    "sku", map(
                                            "value", "mehisucos",
                                            "variations", map(
                                                    "var-size-XS", map("value", "mehisucos-xs"),
                                                    "var-size-S", map("value", "mehisucos-s"),
                                                    "var-size-M", map("value", "mehisucos-m"),
                                                    "var-size-L", map("value", "mehisucos-l"),
                                                    "var-size-XL", map("value", "mehisucos-xl")
                                            )
                                    ),
                                    "size", map(
                                            "value", "",
                                            "variations", map(
                                                    "var-size-XS", map("value", "XS"),
                                                    "var-size-S", map("value", "S"),
                                                    "var-size-M", map("value", "M"),
                                                    "var-size-L", map("value", "L"),
                                                    "var-size-XL", map("value", "XL")
                                            )
                                    ),
                                    "price", map(
                                            "value", 11.0,
                                            "variations", map(
                                                    "var-size-XS", map("value", 11.0),
                                                    "var-size-S", map("value", 12.0),
                                                    "var-size-M", map("value", 13.0),
                                                    "var-size-L", map("value", 14.0),
                                                    "var-size-XL", map("value", 15.0)
                                            )
                                    ),
                                    "image", map("value", List.of("/content/dam/celadon/venia/venia-dresses/mehisucos_img_1.jpeg"))
                            )
                    )
            );
        }

        private Map<String, Object> imageAsset(String name, String path) {
            return map(
                    "class", List.of("assets/asset"),
                    "properties", map(
                            "name", name,
                            "path", path,
                            "contentFragment", false
                    )
            );
        }

        private String normalizeRelativePath(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            String normalized = value.trim();
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        }
    }

    static Map<String, Object> map(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(values[index].toString(), values[index + 1]);
        }
        return map;
    }
}

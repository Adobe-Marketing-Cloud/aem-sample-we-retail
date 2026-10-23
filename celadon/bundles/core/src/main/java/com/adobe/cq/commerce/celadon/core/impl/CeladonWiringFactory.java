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
package com.adobe.cq.commerce.celadon.core.impl;

import graphql.schema.DataFetcher;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CeladonWiringFactory implements WiringFactory {
    private final CatalogService catalogService;
    private final String catalog;
    private final Map<String, DataFetcher<?>> queryFetchers;

    public CeladonWiringFactory(CatalogService catalogService, Map<String, Map<String, String>> metadata, String catalog) {
        this.catalogService = catalogService;
        this.catalog = catalog == null || catalog.isBlank() ? "we-retail" : catalog;
        this.queryFetchers = new LinkedHashMap<>();
        queryFetchers.put("products", environment -> catalogService.products(environment));
        queryFetchers.put("categoryList", environment -> catalogService.categoryList(environment));
        queryFetchers.put("category", environment -> catalogService.category(environment));
        queryFetchers.put("categories", environment -> catalogService.categories(environment));
        queryFetchers.put("customAttributeMetadata", environment -> catalogService.customAttributeMetadata(environment, metadata));
        queryFetchers.put("cart", environment -> Map.of("id", "1"));
        queryFetchers.put("storeConfig", environment -> storefrontConfig());
        queryFetchers.put("dataServicesStorefrontInstanceContext", environment -> storefrontContext());
    }

    @Override
    public boolean providesDataFetcher(FieldWiringEnvironment environment) {
        return true;
    }

    @Override
    public DataFetcher<?> getDataFetcher(FieldWiringEnvironment environment) {
        return getDefaultDataFetcher(environment);
    }

    @Override
    public DataFetcher<?> getDefaultDataFetcher(FieldWiringEnvironment environment) {
        String typeName = environment.getParentType().getName();
        String fieldName = environment.getFieldDefinition().getName();
        if ("Query".equals(typeName)) {
            return queryFetchers.getOrDefault(fieldName, ignored -> Map.of("id", "1"));
        }
        return dataEnvironment -> defaultFieldValue(dataEnvironment.getSource(), fieldName);
    }

    @Override
    public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
        return true;
    }

    @Override
    public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
        return typeResolver();
    }

    @Override
    public boolean providesTypeResolver(UnionWiringEnvironment environment) {
        return true;
    }

    @Override
    public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        return typeResolver();
    }

    private TypeResolver typeResolver() {
        return environment -> {
            Object source = environment.getObject();
            String typeName = null;
            if (source instanceof Map<?, ?> map) {
                Object explicit = map.get("__resolveType");
                if (explicit == null) {
                    explicit = map.get("__typename");
                }
                if (explicit != null) {
                    typeName = explicit.toString();
                }
            }
            if (typeName == null && source != null) {
                typeName = source.getClass().getSimpleName();
            }
            return typeName == null ? null : environment.getSchema().getObjectType(typeName);
        };
    }

    private Object defaultFieldValue(Object source, String fieldName) {
        if (source == null) {
            return null;
        }
        if (source instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        try {
            Method getMethod = source.getClass().getMethod("get", String.class);
            return getMethod.invoke(source, fieldName);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method getter = source.getClass().getMethod("get" + suffix);
            return getter.invoke(source);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private Map<String, Object> storefrontConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("id", "1");
        config.put("root_category_uid", "/");
        config.put("base_currency_code", "USD");
        config.put("store_code", "default");
        config.put("product_url_suffix", PathSupport.PRODUCT_URL_SUFFIX);
        config.put("category_url_suffix", PathSupport.PRODUCT_URL_SUFFIX);
        config.put("configurable_thumbnail_source", "itself");
        config.put("secure_base_media_url", "http://localhost:4502/");
        config.put("secure_base_url", "http://localhost:4502/");
        config.put("store_name", catalog);
        return config;
    }

    private Map<String, Object> storefrontContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("catalog_extension_version", "1.0.0");
        context.put("environment", "development");
        context.put("environment_id", "celadon");
        context.put("store_code", "default");
        context.put("store_id", "1");
        context.put("store_name", catalog);
        context.put("store_url", "http://localhost:4502/content/" + catalog + "/us/en.html");
        context.put("store_view_code", "default");
        context.put("store_view_id", "1");
        context.put("store_view_name", "Default Store View");
        context.put("website_code", "base");
        context.put("website_id", "1");
        context.put("website_name", "Main Website");
        return context;
    }
}

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

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.impl.CatalogService;
import com.adobe.cq.commerce.celadon.core.impl.CeladonWiringFactory;
import com.adobe.cq.commerce.celadon.core.impl.GraphqlRequestContext;
import com.adobe.cq.commerce.celadon.core.impl.attribute.SchemaAddendumBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.Document;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CeladonGraphqlEngine {
    /**
     * Static, non-attribute schema additions: the Celadon storefront-instance context type
     * and the {@code Query} extension exposing it. Per-catalog attribute filter inputs are
     * generated separately by {@link SchemaAddendumBuilder} from the catalog manifest.
     */
    private static final String BASE_SCHEMA_ADDENDUM = """
            type CeladonStorefrontInstanceContext {
              catalog_extension_version: String
              environment: String
              environment_id: String
              store_code: String
              store_id: String
              store_name: String
              store_url: String
              store_view_code: String
              store_view_id: String
              store_view_name: String
              website_code: String
              website_id: String
              website_name: String
            }

            extend type Query {
              dataServicesStorefrontInstanceContext: CeladonStorefrontInstanceContext
            }
            """;

    private static final java.lang.reflect.Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private final FetcherContext fetcherContext;
    private final Gson gson;
    private final GraphQL graphQL;
    private final CatalogGatewayFactory catalogGatewayFactory;
    private final AttributeManifest manifest;
    // Shared, catalog-keyed snapshot cache for the engine's lifetime. The catalog
    // is immutable until a re-import + bundle restart (per the import design), so
    // caching the built snapshot avoids reloading the whole catalog from JCR on
    // every request (which otherwise exceeds CIF's GraphQL client socket timeout).
    private final Map<String, com.adobe.cq.commerce.celadon.core.impl.CatalogSnapshot> snapshotCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public CeladonGraphqlEngine(FetcherContext fetcherContext) {
        this(fetcherContext, new GsonBuilder().serializeNulls().create(), (CatalogGatewayFactory) null, null);
    }

    public CeladonGraphqlEngine(FetcherContext fetcherContext, CatalogGatewayFactory catalogGatewayFactory) {
        this(fetcherContext, new GsonBuilder().serializeNulls().create(), catalogGatewayFactory, null);
    }

    public CeladonGraphqlEngine(FetcherContext fetcherContext, AttributeManifest manifest) {
        this(fetcherContext, new GsonBuilder().serializeNulls().create(), (CatalogGatewayFactory) null, manifest);
    }

    public CeladonGraphqlEngine(FetcherContext fetcherContext,
                                AttributeManifest manifest,
                                CatalogGatewayFactory catalogGatewayFactory) {
        this(fetcherContext, new GsonBuilder().serializeNulls().create(), catalogGatewayFactory, manifest);
    }

    CeladonGraphqlEngine(FetcherContext fetcherContext, Gson gson, CatalogGateway catalogGateway) {
        this(fetcherContext, gson, catalogGateway == null ? null : CatalogGatewayFactory.fixed(catalogGateway), null);
    }

    CeladonGraphqlEngine(FetcherContext fetcherContext, Gson gson, CatalogGatewayFactory catalogGatewayFactory) {
        this(fetcherContext, gson, catalogGatewayFactory, null);
    }

    CeladonGraphqlEngine(FetcherContext fetcherContext,
                         Gson gson,
                         CatalogGatewayFactory catalogGatewayFactory,
                         AttributeManifest manifest) {
        this.fetcherContext = Objects.requireNonNull(fetcherContext, "fetcherContext");
        this.gson = gson == null ? new GsonBuilder().serializeNulls().create() : gson;
        // The core bundle ships no default gateway: the only implementation,
        // AemCatalogGateway, requires a ResourceResolver and lives in the aem
        // bundle. Every real caller (servlet, ProductReadEngine) and test supplies
        // an explicit gateway via a factory or execute(..., gateway), so the
        // no-gateway constructors only fail if execute() is called without one.
        this.catalogGatewayFactory = catalogGatewayFactory == null
                ? () -> {
                    throw new IllegalStateException(
                            "No CatalogGateway available; supply one via the constructor factory or execute(..., gateway)");
                }
                : catalogGatewayFactory;
        this.manifest = manifest == null
                ? AttributeManifest.empty(this.fetcherContext.basePath())
                : manifest;
        GraphQLSchema schema = buildSchema();
        this.graphQL = GraphQL.newGraphQL(schema).build();
    }

    /**
     * Drops all cached catalog snapshots so the next request rebuilds from the
     * current JCR state. Called when the catalog content changes (author edits,
     * importer writes, replication) so live edits are reflected without a restart.
     */
    public void invalidateSnapshots() {
        snapshotCache.clear();
    }

    public FetcherContext fetcherContext() {
        return fetcherContext;
    }

    public Gson gson() {
        return gson;
    }

    public Map<String, Object> execute(String query, String operationName, Map<String, Object> variables) {
        return execute(query, operationName, variables, catalogGatewayFactory);
    }

    public Map<String, Object> execute(String query, String operationName, Map<String, Object> variables, CatalogGateway gateway) {
        return execute(query, operationName, variables, CatalogGatewayFactory.fixed(Objects.requireNonNull(gateway, "gateway")));
    }

    private Map<String, Object> execute(String query,
                                        String operationName,
                                        Map<String, Object> variables,
                                        CatalogGatewayFactory gatewayFactory) {
        try (CatalogGateway gateway = Objects.requireNonNull(gatewayFactory.create(), "gatewayFactory.create()")) {
            if (!gateway.isCatalogReady()) {
                return notReadyResult();
            }
            GraphqlRequestContext requestContext = new GraphqlRequestContext(
                    fetcherContext, gateway, snapshotCache, fetcherContext.basePath());
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                    .query(query == null ? "" : query)
                    .operationName(operationName)
                    .variables(variables == null ? Map.of() : variables)
                    .graphQLContext(Map.of(CatalogService.requestContextKey(), requestContext))
                    .build();
            ExecutionResult result = graphQL.execute(executionInput);
            Map<String, Object> specification = new LinkedHashMap<>(result.toSpecification());
            Object data = specification.get("data");
            if (!(data instanceof Map<?, ?>)) {
                specification.put("data", new LinkedHashMap<>());
            }
            List<?> errors = (List<?>) specification.get("errors");
            if (errors == null || errors.isEmpty()) {
                specification.remove("errors");
            }
            return specification;
        }
    }

    private Map<String, Object> notReadyResult() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", new LinkedHashMap<>());
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("message", "catalog not ready");
        error.put("extensions", Map.of("code", "CATALOG_NOT_READY"));
        response.put("errors", List.of(error));
        return response;
    }

    private GraphQLSchema buildSchema() {
        Document document = new IntrospectionResultToSchema().createSchemaDefinition(buildIntrospectionMap());
        TypeDefinitionRegistry registry = new SchemaParser().buildRegistry(document);
        registry.merge(new SchemaParser().parse(BASE_SCHEMA_ADDENDUM));
        String manifestAddendum = SchemaAddendumBuilder.fromManifest(manifest);
        if (!manifestAddendum.isBlank()) {
            registry.merge(new SchemaParser().parse(manifestAddendum));
        }
        // Make manifest attributes selectable on the product output types. The set of
        // fields already declared on ProductInterface (and its concrete implementors)
        // is reserved so the addendum never re-declares a base field, and the same set
        // tells CatalogService which manifest codes to inject into the result maps.
        ProductOutputContext outputContext = productOutputContext(registry);
        String outputAddendum = SchemaAddendumBuilder.productOutputFields(
                manifest, outputContext.reservedFields(), outputContext.implementors());
        if (!outputAddendum.isBlank()) {
            registry.merge(new SchemaParser().parse(outputAddendum));
        }
        CatalogService catalogService = new CatalogService(manifest, outputContext.reservedFields());
        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(new CeladonWiringFactory(catalogService, buildAttributeMetadata(registry), catalogName()))
                .build();
        return new SchemaGenerator().makeExecutableSchema(registry, wiring);
    }

    /**
     * Derives the storefront catalog name from the configured {@code basePath}
     * (e.g. {@code celadon/we-retail} → {@code we-retail}), so storefront metadata
     * such as {@code store_name}/{@code store_url} self-adjusts to the served catalog.
     */
    private String catalogName() {
        String basePath = fetcherContext.basePath();
        int idx = basePath.lastIndexOf('/');
        return idx >= 0 ? basePath.substring(idx + 1) : basePath;
    }

    private record ProductOutputContext(List<String> implementors, Set<String> reservedFields) {
    }

    /**
     * Collects the concrete types implementing {@code ProductInterface} and the union
     * of all field names declared on the interface and those implementors in the base
     * schema. The field names are reserved so the manifest output addendum cannot
     * redefine an existing field (which graphql-java rejects).
     */
    private ProductOutputContext productOutputContext(TypeDefinitionRegistry registry) {
        Set<String> reserved = new LinkedHashSet<>();
        List<String> implementors = new ArrayList<>();
        if (registry.getType("ProductInterface").orElse(null) instanceof InterfaceTypeDefinition productInterface) {
            productInterface.getFieldDefinitions().forEach(field -> reserved.add(field.getName()));
            for (ObjectTypeDefinition implementor : registry.getImplementationsOf(productInterface)) {
                implementors.add(implementor.getName());
                implementor.getFieldDefinitions().forEach(field -> reserved.add(field.getName()));
            }
        }
        return new ProductOutputContext(List.copyOf(implementors), Set.copyOf(reserved));
    }

    private Map<String, Object> buildIntrospectionMap() {
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("magento-schema-2.4.2ee.json"),
                        "magento-schema-2.4.2ee.json"),
                StandardCharsets.UTF_8)) {
            Map<String, Object> raw = gson.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return Map.of();
            }
            if (raw.containsKey("__schema")) {
                return raw;
            }
            Object data = raw.get("data");
            if (data instanceof Map<?, ?> map && map.containsKey("__schema")) {
                return JsonSupport.map(data);
            }
            return raw;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Magento schema", e);
        }
    }

    private Map<String, Map<String, String>> buildAttributeMetadata(TypeDefinitionRegistry registry) {
        Map<String, Map<String, String>> metadata = new LinkedHashMap<>();
        if (registry.getType("ProductInterface").orElse(null) instanceof InterfaceTypeDefinition definition) {
            definition.getFieldDefinitions().forEach(field -> metadata.put(field.getName(), metadataItem(field.getName(), field.getType())));
        } else if (registry.getType("SimpleProduct").orElse(null) instanceof ObjectTypeDefinition definition) {
            definition.getFieldDefinitions().forEach(field -> metadata.put(field.getName(), metadataItem(field.getName(), field.getType())));
        }
        return metadata;
    }

    private Map<String, String> metadataItem(String attributeCode, Type<?> type) {
        String typeName = namedType(type);
        String attributeType;
        String inputType;
        switch (typeName) {
            case "String", "ID" -> {
                attributeType = "String";
                inputType = "text";
            }
            case "Int" -> {
                attributeType = "Int";
                inputType = "text";
            }
            case "Float" -> {
                attributeType = "Float";
                inputType = "text";
            }
            case "Boolean" -> {
                attributeType = "Boolean";
                inputType = "boolean";
            }
            default -> {
                attributeType = "String";
                inputType = "select";
            }
        }
        return Map.of(
                "attribute_code", attributeCode,
                "attribute_type", attributeType,
                "input_type", inputType
        );
    }

    private String namedType(Type<?> type) {
        if (type instanceof NonNullType nonNullType) {
            return namedType(nonNullType.getType());
        }
        if (type instanceof ListType listType) {
            return namedType(listType.getType());
        }
        if (type instanceof TypeName typeName) {
            return typeName.getName();
        }
        return "String";
    }
}

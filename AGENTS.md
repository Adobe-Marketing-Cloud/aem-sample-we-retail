# We.Retail CIF Migration — Agent Guide

Guidance for AI agents working in this repository.

## Project Overview

This is the **We.Retail** AEM reference site (`com.adobe.cq.sample:we.retail.reactor`,
version `4.0.1-SNAPSHOT`). It is a multi-module Maven project that builds AEM content
packages and an OSGi bundle.

The codebase is mid-migration from **classic AEM Commerce** to **CIF** (Commerce
Integration Framework). The migration keeps the existing `weretail` presentation layer
(HTL, CSS, clientlibs) and replaces the legacy commerce runtime in `core` with CIF-backed
models and routing. The GraphQL backend is the local **Celadon** service
(`com.adobe.cq.commerce.celadon.aem.CeladonGraphqlServlet`), not a real Magento instance.
Celadon is built in-reactor as the `celadon` module and deployed alongside We.Retail.

See `cif_migration.md` for the full migration specification, scope, principles, and
acceptance criteria. Cart, checkout, and order history are explicitly out of scope and
must render empty; product recommendations are deferred (no-op stubs only).

## Modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| `parent` | `we.retail.parent` | Parent POM: dependency/plugin management, shared versions |
| `celadon` | `com.adobe.commerce.cif:celadon` | Local Celadon GraphQL backend: OSGi bundles + integration tests |
| `core` | `we.retail.core` | OSGi bundle: Sling Models, services, CIF adapter layer |
| `ui.apps` | `we.retail.ui.apps` | `/apps` content: components, templates, clientlibs |
| `ui.content` | `we.retail.ui.content` | Sample content under `/content` |
| `catalog` | `we.retail.catalog` | Celadon CFM models + blueprint-mode catalog content under `/content/dam/celadon` |
| `config` | `we.retail.config` | OSGi configs, incl. CIF/Celadon `osgiconfig` |
| `commons.content.slim` | `we.retail.commons.content.slim` | Shared commons content |
| `all` | `we.retail.all` | Combined package embedding all subpackages |
| `all-deps` | `we.retail.all.deps` | Self-contained package: embeds Core WCM, CIF Core, GraphQL client + Magento GraphQL bundles and the `all` subpackage. Only the AEM Commerce Add-on must be pre-installed |

Key versions (in `parent/pom.xml`): UberJar `6.4.4`, Core WCM Components `2.29.0`,
Core CIF Components `2.18.0`, GraphQL client `1.10.0`, Magento GraphQL `9.1.0-magento242ee`.

### Celadon backend (`celadon`)

`com.adobe.commerce.cif:celadon` (`0.1.0-SNAPSHOT`) has two submodules: `bundles`
(`bundles/aem` + `bundles/core`, the GraphQL servlet and engine) and `it` (REST-assured
HTTP integration tests, e.g. `WeRetailBlueprintFidelityIT`). It compiles with
`maven.compiler.release=21`, which is why the full reactor requires JDK 21 (see Build &
Deploy). The we.retail bundle (`core`) still targets Java 8 bytecode.

### CIF adapter layer (`core`)

New CIF code lives under `we.retail.core.commerce.cif`. Recommendation stubs are in
`we.retail.core.commerce.cif.recommendations` (`RecommendationService`,
`NoopRecommendationService`, `RecommendationItem`). Storefront Sling Models being
migrated to CIF are in `we.retail.core.model` (e.g. `ProductModel`, `ProductGrid`,
`ProductGridItem`, `ProductFilter`, `ProductItem`).

Product search: `ProductGrid` shows CIF search results only on the configured
`cq:cifSearchResultsPage` (`?search_query=`). The header search pop-up adds a products
section and a "see all results" link from
`we.retail.core.commerce.cif.search.ProductSuggestionsServlet`
(`<page>.productsuggestions.json?search_query=`); its script lives in the
`weretail/components/structure/search` clientlib. Product links are built by
`ProductGrid.toProductUrl`.

Breadcrumbs keep the classic `we-retail-product-page` policy (start level 5, current page
hidden); `CatalogBreadcrumb` applies it to the trail the CIF breadcrumb builds on category
and product pages.

### CIF/Celadon OSGi config (`config`)

`config/src/content/jcr_root/apps/weretail/osgiconfig/config/` contains:
- `com.adobe.cq.commerce.celadon.aem.CeladonGraphqlServlet.cfg.json` (Celadon backend)
- `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~we-retail.cfg.json`
- `com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl.cfg.json`
- `com.adobe.cq.commerce.core.components.internal.servlets.SpecificPageFilterFactory~default.cfg.json`

### Catalog content (`catalog`)

The `catalog` module ships everything Celadon needs to serve the `we-retail` catalog on a
fresh deploy, with **no import step**:
- CFM models under `conf/we-retail/settings/dam/cfm/models/`: `product`,
  `celadon-option-definition`, `celadon-attribute`. All three are required — without the
  option/attribute models, `configurable_options` (variant axes such as size/color)
  resolve empty.
- Blueprint-mode catalog content under `content/dam/celadon/we-retail/`: the authored
  category tree (`women`, `men`, `equipment`) plus `_manifest` and `_options`. Product
  fragments live under their blueprint categories and carry editorial slugs used for
  PDP/PLP URLs.

The package `filter.xml` covers all three model roots and `/content/dam/celadon`. The
served catalog is `we-retail` (default, `ready=true`, ~60 products / 21 categories).

## Build & Deploy

Run from the repository root with Maven 3. The full reactor requires **JDK 21** because
the `celadon` module compiles with `maven.compiler.release=21`. Surefire/Failsafe honor
`JAVA_HOME` (not the `java` on PATH), so point it at a JDK 21 home before building or
testing:

```bash
export JAVA_HOME=/path/to/jdk-21   # required for the celadon module + its ITs
mvn clean install                              # build all modules
mvn clean install -PautoInstallSinglePackage   # build + deploy the 'all' package to AEM
mvn clean test                                 # unit tests (core)
mvn clean verify                               # incl. Celadon HTTP integration tests (it)
```

Common single-module deploy from within a content module:

```bash
mvn clean install -PautoInstallPackage         # deploy that module's package
```

### One-click deploy (`all-deps`)

For a fresh instance that has **only** the AEM Commerce Add-on pre-installed, deploy the
self-contained `all-deps` package — it brings its own Core WCM, CIF Core, GraphQL client
and Magento GraphQL bundles plus the `all` subpackage, so no other dependency packages
are needed. Build the full reactor first (so the embedded artifacts exist), then install
just `all-deps`:

```bash
export JAVA_HOME=/path/to/jdk-21
mvn clean install                                          # build the whole reactor once
mvn clean install -PautoInstallSinglePackage -pl all-deps  # deploy all-deps to AEM
```

Target a non-default instance with `-Daem.host` / `-Daem.port`:

```bash
mvn clean install -PautoInstallSinglePackage -pl all-deps \
  -Daem.host=localhost -Daem.port=4602
```

### Target AEM instance

Author instance defaults (from `parent/pom.xml`): `localhost:4502`, credentials
`admin`/`admin`. Package Manager: `http://localhost:4502/crx/packmgr/service.jsp`.

To check what is installed on the running instance:

```bash
curl -s -u admin:admin "http://localhost:4502/crx/packmgr/service.jsp?cmd=ls"
```

## Conventions

### Jira

- CIF-related tickets for this project are in the **SITES** project.
- Commit message format:

  ```
  ticket_id - ticket_title
   * description point 1
   * description point 2
  ```

- The branch name for a Jira ticket is the `ticket_id`.
- For `aem-core-cif-components` tickets, use **CIF Components** as the JIRA component.
- Do **not** put internal JIRA links in public GitHub comments — use only the JIRA ticket ID.

### Code style

- Match the commenting style and density of surrounding code. Do not add verbose or
  explanatory comments beyond existing conventions, and never include change rationale
  as code comments.
- Preserve `weretail` resource types, HTL markup, CSS class names, and DOM structure for
  supported storefront surfaces (PDP, PLP, product filter, header). Change Java and
  configuration in preference to changing HTL (see `cif_migration.md` UI Preservation Rules).
- Use package managers / Maven for dependency changes; do not hand-edit version numbers.

### Git workflow

- Do not push, create PRs, rebase, or merge without explicit permission.
- Default working branch context is `cif3` (current migration branch).

## Testing

- Unit tests run in `core` via `mvn clean test` (JUnit 4, Mockito, `io.wcm` AEM mocks).
- Celadon HTTP integration tests live in `celadon/it` (REST-assured, Failsafe, e.g.
  `WeRetailBlueprintFidelityIT`) and run on `mvn clean verify` against a running 4502
  instance. They require `JAVA_HOME` to point at JDK 21.
- After changing a Sling Model or service, update the corresponding tests and run them.
- Suggested coverage per `cif_migration.md` §8.7: PDP/PLP models build the current HTL
  contract from CIF data; unsupported cart/order models removed or empty;
  `NoopRecommendationService` returns no items; feature flags disable transactional actions.

## Notes

- The upstream GitHub project (Adobe-Marketing-Cloud/aem-sample-we-retail) is archived;
  this fork carries the CIF migration work.
- `cif_migration.md` and `cif_migration_execution_prompt.md` are the source of truth for
  migration scope and sequencing — consult them before structural changes.

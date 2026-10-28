# judo-meta-expression-asm — module agent doctrine

## Module purpose

`judo-meta-expression-asm` is the *binding* between the metamodel-independent
expression trees of `judo-meta-expression` and the ASM (Abstract Structural
Model) metamodel — the EMF-shaped view of a generated JUDO application. It
defines no metamodel of its own; it supplies the concrete implementations that
let an expression tree be resolved, measured, validated and *built* against ASM.

"Binding" here is three concrete Java types, not an abstraction:

- **`AsmModelAdapter`** — the ASM instantiation of the twelve-parameter
  `ModelAdapter` contract, declared verbatim as
  `ModelAdapter<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference,
  EClass, EAttribute, EReference, EClassifier, Measure, Unit>`. It answers the
  questions the expression layer cannot answer for itself: resolve a `TypeName`
  (namespace-separated by `::`) to an `EClassifier`, a `MeasureName` to a
  `judo-meta-measure` `Measure`, an attribute or reference selector to an
  `EAttribute` / `EReference`, and decide type compatibility and
  navigability. It reads JUDO semantics out of ASM *extension annotations*
  through `AsmUtils.getExtensionAnnotationCustomValue` — `measured`/`measure`
  and `measured`/`unit` on an `EDataType`, `expression`/`getter`, `setter`,
  `default` and `range` on attributes and references,
  `parameterized`/`type` for parameterized transfer members, and
  `mappedEntityType`/`filter` on a mapped `EClass`. Those annotation names are
  the wire contract with the PSM→ASM transformation upstream. Its constructor
  takes two `ResourceSet`s: one for the ASM model, one for the Measure model.
- **`AsmMeasureProvider`** — `MeasureProvider<Measure, Unit>` over an ASM-side
  measure model, which is what makes `judo-meta-expression`'s `MeasureAdapter`
  unit-aware on ASM: measure/unit lookup, base-measure dimension resolution,
  and the duration rules (`DURATION_UNITS_SUPPORTING_ADDITION` —
  `MILLISECOND`…`WEEK`, i.e. the units for which temporal addition is defined,
  deliberately excluding month/year).
- **`ExpressionValidatorOnAsm`** — the validation entry point,
  `validateExpressionOnAsm(Logger, AsmModel, MeasureModel, ExpressionModel)`,
  throwing `ExpressionValidationException`; it wires an `AsmModelAdapter` into
  the expression validator so the generic EVL/Zeta rules run against real ASM
  types.

On top of the adapter, **`AsmJqlExtractor extends AdaptableJqlExtractor`** is
the build direction: given an ASM `ResourceSet`, a measure `ResourceSet` and a
target expression `ResourceSet` (plus an optional `JqlExpressionBuilderConfig`),
it walks the ASM model, extracts every JQL string carried in those `expression`
annotations, and populates an `ExpressionModel` with the resulting expression
trees and their `binding` elements. When no measure `ResourceSet` is supplied it
falls back to `MeasureModelResourceSupport.createMeasureResourceSet()`.

**Repository:** BlackBeltTechnology/judo-meta-expression-asm ·
**Artifact:** `hu.blackbelt.judo.meta:hu.blackbelt.judo.meta.expression.asm`
(packaging `pom`, version `${revision}` = `1.0.5-SNAPSHOT` Maven /
`1.0.5.qualifier` Eclipse) · **License:** EPL-2.0 · **Java:** 21 ·
**Build:** Maven 3.9.4+ / Eclipse Tycho 4.0.13.

## Reactor map

The root `pom.xml` declares its `<modules>` inside the `modules` profile, which
is active unless `-DskipModules=true` is passed. Build order is the declared
order — `adapter-asm` produces the artifact every other module consumes.

<modules>
  <module>adapter-asm</module>
  <module>adapter-asm-test</module>
  <module>builder-jql-asm</module>
  <module>builder-jql-asm-test</module>
  <module>feature-adapter-asm</module>
  <module>feature-builder-jql-asm</module>
  <module>osgi-itest</module>
  <module>site</module>
</modules>

| Module | Artifact / packaging | What it contributes |
|---|---|---|
| `adapter-asm` | `…expression.model.adapter.asm`, `eclipse-plugin` | The binding itself: `AsmModelAdapter` (the ASM instantiation of `ModelAdapter`, annotation-driven type/measure/expression resolution), `AsmMeasureProvider` (`MeasureProvider<Measure, Unit>` with duration-addition rules), and `ExpressionValidatorOnAsm` (validation entry point). Exports exactly one package, `hu.blackbelt.judo.meta.expression.adapters.asm`. |
| `adapter-asm-test` | `…expression.asm.model.test`, `jar` | JUnit 5 coverage of the adapter against real ASM models: `AsmModelAdapterTest`, `AsmMeasureProviderTest`, `AsmModelAdapterDimensionTest`, plus the runtime fixtures `FullAsmTest`, `MinimalAsmTest`, `MeasuredTest` and `IllegalAsmTest` (the negative case — malformed ASM must be rejected, not silently adapted). |
| `builder-jql-asm` | `…expression.builder.jql.asm`, `eclipse-plugin` | The build direction: `AsmJqlExtractor extends AdaptableJqlExtractor` walks an ASM model, pulls the JQL out of its `expression` annotations, and populates an `ExpressionModel` through an `AsmModelAdapter`. Exports exactly one package, `hu.blackbelt.judo.meta.expression.builder.jql.asm`. |
| `builder-jql-asm-test` | `…expression.builder.jql.asm.test`, `jar` | JUnit 5 coverage of extraction and binding: `AsmJqlExtractorTest`, `AsmJqlExpressionBuilderTest`, `AsmJqlExpressionBindingTest` — the last one asserts that extracted expressions land on the right attribute/reference/filter bindings. |
| `feature-adapter-asm` | `…expression.adapter.asm.feature`, `eclipse-feature` | Installable Eclipse feature wrapping the `adapter-asm` plugin. Pure descriptor module — no sources. |
| `feature-builder-jql-asm` | `…expression.builder.jql.asm.feature`, `eclipse-feature` | Installable Eclipse feature wrapping the `builder-jql-asm` plugin. Pure descriptor module — no sources. |
| `osgi-itest` | `…expression.asm.osgi.test`, `jar` | Pax Exam integration tests that boot an Apache Karaf 4.4.7 container and assert both bundles resolve and wire there — the OSGi import/export check that unit tests structurally cannot make. |
| `site` | `…expression.asm.site`, `eclipse-repository` | Builds the Eclipse P2 update site aggregating both features, published for "Install New Software" and consumed as a P2 repository by downstream Tycho builds. Categories are declared in `site/category.xml`. |

## Build commands

```bash
# Full build (requires Java 21)
./mvnw clean install

# Run all tests
./mvnw clean test

# Run tests for a specific module
./mvnw clean test -pl adapter-asm-test
./mvnw clean test -pl builder-jql-asm-test
./mvnw clean test -pl osgi-itest

# Run a single test class
./mvnw clean test -pl adapter-asm-test -Dtest=AsmModelAdapterTest

# Run a single test method
./mvnw clean test -pl adapter-asm-test -Dtest=AsmModelAdapterTest#testMethodName

# Skip tests
./mvnw clean install -DskipTests

# Update Eclipse P2 category versions
mvn clean install -P update-category-versions -f site/pom.xml

# Deploy to JUDO Nexus
./mvnw deploy -Psign-artifacts -Prelease-judong
```

> **Caveat on `update-category-versions`:** the command is carried forward from
> the previous documentation, but no profile with that id is declared in this
> repository's POMs (it exists in `judo-meta-expression`). Expect Maven to run
> with the profile inert; edit `site/category.xml` P2 URLs by hand here.

### Maven profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Default — activates all submodules (`skipModules != true`). Disable with `-DskipModules=true`. |
| `sign-artifacts` | GPG-signs build artifacts |
| `release-dummy` | Deploys to a dummy/test repository |
| `release-judong` | Deploys to JUDO Nexus (https://nexus.judo.technology) |
| `release-central` | Deploys to Maven Central via Sonatype OSS |
| `generate-github-asciidoc-diagrams` | Generates documentation diagrams |
| `update-source-code-license` | Updates license headers in source files |

## Technology stack

### Core technologies
- **Eclipse EMF** (2.21+) — Eclipse Modeling Framework for metamodel-driven development
- **Tycho** (4.0.13) — Maven plugin for building Eclipse plugins and OSGi bundles
- **Epsilon Runtime** (2.8.0) — Model transformation and execution engine
- **OSGi** — Module system for Java; bundles declare imports/exports in `META-INF/MANIFEST.MF`

### External model dependencies
- **judo-meta-expression** — Expression metamodel (types, operators, variables, bindings) and the `ModelAdapter` contract this module implements
- **judo-meta-asm** — Abstract Structural Model metamodel (`EClass`/`EPackage` wrappers, `AsmUtils` annotation access)
- **judo-meta-measure** — Measure/unit metamodel (`Measure`, `Unit`, `DurationUnit`, `BaseMeasure`, `DerivedMeasure`)
- **judo-meta-jql** — JUDO Query Language metamodel

### Build & quality
- **Maven** 3.9.4 (wrapper: `./mvnw`)
- **JUnit 5** (Jupiter) — Unit testing via Tycho surefire plugin
- **Pax Exam** 4.13.5 + **Apache Karaf** 4.4.7 — OSGi integration testing
- **JaCoCo** 0.8.12 — Code coverage
- **SLF4J** 2.0.16 + **Logback** 1.5.12 — Logging

## Architecture pointers

- `adapter-asm/src/main/java/.../adapters/asm/AsmModelAdapter.java` — the twelve-parameter `ModelAdapter` instantiation; read it before changing anything about ASM extension annotations or `::` namespace resolution.
- `adapter-asm/src/main/java/.../adapters/asm/AsmMeasureProvider.java` — measure/unit resolution and the `DURATION_UNITS_SUPPORTING_ADDITION` list.
- `adapter-asm/src/main/java/.../adapters/asm/ExpressionValidatorOnAsm.java` — `validateExpressionOnAsm(Logger, AsmModel, MeasureModel, ExpressionModel)`.
- `builder-jql-asm/src/main/java/.../builder/jql/asm/AsmJqlExtractor.java` — ASM → ExpressionModel extraction, all four constructor overloads and the measure-`ResourceSet` fallback.
- `adapter-asm/META-INF/MANIFEST.MF` and `builder-jql-asm/META-INF/MANIFEST.MF` — OSGi bundle metadata (imports, exports, `Require-Bundle`) for the two plugins; these govern Tycho resolution, independently of `pom.xml`.
- The root `pom.xml` is the parent POM: `${revision}` version property, module versions, dependency management, plugin configuration, and every profile listed above.
- `site/category.xml` — Eclipse P2 update site category definition; its repository URLs encode version numbers.
- `logback-test.xml` at the repo root is the shared test logging configuration (INFO level, console appender).
- `.github/workflows/` — GitHub Actions CI/CD pipeline definitions.
- [README.md](README.md) — project introduction and architecture overview.
- [CONTRIBUTING.md](CONTRIBUTING.md) — development setup, code structure, and submission guidelines.
- [.github/CIFLOW.md](.github/CIFLOW.md) — branching strategy and CI/CD pipeline details.
- [judo-community](https://github.com/BlackBeltTechnology/judo-community) — parent ecosystem documentation.
- `openspec/specs/adapter-asm/`, `openspec/specs/builder-jql-asm/` — the behavioural specifications for the two production modules.

## Development environment

**Required:**
- Java 21 JDK
- Maven 3.9.4+ (or use the included `./mvnw` wrapper)
- Git

**Optional (for Eclipse development):**
- Eclipse IDE with m2e, Epsilon, and Modeling Tools plugins
- XTend, XText, MWE, MWE2 features (for code generation)

## Git workflow

- **Main Branch:** `develop`
- **Versioning:** `1.0.5-SNAPSHOT` (Maven) / `1.0.5.qualifier` (Eclipse/Tycho)
- **Branching Model:** GitFlow — feature branches (`feature/JNG-*`), release branches, bugfix/hotfix/support branches
- **CI/CD:** GitHub Actions — automatic build, test, deploy, and release workflows
- **Issue Tracking:** JIRA (JNG-* tickets)

## Scope guard — invariants an edit must not break

1. **Every commit must reference a JIRA ticket number** (e.g. `JNG-123`). No commit without a ticket number.
2. **No Lombok** — Tycho does not support it. All source code is generated or hand-written.
3. **OSGi manifests (`META-INF/MANIFEST.MF`) must be updated when public API changes** — they declare bundle exports and imports independently from Maven dependencies.
4. **`adapter-asm` exports a single package:** `hu.blackbelt.judo.meta.expression.adapters.asm`. Adding a second export is an API decision, not a refactor.
5. **`builder-jql-asm` exports a single package:** `hu.blackbelt.judo.meta.expression.builder.jql.asm`.
6. **Version numbers use the `${revision}` property** with Tycho qualifier conversion between Maven (`-SNAPSHOT`) and Eclipse (`.qualifier`) formats; never hand-sync them.
7. **P2 update site URLs encode version numbers.** Historically synchronised via the `update-category-versions` profile — see the caveat under Build commands; today they must be updated in `site/category.xml`.
8. **The `AsmModelAdapter` constructor requires two `ResourceSet` arguments:** one for the ASM model and one for the Measure model.
9. **ASM extension annotation names are a cross-module contract** — `measured`/`measure`, `measured`/`unit`, `expression`/`getter`, `expression`/`setter`, `expression`/`default`, `expression`/`range`, `parameterized`/`type`, `mappedEntityType`/`filter`. Renaming one here silently breaks the PSM→ASM producer upstream and the runtime downstream.

## Code instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

<!-- dox-doctrine -->
## Documentation Update Protocol (WRITE discipline)

Per-directory `AGENTS.md` files form a tree. Each directory `AGENTS.md` is the
per-file record for the files in that directory. This module-root `AGENTS.md`
holds doctrine + architecture pointers only — never a per-file index.

**Keep the root lean.** This file loads into every agent turn — every byte costs
tokens on every turn. A verbose root file buries the rules the model must follow
(signal dilution) and measurably degrades adherence; a lean file keeps doctrine
salient. Default assumption: your update does NOT belong in the root — route it
by the table below.

**Route every doc update by kind:**

| Kind of update | Goes in |
|---|---|
| New file in a directory, or its per-file detail / change history | Nearest directory `AGENTS.md`. Add a `` | `<basename>` | <purpose> | `` row, path-alphabetical. |
| Data flow, protocol, architecture rationale | `docs/architecture.md` or a `docs/<topic>.md` |
| End-user / developer setup | `README.md` |
| Cross-cutting rule every agent needs every turn (rare) | this module-root `AGENTS.md` |

**Read before editing (chain walk).** Before editing a file, read the nearest
`AGENTS.md` chain root→leaf so you know the file's recorded purpose, contracts,
and change history. Do not edit blind.

**Update after editing (closeout pass).** After changing a file, update its row
in the nearest directory `AGENTS.md`: find the file's row, update its purpose in
place; if absent, add it in path-alphabetical order. New directory → scaffold
its `AGENTS.md`. One row per file. The purpose carries a one-line summary, key
exported symbols, contracts/invariants, and `See change: <id>` history.

**Row style (caveman).** Short declarative fragments. Drop articles. Subject →
verb → object, present tense. One fact per row. Prefer concrete tokens (paths,
symbols, env vars) over prose. Keep identifiers verbatim.

**Size rule — split an over-large directory `AGENTS.md` file-based.** pi
auto-injects a directory `AGENTS.md` on every turn when cwd sits at/below it, so
an over-large directory `AGENTS.md` is not supported. Split it file-based: a row
exceeding the length threshold promotes to a per-file `<File>.AGENTS.md`
sidecar carrying that file's full detail (including every `See change:`). The
sidecar is pull-only — its name is not `AGENTS.md`, so pi never auto-injects it
— yet it stays search-indexed (`agents` doc_type). The directory `AGENTS.md`
keeps a one-line summary plus a `→ see `<File>.AGENTS.md`` pointer. Rows within
the threshold stay verbatim (lossless).

## Finding docs (READ discipline)

`kb_*` tools are faster and cheaper than raw search — they return a one-line
purpose + key exports per file, not raw bytes. **This fires on the ACTION, not
the intent** — before you `grep`/`rg` for a symbol, `cat`/read a file to learn
what it does, or chase an import, the kb call goes first. It fires **even
mid-task when you already know the file**; knowing the file does not exempt you.
When your reflex is the left column, run the right column instead:

| You're about to… | Do this FIRST instead |
|---|---|
| `grep -rn "SymbolName" src/` — find where a fn / type / const lives | `kb_search --doc-type agents "SymbolName"` — tree indexes key exports per file |
| `grep -rn "feature\|topic" src/` — how does X work / where's X handled | `kb_search "feature topic"` |
| `cat` / read a file just to learn its purpose before editing | `kb agents <path>` — one-line purpose + exports + change history |
| chase imports / callers across files | `kb_neighbors <path\|heading>` |
| read one doc section in full | `kb_get <path> <section>` |

**Fall-through (explicit):** if the kb call returns nothing relevant, `rg` /
source read is allowed — then add the missing directory `AGENTS.md` row per the
WRITE discipline. kb does NOT replace grep; it goes first.

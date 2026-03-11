# JUDO Expression ASM Adapter - Project Documentation

## Project Overview


**Repository:** BlackBeltTechnology/judo-meta-expression-asm
**License:** Eclipse Public License 2.0 (EPL-2.0)
**Java Version:** 21
**Build System:** Maven 3.9.4 with Tycho 4.0.13 (Eclipse/OSGi plugin builder)

1. Bridges JUDO Expression models with ASM (Abstract Structural Model) models by resolving symbolic references to concrete ASM elements
2. Implements the generic `ModelAdapter` interface for ASM-specific EMF types (`EClass`, `EAttribute`, `EReference`, `EDataType`, `EEnum`)
3. Provides measure and unit resolution for numeric expressions via `AsmMeasureProvider`
4. Extracts JQL (JUDO Query Language) expressions from ASM models and builds corresponding Expression model trees via `AsmJqlExtractor`
5. Packages everything as OSGi bundles distributable through an Eclipse P2 update site

## Code Instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

## Directory Structure

```
judo-meta-expression-asm/
├── adapter-asm/                    # Core ASM adapter (OSGi bundle)
│   └── src/main/java/.../adapters/asm/
├── adapter-asm-test/               # Adapter unit tests (JUnit 5)
│   └── src/test/java/
├── builder-jql-asm/                # JQL builder for ASM (OSGi bundle)
│   └── src/main/java/.../builder/jql/asm/
├── builder-jql-asm-test/           # Builder unit tests (JUnit 5)
│   └── src/test/java/
├── feature-adapter-asm/            # Eclipse feature for adapter plugin
├── feature-builder-jql-asm/        # Eclipse feature for builder plugin
├── osgi-itest/                     # OSGi integration tests (Pax Exam + Karaf)
├── site/                           # Eclipse P2 update site
├── .github/                        # GitHub Actions workflows and docs
├── .mvn/                           # Maven wrapper configuration
├── openspec/                       # OpenSpec change tracking
└── pom.xml                         # Parent POM
```

## Core Modules

### Adapter Layer

| Module | Type | Purpose |
|--------|------|---------|
| `adapter-asm/` | OSGi bundle | Implements `ModelAdapter` for ASM models — resolves types, attributes, references, measures, and expression metadata annotations |
| `adapter-asm-test/` | Test module | JUnit 5 tests for adapter: `AsmModelAdapterTest`, `AsmMeasureProviderTest`, `AsmModelAdapterDimensionTest`, runtime tests (`FullAsmTest`, `MinimalAsmTest`, `MeasuredTest`, `IllegalAsmTest`) |

### Builder Layer

| Module | Type | Purpose |
|--------|------|---------|
| `builder-jql-asm/` | OSGi bundle | `AsmJqlExtractor` extends `AdaptableJqlExtractor` — extracts JQL queries from ASM and builds Expression model trees |
| `builder-jql-asm-test/` | Test module | JUnit 5 tests: `AsmJqlExtractorTest`, `AsmJqlExpressionBuilderTest`, `AsmJqlExpressionBindingTest` |

### Eclipse Distribution

| Module | Type | Purpose |
|--------|------|---------|
| `feature-adapter-asm/` | Eclipse feature | Packages adapter bundle for Eclipse installation |
| `feature-builder-jql-asm/` | Eclipse feature | Packages builder bundle for Eclipse installation |
| `site/` | P2 repository | Compiles features into an installable Eclipse update site |

### Integration Testing

| Module | Type | Purpose |
|--------|------|---------|
| `osgi-itest/` | Integration test | Validates OSGi bundle loading and wiring in Apache Karaf runtime via Pax Exam |

## Technology Stack

### Core Technologies
- **Eclipse EMF** (2.21+) — Eclipse Modeling Framework for metamodel-driven development
- **Tycho** (4.0.13) — Maven plugin for building Eclipse plugins and OSGi bundles
- **Epsilon Runtime** (2.8.0) — Model transformation and execution engine
- **OSGi** — Module system for Java; bundles declare imports/exports in `META-INF/MANIFEST.MF`

### External Model Dependencies
- **judo-meta-expression** — Expression metamodel (types, operators, variables, bindings)
- **judo-meta-asm** — Abstract Structural Model metamodel (EClass, EPackage wrappers)
- **judo-meta-measure** — Measure/unit metamodel (Measure, Unit, DurationUnit, BaseMeasure, DerivedMeasure)
- **judo-meta-jql** — JUDO Query Language metamodel

### Build & Quality
- **Maven** 3.9.4 (wrapper: `./mvnw`)
- **JUnit 5** (Jupiter) — Unit testing via Tycho surefire plugin
- **Pax Exam** 4.13.5 + **Apache Karaf** 4.4.7 — OSGi integration testing
- **JaCoCo** 0.8.12 — Code coverage
- **SLF4J** 2.0.16 + **Logback** 1.5.12 — Logging

## Build Commands

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

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Default — activates all submodules |
| `sign-artifacts` | GPG-signs build artifacts |
| `release-dummy` | Deploys to a dummy/test repository |
| `release-judong` | Deploys to JUDO Nexus (https://nexus.judo.technology) |
| `release-central` | Deploys to Maven Central via Sonatype OSS |
| `generate-github-asciidoc-diagrams` | Generates documentation diagrams |
| `update-source-code-license` | Updates license headers in source files |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Parent POM — defines all module versions, dependencies, profiles, and plugin configuration |
| `adapter-asm/META-INF/MANIFEST.MF` | OSGi bundle metadata for the adapter plugin (imports, exports, required bundles) |
| `builder-jql-asm/META-INF/MANIFEST.MF` | OSGi bundle metadata for the builder plugin |
| `logback-test.xml` | Test logging configuration (INFO level, console appender) |
| `.github/workflows/` | GitHub Actions CI/CD pipeline definitions |
| `site/category.xml` | Eclipse P2 update site category definition |

## Development Environment

**Required:**
- Java 21 JDK
- Maven 3.9.4+ (or use the included `./mvnw` wrapper)
- Git

**Optional (for Eclipse development):**
- Eclipse IDE with m2e, Epsilon, and Modeling Tools plugins
- XTend, XText, MWE, MWE2 features (for code generation)

## Git Workflow

- **Main Branch:** `develop`
- **Versioning:** `1.0.5-SNAPSHOT` (Maven) / `1.0.5.qualifier` (Eclipse/Tycho)
- **Branching Model:** GitFlow — feature branches (`feature/JNG-*`), release branches, bugfix/hotfix/support branches
- **CI/CD:** GitHub Actions — automatic build, test, deploy, and release workflows
- **Issue Tracking:** JIRA (JNG-* tickets)

## Important Notes

1. All commits must reference a JIRA ticket number (e.g., `JNG-123`). No commit without a ticket number.
2. No Lombok — Tycho does not support it. All source code is generated or hand-written.
3. OSGi manifests (`META-INF/MANIFEST.MF`) must be updated when public API changes — they declare bundle exports and imports independently from Maven dependencies.
4. The `adapter-asm` module exports a single package: `hu.blackbelt.judo.meta.expression.adapters.asm`
5. The `builder-jql-asm` module exports a single package: `hu.blackbelt.judo.meta.expression.builder.jql.asm`
6. Version numbers use `${revision}` property with Tycho qualifier conversion between Maven (`-SNAPSHOT`) and Eclipse (`.qualifier`) formats.
7. P2 update site URLs encode version numbers. Use the `update-category-versions` profile to synchronize them.
8. The `AsmModelAdapter` constructor requires two `ResourceSet` arguments: one for ASM model and one for Measure model.

## Related Documentation

- [README.md](README.md) — Project introduction and architecture overview
- [CONTRIBUTING.md](CONTRIBUTING.md) — Development setup, code structure, and submission guidelines
- [.github/CIFLOW.md](.github/CIFLOW.md) — Branching strategy and CI/CD pipeline details
- [judo-community](https://github.com/BlackBeltTechnology/judo-community) — Parent ecosystem documentation

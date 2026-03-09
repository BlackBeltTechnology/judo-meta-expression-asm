<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# Judo Expression ESM Adapter - Project Documentation

## Project Overview

**Repository:** BlackBeltTechnology/judo-meta-expression-esm
**License:** Eclipse Public License 2.0 (EPL-2.0)
**Java Version:** 21
**Build System:** Maven 3.9.4+ with Tycho (Eclipse build tooling)

This is an Eclipse/Tycho-based adapter project that:
1. **Adapts** ESM (Enterprise Service Model) models for use with JUDO Expression Language
2. **Provides** measure support and type mappings between Expression and ESM metamodels
3. **Includes** JQL (Judo Query Language) expression builder integration
4. **Supports** dual validation testing (EVL and Java validators)
5. **Distributes** via both Maven Central and Eclipse P2 repositories

## Directory Structure

```
judo-meta-expression-esm/
├── adapter-esm/                    # Core ESM adapter implementation
├── adapter-esm-test/               # Test utilities and validation tests
├── builder-jql-esm-test/           # JQL builder tests
├── feature-adapter-esm/            # Eclipse feature packaging
├── feature-builder-jql-esm/        # JQL builder feature
├── osgi-itest/                     # OSGi integration tests (Pax Exam)
├── site/                           # P2 update site
├── docs/                           # Documentation
│   └── validation/                 # Validation testing documentation
└── openspec/                       # OpenSpec change management
```

## Core Modules

| Module | Type | Purpose |
|--------|------|---------|
| `adapter-esm/` | eclipse-plugin | Core ESM adapter: EsmModelAdapter, EsmMeasureProvider, ExpressionValidatorOnEsm |
| `adapter-esm-test/` | bundle | Test utilities (EsmTestModelCreator) and dual validation test infrastructure |
| `builder-jql-esm-test/` | bundle | JQL expression builder tests |
| `feature-adapter-esm/` | eclipse-feature | Eclipse feature packaging |
| `osgi-itest/` | bundle | Pax Exam integration tests for Karaf container |
| `site/` | eclipse-repository | P2 update site assembly |

## Key Components

### Adapter Classes (adapter-esm module)

| Class | Purpose |
|-------|---------|
| `EsmModelAdapter` | Adapts ESM models for expression processing (811 lines) |
| `EsmMeasureProvider` | Provides measure support for ESM models (201 lines) |
| `ExpressionValidatorOnEsm` | Validates expressions on ESM models (46 lines) |

### Validation Test Infrastructure (adapter-esm-test module)

| Class | Purpose |
|-------|---------|
| `ValidatorType` | Enum for selecting EVL or Java validator |
| `AbstractExpressionEsmValidationTest` | Base class for dual validation tests |
| `ExpressionEsmValidationTest` | Dual validation test cases |
| `ExpressionEsmValidationPerformanceTest` | Performance comparison tests |

## Validation Architecture

This project imports and executes validators from [judo-meta-expression](https://github.com/BlackBeltTechnology/judo-meta-expression):

- **EVL Validation:** `ExpressionValidator.validateExpression()` - Epsilon Validation Language
- **Java Validation:** `ExpressionZetaValidator.validateExpression()` - Zeta framework

**No validators are implemented in this project** - only test infrastructure.

### Dual Validation Testing

Tests run with both EVL and Java validators to ensure parity:

```java
@ParameterizedTest(name = "testValidation [{0}]")
@EnumSource(ValidatorType.class)
void testValidation(ValidatorType type) throws Exception {
    this.validatorType = type;
    initModels(esmModel, measureModel);

    runValidation(
        ImmutableList.of(),  // expected errors
        ImmutableList.of()   // expected warnings
    );
}
```

## Technology Stack

### Core Technologies
- **Eclipse Modeling Framework (EMF)** 2.38.0+ - Metamodel foundation
- **Ecore** - Model definition language
- **Tycho** 4.0.13 - Eclipse plugin build
- **Epsilon** 2.8.0 - Model validation (EVL)
- **Zeta Framework** - Java validation framework

### Dependencies
- **judo-meta-expression** - Expression metamodel and validators
- **judo-meta-esm** - Enterprise Service Model metamodel
- **judo-meta-jql** - JQL metamodel
- **judo-meta-measure** - Measure metamodel

### Runtime
- **Apache Karaf** 4.4.7 - OSGi container
- **Pax Exam** 4.13.5 - OSGi testing

## Build Commands

```bash
# Standard build
./mvnw clean install

# Skip tests
./mvnw clean install -DskipTests

# Run performance tests
./mvnw test -Dgroups=performance

# Memory requirements (configured in .mvn/jvm.config)
# -Xms1024m -Xmx2048m
```

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Includes all submodules (default) |
| `sign-artifacts` | GPG signing for release |
| `release-central` | Maven Central deployment |
| `release-judong` | Internal Judo repository |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Parent POM with module definitions |
| `.mvn/jvm.config` | JVM arguments for Maven build |
| `adapter-esm/META-INF/MANIFEST.MF` | OSGi bundle manifest |

## Development Environment

**Required:**
- Java 21 JDK
- Maven 3.9.4+ (or use ./mvnw wrapper)

**Optional:**
- Eclipse IDE with m2e and OSGi plugins
- IntelliJ IDEA with Maven and OSGi plugins

## Git Workflow

- **Main Branch:** `develop`
- **Versioning:** SNAPSHOT-based development (currently 1.0.2-SNAPSHOT)
- **Release Process:** CI/CD with Maven Central and P2 deployment

## Related Documentation

- `docs/validation/README.md` - Validation testing overview
- `openspec/AGENTS.md` - OpenSpec workflow for spec-driven development
- `openspec/project.md` - Project conventions for OpenSpec
- [Zeta Framework Documentation](https://github.com/BlackBeltTechnology/judo-zeta) - Java validation framework
- [judo-meta-expression](https://github.com/BlackBeltTechnology/judo-meta-expression) - Expression metamodel
- [judo-meta-esm](https://github.com/BlackBeltTechnology/judo-meta-esm) - ESM metamodel with similar validation pattern

# builder-jql-asm Specification

## Purpose

Extracts JQL (JUDO Query Language) expressions from ASM models and builds corresponding Expression model trees. Provides the ASM-specific entry point for the generic JQL expression builder framework.

## Architecture

The module contains a single public class in package `hu.blackbelt.judo.meta.expression.builder.jql.asm`:

- **`AsmJqlExtractor`** — Extends `AdaptableJqlExtractor` (from `judo-meta-expression`). Accepts ASM, measure, and expression `ResourceSet` instances and internally creates an `AsmModelAdapter` to bridge the generic builder with ASM-specific model navigation. Provides four constructor overloads for different initialization patterns.

### Class Hierarchy

```
AdaptableJqlExtractor (generic JQL extraction framework)
  └── AsmJqlExtractor (ASM-specific implementation)
        └── uses AsmModelAdapter (from adapter-asm)
```

## Requirements

### Requirement: ASM-Specific JQL Extraction

`AsmJqlExtractor` SHALL initialize the generic JQL extraction framework with an `AsmModelAdapter` backed by the provided ASM and measure `ResourceSet` instances.

#### Scenario: Create extractor with ResourceSets
- **GIVEN** an ASM `ResourceSet`, a measure `ResourceSet`, and an expression `ResourceSet`
- **WHEN** `new AsmJqlExtractor(asmResourceSet, measureResourceSet, expressionResourceSet)` is called
- **THEN** an extractor is created with a default `JqlExpressionBuilderConfig` and an internal `AsmModelAdapter`

#### Scenario: Create extractor with custom config
- **GIVEN** an ASM `ResourceSet`, a measure `ResourceSet`, an expression `ResourceSet`, and a custom `JqlExpressionBuilderConfig`
- **WHEN** `new AsmJqlExtractor(asmResourceSet, measureResourceSet, expressionResourceSet, builderConfig)` is called
- **THEN** an extractor is created using the provided configuration

### Requirement: URI-Based Expression ResourceSet Creation

`AsmJqlExtractor` SHALL accept an EMF `URI` instead of a pre-built expression `ResourceSet`, creating the expression resource set internally using `ExpressionModelResourceSupport`.

#### Scenario: Create extractor with URI
- **GIVEN** an ASM `ResourceSet`, a measure `ResourceSet`, and an expression model `URI`
- **WHEN** `new AsmJqlExtractor(asmResourceSet, measureResourceSet, uri)` is called
- **THEN** an expression `ResourceSet` is created from the URI and the extractor initializes normally

### Requirement: Null Measure ResourceSet Handling

`AsmJqlExtractor` SHALL handle a null measure `ResourceSet` by creating an empty measure resource set via `MeasureModelResourceSupport.createMeasureResourceSet()`.

#### Scenario: Create extractor without measure model
- **GIVEN** an ASM `ResourceSet` and a null measure `ResourceSet`
- **WHEN** the extractor is constructed
- **THEN** an empty measure resource set is created and used for the `AsmModelAdapter`

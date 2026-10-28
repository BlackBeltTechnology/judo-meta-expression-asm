# Project Context

## Purpose
JUDO Expression ASM Adapter - Provides ASM (Abstract Semantic Model) integration for the JUDO Expression metamodel. This module enables expression validation and JQL expression building on top of ASM models.

## Tech Stack
- Java 21
- Eclipse EMF (Ecore metamodels)
- Epsilon (EVL for validation, EOL for operations)
- Maven with Tycho (OSGi/Eclipse plugin development)
- JUnit 5 with parameterized tests
- Zeta Validation Framework (Java-based validation alternative to EVL)
- OSGi/Karaf for runtime deployment

## Project Conventions

### Code Style
- Use Lombok annotations (@Slf4j, @Builder, etc.)
- Follow EMF builder patterns for model construction
- Use static imports for builder methods
- Constants for constraint names in validation classes

### Architecture Patterns
- **Dual Validation**: Both EVL (Epsilon) and Java (Zeta) validators run in parallel
- **Model Adapter Pattern**: AsmModelAdapter bridges expression model to ASM types
- **Parameterized Tests**: Use ValidatorType enum to test both validation engines
- Validation classes organized by expression type (numeric, string, temporal, etc.)

### Testing Strategy
- Parameterized tests with `@EnumSource(ValidatorType.class)` for dual validation
- Abstract base test classes (ExecutionContextOnAsmTest) for model setup
- Expected errors/warnings specified by constraint name
- Performance tests with synthetic models matching real-world characteristics

### Git Workflow
- Feature branches: `feature/JNG-XXXX_Description`
- Develop branch as integration target
- Semantic versioning with SNAPSHOT for development

## Domain Context
- **Expression Model**: JQL expressions that operate on ASM/ESM metamodels
- **ASM**: Abstract Semantic Model (Ecore-based runtime metamodel)
- **Measure Model**: Units and measurements support
- **Validation**: Constraint checking for expression correctness and type compatibility

## Important Constraints
- Must maintain backward compatibility with existing EVL validation
- Java validators must produce identical results to EVL validators
- OSGi bundle compatibility required for Karaf deployment
- EMF model integrity must be preserved

## External Dependencies
- judo-meta-expression: Core expression metamodel and validation framework
- judo-meta-asm: ASM metamodel
- judo-meta-measure: Measure model
- judo-zeta: Zeta validation framework (annotations, validation-core, common)
- epsilon-runtime: Epsilon execution engine for EVL

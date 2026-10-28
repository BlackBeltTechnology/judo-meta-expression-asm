# Expression ASM Validation Framework

This module provides dual validation support for Expression models with ASM adapters.

## Overview

Expression validation in the ASM adapter context uses validators imported from `judo-meta-expression`. This module does not implement its own validators - it integrates with the existing validation infrastructure.

Two validation approaches are supported:

1. **EVL Validation** - Epsilon Validation Language-based validation
2. **Java Validation (Zeta)** - Native Java validation using the judo-zeta framework

Both validators produce equivalent results and can be used interchangeably.

## Architecture

```
judo-meta-expression-asm (this module)
├── Imports validators from judo-meta-expression
├── Provides ASM model adapter integration
└── Test infrastructure for dual validation

judo-meta-expression
├── ExpressionValidator (EVL)
└── ExpressionZetaValidator (Java)
```

## Test Infrastructure

### ValidatorType Enum

```java
public enum ValidatorType {
    EVL,   // Epsilon Validation Language
    JAVA   // Native Java (Zeta framework)
}
```

### AbstractExpressionAsmValidationTest

Base class for parameterized tests that run against both validators:

```java
public class MyValidationTest extends AbstractExpressionAsmValidationTest {

    @ParameterizedTest(name = "testCase [{0}]")
    @EnumSource(ValidatorType.class)
    void testCase(ValidatorType type) throws Exception {
        this.validatorType = type;
        // ... setup models ...
        runValidation(expectedErrors, expectedWarnings);
    }
}
```

### Key Methods

- `setModelAdapter(AsmModel, MeasureModel)` - Initialize the model adapter
- `runValidation()` - Run validation expecting no errors
- `runValidation(errors, warnings)` - Run validation with expected results
- `isJavaValidatorAvailable()` - Check if Zeta validator is on classpath

## Dependencies

### Maven Dependencies

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>hu.blackbelt.judo.zeta</groupId>
    <artifactId>hu.blackbelt.judo.zeta.validation-core</artifactId>
    <scope>test</scope>
</dependency>
```

### OSGi/Karaf

The Zeta validation framework bundles are included in the test features:

```xml
<bundle>mvn:hu.blackbelt.judo.zeta/hu.blackbelt.judo.zeta.annotations/${judo-zeta-version}</bundle>
<bundle>mvn:hu.blackbelt.judo.zeta/hu.blackbelt.judo.zeta.common/${judo-zeta-version}</bundle>
<bundle>mvn:hu.blackbelt.judo.zeta/hu.blackbelt.judo.zeta.validation-core/${judo-zeta-version}</bundle>
```

## Performance Considerations

The Java (Zeta) validator typically offers better performance than EVL for large models due to:

- Native Java execution vs. interpreted EVL
- Optimized constraint evaluation
- Parallel validation support

For performance-critical scenarios, use `ValidatorType.JAVA`.

## Migration from EVL-only Tests

Existing EVL-only tests can be migrated to dual validation by:

1. Extend `AbstractExpressionAsmValidationTest` instead of custom base class
2. Change `@Test` to `@ParameterizedTest` with `@EnumSource(ValidatorType.class)`
3. Set `this.validatorType = type` at the start of each test
4. Replace direct validator calls with `runValidation()`

### Before

```java
@Test
void testValidation() throws Exception {
    validateExpressionOnAsm(log, asmModel, measureModel, expressionModel, errors, warnings);
}
```

### After

```java
@ParameterizedTest
@EnumSource(ValidatorType.class)
void testValidation(ValidatorType type) throws Exception {
    this.validatorType = type;
    runValidation(errors, warnings);
}
```

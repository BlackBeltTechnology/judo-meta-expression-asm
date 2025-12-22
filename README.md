# judo-meta-expression-asm

[![Build Status](https://github.com/BlackBeltTechnology/judo-meta-expression-asm/actions/workflows/build.yml/badge.svg?branch=develop)](https://github.com/BlackBeltTechnology/judo-meta-expression-asm/actions/workflows/build.yml)

## Introduction

Connects [Expression](https://github.com/BlackBeltTechnology/judo-meta-expression) models with [ASM](https://github.com/BlackBeltTechnology/judo-meta-asm) models.
Adapters most of the time resolve references (e.g.: ids) defined in models to ASM model elements.

## Validation Framework

This module supports dual validation using both EVL (Epsilon Validation Language) and Java-based (Zeta) validators:

- **EVL Validation**: Uses the Epsilon Validation Language for expression validation
- **Java Validation (Zeta)**: Uses the [judo-zeta](https://github.com/BlackBeltTechnology/judo-zeta) validation framework for faster Java-based validation

Both validators produce identical results and can be run in parallel for testing purposes. The validation logic is imported from the [judo-meta-expression](https://github.com/BlackBeltTechnology/judo-meta-expression) module.

### Using Dual Validation in Tests

Tests can be parameterized to run with both validators:

```java
@ParameterizedTest(name = "testCase [{0}]")
@EnumSource(ValidatorType.class)
void testCase(ValidatorType type) throws Exception {
    this.validatorType = type;
    runValidation();
}
```

See `AbstractExpressionAsmValidationTest` for the full test infrastructure.

## Context

This project is a building block of the [judo-community](https://github.com/BlackBeltTechnology/judo-community) aggregator
project. In order to better understand how this module fits into our ecosystem, please check the corresponding documentation!

## Contributing to the project

Everyone is welcome to contribute to JUDO! As a starter, please read the corresponding [CONTRIBUTING](CONTRIBUTING.md) guide for details!

## License

This project is licensed under the [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/).

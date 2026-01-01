## ADDED Requirements

### Requirement: Zeta Validation Framework Dependencies
The system SHALL include judo-zeta framework dependencies (validation-core, annotations, common) managed through a `judo-zeta-version` property in the root pom.xml.

#### Scenario: Zeta dependencies resolve correctly
- **WHEN** building the project with Maven
- **THEN** all Zeta dependencies (validation-core, annotations, common) resolve successfully
- **AND** the version is controlled by the `judo-zeta-version` property

#### Scenario: Zeta SNAPSHOT version configured
- **WHEN** checking the pom.xml properties
- **THEN** the `judo-zeta-version` property contains the latest SNAPSHOT version

### Requirement: Dual Validation Support in Adapter
The ExpressionValidatorOnAsm class SHALL support both EVL and Java (Zeta) validation execution, with the ability to select the validation engine.

#### Scenario: EVL validation execution
- **WHEN** validation is invoked with ValidatorType.EVL
- **THEN** the expression model is validated using the Epsilon EVL engine
- **AND** validation results match the EVL constraint definitions

#### Scenario: Java validation execution
- **WHEN** validation is invoked with ValidatorType.JAVA
- **THEN** the expression model is validated using the Zeta Java validation framework
- **AND** validation results match the EVL constraint definitions

#### Scenario: Backward compatible API
- **WHEN** the existing validateExpressionOnAsm method without ValidatorType is called
- **THEN** validation executes successfully using the default validation mode
- **AND** existing callers continue to work without modification

### Requirement: Parameterized Dual Validation Tests
All validation tests SHALL be parameterized to run with both EVL and Java validators, producing identical results for the same test cases.

#### Scenario: Same test case runs with both validators
- **WHEN** a parameterized test executes
- **THEN** the test runs once with EVL validator
- **AND** the test runs once with Java validator
- **AND** both executions produce the same validation results

#### Scenario: Existing tests converted to parameterized format
- **WHEN** reviewing the test classes (FullAsmTest, MinimalAsmTest, IllegalAsmTest, MeasuredTest)
- **THEN** each test uses @ParameterizedTest with @EnumSource(ValidatorType.class)
- **AND** test assertions are identical for both validation engines

### Requirement: Performance Benchmarking
The system SHALL include performance tests that compare EVL and Java validation execution times on synthetic models with realistic characteristics.

#### Scenario: Small model performance test
- **WHEN** validating a model with 10 entities and ~100 expressions
- **THEN** both EVL and Java validation complete successfully
- **AND** execution times are logged for comparison

#### Scenario: Large model performance test
- **WHEN** validating a model with 100 entities and ~1000 expressions
- **THEN** both EVL and Java validation complete successfully
- **AND** execution times are logged for comparison
- **AND** Java validation performs at least as fast as EVL validation

#### Scenario: Model generation with rackinspect characteristics
- **WHEN** generating a synthetic test model
- **THEN** the model contains entity types with 8-10 attributes each
- **AND** the model contains 5-6 relations per entity
- **AND** the model includes measured types and enumerations

### Requirement: OSGi/Karaf Integration
The Zeta validation framework bundles SHALL be deployable in OSGi/Karaf environments alongside the existing EVL validation.

#### Scenario: Zeta bundles resolve in Karaf
- **WHEN** the OSGi integration tests execute
- **THEN** Zeta bundles (validation-core, annotations, common) resolve successfully
- **AND** the feature starts without errors

#### Scenario: Validation works in OSGi runtime
- **WHEN** expression validation is invoked in the OSGi test environment
- **THEN** validation executes successfully
- **AND** produces the expected results

### Requirement: Documentation in Markdown Format
Project documentation SHALL be in Markdown format with Mermaid diagrams, referencing Zeta documentation without duplicating it.

#### Scenario: AsciiDoc files converted to Markdown
- **WHEN** reviewing project documentation files
- **THEN** README.md exists (converted from README.adoc)
- **AND** CONTRIBUTING.md exists (converted from CONTRIBUTING.adoc)
- **AND** PlantUML diagrams are converted to Mermaid syntax

#### Scenario: Zeta documentation referenced
- **WHEN** reading the project documentation
- **THEN** links to judo-zeta documentation are provided
- **AND** Zeta validation usage is explained for this adapter
- **AND** no Zeta documentation is duplicated locally

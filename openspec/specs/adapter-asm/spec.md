# adapter-asm Specification

## Purpose

Provides the ASM-specific implementation of the `ModelAdapter` interface, enabling the expression evaluation engine to resolve types, attributes, references, measures, and expression metadata against ASM (Abstract Structural Model) elements.

## Architecture

The module contains three public classes in package `hu.blackbelt.judo.meta.expression.adapters.asm`:

- **`AsmModelAdapter`** — Implements `ModelAdapter<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference, EClass, EAttribute, EReference, EClassifier, Measure, Unit>`. Constructed with two `ResourceSet` instances (ASM + Measure). Delegates measure operations to `AsmMeasureProvider` and `MeasureAdapter`.
- **`AsmMeasureProvider`** — Implements `MeasureProvider<Measure, Unit>`. Backed by an EMF `ResourceSet` containing measure model instances. Provides lookup by namespace/name/symbol, base measure decomposition, and EMF change notification forwarding.
- **`ExpressionValidatorOnAsm`** — Static utility class that wires `AsmModelAdapter` into the generic `ExpressionValidator` for validating expression models against ASM context.

## Requirements

### Requirement: Type Name Resolution

The adapter SHALL build `TypeName` instances from ASM `EClassifier` elements by combining the containing `EPackage` fully-qualified name (using `::` as namespace separator) with the classifier name.

#### Scenario: Resolve an EClass by TypeName
- **GIVEN** an ASM `ResourceSet` containing an `EPackage` "demo" with an `EClass` "Customer"
- **WHEN** `buildTypeName(customerClassifier)` is called
- **THEN** a `TypeName` with namespace "demo" and name "Customer" is returned

#### Scenario: Look up EClassifier by TypeName
- **GIVEN** a `TypeName` with namespace "demo" and name "Customer"
- **WHEN** `get(typeName)` is called
- **THEN** the corresponding `EClassifier` from the ASM model is returned

### Requirement: Type Introspection

The adapter SHALL correctly identify the category of any `EDataType` — numeric (integer or decimal), boolean, string, enumeration, date, timestamp, time, or custom — by delegating to `AsmUtils` methods.

#### Scenario: Identify numeric type
- **WHEN** `isNumeric(eDataType)` is called on an integer or decimal type
- **THEN** it returns `true`

#### Scenario: Identify custom type
- **WHEN** `isCustom(eDataType)` is called on a type that is not boolean, numeric, string, enumeration, date, timestamp, or time
- **THEN** it returns `true`

### Requirement: Structural Navigation

The adapter SHALL navigate ASM model structures to retrieve attributes, references, super types, container types, and transfer object mappings.

#### Scenario: Get attribute by name
- **GIVEN** an `EClass` with an attribute named "email"
- **WHEN** `getAttribute(eClass, "email")` is called
- **THEN** the corresponding `EAttribute` is returned

#### Scenario: Get reference target type
- **GIVEN** an `EReference` pointing to an `EClass` "Address"
- **WHEN** `getTarget(eReference)` is called
- **THEN** the `EClass` "Address" is returned

#### Scenario: Get container types
- **GIVEN** an `EClass` "OrderItem" that is contained by "Order" via a containment reference
- **WHEN** `getContainerTypesOf(orderItemClass)` is called
- **THEN** the result includes "Order" and all its supertypes

### Requirement: Measure and Unit Resolution

The adapter SHALL resolve measures and units for numeric attributes using ASM extension annotations (`constraints.measure`, `constraints.unit`) and delegate to `AsmMeasureProvider`.

#### Scenario: Get unit of a numeric attribute
- **GIVEN** an `EAttribute` with extension annotations `constraints.measure = "measures.Mass"` and `constraints.unit = "kilogram"`
- **WHEN** `getUnit(eAttribute)` is called
- **THEN** the `Unit` named "kilogram" from the "Mass" measure is returned

#### Scenario: Check if a type is measured
- **GIVEN** an `EDataType` with a `measured.measure` extension annotation
- **WHEN** `isMeasuredType(eDataType)` is called
- **THEN** it returns `true`

### Requirement: Duration Unit Arithmetic

The adapter SHALL compute base-to-target duration ratios for `DurationUnit` instances, supporting conversion to seconds and days.

#### Scenario: Convert day unit to seconds ratio
- **GIVEN** a `DurationUnit` of type DAY with rate 1440/1 (minutes per day, base unit = minute)
- **WHEN** `getBaseDurationRatio(unit, DurationType.SECOND)` is called
- **THEN** a `UnitFraction` representing 86400 seconds per base unit is returned (adjusted for the base unit rate)

### Requirement: Expression Metadata Access

The adapter SHALL read expression-related metadata (getter, setter, default, range) from ASM extension annotations on attributes and references.

#### Scenario: Get attribute getter expression
- **GIVEN** a derived `EAttribute` with extension annotation `expression.getter = "self.items!sum(price)"`
- **WHEN** `getAttributeGetter(eAttribute)` is called
- **THEN** `Optional.of("self.items!sum(price)")` is returned

### Requirement: Transfer Object Support

The adapter SHALL distinguish between entity types, mapped transfer object types, and unmapped transfer object types, and provide separate accessor methods for each category.

#### Scenario: List all mapped transfer objects
- **WHEN** `getAllMappedTransferObjectTypes()` is called
- **THEN** all `EClass` elements that are mapped transfer object types are returned

#### Scenario: Get mapped entity type
- **GIVEN** a mapped transfer object `EClass`
- **WHEN** `getMappedEntityType(transferObject)` is called
- **THEN** the corresponding entity `EClass` is returned

### Requirement: Measure Provider Change Notification

`AsmMeasureProvider` SHALL forward EMF model change notifications (add/remove of `BaseMeasure` and `DerivedMeasure` instances) to a registered `MeasureChangedHandler`.

#### Scenario: Notify on measure addition
- **GIVEN** a `MeasureChangedHandler` registered via `setMeasureChangeHandler`
- **WHEN** a `BaseMeasure` is added to the measure `ResourceSet`
- **THEN** `measureChangeHandler.measureAdded(measure)` is called

### Requirement: Expression Validation

`ExpressionValidatorOnAsm` SHALL validate an `ExpressionModel` against an `AsmModel` and `MeasureModel`, throwing `ExpressionValidationException` if validation fails (unless the errors match expected errors).

#### Scenario: Validate valid expression model
- **GIVEN** a valid `ExpressionModel`, `AsmModel`, and `MeasureModel`
- **WHEN** `validateExpressionOnAsm(log, asmModel, measureModel, expressionModel)` is called
- **THEN** no exception is thrown

#### Scenario: Validate with expected errors
- **GIVEN** an `ExpressionModel` with known validation errors
- **WHEN** `validateExpressionOnAsm(log, asmModel, measureModel, expressionModel, expectedErrors, expectedWarnings)` is called
- **THEN** the method succeeds if all actual errors match the expected errors list

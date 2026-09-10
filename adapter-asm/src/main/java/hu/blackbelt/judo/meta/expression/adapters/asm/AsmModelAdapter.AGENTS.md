# `AsmModelAdapter.java`

Binds generic expression metamodel to Ecore/ASM: implements
`ModelAdapter<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference, EClass, EAttribute,
EReference, EClassifier, Measure, Unit>`, so every expression type parameter resolves to an Ecore
element (`EClass` for both entity types and transfer object types, `EAttribute`/`EReference` for
both entity and transfer features). This is the ASM binding — the PSM/ESM siblings bind the same
`ModelAdapter` to their own metamodel classes.

## Construction

`AsmModelAdapter(ResourceSet asmResourceSet, ResourceSet measureResourceSet)` — builds an internal
`AsmUtils` over the ASM set and an `AsmMeasureProvider` + `MeasureAdapter` over the measure set.
Two distinct `ResourceSet`s: passing the same set for both leaves measure lookups empty. Exposes
`getAsmUtils()`.

## Name resolution

`buildTypeName(EClassifier)` and `get(TypeName)` translate between expression `TypeName` and
`EPackage`-qualified Ecore names, swapping `.` for `NAMESPACE_SEPARATOR` = `"::"`. A `TypeName`
whose namespace is written with `.` never resolves; unknown namespace logs `Namespace not found`
and returns `Optional.empty()` rather than throwing. `buildMeasureName(Measure)` / `get(MeasureName)`
do the inverse swap for measures, and `MEASURE_NAME_PATTERN` = `^(.*)\.([^\.]+)$` splits a
dotted measure FQ name — a name with no dot yields empty.

## Ecore feature lookup

`getReference(EClass, name)`, `getAttribute(EClass, name)`, `getTransferRelation`,
`getTransferAttribute`, `getAttributeType(EAttribute)`, `getAttributeType(EClass, name)`,
`getTarget(EReference)`, `getTransferRelationTarget`, `getSuperTypes(EClass)`, `isMixin`,
`getContainerTypesOf(EClass)`, `getAttributes`/`getReferences`/`getTransferAttributes`/
`getTransferRelations`, `getTransferAttributeType`, `getFqName(Object)`, `getName(Object)`.
Lookups walk `getEAllReferences()`/`getEAllAttributes()`, so inherited features resolve. A `null`
classifier returns empty instead of throwing.

## Model enumeration

`getAllEntityTypes()`, `getAllEnums()`, `getAllPrimitiveTypes()`, `getAllStaticSequences()`,
`getAllTransferObjectTypes()`, `getAllMappedTransferObjectTypes()`, `getAllUnmappedTransferObjectTypes()`,
`getAllActorTypes()`, `getAllMeasures()`, `getPrincipal(EClassifier)`, `getMappedEntityType(EClass)`,
`getEntityTypeOfTransferObjectRelationTarget(TypeName, String)`, `getSequence(EClass, String)`.
Backed by protected `getAsmElement(Class)`, which streams `resourceSet.getAllContents()` — cost is
linear in model size per call, so hot loops should hoist the result. `getPrincipal` throws
`IllegalArgumentException("<actorType> principal not found")` when the actor type carries no principal.

## Type predicates

`isObjectType`, `isPrimitiveType`, `isNumeric`, `isInteger`, `isDecimal`, `isBoolean`, `isString`,
`isEnumeration`, `isDate`, `isTimestamp`, `isTime`, `isCustom`, `isSequence`, `isCollection`,
`isCollectionReference(EReference)` / `(TypeName, String)`, `contains(EEnum, memberName)`.

## Extended-metadata annotation contract

Derived-feature and parameter metadata is read from ASM extension annotations via
`AsmUtils.getExtensionAnnotationCustomValue(..., false)` — never from the Ecore structure itself:

- `expression` / `getter`, `setter`, `default`, `range`, `getter.parameter` →
  `getAttributeGetter`, `getAttributeSetter`, `getAttributeDefault`, `getReferenceGetter`,
  `getReferenceSetter`, `getReferenceDefault`, `getReferenceRange`, `getTransferAttributeGetter`,
  `getTransferRelationGetter`, `getTransferRelationSetter`, `getAttributeParameterType`,
  `getReferenceParameterType`
- `parameterized` / `type` → `getTransferAttributeParameterType`, `getTransferRelationParameterType`
- `mappedEntityType` / `filter` → `getFilter(EClass)`
- `measured` / `measure`, `unit` → `isMeasuredType`, `getMeasureOfType`, `getUnitOfType`
- `constraints` / unit name-or-symbol + measure FQ name → `getUnit(EAttribute)`

A model missing these annotations reports every feature as non-derived and unmeasured; the adapter
does not fall back and does not warn.

## Measure and duration arithmetic

`getMeasure`/`getUnit(NumericExpression)`, `getUnits(Measure)`, `getDimension(NumericExpression)`,
`isMeasured`, `isDurationSupportingAddition`, `getUnitName`, `getUnitRates(Unit)` (returns
`UnitFraction(rateDividend, rateDivisor)`), `getBaseDurationRatio(Unit, DurationType)`.
`getBaseDurationRatio` throws `IllegalArgumentException` three ways: unit not a `DurationUnit`
("Unit must be duration"), unit type `MONTH`/`YEAR` ("No duration ration is valid for month and
year."), or `targetType` other than `SECOND`/`DAY` ("Only second and day duration type is
supported."). `getUnit(NumericExpression)` resolves only `NumericAttribute`, `MeasuredDecimal`,
`MeasuredDecimalEnvironmentVariable`, `DecimalVariableReference` — any other numeric expression
yields empty, not an error.

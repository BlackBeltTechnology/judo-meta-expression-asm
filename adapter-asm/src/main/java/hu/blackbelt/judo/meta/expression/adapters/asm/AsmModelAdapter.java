package hu.blackbelt.judo.meta.expression.adapters.asm;

import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils.OperationBehaviour;
import hu.blackbelt.judo.meta.expression.*;
import hu.blackbelt.judo.meta.expression.adapters.ModelAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureProvider;
import hu.blackbelt.judo.meta.expression.constant.MeasuredDecimal;
import hu.blackbelt.judo.meta.expression.numeric.DecimalVariableReference;
import hu.blackbelt.judo.meta.expression.numeric.NumericAttribute;
import hu.blackbelt.judo.meta.expression.variable.MeasuredDecimalEnvironmentVariable;
import hu.blackbelt.judo.meta.measure.*;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newMeasureNameBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;
import static java.util.stream.Collectors.toList;

/**
 * Model adapter for ASM models.
 */
public class AsmModelAdapter implements
		ModelAdapter<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference, EClass, EAttribute, EReference, EClassifier, Measure, Unit> {

	protected static final String NAMESPACE_SEPARATOR = "::";
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(AsmModelAdapter.class);

	private static Pattern MEASURE_NAME_PATTERN = Pattern.compile("^(.*)\\.([^\\.]+)$");

	private final MeasureProvider<Measure, Unit> measureProvider;
	private final MeasureAdapter<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference, EClass, EAttribute, EReference, EClassifier, Measure, Unit> measureAdapter;

	private final AsmUtils asmUtils;

	public AsmModelAdapter(final ResourceSet asmResourceSet, final ResourceSet measureResourceSet) {
		asmUtils = new AsmUtils(asmResourceSet);
		measureProvider = new AsmMeasureProvider(measureResourceSet);
		measureAdapter = new MeasureAdapter<>(measureProvider, this);
	}

	private static Optional<MeasureName> parseMeasureName(final String name) {
		if (name == null) {
			return Optional.empty();
		} else {
			final Matcher m = MEASURE_NAME_PATTERN.matcher(name);
			if (m.matches()) {
				return Optional.of(newMeasureNameBuilder().withNamespace(m.group(1)).withName(m.group(2)).build());
			} else {
				return Optional.empty();
			}
		}
	}

	@Override
	public Optional<TypeName> buildTypeName(final EClassifier namespaceElement) {
		return getAsmElement(EPackage.class).filter(ns -> ns.getEClassifiers().contains(namespaceElement))
				.map(ns -> newTypeNameBuilder()
						.withNamespace(AsmUtils.getPackageFQName(ns).replace(".", NAMESPACE_SEPARATOR))
						.withName(namespaceElement.getName()).build())
				.findAny();
	}

	@Override
	public Optional<MeasureName> buildMeasureName(Measure measure) {
		return measureProvider.getMeasures()
				.filter(mn -> Objects.equals(mn.getNamespace(), measure.getNamespace())
						&& Objects.equals(mn.getName(), measure.getName()))
				.findAny()
				.map(m -> newMeasureNameBuilder().withName(m.getName()).withNamespace(m.getNamespace()).build());
	}

	@Override
	public Optional<? extends EClassifier> get(final TypeName elementName) {
		final Optional<EPackage> namespace = getAsmElement(EPackage.class).filter(p -> Objects
				.equals(AsmUtils.getPackageFQName(p), elementName.getNamespace().replace(NAMESPACE_SEPARATOR, ".")))
				.findAny();

		if (namespace.isPresent()) {
			return namespace.get().getEClassifiers().stream()
					.filter(e -> Objects.equals(e.getName(), elementName.getName())).findAny();
		} else {
			if (elementName.getNamespace() != null && !"".equals(elementName.getNamespace().trim())) {
				log.warn("Namespace not found: {}", elementName.getNamespace());
			}
			return Optional.empty();
		}
	}

	@Override
	public Optional<? extends Measure> get(final MeasureName measureName) {
		return measureProvider.getMeasure(measureName.getNamespace().replace(".", NAMESPACE_SEPARATOR),
				measureName.getName());
	}

	@Override
	public boolean isObjectType(final EClassifier namespaceElement) {
		return namespaceElement instanceof EClass;
	}

	@Override
	public boolean isPrimitiveType(EClassifier namespaceElement) {
		return namespaceElement instanceof EDataType;
	}

	@Override
	public boolean isMeasuredType(EDataType primitiveType) {
		return getMeasureOfType(primitiveType).isPresent();
	}

	@Override
	public Optional<? extends Measure> getMeasureOfType(EDataType primitiveType) {
		Optional<? extends Measure> measure = AsmUtils.getExtensionAnnotationCustomValue(primitiveType, "measured", "measure", false)
				.flatMap(AsmModelAdapter::parseMeasureName)
				.flatMap(this::get);
		return measure;
	}

	@Override
	public Optional<Unit> getUnitOfType(EDataType primitiveType) {
		Optional<Unit> unit = getMeasureOfType(primitiveType)
				.flatMap(measure -> AsmUtils.getExtensionAnnotationCustomValue(primitiveType, "measured", "unit", false)
						.flatMap(unitName -> measureAdapter.getUnit(Optional.of(measure.getNamespace()), Optional.of(measure.getName()), unitName)
						));
		return unit;
	}

	@Override
	public String getUnitName(Unit unit) {
		return unit.getName();
	}

	@Override
	public UnitFraction getUnitRates(Unit unit) {
		return new UnitFraction(unit.getRateDividend(), unit.getRateDivisor());
	}

	@Override
	public UnitFraction getBaseDurationRatio(Unit unit, DurationType targetType) {
		if (!(unit instanceof DurationUnit)) {
			throw new IllegalArgumentException("Unit must be duration");
		};
		DurationUnit durationUnit = (DurationUnit) unit;
		// examples in comments when unit is day, the base unit is minute
		// example2 unit is millisecond, the base unit is minute
		BigDecimal dividendToBase = durationUnit.getRateDividend(); // 1440 |---| 1
		BigDecimal divisorToBase = durationUnit.getRateDivisor();  // 1 |---| 60000
		DurationType target;
		if (hu.blackbelt.judo.meta.measure.DurationType.NANOSECOND.equals(durationUnit.getType())) {
			target = DurationType.NANOSECOND;
		} else if (hu.blackbelt.judo.meta.measure.DurationType.MICROSECOND.equals(durationUnit.getType())) {
			target = DurationType.MICROSECOND;
		} else if (hu.blackbelt.judo.meta.measure.DurationType.MILLISECOND.equals(durationUnit.getType())) {
			target = DurationType.MILLISECOND;
		} else if (hu.blackbelt.judo.meta.measure.DurationType.SECOND.equals(durationUnit.getType())) {
			target = DurationType.SECOND;
		} else if (hu.blackbelt.judo.meta.measure.DurationType.MINUTE.equals(durationUnit.getType())) {
			target = DurationType.MINUTE;
		} else if (hu.blackbelt.judo.meta.measure.DurationType.HOUR.equals(durationUnit.getType())) {
			target = DurationType.HOUR;
		} else if (hu.blackbelt.judo.meta.measure.DurationType.DAY.equals(durationUnit.getType())) {
			target = DurationType.DAY;
		} else if (hu.blackbelt.judo.meta.measure.DurationType.WEEK.equals(durationUnit.getType())) {
			target = DurationType.WEEK;
		} else {
			throw new IllegalArgumentException("No duration ration is valid for month and year.");
		}
		if (targetType.equals(DurationType.SECOND)) {
			UnitFraction secondFraction = target.getSecondUnitFraction(); // 86400/1 |---| 1/1000
			// however, we need to return the ratio calculated in the base unit. The base unit is minute, so the result must be 60/1
			// so the result is: secondFraction * divisorToBase/dividendToBase, that is
			BigDecimal newDividend = secondFraction.getDividend().multiply(divisorToBase);
			BigDecimal newDivisor = secondFraction.getDivisor().multiply(dividendToBase);
			return new UnitFraction(newDividend, newDivisor);
		} else if (targetType.equals(DurationType.DAY)) {
			UnitFraction dayFraction = target.getDayUnitFraction(); // 86400/1 |---| 1/1000
			// however, we need to return the ratio calculated in the base unit. The base unit is minute, so the result must be 60/1
			// so the result is: dayFraction * divisorToBase/dividendToBase, that is
			BigDecimal newDividend = dayFraction.getDividend().multiply(divisorToBase);
			BigDecimal newDivisor = dayFraction.getDivisor().multiply(dividendToBase);
			return new UnitFraction(newDividend, newDivisor);
		} else {
			throw new IllegalArgumentException("Only second and day duration type is supported.");
		}

	}

	@Override
	public Optional<? extends EReference> getReference(final EClass clazz, final String referenceName) {
		return clazz.getEAllReferences().stream().filter(r -> Objects.equals(r.getName(), referenceName)).findAny();
	}

	@Override
	public Optional<? extends EReference> getTransferRelation(EClass transferObject, String relationName) {
		return transferObject.getEAllReferences().stream().filter(r -> Objects.equals(r.getName(), relationName)).findAny();
	}

	@Override
	public boolean isCollection(final ReferenceSelector referenceSelector) {
		return ((EReference) referenceSelector.getReference(this)).isMany();
	}

	@Override
	public boolean isCollectionReference(EReference reference) {
		return reference.isMany();
	}

	@Override
	public Optional<EClass> getAttributeParameterType(EAttribute attribute) {
		return attribute.isDerived()
			   ? AsmUtils.getExtensionAnnotationCustomValue(attribute, "expression", "getter.parameter", false)
						 .flatMap(asmUtils::resolve)
						 .map(eClassifier -> (EClass) eClassifier)
			   : Optional.empty();
	}

	@Override
	public Optional<EClass> getReferenceParameterType(EReference reference) {
		return reference.isDerived()
			   ? AsmUtils.getExtensionAnnotationCustomValue(reference, "expression", "getter.parameter", false)
						 .flatMap(asmUtils::resolve)
						 .map(eClassifier -> (EClass) eClassifier)
			   : Optional.empty();
	}

	@Override
	public Optional<EClass> getTransferAttributeParameterType(EAttribute attribute) {
		return AsmUtils.getExtensionAnnotationCustomValue(attribute, "parameterized", "type", false)
					   .map(t -> (EClass) asmUtils.resolve(t).orElse(null));
	}

	@Override
	public Optional<EClass> getTransferRelationParameterType(EReference reference) {
		return AsmUtils.getExtensionAnnotationCustomValue(reference, "parameterized", "type", false)
					   .map(t -> (EClass) asmUtils.resolve(t).orElse(null));
	}

	@Override
	public EClass getTarget(final EReference reference) {
		return reference.getEReferenceType();
	}

	@Override
	public EClass getTransferRelationTarget(EReference relation) {
		return relation.getEReferenceType();
	}

	@Override
	public Optional<? extends EAttribute> getAttribute(final EClass clazz, final String attributeName) {
		return clazz.getEAllAttributes().stream().filter(r -> Objects.equals(r.getName(), attributeName)).findAny();
	}

	@Override
	public Optional<? extends EAttribute> getTransferAttribute(EClass transferObject, String attributeName) {
		return transferObject.getEAllAttributes().stream().filter(r -> Objects.equals(r.getName(), attributeName)).findAny();
	}

	@Override
	public Optional<? extends EDataType> getAttributeType(EAttribute attribute) {
		return Optional.ofNullable(attribute.getEAttributeType());
	}

	@Override
	public Optional<? extends EDataType> getAttributeType(final EClass clazz, final String attributeName) {
		return getAttribute(clazz, attributeName).map(EAttribute::getEAttributeType);
	}

	@Override
	public Collection<? extends EClass> getSuperTypes(final EClass clazz) {
		return clazz.getEAllSuperTypes();
	}

	@Override
	public boolean isMixin(EClass included, EClass mixin) {
		if (included == null || mixin == null) {
			return false;
		} else if (AsmUtils.equals(included, mixin)) {
			return true;
		}
		return included.getEAllAttributes().stream().allMatch(ia -> mixin.getEAllAttributes().stream().anyMatch(ma -> Objects.equals(ma.getName(), ia.getName())
				&& AsmUtils.equals(ma.getEAttributeType(), ia.getEAttributeType())))
				&& included.getEAllReferences().stream().allMatch(ir -> mixin.getEAllReferences().stream().anyMatch(mr -> Objects.equals(mr.getName(), ir.getName())
                && mr.getLowerBound() == ir.getLowerBound() && mr.getUpperBound() == ir.getUpperBound()
                && AsmUtils.equals(mr.getEReferenceType(), ir.getEReferenceType())));
	}

	@Override
	public boolean isNumeric(final EDataType primitive) {
		return AsmUtils.isNumeric(primitive);
	}

	@Override
	public boolean isInteger(final EDataType primitive) {
		return AsmUtils.isInteger(primitive);
	}

	@Override
	public boolean isDecimal(final EDataType primitive) {
		return AsmUtils.isDecimal(primitive);
	}

	@Override
	public boolean isBoolean(final EDataType primitive) {
		return AsmUtils.isBoolean(primitive);
	}

	@Override
	public boolean isString(final EDataType primitive) {
		return AsmUtils.isString(primitive);
	}

	@Override
	public boolean isEnumeration(final EDataType primitive) {
		return AsmUtils.isEnumeration(primitive);
	}

	@Override
	public boolean isDate(EDataType primitive) {
		return AsmUtils.isDate(primitive);
	}

	@Override
	public boolean isTimestamp(EDataType primitive) {
		return AsmUtils.isTimestamp(primitive);
	}

	@Override
	public boolean isTime(EDataType primitive) {
		return AsmUtils.isTime(primitive);
	}

	@Override
	public boolean isCustom(EDataType primitive) {
		return !AsmUtils.isBoolean(primitive) && !AsmUtils.isNumeric(primitive) && !AsmUtils.isString(primitive)
				&& !AsmUtils.isEnumeration(primitive) && !AsmUtils.isDate(primitive)
				&& !AsmUtils.isTimestamp(primitive)	&& !AsmUtils.isTime(primitive);
	}

	@Override
	public boolean isMeasured(final NumericExpression numericExpression) {
		return measureAdapter.isMeasured(numericExpression);
	}

	@Override
	public boolean contains(final EEnum enumeration, final String memberName) {
		return enumeration.getELiterals().stream().filter(l -> Objects.equals(l.getLiteral(), memberName)).findAny()
				.isPresent();
	}

	@Override
	public boolean isDurationSupportingAddition(final Unit unit) {
		return measureProvider.isDurationSupportingAddition(unit);
	}

	@Override
	public Optional<Measure> getMeasure(final NumericExpression numericExpression) {
		return measureAdapter.getMeasure(numericExpression);
	}

	@Override
	public Optional<Unit> getUnit(final NumericExpression numericExpression) {
		if (numericExpression instanceof NumericAttribute) {
			final EClass objectType = (EClass) ((NumericAttribute) numericExpression).getObjectExpression()
					.getObjectType(this);
			final String attributeName = ((NumericAttribute) numericExpression).getAttributeName();

			return getUnit(objectType, attributeName);
		} else if (numericExpression instanceof MeasuredDecimal) {
			final MeasuredDecimal measuredDecimal = (MeasuredDecimal) numericExpression;
			return measureAdapter.getUnit(
					measuredDecimal.getMeasure() != null
							? Optional.ofNullable(measuredDecimal.getMeasure().getNamespace())
							: Optional.empty(),
					measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getName())
							: Optional.empty(),
					measuredDecimal.getUnitName());
		} else if (numericExpression instanceof MeasuredDecimalEnvironmentVariable) {
			final MeasuredDecimalEnvironmentVariable measuredDecimal = (MeasuredDecimalEnvironmentVariable) numericExpression;
			return measureAdapter.getUnit(
					measuredDecimal.getMeasure() != null
							? Optional.ofNullable(measuredDecimal.getMeasure().getNamespace())
							: Optional.empty(),
					measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getName())
							: Optional.empty(),
					measuredDecimal.getUnitName());
		} else if (numericExpression instanceof DecimalVariableReference) {
			DecimalVariableReference ref = (DecimalVariableReference) numericExpression;
			EDataType objectType = (EDataType) ref.getVariable().getObjectType(this);
			return getUnitOfType(objectType);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public EList<Unit> getUnits(Measure measure) {
		return measureProvider.getUnits(measure);
	}

	@Override
	public Optional<Map<Measure, Integer>> getDimension(final NumericExpression numericExpression) {
		return measureAdapter.getDimension(numericExpression).map(dimensions -> {
			Map<Measure, Integer> measureMap = new HashMap<>();
			dimensions.entrySet().stream().forEach(entry -> {
				MeasureAdapter.MeasureId measureId = entry.getKey();
				Optional<Measure> measure = measureProvider.getMeasure(measureId.getNamespace(), measureId.getName());
				measure.ifPresent(m -> measureMap.put(m, entry.getValue()));
			});
			return measureMap;
		});
	}

	@Override
	public EList<EClass> getAllEntityTypes() {
		return ECollections
				.asEList(getAsmElement(EClass.class).filter(c -> AsmUtils.isEntityType(c)).collect(toList()));
	}

	@Override
	public EList<EEnum> getAllEnums() {
		return ECollections
				.asEList(getAsmElement(EEnum.class).filter(e -> AsmUtils.isEnumeration(e)).collect(toList()));
	}

	@Override
	public EList<EDataType> getAllPrimitiveTypes() {
		return ECollections.asEList(getAsmElement(EDataType.class).collect(toList()));
	}

	@Override
	public EList<EClassifier> getAllStaticSequences() {
		// TODO
		return ECollections.emptyEList();
	}

	@Override
	public Optional<? extends EClassifier> getSequence(EClass clazz, String sequenceName) {
		// TODO
		return Optional.empty();
	}

	@Override
	public boolean isSequence(EClassifier namespaceElement) {
		// TODO
		return false;
	}

	@Override
	public boolean isDerivedAttribute(EAttribute attribute) {
		return attribute.isDerived();
	}

	@Override
	public boolean isDerivedTransferAttribute(EAttribute attribute) {
		return isDerivedAttribute(attribute);
	}

	@Override
	public Optional<String> getAttributeGetter(EAttribute attribute) {
		return AsmUtils.getExtensionAnnotationCustomValue(attribute, "expression", "getter", false);
	}

	@Override
	public Optional<String> getTransferAttributeGetter(EAttribute attribute) {
		return getAttributeGetter(attribute);
	}

	@Override
	public Optional<String> getAttributeSetter(EAttribute attribute) {
		return AsmUtils.getExtensionAnnotationCustomValue(attribute, "expression", "setter", false);
	}

	@Override
	public Optional<String> getAttributeDefault(EAttribute attribute) {
		return AsmUtils.getExtensionAnnotationCustomValue(attribute, "expression", "default", false);
	}

	@Override
	public boolean isDerivedReference(EReference reference) {
		return reference.isDerived();
	}

	@Override
	public boolean isDerivedTransferRelation(EReference relation) {
		return relation.isDerived();
	}

	@Override
	public Optional<String> getReferenceGetter(EReference reference) {
		return AsmUtils.getExtensionAnnotationCustomValue(reference, "expression", "getter", false);
	}

	@Override
	public Optional<String> getTransferRelationGetter(EReference relation) {
		return getReferenceGetter(relation);
	}

	@Override
	public Optional<String> getReferenceDefault(EReference reference) {
		return AsmUtils.getExtensionAnnotationCustomValue(reference, "expression", "default", false);
	}

	@Override
	public Optional<String> getReferenceRange(EReference reference) {
		return AsmUtils.getExtensionAnnotationCustomValue(reference, "expression", "range", false);
	}

	@Override
	public Optional<String> getFilter(EClass clazz) {
		return AsmUtils.getExtensionAnnotationCustomValue(clazz, "mappedEntityType", "filter", false);
	}

	@Override
	public Optional<String> getReferenceSetter(EReference reference) {
		return AsmUtils.getExtensionAnnotationCustomValue(reference, "expression", "setter", false);
	}

	@Override
	public Optional<String> getTransferRelationSetter(EReference relation) {
		return getReferenceSetter(relation);
	}

	@Override
	public EList<Measure> getAllMeasures() {
		return ECollections.asEList(measureProvider.getMeasures().collect(toList()));
	}

	protected <T> Stream<T> getAsmElement(final Class<T> clazz) {
		return asmUtils.all(clazz);
	}

	Optional<Unit> getUnit(final EClass objectType, final String attributeName) {
		final Optional<Optional<Unit>> unit = objectType.getEAllAttributes().stream()
				.filter(a -> Objects.equals(a.getName(), attributeName)).map(a -> getUnit(a)).findAny();
		if (unit.isPresent()) {
			return unit.get();
		} else {
			log.error("Attribute not found: {}", attributeName);
			return Optional.empty();
		}
	}

	public Optional<Unit> getUnit(final EAttribute attribute) {
		if (AsmUtils.isNumeric(attribute.getEAttributeType())) {
			final Optional<String> unitNameOrSymbol = AsmUtils.getExtensionAnnotationCustomValue(attribute,
					"constraints", "unit", false);
			final Optional<String> measureFqName = AsmUtils.getExtensionAnnotationCustomValue(attribute, "constraints",
					"measure", false);
			if (unitNameOrSymbol.isPresent()) {
				if (measureFqName.isPresent()) {
					final Optional<MeasureName> measureName = measureFqName.isPresent()
							? parseMeasureName(measureFqName.get())
							: Optional.empty();
					if (!measureName.isPresent()) {
						log.error("Failed to parse measure name: {}", measureFqName.get());
						return Optional.empty();
					}
					final String replacedMeasureName = measureName.get().getNamespace().replace(".",
							NAMESPACE_SEPARATOR);
					final Optional<Measure> measure = measureName.isPresent()
							? measureProvider.getMeasure(replacedMeasureName, measureName.get().getName())
							: Optional.empty();
					if (!measure.isPresent() && measureName.isPresent()) {
						log.error("Measure is defined but not resolved: namespace={}, name={}", replacedMeasureName,
								measureName.get().getName());
						return Optional.empty();
					}
					return measureProvider.getUnitByNameOrSymbol(measure, unitNameOrSymbol.get());
				} else {
					return measureProvider.getUnitByNameOrSymbol(Optional.empty(), unitNameOrSymbol.get());
				}
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}

	@Override
	public EList<EClass> getContainerTypesOf(final EClass clazz) {
		return ECollections.asEList(asmUtils.all(EClass.class)
				.filter(container -> container.getEAllContainments().stream()
						.anyMatch(c -> EcoreUtil.equals(c.getEReferenceType(), clazz)))
				.flatMap(container -> Stream.concat(container.getEAllSuperTypes().stream(),
						Collections.singleton(container).stream()))
				.collect(toList()));
	}

	@Override
	public EList<EClass> getAllTransferObjectTypes() {
		return ECollections.asEList(getAsmElement(EClass.class)
				.filter(c -> !AsmUtils.isEntityType(c) || asmUtils.isMappedTransferObjectType(c)).collect(toList()));
	}

	@Override
	public EList<EClass> getAllMappedTransferObjectTypes() {
		return ECollections
				.asEList(getAsmElement(EClass.class).filter(asmUtils::isMappedTransferObjectType).collect(toList()));
	}

	@Override
	public EList<EClass> getAllUnmappedTransferObjectTypes() {
		return ECollections.asEList(getAsmElement(EClass.class)
				.filter(c -> !AsmUtils.isEntityType(c) && !asmUtils.isMappedTransferObjectType(c)).collect(toList()));
	}

	public AsmUtils getAsmUtils() {
		return asmUtils;
	}

	@Override
	public Optional<EClass> getEntityTypeOfTransferObjectRelationTarget(TypeName transferObjectTypeName,
			String transferObjectRelationName) {

		Optional<? extends EClassifier> transferObjectType = this.get(transferObjectTypeName);

		if (!transferObjectType.isPresent()) {

			return Optional.empty();

		} else if (transferObjectType.get() instanceof EClass) {

			Optional<? extends EReference> eReference = this.getReference((EClass) transferObjectType.get(),
					transferObjectRelationName);

			if (!eReference.isPresent()) {
				return Optional.empty();
			} else {
				EClass referenceTarget = getTarget(eReference.get());

				if (AsmUtils.isEntityType(referenceTarget)) {

					return Optional.of(referenceTarget);

				} else if (asmUtils.isMappedTransferObjectType(referenceTarget)) {

					return asmUtils.getMappedEntityType(referenceTarget);

				} else {
					return Optional.empty();
				}
			}

		} else {
			return Optional.empty();
		}
	}

	@Override
	public boolean isCollectionReference(TypeName elementName, String referenceName) {
		return this.get(elementName).filter(c -> c instanceof EClass)
				.flatMap(c -> getReference((EClass) c, referenceName)).map(this::isCollectionReference)
				.filter(Boolean::booleanValue).isPresent();
	}

	@Override
	public Optional<EClass> getMappedEntityType(EClass mappedTransferObjectType) {
		return asmUtils.getMappedEntityType(mappedTransferObjectType);
	}

	@Override
	public String getFqName(Object object) {
		if (object instanceof EClassifier) {
			return AsmUtils.getClassifierFQName((EClassifier) object);
		} else if (object instanceof EReference) {
			return AsmUtils.getReferenceFQName((EReference) object);
		} else if (object instanceof EAttribute) {
			return AsmUtils.getAttributeFQName((EAttribute) object);
		} else
			return null;
	}

	@Override
	public Optional<String> getName(Object object) {
		if (object instanceof ENamedElement) {
			return Optional.of(((ENamedElement) object).getName());
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Collection<EAttribute> getAttributes(EClass clazz) {
		return clazz.getEAttributes();
	}

	@Override
	public Collection<EReference> getReferences(EClass clazz) {
		return clazz.getEReferences();
	}

	@Override
	public Collection<EAttribute> getTransferAttributes(EClass transferObjectType) {
		return transferObjectType.getEAttributes();
	}

	@Override
	public Collection<EReference> getTransferRelations(EClass transferObjectType) {
		return transferObjectType.getEReferences();
	}

	@Override
	public EDataType getTransferAttributeType(EAttribute transferAttribute) {
		return transferAttribute.getEAttributeType();
	}

	@Override
	public List<EClassifier> getAllActorTypes() {
		return new ArrayList<>(asmUtils.getAllActorTypes());
	}

	@Override
	public EClass getPrincipal(EClassifier actorType) {
		EClass principal = null;
		if (actorType instanceof EClass) {
			EList<EOperation> eOperations = ((EClass) actorType).getEOperations();
			for (EOperation operation : eOperations) {
				Optional<OperationBehaviour> behaviour = AsmUtils.getBehaviour(operation)
						.filter(b -> b.equals(OperationBehaviour.GET_PRINCIPAL));
				if (behaviour.isPresent()) {
					EClassifier eType = operation.getEType();
					if (eType instanceof EClass) {
						principal = (EClass) eType;
					}
				}
			}
		}
		if (principal != null) {
			return principal;
		} else {
			throw new IllegalArgumentException(String.format("%s principal not found", actorType));
		}
	}

}

package hu.blackbelt.judo.meta.expression.asm.osgi.itest;

/*-
 * #%L
 * JUDO :: Expression :: Model :: ASM
 * %%
 * Copyright (C) 2018 - 2022 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.judo.meta.expression.operator.DecimalOperator;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel.ExpressionValidationException;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel.AsmValidationException;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel.SaveArguments;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import hu.blackbelt.judo.meta.measure.BaseMeasure;
import hu.blackbelt.judo.meta.measure.DerivedMeasure;
import hu.blackbelt.judo.meta.measure.DurationType;
import hu.blackbelt.judo.meta.measure.runtime.MeasureModel;
import hu.blackbelt.osgi.utils.osgi.api.BundleTrackerManager;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.*;

import javax.inject.Inject;
import java.io.*;
import java.math.BigDecimal;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.buildAsmModel;
import static hu.blackbelt.judo.meta.expression.adapters.asm.ExpressionValidatorOnAsm.validateExpressionOnAsm;
import static hu.blackbelt.judo.meta.expression.asm.osgi.itest.KarafFeatureProvider.karafConfig;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newDecimalConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.newDecimalArithmeticExpressionBuilder;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionModel.buildExpressionModel;
import static hu.blackbelt.judo.meta.measure.runtime.MeasureModel.buildMeasureModel;
import static hu.blackbelt.judo.meta.measure.util.builder.MeasureBuilders.*;
import static hu.blackbelt.judo.meta.measure.util.builder.MeasureBuilders.newBaseMeasureTermBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.useEPackage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.swissbox.core.BundleUtils.getBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@Slf4j
public class ExpressionWithASMAdapterBundleITest {

    public static final String HU_BLACKBELT_JUDO_META_EXPRESSION_OSGI = "hu.blackbelt.judo.meta.expression.osgi";
    public static final String HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_MEASURE = "hu.blackbelt.judo.meta.expression.model.adapter.measure";
    public static final String HU_BLACKBELT_JUDO_META = "hu.blackbelt.judo.meta";
    public static final String HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_ASM = "hu.blackbelt.judo.meta.expression.model.adapter.asm";
    public static final String HU_BLACKBELT_JUDO_META_MEASURE_OSGI = "hu.blackbelt.judo.meta.measure.osgi";
    public static final String HU_BLACKBELT_JUDO_META_ASM_OSGI = "hu.blackbelt.judo.meta.asm.osgi";

    private static final String DEMO_ASM = "test";
    private static final String DEMO_EXPRESSION = "t002";

    @Inject
    protected BundleTrackerManager bundleTrackerManager;

    @Inject
    BundleContext bundleContext;

    @Inject
    AsmModel asmModel;

    @Inject
    ExpressionModel expressionModel;

    @Inject
    MeasureModel measureModel;

    @Configuration
    public Option[] config() throws IOException, ExpressionValidationException, AsmValidationException, MeasureModel.MeasureValidationException {

        return combine(karafConfig(this.getClass()),
                mavenBundle(maven()
                        .groupId(HU_BLACKBELT_JUDO_META)
                        .artifactId(HU_BLACKBELT_JUDO_META_EXPRESSION_OSGI)
                        .versionAsInProject()),

                mavenBundle(maven()
                        .groupId(HU_BLACKBELT_JUDO_META)
                        .artifactId(HU_BLACKBELT_JUDO_META_ASM_OSGI)
                        .versionAsInProject()),

//                mavenBundle(maven()
//                        .groupId(HU_BLACKBELT_JUDO_META)
//                        .artifactId(HU_BLACKBELT_JUDO_META_PSM_OSGI)
//                        .versionAsInProject()),
//
                mavenBundle(maven()
                        .groupId(HU_BLACKBELT_JUDO_META)
                        .artifactId(HU_BLACKBELT_JUDO_META_MEASURE_OSGI)
                        .versionAsInProject()),

                /*
                mavenBundle(maven()
                        .groupId(HU_BLACKBELT_JUDO_META)
                        .artifactId(HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_ASM)
                        .versionAsInProject()),
                */
                mavenBundle(maven()
                        .groupId(HU_BLACKBELT_JUDO_META)
                        .artifactId(HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_ASM)
                        .versionAsInProject()),

                mavenBundle(maven()
                        .groupId(HU_BLACKBELT_JUDO_META)
                        .artifactId(HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_MEASURE)
                        .versionAsInProject()),
                getProvisonModelBundle()

        );
    }

    @Test
    public void testBundleActive() {
        assertNotNull(getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_OSGI));
        assertEquals(Bundle.ACTIVE, getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_OSGI)
                .getState());

        /*
        assertNotNull(getBundle(bundleContext, HU_BLACKBELT_JUDO_META_ASM_OSGI));
        assertEquals(Bundle.ACTIVE, getBundle(bundleContext, HU_BLACKBELT_JUDO_META_ASM_OSGI)
                .getState());
        */

        assertNotNull(getBundle(bundleContext, HU_BLACKBELT_JUDO_META_ASM_OSGI));
        assertEquals(Bundle.ACTIVE, getBundle(bundleContext, HU_BLACKBELT_JUDO_META_ASM_OSGI)
                .getState());

        assertNotNull(getBundle(bundleContext, HU_BLACKBELT_JUDO_META_MEASURE_OSGI));
        assertEquals(Bundle.ACTIVE, getBundle(bundleContext, HU_BLACKBELT_JUDO_META_MEASURE_OSGI)
                .getState());

        /*
        assertNotNull(getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_ASM));
        assertEquals(Bundle.ACTIVE, getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_ASM)
                .getState());
        */

        assertNotNull(getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_ASM));
        assertEquals(Bundle.ACTIVE, getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_ASM)
                .getState());

        assertNotNull(getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_MEASURE));
        assertEquals(Bundle.ACTIVE, getBundle(bundleContext, HU_BLACKBELT_JUDO_META_EXPRESSION_MODEL_ADAPTER_MEASURE)
                .getState());

    }

    public Option getProvisonModelBundle() throws IOException, ExpressionValidationException, AsmValidationException, MeasureModel.MeasureValidationException {
        return provision(
                getAsmModelBundle(),
                getExpressionModelBundle(),
                getMeasureModelBundle()
        );
    }

    private InputStream getAsmModelBundle() throws IOException, AsmValidationException {

        AsmModel asmModel = buildAsmModel()
                .build();

        populateAsmModel(asmModel);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        asmModel.saveAsmModel(SaveArguments.asmSaveArgumentsBuilder().outputStream(os));

        return bundle()
                .add( "model/" + DEMO_ASM+ ".judo-meta-asm",
                        new ByteArrayInputStream(os.toByteArray()))
                .set( Constants.BUNDLE_MANIFESTVERSION, "2")
                .set( Constants.BUNDLE_SYMBOLICNAME, DEMO_ASM + "-psm" )
                .set( "Asm-Models", "file=model/" + DEMO_ASM + ".judo-meta-asm;name=" + DEMO_ASM)
                .set( "Measure-Models", "file=model/" + DEMO_ASM + ".judo-meta-asm;name=" + DEMO_ASM)
                .build( withBnd());
    }


    private InputStream getMeasureModelBundle() throws IOException, MeasureModel.MeasureValidationException {

        MeasureModel measureModel = buildMeasureModel()
                .build();

        populateMeasureModel(measureModel);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        measureModel.saveMeasureModel(hu.blackbelt.judo.meta.measure.runtime.MeasureModel.SaveArguments.
                measureSaveArgumentsBuilder().outputStream(os)
        );

        return bundle()
                .add( "model/" + DEMO_ASM+ ".judo-meta-measure",
                        new ByteArrayInputStream(os.toByteArray()))
                .set( Constants.BUNDLE_MANIFESTVERSION, "2")
                .set( Constants.BUNDLE_SYMBOLICNAME, DEMO_ASM + "-psm" )
                .set( "Measure-Models", "file=model/" + DEMO_ASM + ".judo-meta-measure;name=" + DEMO_ASM)
                .build( withBnd());
    }



    private InputStream getExpressionModelBundle() throws IOException, ExpressionValidationException {

        ExpressionModel expressionModel = buildExpressionModel()
                .name(DEMO_EXPRESSION)
                .build();

        populateExpressionModel(expressionModel);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        expressionModel.saveExpressionModel(hu.blackbelt.judo.meta.expression.runtime.ExpressionModel.SaveArguments
                .expressionSaveArgumentsBuilder().outputStream(os));

        return bundle()
                .add( "model/" + DEMO_EXPRESSION + ".judo-meta-expression",
                        new ByteArrayInputStream(os.toByteArray()))
                .set( Constants.BUNDLE_MANIFESTVERSION, "2")
                .set( Constants.BUNDLE_SYMBOLICNAME, DEMO_EXPRESSION + "-expression" )
                .set( "Expression-Models", "file=model/" + DEMO_EXPRESSION + ".judo-meta-expression;name=" + DEMO_EXPRESSION)
                .build( withBnd());
    }


    @Test
    public void testModelValidation() throws Exception {
        validateExpressionOnAsm(log, asmModel, measureModel, expressionModel);
    }


//    <T> Stream<T> getMeasureElement(AsmModel psmModel, final Class<T> clazz) {
//        final Iterable<Notifier> measureContents = psmModel.getResourceSet()::getAllContents;
//        return StreamSupport.stream(measureContents.spliterator(), true)
//                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
//    }


//    private void populateMeasureModel(Package measures) {
//        Measure time = newMeasureBuilder().withName("Time").withUnits(
//                        newDurationUnitBuilder().withName("nanosecond").withSymbol("ns").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0E+9)).withUnitType(DurationType.NANOSECOND).build(),
//                        newDurationUnitBuilder().withName("microsecond").withSymbol("μs").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1000000.0)).withUnitType(DurationType.MICROSECOND).build(),
//                        newDurationUnitBuilder().withName("millisecond").withSymbol("ms").withRateDividend(new Double(0.001)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.MILLISECOND).build(),
//                        newDurationUnitBuilder().withName("second").withSymbol("s").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.SECOND).build(),
//                        newDurationUnitBuilder().withName("minute").withSymbol("min").withRateDividend(new Double(60.0)).withRateDivisor(new Double(1.0)).build(),
//                        newDurationUnitBuilder().withName("hour").withSymbol("h").withRateDividend(new Double(3600.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.HOUR).build(),
//                        newDurationUnitBuilder().withName("day").withSymbol("").withRateDividend(new Double(86400.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.DAY).build(),
//                        newDurationUnitBuilder().withName("week").withSymbol("").withRateDividend(new Double(604800.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.WEEK).build(),
//                        newDurationUnitBuilder().withName("halfDay").withSymbol("").withRateDividend(new Double(43200.0)).withRateDivisor(new Double(1.0)).build())
//                .build();
//
//        Measure monthBasedTime = newMeasureBuilder().withName("MonthBasedTime").withUnits(
//                        newDurationUnitBuilder().withName("month").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.MONTH).build(),
//                        newDurationUnitBuilder().withName("year").withRateDividend(new Double(12.0)).withRateDivisor(new Double(1.0)).withUnitType(DurationType.YEAR).build())
//                .build();
//
//        Measure mass = newMeasureBuilder().withName("Mass").withUnits(
//                        newUnitBuilder().withName("milligram").withSymbol("mg").withRateDividend(new Double(0.0000010)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("gram").withSymbol("g").withRateDividend(new Double(0.001)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("dekagram").withSymbol("dkg").withRateDividend(new Double(0.01)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("kilogram").withSymbol("kg").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("quintal").withSymbol("q").withRateDividend(new Double(100.0)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("tonne").withSymbol("t").withRateDividend(new Double(1000.0)).withRateDivisor(new Double(1.0)).build())
//                .build();
//
//        Measure length = newMeasureBuilder().withName("Length").withUnits(
//                        newUnitBuilder().withName("nanometre").withSymbol("nm").withRateDividend(new Double(1.0E-9)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("micrometre").withSymbol("μm").withRateDividend(new Double(0.0000010)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("millimetre").withSymbol("mm").withRateDividend(new Double(0.001)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("centimetre").withSymbol("cm").withRateDividend(new Double(0.01)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("decimetre").withSymbol("dm").withRateDividend(new Double(0.1)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("metre").withSymbol("m").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("kilometre").withSymbol("km").withRateDividend(new Double(1000.0)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("inch").withSymbol("&quot;").withRateDividend(new Double(0.0254)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("foot").withSymbol("ft").withRateDividend(new Double(0.3048)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("mile").withSymbol("mi").withRateDividend(new Double(1609.344)).withRateDivisor(new Double(1.0)).build())
//                .build();
//
//        DerivedMeasure velocity = newDerivedMeasureBuilder().withName("Velocity").withUnits(
//                        newUnitBuilder().withName("kilometrePerHour").withSymbol("km/h").withRateDividend(new Double(1.0)).withRateDivisor(new Double(3.6)).build(),
//                        newUnitBuilder().withName("metrePerSecond").withSymbol("m/s").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build())
//                .withTerms(newMeasureDefinitionTermBuilder().withExponent(1).withUnit(length.getUnits().get(6)).build())
//                .withTerms(newMeasureDefinitionTermBuilder().withExponent(-1).withUnit(time.getUnits().get(5)).build())
//                .build();
//
//        DerivedMeasure area = newDerivedMeasureBuilder().withName("Area").withUnits(
//                        newUnitBuilder().withName("squareMillimetre").withSymbol("mm²").withRateDividend(new Double(0.0000010)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("squareCentimetre").withSymbol("cm²").withRateDividend(new Double(0.00010)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("squareDecimetre").withSymbol("dm²").withRateDividend(new Double(0.01)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("squareMetre").withSymbol("m²").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("hectare").withSymbol("ha").withRateDividend(new Double(10000.0)).withRateDivisor(new Double(1.0)).build(),
//                        newUnitBuilder().withName("squareKilometre").withSymbol("km²").withRateDividend(new Double(1000000.0)).withRateDivisor(new Double(1.0)).build())
//                .withTerms(newMeasureDefinitionTermBuilder().withExponent(2).withUnit(length.getUnits().get(0)).build())
//                .build();
//
//        DerivedMeasure force = newDerivedMeasureBuilder().withName("Force").withUnits(
//                        newUnitBuilder().withName("newton").withSymbol("N").withRateDividend(new Double(1.0)).withRateDivisor(new Double(1.0)).build())
//                .withTerms(newMeasureDefinitionTermBuilder().withExponent(-2).withUnit(time.getUnits().get(0)).build())
//                .withTerms(newMeasureDefinitionTermBuilder().withExponent(1).withUnit(mass.getUnits().get(0)).build())
//                .withTerms(newMeasureDefinitionTermBuilder().withExponent(1).withUnit(length.getUnits().get(0)).build())
//                .build();
//
//        usePackage(measures).withElements(time,mass,length,velocity,area,force,monthBasedTime).build();
//        return;

//    }
//    private void populateAsmModel(AsmModel asmModel) {
//        Model model = newModelBuilder().withName("demo").build();
//
//        psmModel.addContent(model);
//
//        hu.blackbelt.judo.meta.psm.namespace.Package measures = newPackageBuilder().withName("measures").build();
//        useModel(model).withPackages(measures).build();
//
//        populateMeasureModel(measures);
//
//        Primitive timestampType = newTimestampTypeBuilder()
//                .withName("Timestamp")
//                .build();
//
//        Primitive phoneType = newStringTypeBuilder()
//                .withName("Phone")
//                .withMaxLength(256)
//                .build();
//
//        Primitive booleanType = newBooleanTypeBuilder()
//                .withName("Boolean")
//                .build();
//
//        Primitive integerType = newNumericTypeBuilder()
//                .withName("Integer")
//                .withPrecision(8)
//                .build();
//
//        Primitive stringType = newStringTypeBuilder()
//                .withMaxLength(256)
//                .withName("String")
//                .build();
//
//        Primitive binaryType = newCustomTypeBuilder()
//                .withName("Binary")
//                .build();
//
//        Primitive doubleType = newNumericTypeBuilder()
//                .withName("Double")
//                .withScale(7)
//                .withPrecision(18)
//                .build();
//
//        Primitive kgType = newMeasuredTypeBuilder()
//                .withName("kg")
//                .withPrecision(18)
//                .withScale(7)
//                .withStoreUnit(getMeasureElement(psmModel, Unit.class).filter(u -> "kg".equals(u.getSymbol())).findAny().get())
//                .build();
//
//        Primitive cmType = newMeasuredTypeBuilder()
//                .withName("cm")
//                .withPrecision(18)
//                .withScale(7)
//                .withStoreUnit(getMeasureElement(psmModel, Unit.class).filter(u -> "cm".equals(u.getSymbol())).findAny().get())
//                .build();
//
//        EnumerationType countries = newEnumerationTypeBuilder().withName("Countries").withMembers(
//                newEnumerationMemberBuilder().withName("HU").withOrdinal(0).build(),
//                newEnumerationMemberBuilder().withName("AT").withOrdinal(1).build(),
//                newEnumerationMemberBuilder().withName("RO").withOrdinal(2).build(),
//                newEnumerationMemberBuilder().withName("SK").withOrdinal(3).build()
//        ).build();
//        EnumerationType titles = newEnumerationTypeBuilder().withName("Titles").withMembers(
//                newEnumerationMemberBuilder().withName("MS").withOrdinal(0).build(),
//                newEnumerationMemberBuilder().withName("MRS").withOrdinal(1).build(),
//                newEnumerationMemberBuilder().withName("MR").withOrdinal(2).build(),
//                newEnumerationMemberBuilder().withName("DR").withOrdinal(3).build()
//        ).build();
//
//        EntityType product = newEntityTypeBuilder()
//                .withName("Product")
//                .withAttributes(ImmutableList.of(
//                        newAttributeBuilder().withName("discount").withDataType(doubleType).build(),
//                        newAttributeBuilder().withName("discounted").withDataType(booleanType).build(),
//                        newAttributeBuilder().withName("url").withDataType(stringType).build(),
//                        newAttributeBuilder().withName("productName").withDataType(stringType).build(),
//                        newAttributeBuilder().withName("unitPrice").withDataType(doubleType).build(),
//                        newAttributeBuilder().withName("weight").withDataType(kgType).build(),
//                        newAttributeBuilder().withName("height").withDataType(cmType).build()
//                )).build();
//
//        EntityType customer = newEntityTypeBuilder()
//                .withName("Customer")
//                .build();
//
//        EntityType company = newEntityTypeBuilder()
//                .withName("Company")
//                .withSuperEntityTypes(customer)
//                .build();
//
//        EntityType order = newEntityTypeBuilder()
//                .withName("Order")
//                .withAttributes(newAttributeBuilder().withName("orderDate").withDataType(timestampType).build(),
//                        newAttributeBuilder().withName("shippedDate").withDataType(timestampType).build(),
//                        newAttributeBuilder().withName("exciseTax").withDataType(doubleType).build())
//                .build();
//
//        EntityType internationalOrder = newEntityTypeBuilder()
//                .withName("InternationalOrder")
//                .withSuperEntityTypes(order)
//                .build();
//
//        EntityType orderDetail = newEntityTypeBuilder()
//                .withName("OrderDetail")
//                .withAttributes(newAttributeBuilder().withName("unitPrice").withDataType(doubleType).build(),
//                        newAttributeBuilder().withName("discount").withDataType(doubleType).build(),
//                        newAttributeBuilder().withName("quantity").withDataType(integerType).build())
//                .build();
//
//        EntityType shipper = newEntityTypeBuilder()
//                .withName("Shipper")
//                .withAttributes(newAttributeBuilder().withName("companyName").withDataType(stringType).build())
//                .build();
//
//        EntityType category = newEntityTypeBuilder()
//                .withName("Category")
//                .withAttributes(newAttributeBuilder().withName("categoryName").withDataType(stringType).build(),
//                        newAttributeBuilder().withName("picture").withDataType(binaryType).build())
//                .build();
//
//        EntityType address = newEntityTypeBuilder()
//                .withName("Address")
//                .withAttributes(newAttributeBuilder().withName("postalCode").withDataType(phoneType).build())
//                .build();
//
//        EntityType employee = newEntityTypeBuilder()
//                .withName("Employee")
//                .withAttributes(newAttributeBuilder().withName("firstName").withDataType(stringType).build(),
//                        newAttributeBuilder().withName("lastName").withDataType(stringType).build())
//                .build();
//
//        EntityType intAddress = newEntityTypeBuilder().withName("InternationalAddress").withSuperEntityTypes(address)
//                .withAttributes(newAttributeBuilder().withName("country").withDataType(countries).build())
//                .build();
//
//        AssociationEnd products = newAssociationEndBuilder().withName("products").withTarget(product)
//                .withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
//        AssociationEnd customerForOrder = newAssociationEndBuilder().withName("customer").withTarget(customer)
//                .withCardinality(newCardinalityBuilder().withLower(0).withUpper(1).build()).build();
//        AssociationEnd productOrderDetail = newAssociationEndBuilder().withName("product").withTarget(product)
//                .withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
//        AssociationEnd categoryRef = newAssociationEndBuilder().withName("category").withTarget(category)
//                .withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
//        Containment orderDetails = newContainmentBuilder().withName("orderDetails").withTarget(orderDetail)
//                .withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
//        Containment addresses = newContainmentBuilder().withName("addresses").withTarget(address)
//                .withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
//        AssociationEnd shipperRef = newAssociationEndBuilder().withName("shipper").withTarget(shipper)
//                .withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
//        AssociationEnd emplRef = newAssociationEndBuilder().withName("employee").withTarget(employee)
//                .withCardinality(newCardinalityBuilder().withLower(1).withUpper(1).build()).build();
//        AssociationEnd orders = newAssociationEndBuilder().withName("orders").withTarget(order)
//                .withCardinality(newCardinalityBuilder().withLower(0).withUpper(-1).build()).build();
//
//        useAssociationEnd(products).withPartner(categoryRef).build();
//        useAssociationEnd(categoryRef).withPartner(products).build();
//        useAssociationEnd(orders).withPartner(emplRef).build();
//        useAssociationEnd(emplRef).withPartner(orders).build();
//
//        useEntityType(order).withRelations(orderDetails,shipperRef,emplRef,customerForOrder).build();
//        useEntityType(category).withRelations(products).build();
//        useEntityType(product).withRelations(categoryRef).build();
//        useEntityType(customer).withRelations(addresses).build();
//        useEntityType(employee).withRelations(orders).build();
//        useEntityType(orderDetail).withRelations(productOrderDetail).build();
//
//        hu.blackbelt.judo.meta.psm.namespace.Package entities = newPackageBuilder().withName("entities").withElements(category,order,customer,company,product,orderDetail,shipper,address,intAddress,internationalOrder,employee).build();
//        Package types = newPackageBuilder().withName("types").withElements(stringType,doubleType,kgType,cmType,countries,titles,timestampType,phoneType,booleanType,integerType,binaryType).build();
//
//        useModel(model).withPackages(entities,types).build();
//    }



//    public void setUp() throws Exception {
//        asmModel = buildAsmModel()
//                .uri(URI.createURI("urn:asm.judo-meta-asm"))
//                .build();
//        asmUtils = new AsmUtils(asmModel.getResourceSet());
//        populateAsmModel();
//
//        measureModel = buildMeasureModel()
//                .name(asmModel.getName())
//                .build();
//        populateMeasureModel();
//
//        log.info(asmModel.getDiagnosticsAsString());
//        log.info(measureModel.getDiagnosticsAsString());
//        assertTrue(asmModel.isValid());
//        assertTrue(measureModel.isValid());
//    }

    private void populateExpressionModel(ExpressionModel expressionModel) {

        ExpressionModelResourceSupport expressionModelResourceSupport = expressionModel.getExpressionModelResourceSupport();

        expressionModelResourceSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("kg").build())
                .withOperator(DecimalOperator.ADD)
                .withRight(newDecimalConstantBuilder().withValue(BigDecimal.TEN).build())
                .build());

        expressionModelResourceSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("kg").build())
                .withOperator(DecimalOperator.MULTIPLY)
                .withRight(newDecimalConstantBuilder().withValue(BigDecimal.TEN).build())
                .build());

        expressionModelResourceSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("cm").build())
                .withOperator(DecimalOperator.ADD)
                .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.TEN).withUnitName("m").build())
                .build());

        expressionModelResourceSupport.addContent(newDecimalArithmeticExpressionBuilder()
                .withLeft(newDecimalConstantBuilder().withValue(BigDecimal.ONE).build())
                .withOperator(DecimalOperator.ADD)
                .withRight(newDecimalConstantBuilder().withValue(BigDecimal.TEN).build())
                .build());
    }


    private void populateAsmModel(AsmModel asmModel) {
        AsmUtils asmUtils = new AsmUtils(asmModel.getResourceSet());
        //enum
        EEnum countriesEnum = newEEnumBuilder().withName("Countries").withELiterals(newEEnumLiteralBuilder().withLiteral("HU").withName("HU").withValue(0).build(),
                newEEnumLiteralBuilder().withLiteral("AT").withName("AT").withValue(1).build(),
                newEEnumLiteralBuilder().withLiteral("RO").withName("RO").withValue(2).build(),
                newEEnumLiteralBuilder().withLiteral("SK").withName("SK").withValue(3).build()).build();

        EEnum titlesEnum = newEEnumBuilder().withName("Titles").withELiterals(newEEnumLiteralBuilder().withLiteral("MS").withName("MS").withValue(0).build(),
                newEEnumLiteralBuilder().withLiteral("MRS").withName("MRS").withValue(1).build(),
                newEEnumLiteralBuilder().withLiteral("MR").withName("MR").withValue(2).build(),
                newEEnumLiteralBuilder().withLiteral("DR").withName("DR").withValue(3).build()).build();

        //types
        EDataType timestamp = newEDataTypeBuilder().withName("Timestamp").withInstanceClassName("java.time.LocalDateTime").build();
        EDataType stringType = newEDataTypeBuilder().withName("String").withInstanceClassName("java.lang.String").build();
        EDataType doubleType = newEDataTypeBuilder().withName("Double").withInstanceClassName("java.lang.Double").build();
        EDataType integerType = newEDataTypeBuilder().withName("Integer").withInstanceClassName("java.lang.Integer").build();
        EDataType binary = newEDataTypeBuilder().withName("Binary").withInstanceClassName("java.lang.Object").build();
        EDataType timeStoredInMonths = newEDataTypeBuilder().withName("TimeStoredInMonths").withInstanceClassName("java.lang.Integer").build();
        EDataType timeStoredInSeconds = newEDataTypeBuilder().withName("TimeStoredInSeconds").withInstanceClassName("java.lang.Double").build();
        EDataType dateType = newEDataTypeBuilder().withName("Date").withInstanceClassName("java.time.LocalDate").build();
        EDataType phoneType = newEDataTypeBuilder().withName("Phone").withInstanceClassName("java.lang.String").build();
        EDataType booleanType = newEDataTypeBuilder().withName("Boolean").withInstanceClassName("java.lang.Boolean").build();
        EDataType massStoredInKilograms = newEDataTypeBuilder().withName("MassStoredInKilograms").withInstanceClassName("java.lang.Double").build();
        EDataType lengthStoredInCentimetres = newEDataTypeBuilder().withName("LengthStoredInCentimetres").withInstanceClassName("java.lang.Double").build();

        //attributes
        EAttribute orderDate = newEAttributeBuilder().withName("orderDate").withEType(timestamp).build();
        EAttribute shippedDate = newEAttributeBuilder().withName("shippedDate").withEType(timestamp).build();
        EAttribute companyName = newEAttributeBuilder().withName("companyName").withEType(stringType).build();
        EAttribute exciseTax = newEAttributeBuilder().withName("exciseTax").withEType(doubleType).build();
        EAttribute customsDescription = newEAttributeBuilder().withName("customsDescription").withEType(stringType).build();
        EAttribute productName = newEAttributeBuilder().withName("productName").withEType(stringType).build();
        EAttribute unitPrice = newEAttributeBuilder().withName("unitPrice").withEType(doubleType).build();
        EAttribute categoryName = newEAttributeBuilder().withName("categoryName").withEType(stringType).build();
        EAttribute unitPriceOrderDetail = newEAttributeBuilder().withName("unitPrice").withEType(doubleType).build();
        EAttribute quantity = newEAttributeBuilder().withName("quantity").withEType(integerType).build();
        EAttribute discount = newEAttributeBuilder().withName("discount").withEType(doubleType).build();
        EAttribute country = newEAttributeBuilder().withName("country").withEType(countriesEnum).build();
        EAttribute picture = newEAttributeBuilder().withName("picture").withEType(binary).build();
        EAttribute quantityPerUnit = newEAttributeBuilder().withName("quantityPerUnit").withEType(integerType).build();
        EAttribute firstName = newEAttributeBuilder().withName("firstName").withEType(stringType).build();
        EAttribute firstNameEmployee = newEAttributeBuilder().withName("firstName").withEType(stringType).build();
        EAttribute lastNameEmployee = newEAttributeBuilder().withName("lastName").withEType(stringType).build();
        EAttribute phone = newEAttributeBuilder().withName("phone").withEType(phoneType).build();
        EAttribute discounted = newEAttributeBuilder().withName("discounted").withEType(booleanType).build();
        EAttribute weight = newEAttributeBuilder().withName("weight").withEType(massStoredInKilograms).build();
        EAttribute height = newEAttributeBuilder().withName("height").withEType(lengthStoredInCentimetres).build();
        EAttribute freight = newEAttributeBuilder().withName("freight").withEType(doubleType).build();
        EAttribute price = newEAttributeBuilder().withName("price").withEType(doubleType).build();
        EAttribute postalCode = newEAttributeBuilder().withName("postalCode").withEType(phoneType).build();

        //relations
        EReference orderDetails = newEReferenceBuilder().withName("orderDetails").withContainment(true).withLowerBound(0).withUpperBound(-1).build();
        EReference productRef = newEReferenceBuilder().withName("product").withLowerBound(1).withUpperBound(1).build();
        EReference categoryRef = newEReferenceBuilder().withName("category").withLowerBound(1).withUpperBound(1).build();
        EReference productsRef = newEReferenceBuilder().withName("products").withLowerBound(0).withUpperBound(-1).build();
        EReference categories = newEReferenceBuilder().withName("categories").withLowerBound(0).withUpperBound(-1).build();
        EReference ordersRef = newEReferenceBuilder().withName("orders").withLowerBound(0).withUpperBound(-1).build();
        EReference employeeRef = newEReferenceBuilder().withName("employee").withLowerBound(0).withUpperBound(1).build();
        EReference shipperOrdersRef = newEReferenceBuilder().withName("shipperOrders").withLowerBound(0).withUpperBound(-1).build();
        EReference shipperRef = newEReferenceBuilder().withName("shipper").withLowerBound(0).withUpperBound(1).build();
        EReference ordersCustomer = newEReferenceBuilder().withName("orders").withLowerBound(0).withUpperBound(-1).build();
        EReference addressesCustomer = newEReferenceBuilder().withName("addresses").withLowerBound(0).withUpperBound(-1)
                .withContainment(true).build();
        EReference customerOrder = newEReferenceBuilder().withName("customer").withLowerBound(0).withUpperBound(1).build();
        EReference owner = newEReferenceBuilder().withName("owner").withLowerBound(0).withUpperBound(1).build();
        EReference categoryEmployee = newEReferenceBuilder().withName("category").withLowerBound(0).withUpperBound(-1).build();
        EReference territoryRef = newEReferenceBuilder().withName("territory").withLowerBound(0).withUpperBound(1).build();
        EReference shipperTerritory = newEReferenceBuilder().withName("shipper").withLowerBound(0).withUpperBound(1).build();
        EReference shipAddress = newEReferenceBuilder().withName("shipAddress").withLowerBound(0).withUpperBound(1).build();

        //classes
        EClass order = newEClassBuilder().withName("Order")
                .withEStructuralFeatures(orderDate,orderDetails,categories,employeeRef,shipperRef,customerOrder,shipAddress,freight,exciseTax,shippedDate).build();
        EClass orderDetail = newEClassBuilder().withName("OrderDetail").withEStructuralFeatures(productRef,unitPriceOrderDetail,quantity,discount,price).build();
        EClass product = newEClassBuilder().withName("Product").withEStructuralFeatures(categoryRef,productName,unitPrice,quantityPerUnit,discounted,weight,height).build();
        EClass category = newEClassBuilder().withName("Category").withEStructuralFeatures(productsRef,categoryName,picture,owner).build();
        EClass employee = newEClassBuilder().withName("Employee").withEStructuralFeatures(ordersRef,categoryEmployee,firstNameEmployee,lastNameEmployee).build();
        EClass internationalOrder = newEClassBuilder().withName("InternationalOrder").withEStructuralFeatures(customsDescription)
                .withESuperTypes(order).build();
        EClass customer = newEClassBuilder().withName("Customer").withEStructuralFeatures(ordersCustomer,addressesCustomer).build();
        EClass address = newEClassBuilder().withName("Address").withEStructuralFeatures(postalCode).build();
        EClass internationalAddress = newEClassBuilder().withName("InternationalAddress")
                .withESuperTypes(address).withEStructuralFeatures(country).build();
        EClass company = newEClassBuilder().withName("Company").withESuperTypes(customer).build();
        EClass shipper = newEClassBuilder().withName("Shipper").withEStructuralFeatures(companyName,shipperOrdersRef,phone,territoryRef)
                .withESuperTypes(company).build();
        EClass onlineOrder = newEClassBuilder().withName("OnlineOrder")
                .withESuperTypes(order).build();
        EClass individual = newEClassBuilder().withName("Individual").withEStructuralFeatures(firstName)
                .withESuperTypes(customer).build();
        EClass supplier = newEClassBuilder().withName("Supplier")
                .withESuperTypes(company).build();
        EClass territory = newEClassBuilder().withName("Territory").withEStructuralFeatures(shipperTerritory).build();

        //relations again
        useEReference(orderDetails).withEType(orderDetail).build();
        useEReference(productRef).withEType(product).build();
        useEReference(categoryRef).withEType(category).withEOpposite(productsRef).build();
        useEReference(productsRef).withEType(product).withEOpposite(categoryRef).build();
        useEReference(categories).withEType(category).build();
        useEReference(ordersRef).withEType(order).withEOpposite(employeeRef).build();
        useEReference(employeeRef).withEType(employee).withEOpposite(ordersRef).build();
        useEReference(shipperOrdersRef).withEType(order).withEOpposite(shipperRef).build();
        useEReference(shipperRef).withEType(shipper).withEOpposite(shipperOrdersRef).build();
        useEReference(addressesCustomer).withEType(address).build();
        useEReference(ordersCustomer).withEType(order).withEOpposite(customerOrder).build();
        useEReference(customerOrder).withEType(customer).withEOpposite(ordersCustomer).build();
        useEReference(owner).withEType(employee).withEOpposite(categoryEmployee).build();
        useEReference(categoryEmployee).withEType(category).withEOpposite(owner).build();
        useEReference(shipperTerritory).withEType(shipper).withEOpposite(territoryRef).build();
        useEReference(territoryRef).withEType(territory).withEOpposite(shipperTerritory).build();
        useEReference(shipAddress).withEType(address).build();

        //packages
        EPackage demo = newEPackageBuilder().withName("demo").withNsURI("http://blackbelt.hu/judo/northwind/northwind/demo")
                .withNsPrefix("runtimenorthwindNorthwindDemo").build();
        EPackage services = newEPackageBuilder().withName("services").withNsURI("http://blackbelt.hu/judo/northwind/northwind/services")
                .withNsPrefix("runtimenorthwindNorthwindServices").build();
        EPackage entities = newEPackageBuilder().withName("entities")
                .withEClassifiers(order,
                        orderDetail,product,category,employee,
                        shipper,internationalOrder,customer,address,
                        internationalAddress,company,onlineOrder,individual,supplier,territory)
                .withNsURI("http://blackbelt.hu/judo/northwind/northwind/entities")
                .withNsPrefix("runtimenorthwindNorthwindEntities").build();
        EPackage types = newEPackageBuilder().withName("types")
                .withEClassifiers(timestamp,stringType,doubleType,integerType,binary,dateType,countriesEnum,phoneType,booleanType,titlesEnum)
                .withNsURI("http://blackbelt.hu/judo/northwind/northwind/types")
                .withNsPrefix("runtimenorthwindNorthwindTypes").build();
        EPackage measured = newEPackageBuilder().withName("measured").withEClassifiers(timeStoredInMonths,timeStoredInSeconds,massStoredInKilograms,lengthStoredInCentimetres)
                .withNsURI("http://blackbelt.hu/judo/northwind/demo/types/measured")
                .withNsPrefix("runtimenorthwindDemoTypesMeasured").build();
        EPackage measures = newEPackageBuilder().withName("measures")
                .withNsURI("http://blackbelt.hu/judo/northwind/demo/measures")
                .withNsPrefix("runtimenorthwindDemoMeasures").build();

        //packages again
        useEPackage(demo).withESubpackages(services,entities,types,measures).build();
        useEPackage(types).withESubpackages(measured).build();

        asmModel.addContent(demo);

        //annotations
        EAnnotation orderAnnotation = asmUtils.getExtensionAnnotationByName(order, "entity", true).get();
        orderAnnotation.getDetails().put("value", "true");
        EAnnotation orderDetailAnnotation = asmUtils.getExtensionAnnotationByName(orderDetail, "entity", true).get();
        orderDetailAnnotation.getDetails().put("value", "true");
        EAnnotation productAnnotation = asmUtils.getExtensionAnnotationByName(product, "entity", true).get();
        productAnnotation.getDetails().put("value", "true");
        EAnnotation categoryAnnotation = asmUtils.getExtensionAnnotationByName(category, "entity", true).get();
        categoryAnnotation.getDetails().put("value", "true");
        EAnnotation employeeAnnotation = asmUtils.getExtensionAnnotationByName(employee, "entity", true).get();
        employeeAnnotation.getDetails().put("value", "true");
        EAnnotation shipperAnnotation = asmUtils.getExtensionAnnotationByName(shipper, "entity", true).get();
        shipperAnnotation.getDetails().put("value", "true");
        EAnnotation intOrderAnnotation = asmUtils.getExtensionAnnotationByName(internationalOrder, "entity", true).get();
        intOrderAnnotation.getDetails().put("value", "true");
        EAnnotation addressAnnotation = asmUtils.getExtensionAnnotationByName(address, "entity", true).get();
        addressAnnotation.getDetails().put("value", "true");
        EAnnotation customerAnnotation = asmUtils.getExtensionAnnotationByName(customer, "entity", true).get();
        customerAnnotation.getDetails().put("value", "true");
        EAnnotation intAddrAnnotation = asmUtils.getExtensionAnnotationByName(internationalAddress, "entity", true).get();
        intAddrAnnotation.getDetails().put("value", "true");
        EAnnotation companyAnnotation = asmUtils.getExtensionAnnotationByName(company, "entity", true).get();
        companyAnnotation.getDetails().put("value", "true");
        EAnnotation onlineOrderAnnotation = asmUtils.getExtensionAnnotationByName(onlineOrder, "entity", true).get();
        onlineOrderAnnotation.getDetails().put("value", "true");
        EAnnotation individaulAnnotation = asmUtils.getExtensionAnnotationByName(individual, "entity", true).get();
        individaulAnnotation.getDetails().put("value", "true");
        EAnnotation supplierAnnotation = asmUtils.getExtensionAnnotationByName(supplier, "entity", true).get();
        supplierAnnotation.getDetails().put("value", "true");
        EAnnotation territoryAnnotation = asmUtils.getExtensionAnnotationByName(territory, "entity", true).get();
        territoryAnnotation.getDetails().put("value", "true");
        EAnnotation weightAnnotation = asmUtils.getExtensionAnnotationByName(weight, "constraints", true).get();
        weightAnnotation.getDetails().put("precision", "15");
        weightAnnotation.getDetails().put("scale", "4");
        weightAnnotation.getDetails().put("measure", "demo.measures.Mass");
        weightAnnotation.getDetails().put("unit", "kilogram");
        EAnnotation heightAnnotation = asmUtils.getExtensionAnnotationByName(height, "constraints", true).get();
        heightAnnotation.getDetails().put("precision", "15");
        heightAnnotation.getDetails().put("scale", "4");
        heightAnnotation.getDetails().put("measure", "demo.measures.Length");
        heightAnnotation.getDetails().put("unit", "centimetre");
    }

    private void populateMeasureModel(MeasureModel measureModel) {

        BaseMeasure time = newBaseMeasureBuilder().withName("Time").withNamespace("demo::measures").withUnits(
                        newDurationUnitBuilder().withName("nanosecond").withSymbol("ns").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0E+9)).withType(DurationType.NANOSECOND).build(),
                        newDurationUnitBuilder().withName("microsecond").withSymbol("μs").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1000000.0)).withType(DurationType.MICROSECOND).build(),
                        newDurationUnitBuilder().withName("millisecond").withSymbol("ms").withRateDividend(new BigDecimal(0.001)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.MILLISECOND).build(),
                        newDurationUnitBuilder().withName("second").withSymbol("s").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.SECOND).build(),
                        newDurationUnitBuilder().withName("minute").withSymbol("min").withRateDividend(new BigDecimal(60.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newDurationUnitBuilder().withName("hour").withSymbol("h").withRateDividend(new BigDecimal(3600.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.HOUR).build(),
                        newDurationUnitBuilder().withName("day").withSymbol("").withRateDividend(new BigDecimal(86400.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.DAY).build(),
                        newDurationUnitBuilder().withName("week").withSymbol("").withRateDividend(new BigDecimal(604800.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.WEEK).build(),
                        newDurationUnitBuilder().withName("halfDay").withSymbol("").withRateDividend(new BigDecimal(43200.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .build();

        BaseMeasure monthBasedTime = newBaseMeasureBuilder().withName("MonthBasedTime").withNamespace("demo::measures").withUnits(
                        newDurationUnitBuilder().withName("month").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.MONTH).build(),
                        newDurationUnitBuilder().withName("year").withRateDividend(new BigDecimal(12.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.YEAR).build())
                .build();

        BaseMeasure mass = newBaseMeasureBuilder().withName("Mass").withNamespace("demo::measures").withUnits(
                        newUnitBuilder().withName("milligram").withSymbol("mg").withRateDividend(new BigDecimal(0.0000010)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("gram").withSymbol("g").withRateDividend(new BigDecimal(0.001)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("dekagram").withSymbol("dkg").withRateDividend(new BigDecimal(0.01)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("kilogram").withSymbol("kg").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("quintal").withSymbol("q").withRateDividend(new BigDecimal(100.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("tonne").withSymbol("t").withRateDividend(new BigDecimal(1000.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .build();

        BaseMeasure length = newBaseMeasureBuilder().withName("Length").withNamespace("demo::measures").withUnits(
                        newUnitBuilder().withName("nanometre").withSymbol("nm").withRateDividend(new BigDecimal(1.0E-9)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("micrometre").withSymbol("μm").withRateDividend(new BigDecimal(0.0000010)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("millimetre").withSymbol("mm").withRateDividend(new BigDecimal(0.001)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("centimetre").withSymbol("cm").withRateDividend(new BigDecimal(0.01)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("decimetre").withSymbol("dm").withRateDividend(new BigDecimal(0.1)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("metre").withSymbol("m").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("kilometre").withSymbol("km").withRateDividend(new BigDecimal(1000.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("inch").withSymbol("&quot;").withRateDividend(new BigDecimal(0.0254)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("foot").withSymbol("ft").withRateDividend(new BigDecimal(0.3048)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("mile").withSymbol("mi").withRateDividend(new BigDecimal(1609.344)).withRateDivisor(new BigDecimal(1.0)).build())
                .build();

        DerivedMeasure velocity = newDerivedMeasureBuilder().withName("Velocity").withNamespace("demo::measures").withUnits(
                        newUnitBuilder().withName("kilometrePerHour").withSymbol("km/h").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(3.6)).build(),
                        newUnitBuilder().withName("metrePerSecond").withSymbol("m/s").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(1).withBaseMeasure(length).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(-1).withBaseMeasure(time).build())
                .build();

        DerivedMeasure area = newDerivedMeasureBuilder().withName("Area").withNamespace("demo::measures").withUnits(
                        newUnitBuilder().withName("squareMillimetre").withSymbol("mm²").withRateDividend(new BigDecimal(0.0000010)).withRateDivisor(new BigDecimal(3.6)).build(),
                        newUnitBuilder().withName("squareCentimetre").withSymbol("cm²").withRateDividend(new BigDecimal(0.00010)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("squareDecimetre").withSymbol("dm²").withRateDividend(new BigDecimal(0.01)).withRateDivisor(new BigDecimal(3.6)).build(),
                        newUnitBuilder().withName("squareMetre").withSymbol("m²").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("hectare").withSymbol("ha").withRateDividend(new BigDecimal(10000.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                        newUnitBuilder().withName("squareKilometre").withSymbol("km²").withRateDividend(new BigDecimal(1000000.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(2).withBaseMeasure(length).build())
                .build();

        DerivedMeasure force = newDerivedMeasureBuilder().withName("Force").withNamespace("demo::measures").withUnits(
                        newUnitBuilder().withName("newton").withSymbol("N").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(-2).withBaseMeasure(time).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(1).withBaseMeasure(mass).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(1).withBaseMeasure(length).build())
                .build();

        measureModel.addContent(time);
        measureModel.addContent(mass);
        measureModel.addContent(length);
        measureModel.addContent(velocity);
        measureModel.addContent(area);
        measureModel.addContent(force);
        measureModel.addContent(monthBasedTime);
    }
}

package hu.blackbelt.judo.meta.expression.adapters.asm;

/*-
 * #%L
 * JUDO :: Expression :: ASM Adapter Parent
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

import hu.blackbelt.judo.meta.expression.ExecutionContextOnAsmTest;
import hu.blackbelt.judo.meta.expression.TypeName;
import hu.blackbelt.judo.meta.expression.adapters.ModelAdapter;
import hu.blackbelt.judo.meta.expression.constant.Instance;
import hu.blackbelt.judo.meta.expression.constant.IntegerConstant;
import hu.blackbelt.judo.meta.expression.constant.MeasuredDecimal;
import hu.blackbelt.judo.meta.expression.numeric.NumericAttribute;
import hu.blackbelt.judo.meta.measure.Measure;
import hu.blackbelt.judo.meta.measure.Unit;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.*;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.newIntegerAttributeBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectVariableReferenceBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newMeasureNameBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEClassBuilder;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEEnumBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsmModelAdapterTest extends ExecutionContextOnAsmTest {

    private ModelAdapter<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference, EClass, EAttribute, EReference, EClassifier, Measure, Unit> modelAdapter;

    @BeforeEach
    public void setUp() throws Exception {

        super.setUp();
        modelAdapter = new AsmModelAdapter(asmModel.getResourceSet(), measureModel.getResourceSet());
    }

    @AfterEach
    public void tearDown() {
        modelAdapter = null;
    }

    @Test
    public void testGetTypeName() {
        Optional<TypeName> categoryTypeName = modelAdapter.buildTypeName(asmUtils.resolve("demo.entities.Category").get());

        assertTrue(categoryTypeName.isPresent());
        assertThat(categoryTypeName.get().getName(), is("Category")); //TODO: check, seems kinda silly (+psm)
        assertThat(categoryTypeName.get().getNamespace(), is("demo::entities"));
        //TODO: negtest maaaaybe? (+psm)
    }

    @Test
    public void testGet() {
        //TODO: check if needed
        final TypeName orderTypeName = newTypeNameBuilder()
                .withNamespace("demo.entities")
                .withName("Order")
                .build();

        final Optional<? extends EClassifier> orderEClassifier = modelAdapter.get(orderTypeName);
        assertTrue(orderEClassifier.isPresent());
        assertThat(orderEClassifier.get(), instanceOf(EClass.class));
        assertThat(orderEClassifier.get().getName(), is("Order"));
        assertThat(asmUtils.getPackageFQName(orderEClassifier.get().getEPackage()), is("demo.entities"));

        final TypeName negtest_name_TypeName = newTypeNameBuilder()
                .withNamespace("demo::entities")
                .withName("negtest")
                .build();
        final Optional<? extends EClassifier> negtest_name_NamespaceElement = modelAdapter.get(negtest_name_TypeName);
        assertThat(negtest_name_NamespaceElement.isPresent(), is(Boolean.FALSE));

        //TODO: remove b\c not needed?
        final TypeName negtest_namespace_TypeName = newTypeNameBuilder()
                .withNamespace("demo::negtest")
                .withName("negtest")
                .build();
        assertFalse(modelAdapter.get(negtest_namespace_TypeName).isPresent());
    }

    @Test
    public void testIsObjectType() {
        EClass eClass = newEClassBuilder().withName("EClass").build();
        EEnum eEnum = newEEnumBuilder().withName("EEnum").build();

        assertTrue(modelAdapter.isObjectType(eClass));
        assertFalse(modelAdapter.isObjectType(eEnum));
    }


    @Test
    public void testGetReference() {
        Optional<EClass> containerClass = asmUtils.resolve("demo.entities.Category").map(c -> (EClass) c);
        Optional<EReference> productsReference = containerClass.get().getEReferences().stream().filter(r -> "products".equals(r.getName())).findAny();
        Optional<EClass> targetClass = asmUtils.resolve("demo.entities.Product").map(c -> (EClass) c);

        assertTrue(containerClass.isPresent());
        assertTrue(targetClass.isPresent());
        assertTrue(productsReference.isPresent());
        assertThat(modelAdapter.getTarget(productsReference.get()), is(targetClass.get()));
    }

    @Test
    public void testGetAttribute() {
        Optional<EClass> eClass = asmUtils.resolve("demo.entities.Category").map(c -> (EClass) c);
        Optional<EAttribute> categoryNameEAttribute = eClass.get().getEAttributes().stream().filter(a -> "categoryName".equals(a.getName())).findAny();

        assertTrue(eClass.isPresent());
        assertTrue(categoryNameEAttribute.isPresent());
        assertThat(modelAdapter.getAttribute(eClass.get(), "categoryName"), is(categoryNameEAttribute));
        assertThat(modelAdapter.getAttribute(eClass.get(), "productName"), is(Optional.empty()));
    }

    @Test
    public void testGetAttributeType() {
        Optional<EClass> eClass = asmUtils.resolve("demo.entities.Category").map(c -> (EClass) c);
        Optional<EAttribute> eAttribute = eClass.get().getEAttributes().stream().filter(a -> "categoryName".equals(a.getName())).findAny();

        assertTrue(eClass.isPresent());
        assertTrue(eAttribute.isPresent());
        assertThat(modelAdapter.getAttributeType(eClass.get(), "categoryName").get(),
                is(eAttribute.get().getEAttributeType()));
    }

    @Test
    public void testGetSuperType() {
        Optional<EClass> childClass = asmUtils.resolve("demo.entities.Company").map(c -> (EClass) c);
        Optional<EClass> superClass = asmUtils.resolve("demo.entities.Customer").map(c -> (EClass) c);

        assertTrue(childClass.isPresent());
        assertTrue(superClass.isPresent());
        assertTrue(modelAdapter.getSuperTypes(childClass.get()).contains(superClass.get()));

        Optional<EClass> negtestClass = asmUtils.resolve("demo.entities.Order").map(c -> (EClass) c);
        assertTrue(negtestClass.isPresent());
        assertFalse(modelAdapter.getSuperTypes(childClass.get()).contains(negtestClass.get()));
        assertFalse(modelAdapter.getSuperTypes(negtestClass.get()).contains(superClass.get()));
    }

    @Test
    public void testContains() {
        Optional<EEnum> countries = getAsmElement(EEnum.class).filter(e -> "Countries".equals(e.getName())).findAny();
        assertTrue(countries.isPresent());
        assertTrue(modelAdapter.contains(countries.get(), "HU"));
        assertFalse(modelAdapter.contains(countries.get(), "MS"));

        Optional<EEnum> titles = getAsmElement(EEnum.class).filter(e -> "Titles".equals(e.getName())).findAny();
        assertTrue(titles.isPresent());
        assertFalse(modelAdapter.contains(titles.get(), "HU"));
    }

    @Test
    public void testIsDurationSupportingAddition() {
        Optional<Measure> time = getMeasureByName("Time");
        Optional<Unit> day = time.get().getUnits().stream().filter(u -> "day".equals(u.getName())).findAny();
        Optional<Unit> microsecond = time.get().getUnits().stream().filter(u -> "microsecond".equals(u.getName())).findAny();

        assertTrue(time.isPresent());
        assertTrue(day.isPresent());
        assertTrue(modelAdapter.isDurationSupportingAddition(day.get()));
        assertTrue(microsecond.isPresent());
        assertFalse(modelAdapter.isDurationSupportingAddition(microsecond.get()));
    }

    //TODO
    @Test
    void testGetUnit() {
        TypeName type = modelAdapter.buildTypeName(asmUtils.resolve("demo.entities.Product").get()).get();
        Instance instance = newInstanceBuilder().withElementName(type).build();
        //EClass eClass = newEClassBuilder().withName("Product")..build();

        Optional<Unit> kilogram = getUnitByName("kilogram");
        NumericAttribute numericAttribute = newIntegerAttributeBuilder()
                .withAttributeName("weight")
                .withObjectExpression(
                        newObjectVariableReferenceBuilder()
                                .withVariable(instance)
                                .build()
                )
                .build();
        assertTrue(modelAdapter.getUnit(numericAttribute).isPresent());
        assertThat(modelAdapter.getUnit(numericAttribute).get(), is(kilogram.get()));

        NumericAttribute nonMeasureNumericAttribute = newIntegerAttributeBuilder()
                .withAttributeName("quantityPerUnit")
                .withObjectExpression(
                        newObjectVariableReferenceBuilder()
                                .withVariable(instance)
                                .build()
                )
                .build();
        assertFalse(modelAdapter.getUnit(nonMeasureNumericAttribute).isPresent());
        assertThat(modelAdapter.getUnit(nonMeasureNumericAttribute), is(Optional.empty()));

        NumericAttribute notFoundAttributeNumericAttribute = newIntegerAttributeBuilder()
                .withAttributeName("somethingNonExistent")
                .withObjectExpression(
                        newObjectVariableReferenceBuilder()
                                .withVariable(instance)
                                .build()
                )
                .build();
        assertFalse(modelAdapter.getUnit(notFoundAttributeNumericAttribute).isPresent());
        assertThat(modelAdapter.getUnit(notFoundAttributeNumericAttribute), is(Optional.empty()));
        //--------------------------
        Optional<Unit> inch = getUnitByName("inch");
        assertTrue(inch.isPresent());
        //5cm téll hossz, 5m as dist -> length-e

        MeasuredDecimal inchMeasuredDecimal = newMeasuredDecimalBuilder()
                .withUnitName(
                        "inch"
                ).withMeasure(
                        newMeasureNameBuilder().withNamespace("demo::measures").withName("Length").build()
                ).build();
        assertThat(modelAdapter.getUnit(inchMeasuredDecimal).get(), is(inch.get()));
        //--------------------------
        MeasuredDecimal measuredDecimal = newMeasuredDecimalBuilder()
                .withUnitName("TODO")
                .withMeasure(
                        newMeasureNameBuilder().withName("TODO").build()
                ).build();
        assertFalse(modelAdapter.getUnit(measuredDecimal).isPresent());
        assertThat(modelAdapter.getUnit(measuredDecimal), is(Optional.empty()));

        IntegerConstant integerConstant = newIntegerConstantBuilder().build();
        assertThat(modelAdapter.getUnit(integerConstant), is(Optional.empty()));
    }

    <T> Stream<T> getAsmElement(final Class<T> clazz) {
        final Iterable<Notifier> asmContents = asmModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(asmContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }

    private Optional<Measure> getMeasureByName(final String measureName) {
        final Iterable<Notifier> measureContents = measureModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(measureContents.spliterator(), true)
                .filter(e -> Measure.class.isAssignableFrom(e.getClass())).map(e -> (Measure) e)
                .filter(m -> measureName.equals(m.getName()))
                .findAny();
    }

    private Optional<Unit> getUnitByName(final String unitName) {
        final Iterable<Notifier> measureContents = measureModel.getResourceSet()::getAllContents;
        return StreamSupport.stream(measureContents.spliterator(), true)
                .filter(e -> Unit.class.isAssignableFrom(e.getClass())).map(e -> (Unit) e)
                .filter(u -> unitName.equals(u.getName()))
                .findAny();
    }
}

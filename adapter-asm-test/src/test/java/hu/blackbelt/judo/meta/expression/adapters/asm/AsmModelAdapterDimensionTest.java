package hu.blackbelt.judo.meta.expression.adapters.asm;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.measure.util.builder.MeasureBuilders.newBaseMeasureBuilder;
import static hu.blackbelt.judo.meta.measure.util.builder.MeasureBuilders.newBaseMeasureTermBuilder;
import static hu.blackbelt.judo.meta.measure.util.builder.MeasureBuilders.newDerivedMeasureBuilder;
import static hu.blackbelt.judo.meta.measure.util.builder.MeasureBuilders.newUnitBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import hu.blackbelt.judo.meta.expression.NumericExpression;
import hu.blackbelt.judo.meta.expression.adapters.ModelAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureAdapter;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureProvider;
import hu.blackbelt.judo.meta.expression.constant.MeasuredDecimal;
import hu.blackbelt.judo.meta.measure.BaseMeasure;
import hu.blackbelt.judo.meta.measure.DerivedMeasure;
import hu.blackbelt.judo.meta.measure.Measure;
import hu.blackbelt.judo.meta.measure.Unit;
import hu.blackbelt.judo.meta.measure.support.MeasureModelResourceSupport;

public class AsmModelAdapterDimensionTest {

    private MeasureAdapter<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, Measure, Unit> measureAdapter;
    private MeasureProvider<Measure, Unit> measureProvider;
    private Resource resource;

    @BeforeEach
    public void setUp() {
        final ResourceSet resourceSet = MeasureModelResourceSupport.createMeasureResourceSet();
        resource = resourceSet.createResource(URI.createURI("urn:measure.judo-meta-measure"));
        measureProvider = new AsmMeasureProvider(resourceSet);

        final ModelAdapter<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference, EClass, EAttribute, EReference, EClassifier, Measure, Unit> modelAdapter = Mockito.mock(ModelAdapter.class);

        Mockito.doAnswer(invocationOnMock -> {
            final Object[] args = invocationOnMock.getArguments();
            if (args[0] instanceof MeasuredDecimal) {
                final MeasuredDecimal measuredDecimal = (MeasuredDecimal) args[0];
                return measureAdapter.getUnit(measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getNamespace()) : Optional.empty(),
                        measuredDecimal.getMeasure() != null ? Optional.ofNullable(measuredDecimal.getMeasure().getName()) : Optional.empty(),
                        measuredDecimal.getUnitName());
            } else {
                throw new IllegalStateException("Not supported by mock");
            }
        }).when(modelAdapter).getUnit(any(NumericExpression.class));

        measureAdapter = new MeasureAdapter<>(measureProvider, modelAdapter);
    }

    @AfterEach
    public void tearDown() {
        resource = null;
        measureAdapter = null;
    }

    @Test
    public void testBaseMeasuresChanged() {
        final Measure length = newBaseMeasureBuilder()
                .withNamespace("custom")
                .withName("Length")
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("metre")
                                .withRateDividend(BigDecimal.ONE)
                                .withRateDivisor(BigDecimal.ONE)
                                .withSymbol("m")
                                .build()
                )).build();

        // base measure is added
        resource.getContents().add(length);
        try {
            assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m").build()).get(),
                    is(Collections.singletonMap(measureIdFrom(length), 1)));

            // test cleanup
            resource.getContents().remove(length);
            assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m").build()).isPresent());
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @Test
    public void testDerivedMeasuresChanged() {
        final BaseMeasure length = newBaseMeasureBuilder()
                .withNamespace("custom")
                .withName("Length")
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("metre")
                                .withRateDividend(BigDecimal.ONE)
                                .withRateDivisor(BigDecimal.ONE)
                                .withSymbol("m")
                                .build()
                )).build();
        final DerivedMeasure area = newDerivedMeasureBuilder()
                .withNamespace("custom")
                .withName("Area")
                .withTerms(
                        newBaseMeasureTermBuilder()
                                .withBaseMeasure(length)
                                .withExponent(2)
                                .build())
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("square metre")
                                .withRateDividend(BigDecimal.ONE)
                                .withRateDivisor(BigDecimal.ONE)
                                .withSymbol("m²")
                                .build()
                )).build();
        final BaseMeasure time = newBaseMeasureBuilder()
                .withNamespace("custom")
                .withName("Time")
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("second")
                                .withRateDividend(BigDecimal.ONE)
                                .withRateDivisor(BigDecimal.ONE)
                                .withSymbol("s")
                                .build()
                )).build();
        final Measure velocity = newDerivedMeasureBuilder()
                .withNamespace("custom")
                .withName("Velocity")
                .withTerms(ImmutableList.of(newBaseMeasureTermBuilder()
                                .withBaseMeasure(length)
                                .withExponent(1)
                                .build(),
                        newBaseMeasureTermBuilder()
                                .withBaseMeasure(time)
                                .withExponent(-1)
                                .build()))
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("second")
                                .withRateDividend(BigDecimal.ONE)
                                .withRateDivisor(BigDecimal.ONE)
                                .withSymbol("m/s")
                                .build()
                )).build();

        resource.getContents().addAll(Arrays.asList(length, area, time));

        // base and derived measures are added as collections
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m").build()).get(),
                is(Collections.singletonMap(measureIdFrom(length), 1)));
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m²").build()).get(),
                is(Collections.singletonMap(measureIdFrom(length), 2)));
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("s").build()).get(),
                is(Collections.singletonMap(measureIdFrom(time), 1)));

        resource.getContents().add(velocity);

        // derived measure is added
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m/s").build()).get(),
                is(ImmutableMap.of(measureIdFrom(length), 1, measureIdFrom(time), -1)));

        final DerivedMeasure volume = newDerivedMeasureBuilder()
                .withNamespace("custom")
                .withName("Volume")
                .withUnits(ImmutableList.of(
                        newUnitBuilder()
                                .withName("cubic metre")
                                .withRateDividend(BigDecimal.ONE)
                                .withRateDivisor(BigDecimal.valueOf(1000L))
                                .withSymbol("dm³")
                                .build()
                )).build();
        resource.getContents().add(volume);

        // dimension of volume is not defined yet
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("dm³").build()).get(),
                is(Collections.emptyMap()));

        volume.getTerms().add(newBaseMeasureTermBuilder()
                .withBaseMeasure(length)
                .withExponent(3)
                .build());

        // dimension of volume is defined
        assertThat(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("dm³").build()).get(),
                is(Collections.singletonMap(measureIdFrom(length), 3)));

        resource.getContents().removeAll(Arrays.asList(length, area, volume, time));
        resource.getContents().remove(velocity);

        // test cleanup
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m²").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("s").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("m/s").build()).isPresent());
        assertFalse(measureAdapter.getDimension(newMeasuredDecimalBuilder().withValue(BigDecimal.ONE).withUnitName("dm³").build()).isPresent());
    }

    private MeasureAdapter.MeasureId measureIdFrom(final Measure measure) {
        return MeasureAdapter.MeasureId.fromMeasure(measureProvider, measure);
    }
}

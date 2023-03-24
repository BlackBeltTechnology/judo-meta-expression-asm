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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.eclipse.emf.common.notify.Notifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import hu.blackbelt.judo.meta.expression.ExecutionContextOnAsmTest;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureProvider;
import hu.blackbelt.judo.meta.measure.Measure;
import hu.blackbelt.judo.meta.measure.Unit;

public class AsmMeasureProviderTest extends ExecutionContextOnAsmTest {

    private MeasureProvider<Measure, Unit> measureProvider;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        measureProvider = new AsmMeasureProvider(measureModel.getResourceSet());
    }

    @AfterEach
    public void tearDown() {
        measureModel = null;
        measureProvider = null;
    }

    @Test
    public void testGetMeasureNamespace() {
        final Optional<Measure> length = getMeasureByName("Length");

        assertTrue(length.isPresent());
        assertThat(measureProvider.getMeasureNamespace(length.get()), is("demo::measures"));
    }

    @Test
    public void testGetMeasureName() {
        final Optional<Measure> length = measureProvider.getMeasures()
                .filter(m -> m.getUnits().stream().anyMatch(u -> "metre".equals(u.getName())))
                .findAny();

        assertTrue(length.isPresent());
        assertThat(measureProvider.getMeasureName(length.get()), is("Length"));
    }

    @Test
    public void testGetMeasure() {
        final Optional<Measure> length = measureProvider.getMeasure("demo::measures", "Length");

        final Optional<Measure> invalidName = measureProvider.getMeasure("demo::measures", "Price");
        final Optional<Measure> invalidNamespace = measureProvider.getMeasure("invalid::measures", "Length");
        final Optional<Measure> invalidNameAndNamespace = measureProvider.getMeasure("demo::measures", "Price");

        final Optional<Measure> expectedLength = getMeasureByName("Length");

        assertTrue(length.isPresent());
        assertThat(length, is(expectedLength));
        assertFalse(invalidName.isPresent());
        assertFalse(invalidNamespace.isPresent());
        assertFalse(invalidNameAndNamespace.isPresent());
    }

    @Test
    public void testBaseMeasuresOfBaseMeasure() {
        final Measure length = getMeasureByName("Length").get();

        assertThat(measureProvider.getBaseMeasures(length).map(), is(Collections.singletonMap(length, 1)));
    }

    @Test
    public void testBaseMeasuresOfDerivedMeasure() {
        final Measure length = getMeasureByName("Length").get();
        final Measure area = getMeasureByName("Area").get();
        final Measure mass = getMeasureByName("Mass").get();
        final Measure time = getMeasureByName("Time").get();
        final Measure force = getMeasureByName("Force").get();

        assertThat(measureProvider.getBaseMeasures(area).map(), is(Collections.singletonMap(length, 2)));
        assertThat(measureProvider.getBaseMeasures(force).map(), is(ImmutableMap.of(
                mass, 1,
                length, 1,
                time, -2
        )));
    }

    @Test
    public void testGetUnits() {
        final Measure length = getMeasureByName("Length").get();

        assertThat(new HashSet<>(measureProvider.getUnits(length)), is(new HashSet<>(length.getUnits())));
    }

    @Test
    public void testIsDurationSupportingAddition() {
        final Unit second = getUnitByName("millisecond").get();
        final Unit metre = getUnitByName("metre").get();
        final Unit microsecond = getUnitByName("microsecond").get();
        final Unit month = getUnitByName("month").get();

        assertTrue(measureProvider.isDurationSupportingAddition(second));
        assertFalse(measureProvider.isDurationSupportingAddition(metre));
        assertFalse(measureProvider.isDurationSupportingAddition(microsecond));
        assertFalse(measureProvider.isDurationSupportingAddition(month));
    }

    @Test
    public void testGetUnitByNameOrSymbol() {
        final Optional<Measure> length = getMeasureByName("Length");
        final Optional<Unit> metre = getUnitByName("metre");
        final Optional<Measure> time = getMeasureByName("Time");
        final Optional<Unit> halfDay = getUnitByName("halfDay");

        assertThat(measureProvider.getUnitByNameOrSymbol(length, "metre"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(Optional.empty(), "metre"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(length, "m"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(Optional.empty(), "m"), is(metre));
        assertThat(measureProvider.getUnitByNameOrSymbol(time, "halfDay"), is(halfDay));
        assertFalse(measureProvider.getUnitByNameOrSymbol(time, null).isPresent()); // units are not compared by symbol if is it not defined
        assertThat(measureProvider.getUnitByNameOrSymbol(Optional.empty(), "halfDay"), is(halfDay));
        assertFalse(measureProvider.getUnitByNameOrSymbol(Optional.empty(), null).isPresent()); // nothing is defined
    }

    @Test
    public void testGetMeasures() {
        assertThat(measureProvider.getMeasures().count(), is(7L));
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
                .filter(e -> Measure.class.isAssignableFrom(e.getClass())).map(e -> (Measure) e)
                .map(m -> m.getUnits().stream().filter(u -> unitName.equals(u.getName())).findAny())
                .filter(u -> u.isPresent()).map(u -> u.get())
                .findAny();
    }
}

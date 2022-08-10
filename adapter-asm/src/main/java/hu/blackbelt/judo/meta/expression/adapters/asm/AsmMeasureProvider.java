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

import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureChangedHandler;
import hu.blackbelt.judo.meta.expression.adapters.measure.MeasureProvider;
import hu.blackbelt.judo.meta.measure.BaseMeasure;
import hu.blackbelt.judo.meta.measure.DerivedMeasure;
import hu.blackbelt.judo.meta.measure.DurationType;
import hu.blackbelt.judo.meta.measure.DurationUnit;
import hu.blackbelt.judo.meta.measure.Measure;
import hu.blackbelt.judo.meta.measure.MeasurePackage;
import hu.blackbelt.judo.meta.measure.Unit;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Measure provider for measure metamodel that is used runtime (with ASM models).
 */
public class AsmMeasureProvider implements MeasureProvider<Measure, Unit> {

    private static final List<DurationType> DURATION_UNITS_SUPPORTING_ADDITION = Arrays.asList(DurationType.MILLISECOND, DurationType.SECOND, DurationType.MINUTE, DurationType.HOUR, DurationType.DAY, DurationType.WEEK);
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AsmMeasureProvider.class);

    private final ResourceSet resourceSet;

    public AsmMeasureProvider(final ResourceSet resourceSet) {
        this.resourceSet = resourceSet;
    }

    @Override
    public String getMeasureNamespace(final Measure measure) {
        return measure.getNamespace();
    }

    @Override
    public String getMeasureName(final Measure measure) {
        return measure.getName();
    }

    @Override
    public Optional<Measure> getMeasure(final String namespace, final String name) {
        return getMeasureElement(Measure.class)
                .filter(m -> Objects.equals(m.getNamespace(), namespace) && Objects.equals(m.getName(), name))
                .findAny();
    }

    @Override
    public EMap<Measure, Integer> getBaseMeasures(final Measure measure) {
        if (measure instanceof DerivedMeasure) {
            final DerivedMeasure derivedMeasure = (DerivedMeasure) measure;
            return ECollections.asEMap(derivedMeasure.getTerms().stream().collect(Collectors.toMap(t -> t.getBaseMeasure(), t -> t.getExponent())));
        } else {
            return ECollections.singletonEMap(measure, 1);
        }
    }

    @Override
    public EList<Unit> getUnits(final Measure measure) {
        return ECollections.asEList(measure.getUnits());
    }

    @Override
    public boolean isDurationSupportingAddition(final Unit unit) {
        if (unit instanceof DurationUnit) {
            return DURATION_UNITS_SUPPORTING_ADDITION.contains(((DurationUnit) unit).getType());
        } else {
            return false;
        }
    }

    @Override
    public Optional<Unit> getUnitByNameOrSymbol(final Optional<Measure> measure, final String nameOrSymbol) {
        if (measure.isPresent()) {
            return measure.map(m -> m.getUnits().stream()
                    .filter(u -> Objects.equals(u.getName(), nameOrSymbol) || u.getSymbol() != null && Objects.equals(u.getSymbol(), nameOrSymbol))
                    .findAny().orElse(null));
        } else {
            return getMeasureElement(Unit.class)
                    .filter(u -> Objects.equals(u.getName(), nameOrSymbol) || u.getSymbol() != null && Objects.equals(u.getSymbol(), nameOrSymbol))
                    .reduce((u1, u2) -> { throw new IllegalStateException("Ambiguous unit symbol, more than one measure contains " + nameOrSymbol); });
        }
    }

    @Override
    public Stream<Measure> getMeasures() {
        return getMeasureElement(Measure.class);
    }

    @Override
    public Stream<Unit> getUnits() {
        return getMeasureElement(Unit.class);
    }

    @Override
    public void setMeasureChangeHandler(final MeasureChangedHandler measureChangeHandler) {
        resourceSet.eAdapters().add(new EContentAdapter() {
            @Override
            public void notifyChanged(final Notification notification) {
                super.notifyChanged(notification);

                if (measureChangeHandler == null) {
                    return;
                }

                switch (notification.getEventType()) {
                    case Notification.ADD:
                    case Notification.ADD_MANY:
                        if (notification.getNewValue() instanceof BaseMeasure) {
                            measureChangeHandler.measureAdded(notification.getNewValue());
                        } else if (notification.getNewValue() instanceof DerivedMeasure) {
                            measureChangeHandler.measureAdded(notification.getNewValue());
                        } else if (notification.getNewValue() instanceof Collection) {
                            ((Collection) notification.getNewValue()).forEach(newValue -> {
                                if (newValue instanceof BaseMeasure) {
                                    measureChangeHandler.measureAdded(newValue);
                                } else if (newValue instanceof DerivedMeasure) {
                                    measureChangeHandler.measureAdded(newValue);
                                }
                            });
                        } else if (notification.getFeatureID(DerivedMeasure.class) == MeasurePackage.DERIVED_MEASURE__TERMS) {
                            measureChangeHandler.measureChanged(notification.getNotifier());
                        }
                        break;
                    case Notification.REMOVE:
                    case Notification.REMOVE_MANY:
                        if (notification.getOldValue() instanceof BaseMeasure) {
                            measureChangeHandler.measureRemoved(notification.getOldValue());
                        } else if (notification.getOldValue() instanceof DerivedMeasure) {
                            measureChangeHandler.measureRemoved(notification.getOldValue());
                        } else if (notification.getOldValue() instanceof Collection) {
                            ((Collection) notification.getOldValue()).forEach(oldValue -> {
                                if (oldValue instanceof BaseMeasure) {
                                    measureChangeHandler.measureRemoved(oldValue);
                                } else if (oldValue instanceof DerivedMeasure) {
                                    measureChangeHandler.measureRemoved(oldValue);
                                }
                            });
                        } else if (notification.getFeatureID(DerivedMeasure.class) == MeasurePackage.DERIVED_MEASURE__TERMS) {
                            measureChangeHandler.measureChanged(notification.getNotifier());
                        }
                        break;
                }
            }
        });
    }

    @Override
    public boolean isBaseMeasure(Measure measure) {
        return measure instanceof BaseMeasure;
    }

    <T> Stream<T> getMeasureElement(final Class<T> clazz) {
        final Iterable<Notifier> asmContents = resourceSet::getAllContents;
        return StreamSupport.stream(asmContents.spliterator(), true)
                .filter(e -> clazz.isAssignableFrom(e.getClass())).map(e -> (T) e);
    }
}

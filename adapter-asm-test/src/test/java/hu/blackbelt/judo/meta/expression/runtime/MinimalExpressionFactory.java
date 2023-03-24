package hu.blackbelt.judo.meta.expression.runtime;

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

import com.google.common.collect.ImmutableList;
import hu.blackbelt.judo.meta.expression.MeasureName;
import org.eclipse.emf.ecore.EObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newBooleanConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newDecimalConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newIntegerConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newLiteralBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newStringConstantBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newMeasureNameBuilder;

abstract class MinimalExpressionFactory {

    static List<EObject> createMinimalExpression() {
        final MeasureName measureName = newMeasureNameBuilder().withNamespace("demo::measures").withName("Mass").build();

        return ImmutableList.of(
                newIntegerConstantBuilder().withValue(BigInteger.valueOf(10)).build(),
                newDecimalConstantBuilder().withValue(BigDecimal.valueOf(3.14)).build(),
                newStringConstantBuilder().withValue("Sample text").build(),
                newBooleanConstantBuilder().withValue(true).build(),
                newLiteralBuilder().withValue("RED").build(),
                measureName,
                newMeasuredDecimalBuilder()
                        .withMeasure(measureName)
                        .withValue(BigDecimal.valueOf(10))
                        .withUnitName("kilogram")
                        .build());
    }
}

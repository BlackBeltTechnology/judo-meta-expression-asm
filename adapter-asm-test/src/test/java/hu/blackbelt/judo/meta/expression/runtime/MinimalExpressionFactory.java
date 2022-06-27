package hu.blackbelt.judo.meta.expression.runtime;

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

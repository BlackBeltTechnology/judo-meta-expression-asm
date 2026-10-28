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

import hu.blackbelt.judo.meta.expression.ExecutionContextOnAsmTest;
import hu.blackbelt.judo.meta.expression.adapters.asm.validation.AbstractExpressionAsmValidationTest;
import hu.blackbelt.judo.meta.expression.adapters.asm.validation.ValidatorType;
import hu.blackbelt.judo.meta.expression.operator.DecimalOperator;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newDecimalConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.newDecimalArithmeticExpressionBuilder;

@Slf4j
public class MeasuredTest extends AbstractExpressionAsmValidationTest {

    private final ExecutionContextOnAsmTest context = new ExecutionContextOnAsmTest();

    @BeforeEach
    public void setUp() throws Exception {
        context.setUp();
        setModelAdapter(context.asmModel, context.measureModel);

        final ExpressionModelResourceSupport expressionModelResourceSupport = ExpressionModelResourceSupport.expressionModelResourceSupportBuilder()
                .uri(URI.createURI("expr:test"))
                .build();

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

        expressionModel = ExpressionModel.buildExpressionModel()
                .name("expr")
                .expressionModelResourceSupport(expressionModelResourceSupport)
                .build();
    }

    @ParameterizedTest(name = "testAdditionOfMeasuredConstants [{0}]")
    @EnumSource(ValidatorType.class)
    void testAdditionOfMeasuredConstants(ValidatorType type) throws Exception {
        this.validatorType = type;
        // EVL returns full message, Java (Zeta) returns just constraint ID
        Collection<String> expectedErrors = type == ValidatorType.EVL
                ? Arrays.asList("MeasureOfAdditionIsValid|Measures of addition are not matching: (1[kg] + 10)")
                : Arrays.asList("MeasureOfAdditionIsValid");
        runValidation(
                expectedErrors,
                Collections.emptyList()
        );
    }
}

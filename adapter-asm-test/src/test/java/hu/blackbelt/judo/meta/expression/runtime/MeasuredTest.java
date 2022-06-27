package hu.blackbelt.judo.meta.expression.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.ExecutionContextOnAsmTest;
import hu.blackbelt.judo.meta.expression.operator.DecimalOperator;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static hu.blackbelt.judo.meta.expression.adapters.asm.ExpressionEpsilonValidatorOnAsm.validateExpressionOnAsm;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newDecimalConstantBuilder;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newMeasuredDecimalBuilder;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.newDecimalArithmeticExpressionBuilder;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;

@Slf4j
public class MeasuredTest extends ExecutionContextOnAsmTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        
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

    @Test
    void testAdditionOfMeasuredConstants() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            validateExpressionOnAsm(bufferedLog,
                                    asmModel, measureModel, expressionModel,
                                    calculateExpressionValidationScriptURI(),
                                    Arrays.asList("MeasureOfAdditionIsValid|Measures of addition are not matching: (1[kg] + 10)"),
                                    Collections.emptyList());
        }
    }
}

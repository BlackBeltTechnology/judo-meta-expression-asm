package hu.blackbelt.judo.meta.expression.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.ScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.*;
import hu.blackbelt.judo.meta.expression.constant.Instance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static hu.blackbelt.judo.meta.expression.adapters.asm.ExpressionEpsilonValidatorOnAsm.validateExpressionOnAsm;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newInstanceBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectNavigationExpressionBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectVariableReferenceBuilder;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;
import static hu.blackbelt.judo.meta.expression.string.util.builder.StringBuilders.newStringAttributeBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class IllegalAsmTest extends ExecutionContextOnAsmTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        
        expressionModel = ExpressionModelForTest.createExpressionModel();
        
        final TypeName orderType = newTypeNameBuilder().withNamespace("demo::entities").withName("InternationalOrder").build();
        final Instance orderVar = newInstanceBuilder()
                .withElementName(orderType)
                .withName("self")
                .build();

        final StringExpression shipperName = newStringAttributeBuilder()
                .withObjectExpression(newObjectNavigationExpressionBuilder()
                        .withObjectExpression(newObjectVariableReferenceBuilder()
                                .withVariable(orderVar)
                                .build())
                        .withReferenceName("shippers")
                        .build())
                .withAttributeName("companyName")
                .build();
        
        expressionModel.addContent(orderType);
        expressionModel.addContent(orderVar);
        expressionModel.addContent(shipperName);
        
        log.info(expressionModel.getDiagnosticsAsString());
    	assertTrue(expressionModel.isValid());
    }
   
    @Test
    void test() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            assertThrows(
                    ScriptExecutionException.class,
                    () -> validateExpressionOnAsm(bufferedLog, asmModel, measureModel, expressionModel, calculateExpressionValidationScriptURI())
            );
        }
    }
}

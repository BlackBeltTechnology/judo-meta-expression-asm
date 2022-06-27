package hu.blackbelt.judo.meta.expression.runtime;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.ExecutionContextOnAsmTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static hu.blackbelt.judo.meta.expression.adapters.asm.ExpressionEpsilonValidatorOnAsm.validateExpressionOnAsm;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FullAsmTest extends ExecutionContextOnAsmTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        expressionModel = ExpressionModelForTest.createExpressionModel();
        log.info(expressionModel.getDiagnosticsAsString());
    	assertTrue(expressionModel.isValid());
    }

    @Test
    void test() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            validateExpressionOnAsm(bufferedLog, asmModel, measureModel, expressionModel, calculateExpressionValidationScriptURI());
        }
    }
}

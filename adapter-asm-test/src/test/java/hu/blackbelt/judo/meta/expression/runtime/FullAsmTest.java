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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FullAsmTest extends AbstractExpressionAsmValidationTest {

    private final ExecutionContextOnAsmTest context = new ExecutionContextOnAsmTest();

    @BeforeEach
    public void setUp() throws Exception {
        context.setUp();
        setModelAdapter(context.asmModel, context.measureModel);
        expressionModel = ExpressionModelForTest.createExpressionModel();
        log.info(expressionModel.getDiagnosticsAsString());
        assertTrue(expressionModel.isValid());
    }

    @ParameterizedTest(name = "testValidExpression [{0}]")
    @EnumSource(ValidatorType.class)
    void test(ValidatorType type) throws Exception {
        this.validatorType = type;
        runValidation();
    }
}

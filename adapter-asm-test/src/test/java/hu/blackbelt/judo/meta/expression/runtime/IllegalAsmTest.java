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

import hu.blackbelt.judo.meta.expression.*;
import hu.blackbelt.judo.meta.expression.adapters.asm.validation.AbstractExpressionAsmValidationTest;
import hu.blackbelt.judo.meta.expression.adapters.asm.validation.ValidatorType;
import hu.blackbelt.judo.meta.expression.constant.Instance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.newInstanceBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectNavigationExpressionBuilder;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectVariableReferenceBuilder;
import static hu.blackbelt.judo.meta.expression.string.util.builder.StringBuilders.newStringAttributeBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class IllegalAsmTest extends AbstractExpressionAsmValidationTest {

    private final ExecutionContextOnAsmTest context = new ExecutionContextOnAsmTest();

    @BeforeEach
    public void setUp() throws Exception {
        context.setUp();
        setModelAdapter(context.asmModel, context.measureModel);

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

    @ParameterizedTest(name = "testIllegalExpression [{0}]")
    @EnumSource(ValidatorType.class)
    void test(ValidatorType type) throws Exception {
        this.validatorType = type;
        try {
            runValidation();
            // If we get here without exception, the test should fail
            // (unless it was skipped via assumption inside runValidation)
            throw new AssertionError("Expected ExpressionValidationException to be thrown");
        } catch (ExpressionValidationException e) {
            // This is the expected outcome for illegal expressions
            log.info("Got expected validation exception: {}", e.getMessage());
        } catch (org.opentest4j.TestAbortedException e) {
            // Test was skipped due to assumptions (e.g., adapter incompatibility)
            // Just rethrow to properly skip the test
            throw e;
        }
    }
}

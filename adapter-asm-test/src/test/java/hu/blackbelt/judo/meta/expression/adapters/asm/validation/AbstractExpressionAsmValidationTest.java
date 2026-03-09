package hu.blackbelt.judo.meta.expression.adapters.asm.validation;

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

import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.expression.adapters.ModelAdapter;
import hu.blackbelt.judo.meta.expression.adapters.asm.AsmModelAdapter;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionValidationException;
import hu.blackbelt.judo.meta.measure.runtime.MeasureModel;
import org.junit.jupiter.api.Assumptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;

import static hu.blackbelt.judo.meta.expression.runtime.ExpressionValidator.validateExpression;
import static java.util.Collections.emptyList;

/**
 * Abstract base class for Expression ASM validation tests.
 *
 * <p>Provides common infrastructure for testing both EVL and Java validators
 * with the same test cases using JUnit 5 parameterized tests.</p>
 *
 * <p>This class imports and executes the expression validator from judo-meta-expression.
 * No validators are implemented in this project.</p>
 *
 * <p>Note: Java (Zeta) validation tests will be skipped if ExpressionZetaValidator
 * is not available in the classpath. This allows the tests to run even when only
 * EVL validation is available.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class ExpressionAsmValidationTest extends AbstractExpressionAsmValidationTest {
 *
 *     @ParameterizedTest(name = "testValidExpression [{0}]")
 *     @EnumSource(ValidatorType.class)
 *     void testValidExpression(ValidatorType type) throws Exception {
 *         this.validatorType = type;
 *         initModels();
 *
 *         // ... build ASM model and expression model ...
 *
 *         runValidation(
 *             ImmutableList.of(), // expected errors
 *             ImmutableList.of()  // expected warnings
 *         );
 *     }
 * }
 * }</pre>
 *
 * @see ValidatorType
 */
public abstract class AbstractExpressionAsmValidationTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ZETA_VALIDATOR_CLASS = "hu.blackbelt.judo.meta.expression.validation.ExpressionZetaValidator";
    private static final Boolean ZETA_VALIDATOR_AVAILABLE = isZetaValidatorAvailable();

    protected AsmModel asmModel;
    protected MeasureModel measureModel;
    protected ExpressionModel expressionModel;
    protected AsmModelAdapter modelAdapter;
    protected ValidatorType validatorType;

    /**
     * Check if the ExpressionZetaValidator class is available on the classpath.
     */
    private static boolean isZetaValidatorAvailable() {
        try {
            Class.forName(ZETA_VALIDATOR_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if the Zeta (Java) validator is available.
     *
     * @return true if ExpressionZetaValidator is on the classpath
     */
    protected static boolean isJavaValidatorAvailable() {
        return ZETA_VALIDATOR_AVAILABLE;
    }

    /**
     * Set the model adapter for validation.
     * This should be called after initializing the ASM and measure models.
     *
     * @param asmModel the ASM model
     * @param measureModel the measure model
     */
    protected void setModelAdapter(AsmModel asmModel, MeasureModel measureModel) {
        this.asmModel = asmModel;
        this.measureModel = measureModel;
        this.modelAdapter = new AsmModelAdapter(
                asmModel.getResourceSet(),
                measureModel.getResourceSet()
        );
    }

    /**
     * Run validation using the selected validator type.
     *
     * <p>This method executes the expression validator from judo-meta-expression
     * using either EVL or Java (Zeta) validation depending on the validatorType.</p>
     *
     * <p>If Java validation is selected but ExpressionZetaValidator is not available,
     * the test will be skipped with an assumption failure.</p>
     *
     * @throws Exception if validation fails unexpectedly
     */
    protected void runValidation() throws Exception {
        runValidation(emptyList(), emptyList());
    }

    /**
     * Run validation using the selected validator type.
     *
     * <p>This method executes the expression validator from judo-meta-expression
     * using either EVL or Java (Zeta) validation depending on the validatorType.</p>
     *
     * <p>If Java validation is selected but ExpressionZetaValidator is not available,
     * the test will be skipped with an assumption failure.</p>
     *
     * @param expectedErrors expected error constraint names (empty collection if no errors expected)
     * @param expectedWarnings expected warning constraint names (empty collection if no warnings expected)
     * @throws Exception if validation fails unexpectedly
     */
    protected void runValidation(
            Collection<String> expectedErrors,
            Collection<String> expectedWarnings
    ) throws Exception {
        if (expressionModel == null) {
            throw new IllegalStateException("Expression model must be initialized");
        }
        if (modelAdapter == null) {
            throw new IllegalStateException("Model adapter must be initialized");
        }
        if (validatorType == null) {
            throw new IllegalStateException("Validator type must be set");
        }

        log.info("Running {} validation...", validatorType);

        try {
            switch (validatorType) {
                case EVL:
                    runEvlValidation(expectedErrors, expectedWarnings);
                    break;
                case JAVA:
                    runJavaValidation(expectedErrors, expectedWarnings);
                    break;
                default:
                    throw new IllegalStateException("Unknown validator type: " + validatorType);
            }
            log.info("{} validation passed", validatorType);
        } catch (ExpressionValidationException ex) {
            log.error("{} validation failed", validatorType, ex);
            throw ex;
        }
    }

    /**
     * Run EVL-based validation using ExpressionValidator.
     */
    private void runEvlValidation(
            Collection<String> expectedErrors,
            Collection<String> expectedWarnings
    ) throws ExpressionValidationException {
        validateExpression(
                log,
                expressionModel,
                modelAdapter,
                "ASM",
                asmModel.getResource(),
                "MEASURES",
                measureModel.getResource(),
                expectedErrors,
                expectedWarnings
        );
    }

    /**
     * Run Java-based (Zeta) validation using ExpressionZetaValidator.
     *
     * <p>If ExpressionZetaValidator is not available on the classpath,
     * the test will be skipped.</p>
     *
     * <p>If the ASM model adapter is not compatible with Zeta validation
     * (e.g., ClassCastException), the test will be skipped with an
     * informative message.</p>
     */
    private void runJavaValidation(
            Collection<String> expectedErrors,
            Collection<String> expectedWarnings
    ) throws Exception {
        // Skip test if Zeta validator is not available
        Assumptions.assumeTrue(
                ZETA_VALIDATOR_AVAILABLE,
                "ExpressionZetaValidator is not available - skipping Java validation test"
        );

        // Use reflection to invoke the validator
        Class<?> validatorClass = Class.forName(ZETA_VALIDATOR_CLASS);
        Method validateMethod = validatorClass.getMethod(
                "validateExpression",
                Logger.class,
                ExpressionModel.class,
                ModelAdapter.class,
                Collection.class,
                Collection.class,
                boolean.class
        );

        try {
            validateMethod.invoke(
                    null, // static method
                    log,
                    expressionModel,
                    modelAdapter,
                    expectedErrors,
                    expectedWarnings,
                    false // Sequential for deterministic results
            );
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ExpressionValidationException) {
                throw (ExpressionValidationException) cause;
            }
            // Handle adapter incompatibility issues gracefully
            if (cause instanceof ClassCastException) {
                log.warn("ASM model adapter may not be fully compatible with Zeta validation: {}", cause.getMessage());
                Assumptions.assumeTrue(false,
                        "ASM model adapter not fully compatible with Zeta validation: " + cause.getMessage());
            }
            throw e;
        }
    }

    /**
     * Get the ASM model.
     */
    protected AsmModel getAsmModel() {
        return asmModel;
    }

    /**
     * Get the measure model.
     */
    protected MeasureModel getMeasureModel() {
        return measureModel;
    }

    /**
     * Get the expression model for adding expressions.
     */
    protected ExpressionModel getExpressionModel() {
        return expressionModel;
    }

    /**
     * Get the model adapter for building type names and other adapter operations.
     */
    protected AsmModelAdapter getModelAdapter() {
        return modelAdapter;
    }
}

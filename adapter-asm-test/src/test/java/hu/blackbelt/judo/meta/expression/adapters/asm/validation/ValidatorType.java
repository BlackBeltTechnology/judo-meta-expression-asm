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

/**
 * Enum selector for choosing which validation engine to use in tests.
 *
 * <p>This enum is used with JUnit 5 parameterized tests to run the same
 * test cases against both EVL and Java validators, ensuring parity between
 * the two validation implementations.</p>
 *
 * @see AbstractExpressionAsmValidationTest
 */
public enum ValidatorType {
    /**
     * Use EVL (Epsilon Validation Language) based validation.
     * Executed via {@code ExpressionValidator.validateExpression()}.
     */
    EVL,

    /**
     * Use native Java-based validation (Zeta framework).
     * Executed via {@code ExpressionZetaValidator.validateExpression()}.
     */
    JAVA
}

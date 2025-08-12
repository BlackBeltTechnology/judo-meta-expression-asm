package hu.blackbelt.judo.meta.expression.adapters.asm;

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

import hu.blackbelt.judo.meta.expression.runtime.ExpressionValidationException;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionValidator;
import org.slf4j.Logger;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.measure.runtime.MeasureModel;

import java.util.Collection;

import static java.util.Collections.emptyList;

public class ExpressionValidatorOnAsm {

    public static void validateExpressionOnAsm(Logger log, AsmModel asmModel, MeasureModel measureModel, ExpressionModel expressionModel)
            throws ExpressionValidationException {
        validateExpressionOnAsm(log, asmModel, measureModel, expressionModel, emptyList(), emptyList());
    }

    public static void validateExpressionOnAsm(Logger log, AsmModel asmModel, MeasureModel measureModel, ExpressionModel expressionModel,
                                               Collection<String> expectedErrors, Collection<String> expectedWarnings)
            throws ExpressionValidationException {
        ExpressionValidator.validateExpression(log, expressionModel,
                new AsmModelAdapter(asmModel.getResourceSet(), measureModel.getResourceSet()),
                "ASM", asmModel.getResource(), "MEASURES", measureModel.getResource(),
                expectedErrors, expectedWarnings);
    }

}

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
                new AsmModelAdapter(asmModel.getResourceSet(), measureModel.getResourceSet()), expectedErrors, expectedWarnings);

        /*
            context EXPR!TypeName {

                // object type with name defined by ElementName must exists in the namespace
                constraint ObjectTypeIsValid {
                    check: self.get(modelAdapter).isDefined()
                    message: "Element named " + self.name + " not found in namespace " + self.namespace
                }
            }

            context EXPR!Expression {

                // variable reference in lambda expression is referencing to variable visible from its scope
                constraint LambdaVariableIsValid {
                    guard: evaluator.isLambdaFunction(self)

                    check: evaluator.getVariablesOfScope(self).containsAll(evaluator.getExpressionTerms(self).select(e | e.isKindOf(EXPR!VariableReference)).collect(e | e.variable))
                    message: "Invalid variable references: " + evaluator.getExpressionTerms(self).select(e | e.isKindOf(EXPR!VariableReference)).collect(e | e.variable).excludingAll(evaluator.getVariablesOfScope(self)) + " in expression: " + self
                }
            }
         */
        /*
        final Map<String, Object> injections = new HashMap<>();
        injections.put("evaluator", new ExpressionEvaluator());
        injections.put("modelAdapter", new AsmModelAdapter(asmModel.getResourceSet(), measureModel.getResourceSet()));

        ExecutionContext executionContext = executionContextBuilder()
                .log(log)
                .resourceSet(asmModel.getResourceSet())
                .metaModels(emptyList())
                .modelContexts(Arrays.asList(
                        wrappedEmfModelContextBuilder()
                                .log(log)
                                .name("ASM")
                                .resource(asmModel.getResource())
                                .validateModel(false)
                                .useCache(useCache)
                                .build(),
                        wrappedEmfModelContextBuilder()
                                .log(log)
                                .name("MEASURES")
                                .resource(measureModel.getResource())
                                .validateModel(false)
                                .useCache(useCache)
                                .build(),
                        wrappedEmfModelContextBuilder()
                                .log(log)
                                .name("EXPR")
                                .resource(expressionModel.getResource())
                                .validateModel(false)
                                .useCache(useCache)
                                .build()))
                .injectContexts(injections)
                .build();

        try {
            // run the model / metadata loading
            executionContext.load();

            // Transformation script
            executionContext
                    .executeProgram(evlExecutionContextBuilder().source(UriUtil.resolve("expression.evl", scriptRoot))
                            .parallel(true)
                            .expectedErrors(expectedErrors).expectedWarnings(expectedWarnings).build());

        } finally {
            executionContext.commit();
            try {
                executionContext.close();
            } catch (Exception e) {
            }
        }
        */
    }
}

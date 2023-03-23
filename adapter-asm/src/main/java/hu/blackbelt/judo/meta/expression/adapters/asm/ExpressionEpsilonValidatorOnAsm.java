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

import hu.blackbelt.epsilon.runtime.execution.ExecutionContext;
import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.exceptions.ScriptExecutionException;
import hu.blackbelt.judo.meta.asm.runtime.AsmModel;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionEvaluator;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.measure.runtime.MeasureModel;
import org.eclipse.epsilon.common.util.UriUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static hu.blackbelt.epsilon.runtime.execution.ExecutionContext.executionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.contexts.EvlExecutionContext.evlExecutionContextBuilder;
import static hu.blackbelt.epsilon.runtime.execution.model.emf.WrappedEmfModelContext.wrappedEmfModelContextBuilder;
import static java.util.Collections.emptyList;

public class ExpressionEpsilonValidatorOnAsm extends ExpressionEpsilonValidator {

    public static void validateExpressionOnAsm(Log log, AsmModel asmModel, MeasureModel measureModel, ExpressionModel expressionModel, URI scriptRoot)
            throws ScriptExecutionException, URISyntaxException {
        validateExpressionOnAsm(log, asmModel, measureModel, expressionModel, scriptRoot, emptyList(), emptyList());
    }

    public static void validateExpressionOnAsm(Log log, AsmModel asmModel, MeasureModel measureModel, ExpressionModel expressionModel, URI scriptRoot,
                                   Collection<String> expectedErrors, Collection<String> expectedWarnings)
            throws ScriptExecutionException, URISyntaxException {

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
                                .build(),
                        wrappedEmfModelContextBuilder()
                                .log(log)
                                .name("MEASURES")
                                .resource(measureModel.getResource())
                                .validateModel(false)
                                .build(),
                        wrappedEmfModelContextBuilder()
                                .log(log)
                                .name("EXPR")
                                .resource(expressionModel.getResource())
                                .validateModel(false)
                                .build()))
                .injectContexts(injections)
                .build();

        try {
            // run the model / metadata loading
            executionContext.load();

            // Transformation script
            executionContext
                    .executeProgram(evlExecutionContextBuilder().source(UriUtil.resolve("expression.evl", scriptRoot))
                            .expectedErrors(expectedErrors).expectedWarnings(expectedWarnings).build());

        } finally {
            executionContext.commit();
            try {
                executionContext.close();
            } catch (Exception e) {
            }
        }
    }
}

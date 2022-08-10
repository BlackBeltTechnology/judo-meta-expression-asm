package hu.blackbelt.judo.meta.expression.builder.jql.asm;

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

import hu.blackbelt.judo.meta.expression.adapters.asm.AsmModelAdapter;
import hu.blackbelt.judo.meta.expression.builder.jql.AdaptableJqlExtractor;
import hu.blackbelt.judo.meta.expression.builder.jql.JqlExpressionBuilderConfig;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import hu.blackbelt.judo.meta.measure.support.MeasureModelResourceSupport;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

public class AsmJqlExtractor extends AdaptableJqlExtractor {

    public AsmJqlExtractor(final ResourceSet asmResourceSet, final ResourceSet measureResourceSet, final ResourceSet expressionResourceSet, JqlExpressionBuilderConfig builderConfig) {
        super(asmResourceSet, measureResourceSet, expressionResourceSet, new AsmModelAdapter(asmResourceSet, measureResourceSet != null ? measureResourceSet : MeasureModelResourceSupport.createMeasureResourceSet()), builderConfig);
    }

    public AsmJqlExtractor(final ResourceSet asmResourceSet, final ResourceSet measureResourceSet, final ResourceSet expressionResourceSet) {
        this(asmResourceSet, measureResourceSet, expressionResourceSet, new JqlExpressionBuilderConfig());
    }

    public AsmJqlExtractor(final ResourceSet asmResourceSet, final ResourceSet measureResourceSet, final URI uri) {
        this(asmResourceSet, measureResourceSet, uri, new JqlExpressionBuilderConfig());
    }
    
    public AsmJqlExtractor(final ResourceSet asmResourceSet, final ResourceSet measureResourceSet, final URI uri, JqlExpressionBuilderConfig builderConfig) {
        this(asmResourceSet, measureResourceSet, ExpressionModelResourceSupport.expressionModelResourceSupportBuilder()
                .uri(uri)
                .build().getResourceSet(), builderConfig);
    }

}

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

import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.judo.meta.expression.builder.jql.JqlExtractor;
import hu.blackbelt.judo.meta.expression.collection.CollectionNavigationFromCollectionExpression;
import hu.blackbelt.judo.meta.expression.collection.CollectionNavigationFromObjectExpression;
import hu.blackbelt.judo.meta.expression.runtime.*;
import hu.blackbelt.judo.meta.expression.string.StringAttribute;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.*;

import static hu.blackbelt.judo.meta.expression.adapters.asm.ExpressionValidatorOnAsm.validateExpressionOnAsm;
import static hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport.SaveArguments.expressionSaveArgumentsBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class AsmJqlExtractorTest extends ExecutionContextOnAsmTest {

    private static final String TARGET_TEST_CLASSES = "target/test-classes";

    private JqlExtractor jqlExtractor;
    private ResourceSet expressionResourceSet;
    ExpressionModelResourceSupport expressionModelResourceSupport;
    ExpressionModel expressionModel;
    ExpressionUtils expressionUtils;

    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        jqlExtractor = new AsmJqlExtractor(asmModel.getResourceSet(), measureModel.getResourceSet(), URI.createURI("urn:test.judo-meta-expression"));
    }

    @AfterEach
    void tearDown(final TestInfo testInfo) throws Exception {
        expressionModelResourceSupport = ExpressionModelResourceSupport.expressionModelResourceSupportBuilder()
                .resourceSet(expressionResourceSet)
                .build();

        expressionModelResourceSupport.saveExpression(expressionSaveArgumentsBuilder()
                .file(new File(TARGET_TEST_CLASSES, testInfo.getDisplayName().replace("(", "").replace(")", "") + "-expression.model"))
                .build());

        jqlExtractor = null;
    }

    @Test
    void testExtract() throws Exception {
        final long startTs = System.currentTimeMillis();
        expressionResourceSet = jqlExtractor.extractExpressions();
        final long endTs = System.currentTimeMillis();

        log.info("Extracted expressions in {} ms", (endTs - startTs));

        expressionModelResourceSupport = ExpressionModelResourceSupport.expressionModelResourceSupportBuilder()
                .resourceSet(expressionResourceSet)
                .build();

        expressionModel = ExpressionModel.buildExpressionModel()
                .expressionModelResourceSupport(expressionModelResourceSupport)
                .name(asmModel.getName())
                .build();

          log.info(expressionModel.getDiagnosticsAsString());
        assertTrue(expressionModel.isValid());
        try (BufferedSlf4jLogger bufferedLog = new BufferedSlf4jLogger(log)) {
            validateExpressionOnAsm(bufferedLog, asmModel, measureModel, expressionModel);
        }

        expressionUtils = new ExpressionUtils(expressionResourceSet);

        Optional<EClassifier> shipper = asmUtils.resolve("demo.entities.Shipper");
        Optional<StringAttribute> shipperNameGetter = expressionUtils.all()
                .filter(e -> e instanceof StringAttribute).map(e -> (StringAttribute)e)
                .filter(e -> e.getAttributeName().equals("companyName"))
                .findAny();

        assertTrue(shipper.isPresent());
        assertTrue(shipperNameGetter.isPresent());
        assertTrue(shipperNameGetter.get().getObjectExpression().getObjectType(modelAdapter).equals(shipper.get()));

        Optional<EClassifier> employee = asmUtils.resolve("demo.entities.Employee");
        Optional<CollectionNavigationFromCollectionExpression> ordersAssignedToEmployeesGetter = expressionUtils.all()
                .filter(e -> e instanceof CollectionNavigationFromCollectionExpression)
                .map(e -> (CollectionNavigationFromCollectionExpression)e)
                .filter(e -> e.getCollectionExpression().toString().equals("demo::entities::Employee"))
                .findAny();

        assertTrue(employee.isPresent());
        assertTrue(ordersAssignedToEmployeesGetter.isPresent());
        assertTrue(ordersAssignedToEmployeesGetter.get().getCollectionExpression().getObjectType(modelAdapter).equals(employee.get()));

        Optional<EClassifier> product = asmUtils.resolve("demo.entities.Product");
        Optional<EClassifier> category = asmUtils.resolve("demo.entities.Category");

        Optional<CollectionNavigationFromObjectExpression> categoriesGetter = expressionUtils.all()
                .filter(e -> e instanceof CollectionNavigationFromObjectExpression)
                .map(e -> (CollectionNavigationFromObjectExpression)e)
                .filter(e -> e.getObjectType(modelAdapter).equals(category.get()))
                .findAny();

        assertTrue(product.isPresent());
        assertTrue(category.isPresent());
        assertTrue(categoriesGetter.isPresent());
        assertTrue(categoriesGetter.get().getObjectExpression().getObjectType(modelAdapter).equals(employee.get()));

        Optional<StringAttribute> productNameGetter = expressionUtils.all()
                .filter(e -> e instanceof StringAttribute).map(e -> (StringAttribute)e)
                .filter(e -> e.getAttributeName().equals("productName"))
                .findAny();

        assertTrue(productNameGetter.isPresent());
        assertTrue(productNameGetter.get().getObjectExpression().getObjectType(modelAdapter).equals(product.get()));

        // test extracting expressions that are already extracted
        final long startTs2 = System.currentTimeMillis();
        expressionResourceSet = jqlExtractor.extractExpressions();
        final long endTs2 = System.currentTimeMillis();

        log.info("Extracted expressions again in {} ms", (endTs2 - startTs2));
    }

    /**
     * Iterator variable names (_iterator_N) must not depend on the order in which the ASM elements are visited
     * (spec expression-iterator-determinism): ETL and Zeta psm2asm emit the same elements in different orders.
     */
    @Test
    void testIteratorNamesDoNotDependOnExtractionOrder() {
        // A second lambda in another entity (Category already has self.products!head(p | ...)): with a shared
        // iterator counter its name would depend on whether Order or Category is extracted first.
        EClass order = (EClass) asmUtils.resolve("demo.entities.Order").orElseThrow();
        AsmUtils.getExtensionAnnotationByName(order.getEStructuralFeature("categories"), "expression", false).orElseThrow()
                .getDetails().put("getter", "self.orderDetails!filter(d | d.product.productName == 'x').product.category");
        Map<String, List<String>> forward = iteratorNamesByBinding(jqlExtractor.extractExpressions());
        assertTrue(forward.values().stream().filter(names -> !names.isEmpty()).count() > 1,
                "fixture must contain more than one expression with an iterator");

        // Same ASM with every classifier list reversed, in its own ResourceSet (AsmUtils caches element order per ResourceSet).
        ResourceSet reversedAsm = new ResourceSetImpl();
        reversedAsm.setResourceFactoryRegistry(asmModel.getResourceSet().getResourceFactoryRegistry());
        asmModel.getResourceSet().getResources().forEach(r ->
                reversedAsm.createResource(r.getURI()).getContents().addAll(EcoreUtil.copyAll(r.getContents())));
        List<EPackage> packages = new ArrayList<>();
        reversedAsm.getAllContents().forEachRemaining(o -> { if (o instanceof EPackage) packages.add((EPackage) o); });
        packages.forEach(p -> ECollections.reverse(p.getEClassifiers()));

        expressionResourceSet = new AsmJqlExtractor(reversedAsm, measureModel.getResourceSet(),
                URI.createURI("urn:test.reversed.judo-meta-expression")).extractExpressions();
        assertEquals(forward, iteratorNamesByBinding(expressionResourceSet));
    }

    /** Binding (type, feature, role) -> sorted generated iterator names of its expression. */
    private static Map<String, List<String>> iteratorNamesByBinding(ResourceSet expressions) {
        Map<String, List<String>> result = new TreeMap<>();
        expressions.getAllContents().forEachRemaining(o -> {
            if (!(o instanceof EObject) || !((EObject) o).eClass().getEAllSuperTypes().stream().anyMatch(c -> c.getName().equals("Binding"))) {
                return;
            }
            EObject binding = (EObject) o;
            EObject expression = (EObject) get(binding, "expression");
            List<String> names = new ArrayList<>();
            if (expression != null) {
                List<EObject> nodes = new ArrayList<>(List.of(expression));
                expression.eAllContents().forEachRemaining(nodes::add);
                nodes.forEach(c -> {
                    // iterator variables live in the resource; the expression references them via iteratorVariable
                    Object name = get((EObject) get(c, "iteratorVariable"), "name");
                    if (name instanceof String && ((String) name).startsWith("_iterator_")) {
                        names.add((String) name);
                    }
                });
            }
            Collections.sort(names);
            EObject typeName = (EObject) get(binding, "typeName");
            String key = binding.eClass().getName() + ":" + get(typeName, "namespace") + "::" + get(typeName, "name") + ":"
                    + Optional.ofNullable(get(binding, "attributeName")).orElse(get(binding, "referenceName")) + ":" + get(binding, "role");
            result.merge(key, names, (a, b) -> { List<String> m = new ArrayList<>(a); m.addAll(b); Collections.sort(m); return m; });
        });
        return result;
    }

    private static Object get(EObject o, String feature) {
        if (o == null) return null;
        EStructuralFeature f = o.eClass().getEStructuralFeature(feature);
        return f == null ? null : o.eGet(f);
    }
}

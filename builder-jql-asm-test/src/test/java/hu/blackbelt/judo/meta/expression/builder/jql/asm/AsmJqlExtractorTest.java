package hu.blackbelt.judo.meta.expression.builder.jql.asm;

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.expression.builder.jql.JqlExtractor;
import hu.blackbelt.judo.meta.expression.collection.CollectionNavigationFromCollectionExpression;
import hu.blackbelt.judo.meta.expression.collection.CollectionNavigationFromObjectExpression;
import hu.blackbelt.judo.meta.expression.runtime.*;
import hu.blackbelt.judo.meta.expression.string.StringAttribute;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Optional;

import static hu.blackbelt.judo.meta.expression.adapters.asm.ExpressionEpsilonValidatorOnAsm.validateExpressionOnAsm;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;
import static hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport.SaveArguments.expressionSaveArgumentsBuilder;
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
		try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
			validateExpressionOnAsm(bufferedLog, asmModel, measureModel, expressionModel, calculateExpressionValidationScriptURI());
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
}

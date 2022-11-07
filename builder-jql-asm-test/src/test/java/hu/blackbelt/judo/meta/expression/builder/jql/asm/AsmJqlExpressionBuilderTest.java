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

import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.judo.meta.asm.runtime.AsmUtils;
import hu.blackbelt.judo.meta.expression.*;
import hu.blackbelt.judo.meta.expression.builder.jql.*;
import hu.blackbelt.judo.meta.expression.collection.*;
import hu.blackbelt.judo.meta.expression.constant.DateConstant;
import hu.blackbelt.judo.meta.expression.constant.MeasuredDecimal;
import hu.blackbelt.judo.meta.expression.numeric.DecimalSwitchExpression;
import hu.blackbelt.judo.meta.expression.object.*;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import hu.blackbelt.judo.meta.expression.temporal.TimeDifferenceExpression;
import hu.blackbelt.judo.meta.expression.temporal.TimestampDifferenceExpression;
import hu.blackbelt.judo.meta.measure.Measure;
import hu.blackbelt.judo.meta.measure.Unit;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.hamcrest.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static hu.blackbelt.judo.meta.expression.adapters.asm.ExpressionEpsilonValidatorOnAsm.validateExpressionOnAsm;
import static hu.blackbelt.judo.meta.expression.builder.jql.JqlExpressionBuilder.BindingType.ATTRIBUTE;
import static hu.blackbelt.judo.meta.expression.builder.jql.JqlExpressionBuilder.BindingType.RELATION;
import static hu.blackbelt.judo.meta.expression.builder.jql.expression.JqlNavigationFeatureTransformer.INVALID_ATTRIBUTE_SELECTOR;
import static hu.blackbelt.judo.meta.expression.runtime.ExpressionEpsilonValidator.calculateExpressionValidationScriptURI;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class AsmJqlExpressionBuilderTest extends ExecutionContextOnAsmTest {

    private JqlExpressionBuilder<EClassifier, EDataType, EEnum, EClass, EAttribute, EReference, EClass, EAttribute, EReference, EClassifier, Measure, Unit> expressionBuilder;

    private ExpressionModelResourceSupport expressionModelResourceSupport;

    @Override
    protected void populateAsmModel() {
        super.populateAsmModel();
        populateSchoolModel();
    }

    private void populateSchoolModel() {
        EEnum genderEnum = newEEnumBuilder()
                .withName("Gender").withELiterals(
                        newEEnumLiteralBuilder().withLiteral("MALE").withName("MALE").withValue(0).build(),
                        newEEnumLiteralBuilder().withLiteral("FEMALE").withName("FEMALE").withValue(1).build())
                .build();

        EAttribute height = newEAttributeBuilder().withName("height").withEType(doubleType).build();
        EAttribute gender = newEAttributeBuilder().withName("gender").withEType(genderEnum).build();
        EClass person = newEClassBuilder().withName("Person").withEStructuralFeatures(height, gender).build();
        EReference parents = newEReferenceBuilder().withName("parents").withLowerBound(1).withUpperBound(-1).withEType(person).build();
        EReference mother = newEReferenceBuilder().withName("mother").withDerived(true).withLowerBound(1).withUpperBound(1).withEType(person).build();
        EClass student = newEClassBuilder().withName("Student")
                .withESuperTypes(person)
                .withEStructuralFeatures(parents, mother)
                .build();
        EClass parent = newEClassBuilder().withName("Parent").withESuperTypes(person).build();
        EReference classStudents = newEReferenceBuilder().withName("students").withEType(student).withLowerBound(0).withUpperBound(-1).build();
        EReference tallestStudent = newEReferenceBuilder().withName("tallestStudent").withDerived(true).withEType(student).withLowerBound(0).withUpperBound(1).build();
        EReference tallestStudents = newEReferenceBuilder().withName("tallestStudents").withDerived(true).withEType(student).withLowerBound(0).withUpperBound(1).build();
        EClass clazz = newEClassBuilder().withName("Class").withEStructuralFeatures(classStudents, tallestStudent, tallestStudents).build();
        EReference schoolClasses = newEReferenceBuilder().withName("classes").withEType(clazz).withContainment(true).withLowerBound(0).withUpperBound(-1).build();
        EClass school = newEClassBuilder().withName("School").withEStructuralFeatures(schoolClasses).build();

        EAnnotation getterAnnotationForMother = AsmUtils.getExtensionAnnotationByName(mother, "expression", true).get();
        getterAnnotationForMother.getDetails().put("getter", "self.parents!filter(p | p.gender == schools::Gender#FEMALE)!any()");
        getterAnnotationForMother.getDetails().put("getter.dialect", "JQL");

        EAnnotation getterAnnotationTallestStudent = AsmUtils.getExtensionAnnotationByName(tallestStudent, "expression", true).get();
        getterAnnotationTallestStudent.getDetails().put("getter", "self.students!head(s | s.height DESC)");
        getterAnnotationTallestStudent.getDetails().put("getter.dialect", "JQL");

        EAnnotation getterAnnotationTallestStudents = AsmUtils.getExtensionAnnotationByName(tallestStudents, "expression", true).get();
        getterAnnotationTallestStudents.getDetails().put("getter", "self.students!heads(s | s.height DESC)");
        getterAnnotationTallestStudents.getDetails().put("getter.dialect", "JQL");

        EPackage schools = newEPackageBuilder().withName("schools").withNsURI("http://blackbelt.hu/judo/expression/test/schools")
                .withNsPrefix("expressionTestSchools").withEClassifiers(school, clazz, person, student, parent, genderEnum).build();

        markAsEntity(school, clazz, person, student, parent);

        asmModel.addContent(schools);
    }

    private void markAsEntity(EClass... eClasses) {
        for (EClass eClass : eClasses) {
            EAnnotation orderAnnotation = AsmUtils.getExtensionAnnotationByName(eClass, "entity", true).get();
            orderAnnotation.getDetails().put("value", "true");
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        expressionModelResourceSupport = ExpressionModelResourceSupport.expressionModelResourceSupportBuilder()
                .uri(URI.createURI("urn:test.judo-meta-expression"))
                .build();
        expressionBuilder = new JqlExpressionBuilder<>(modelAdapter, expressionModelResourceSupport.getResource());
    }

    @AfterEach
    void tearDown(final TestInfo testInfo) throws Exception {
        ExpressionModel expressionModel = ExpressionModel.buildExpressionModel()
                .expressionModelResourceSupport(expressionModelResourceSupport)
                .name(asmModel.getName())
                .build();

        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            validateExpressionOnAsm(bufferedLog, asmModel, measureModel, expressionModel, calculateExpressionValidationScriptURI());
        }
        modelAdapter = null;
        asmUtils = null;
        expressionModelResourceSupport = null;
    }

    private Expression createExpression(String jqlExpressionAsString) {
        return createExpression(null, jqlExpressionAsString);
    }

    private Expression createExpression(final EClass clazz, final String jqlExpressionString) {
        Expression expression =
                expressionBuilder.createExpression(CreateExpressionArguments.<EClass, EClass, EClassifier>builder()
                                                                            .withClazz(clazz)
                                                                            .withJqlExpressionAsString(jqlExpressionString)
                                                                            .build());
        assertThat(expression, notNullValue());
        expressionBuilder.storeExpression(expression);
        return expression;
    }

    private Matcher<Expression> collectionOf(String typeName) {
        return new DiagnosingMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("A collection of type with name: ").appendText(typeName);
            }

            @Override
            protected boolean matches(Object item, Description mismatchDescription) {
                boolean result = false;
                if (item instanceof CollectionExpression) {
                    CollectionExpression collection = (CollectionExpression) item;
                    TypeName collectionTypeName = modelAdapter.buildTypeName((EClassifier) (collection).getObjectType(modelAdapter)).get();
                    if (collectionTypeName.getName().equals(typeName)) {
                        result = true;
                    } else {
                        mismatchDescription.appendValue(collectionTypeName.getName());
                    }
                } else {
                    mismatchDescription.appendText("Not a collection: ").appendValue(item);
                }
                return result;
            }
        };
    }

    private Matcher<Expression> objectOf(String typeName) {
        return new DiagnosingMatcher<>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("An object of type with name: ").appendText(typeName);
            }

            @Override
            protected boolean matches(Object item, Description mismatchDescription) {
                boolean result = false;
                if (item instanceof ObjectExpression) {
                    ObjectExpression oe = (ObjectExpression) item;
                    TypeName objectTypeName = modelAdapter.buildTypeName((EClassifier) (oe).getObjectType(modelAdapter)).get();
                    if (objectTypeName.getName().equals(typeName)) {
                        result = true;
                    } else {
                        mismatchDescription.appendValue(objectTypeName.getName());
                    }
                } else {
                    mismatchDescription.appendText("Not an object: ").appendValue(item);
                }
                return result;
            }

        };
    }

    private EClass findBase(String entityName) {
        return asmUtils.all(EClass.class).filter(c -> entityName.equals(c.getName())).findAny().orElseThrow(IllegalArgumentException::new);
    }

    @Test
    void testOrderDate() {
        final EClass order = asmUtils.all(EClass.class).filter(c -> "Order".equals(c.getName())).findAny().get();
        final Expression expression = createExpression(order, "self.orderDate");
        assertNotNull(expression);

        expressionBuilder.createGetterBinding(order, expression, "orderDate", ATTRIBUTE);
        log.info("Order date: " + expression);
    }

    @Test
    void testOrderCategories() {
        EClass order = findBase("Order");
        ReferenceExpression expression = (ReferenceExpression) createExpression(order, "self.orderDetails.product.category");
        assertNotNull(expression);
        expressionBuilder.createGetterBinding(order, expression, "categories", RELATION);
        assertThat(expression, collectionOf("Category"));
    }

    @Test
    void testSimpleArithmeticOperation() {
        final Expression expression = createExpression("2*3");
        log.info("Simple arithmetic operation: " + expression);
        assertNotNull(expression);
    }

    @Test
    void testNonConstantEnvironmentVariable() {
        assertThrows(JqlExpressionBuildException.class, () -> createExpression("demo::types::String!getVariable('ENVIRONMENT', demo::types::String!getVariable('ENVIRONMENT', 's'))"), "Variable name must be constant");
    }

    @Test
    void testStaticExpressions() {
        Expression allProducts = createExpression("demo::entities::Product");
        assertThat(allProducts, instanceOf(CollectionExpression.class));
        allProducts = createExpression("(demo::entities::Product)");
        assertThat(allProducts, instanceOf(CollectionExpression.class));
        Expression allOrdersCount = createExpression("demo::entities::Order!count()");
        assertThat(allOrdersCount, instanceOf(IntegerExpression.class));
        Expression allEmployeeOrders = createExpression("demo::entities::Employee=>orders");
        assertThat(allEmployeeOrders, instanceOf(CollectionExpression.class));
        assertThat(allEmployeeOrders, collectionOf("Order"));
        Expression allProductsSorted = createExpression("demo::entities::Product!sort()");
        assertThat(allProductsSorted, instanceOf(SortExpression.class));
    }

    @Test
    void testSimpleDaoTest() {
        EClass order = findBase("Order");
        createExpression(order, "self.orderDetails");
        Expression orderCategories = createExpression(order, "self.orderDetails.product.category");
        assertThat(orderCategories, collectionOf("Category"));
        orderCategories = createExpression(order, "((((self).orderDetails).product).category)");
        assertThat(orderCategories, collectionOf("Category"));

        createExpression(order, "self.shipper.companyName");
        createExpression(order, "(self).shipper.companyName");
        createExpression(order, "self.shipper");
        createExpression(order, "(self.shipper)");
        createExpression(order, "self.orderDate");

        EClass internationalOrder = findBase("InternationalOrder");
        createExpression(internationalOrder, "self.customsDescription");
        createExpression(internationalOrder, "self.exciseTax");

        EClass product = findBase("Product");
        createExpression(product, "self.category");
        createExpression(product, "self.productName");
        createExpression(product, "self.unitPrice");

        EClass shipper = findBase("Shipper");
        createExpression(shipper, "self.companyName");

        EClass category = findBase("Category");
        createExpression(category, "self.products");
        createExpression(category, "self.categoryName");

        EClass orderDetail = findBase("OrderDetail");
        createExpression(orderDetail, "self.product");
        createExpression(orderDetail, "self.product.category");
        createExpression(orderDetail, "self.product.productName");
        createExpression(orderDetail, "self.unitPrice");
        createExpression(orderDetail, "self.quantity");
        createExpression(orderDetail, "self.discount");
        createExpression(orderDetail, "self.quantity * self.unitPrice * (1 - self.discount)");

    }

    @Test
    void testCast() {
        EClass customer = findBase("Customer");
        createExpression(customer, "self=>orders!asCollection(demo::entities::InternationalOrder)");
        createExpression(customer, "self=>addresses!sort()!head()!asType(demo::entities::InternationalAddress).country");

        EClass order = findBase("Order");
        createExpression(order, "self=>customer!asType(demo::entities::Company)");
    }

    @Test
    void testCastWithShortenedNames() {
        EClass customer = findBase("Customer");
        createExpression(customer, "self=>orders!asCollection(InternationalOrder)");
        createExpression(customer, "self=>addresses!sort()!head()!asType(InternationalAddress).country");

        EClass order = findBase("Order");
        createExpression(order, "self=>customer!asType(Company)");
    }

    @Test
    void testContainer() {
        EClass address = findBase("Address");
        createExpression(address, "self!container(demo::entities::Customer)");
    }

    @Test
    void testContainerWithShortenedNames() {
        EClass address = findBase("Address");
        createExpression(address, "self!container(Customer)");
    }

    @Test
    void testConstants() {
        createExpression("1");
        createExpression("1.2");
        createExpression("true");
        createExpression("false");
        createExpression("'a'");
        createExpression("\"\"");
        createExpression("\"b\"");
        DateConstant dateConstant = (DateConstant) createExpression("`2019-12-31`");
        createExpression("`2019-12-31T13:52:03+02:00`");
        createExpression("demo::entities::Category!sort()!head().picture");
    }

    @Test
    void testDateOperations() {
        createExpression("`2019-12-31`!elapsedTimeFrom(`2020-01-01`)");
        createExpression("`2019-12-31` < `2020-01-01`");
        createExpression("`2019-12-31` > `2020-01-01`");
        createExpression("`2019-12-31` == `2020-01-01`");
        createExpression("`2019-12-31` != `2020-01-01`");
        createExpression("`2019-12-31` <= `2020-01-01`");
        createExpression("`2019-12-31` >= `2020-01-01`");
        assertThrows(UnsupportedOperationException.class, () -> createExpression("`2019-12-31` + 1"));
        createExpression("`2019-12-31` + 1[day]");
        createExpression("1[day] + `2019-12-31`");
        assertThrows(IllegalArgumentException.class, () -> createExpression("1[day] - `2019-12-31`"));

        assertThat(createExpression("`2019-12-31`!asString()"), instanceOf(StringExpression.class));
    }

    @Test
    void testTimestampOperations() {
        EClass orderType = asmUtils.getClassByFQName("demo.entities.Order").get();
        Expression expression = createExpression(orderType, "self.orderDate + 10.5[day]");
        assertThat(expression, instanceOf(TimestampExpression.class));
        TimestampDifferenceExpression elapsedTimeFrom = (TimestampDifferenceExpression) createExpression("`2019-12-31T00:00:00.000+01:00`!elapsedTimeFrom(`2019-12-31T00:00:00.000+02:00`)");
        assertThat(elapsedTimeFrom.getMeasure(), notNullValue());
        createExpression("`2019-12-31T00:00:00.000+01:00` > `2019-12-31T00:00:00.000+02:00`");
        createExpression("`2019-12-31T00:00:00.000+01:00` < `2019-12-31T00:00:00.000+02:00`");
        createExpression("`2019-12-31T00:00:00.000+01:00` == `2019-12-31T00:00:00.000+02:00`");
        createExpression("`2019-12-31T00:00:00.000+01:00` != `2019-12-31T00:00:00.000+02:00`");
        createExpression("`2019-12-31T00:00:00.000+01:00` <= `2019-12-31T00:00:00.000+02:00`");
        createExpression("`2019-12-31T00:00:00.000+01:00` >= `2019-12-31T00:00:00.000+02:00`");
        createExpression("`2019-12-31T00:00:00.000+01:00` + 1[week]");
        createExpression("`2019-12-31T00:00:00.000+01:00` - 1[demo::measures::Time#day]");
        assertThrows(UnsupportedOperationException.class, () -> createExpression("`2019-12-31T00:00:00.000+01:00` + 1"));
        assertThrows(UnsupportedOperationException.class, () -> createExpression("`2019-12-31T00:00:00.000+01:00` - 1"));
        assertThrows(IllegalArgumentException.class, () -> createExpression("1[day] - `2019-12-31T00:00:00.000+01:00`"));

        assertThat(createExpression("`2019-12-31T00:00:00.000+01:00`!asString()"), instanceOf(StringExpression.class));
    }

    @Test
    void testTimeOperations() {
        EClass orderType = asmUtils.getClassByFQName("demo.entities.Order").get();
        Expression expression = createExpression(orderType, "self.deliveryFrom + 10.5[hour]");
        assertThat(expression, instanceOf(TimeExpression.class));
        TimeDifferenceExpression elapsedTimeFrom = (TimeDifferenceExpression) createExpression("`10:00:00.000`!elapsedTimeFrom(`09:00:00.000`)");
        assertThat(elapsedTimeFrom.getMeasure(), notNullValue());
        createExpression("`00:00:00.000` > `10:00:00.000`");
        createExpression("`00:00:00.000` < `10:00:00.000`");
        createExpression("`00:00:00.000` == `10:00:00.000`");
        createExpression("`00:00:00.000` != `10:00:00.000`");
        createExpression("`00:00:00.000` <= `10:00:00.000`");
        createExpression("`00:00:00.000` >= `10:00:00.000`");
        createExpression("`00:00:00.000` + 1[second]");
        createExpression("`00:00:00.000` - 1[demo::measures::Time#second]");
        assertThrows(UnsupportedOperationException.class, () -> createExpression("`00:00:00.000` + 1"));
        assertThrows(UnsupportedOperationException.class, () -> createExpression("`00:00:00.000` - 1"));
        assertThrows(IllegalArgumentException.class, () -> createExpression("1[second] - `00:00:00.000`"));
        assertThat(createExpression("`00:00:00.000`!asString()"), instanceOf(StringExpression.class));
    }

    @Test
    void testDecimalOperations() {
        createExpression("1.0 < 3.14");
        createExpression("1.0 > 3.14");
        createExpression("1.0 <= 3.14");
        createExpression("1.0 >= 3.14");
        createExpression("1.0 == 3.14");
        createExpression("1.0 != 3.14");
        createExpression("1.0 + 3.14");
        createExpression("1.0 - 3.14");
        createExpression("1.0 * 3.14");
        createExpression("1.0 / 3.14");
        createExpression("1 < 3.14");
        createExpression("1.0 mod 2");
        createExpression("1.0 div 2");
        assertThat(createExpression("1 / 2"), instanceOf(DecimalExpression.class));
    }

    @Test
    void testTernaryOperation() {
        createExpression("true ? 1 : 2");
        createExpression("true ? 1 : 2 + 3");
        createExpression("true ? 1.0 : 2.0 + 3");
        Expression decimalSwitch1 = createExpression("true ? 1 : 2.0");
        assertThat(decimalSwitch1, instanceOf(DecimalSwitchExpression.class));
        Expression decimalSwitch2 = createExpression("true ? 1.0 : 2");
        assertThat(decimalSwitch2, instanceOf(DecimalSwitchExpression.class));

        Expression stringSwitch = createExpression("false ? demo::entities::Order!any().shipper.companyName : 'b'");
        assertThat(stringSwitch, instanceOf(StringExpression.class));

        Expression sametype = createExpression("true ? demo::entities::Order : demo::entities::Order");
        assertThat(((CollectionSwitchExpression) sametype).getElementName().getName(), is("Order"));

        Expression ancestor = createExpression("true ? demo::entities::OnlineOrder : demo::entities::Order");
        assertThat(((CollectionSwitchExpression) ancestor).getElementName().getName(), is("Order"));

        Expression expression = createExpression("true ? demo::entities::OnlineOrder : demo::entities::InternationalOrder");
        assertThat(((CollectionSwitchExpression) expression).getElementName().getName(), is("Order"));

        Expression customerAncestor = createExpression("true ? demo::entities::Individual : demo::entities::Shipper");
        assertThat(((CollectionSwitchExpression) customerAncestor).getElementName().getName(), is("Customer"));

        Expression companyAncestor = createExpression("true ? demo::entities::Supplier : demo::entities::Shipper");
        assertThat(((CollectionSwitchExpression) companyAncestor).getElementName().getName(), is("Company"));

        assertThrows(IllegalArgumentException.class, () -> createExpression("true ? demo::entities::Product : demo::entities::Order"));

        createExpression("true ? demo::entities::Category!sort()!head().picture : demo::entities::Product!sort()!head()->category.picture");
        createExpression("true ? demo::types::Countries#AT : demo::types::Countries#RO");

        Expression objectExpression = createExpression("true ? demo::entities::Category!sort()!head() : demo::entities::Product!sort()!head()->category");
        assertThat(objectExpression, instanceOf(ObjectSwitchExpression.class));
        assertThat(((ObjectSwitchExpression) objectExpression).getElementName().getName(), is("Category"));

        createExpression("true ? 'a' : demo::entities::Category!sort()!head().categoryName");
        createExpression("true ? `2019-10-12` : `2019-10-23`");
        createExpression("true ? `2019-10-12T00:00:00+00:00` : `2019-10-23T00:00:00+00:00`");
    }

    @Test
    void testCollectionVariableNames() {
        Expression productListOrdered = createExpression("demo::entities::Category=>products!sort(e | e.unitPrice ASC, e.productName DESC)!sort(p | p.unitPrice)!join(s | s.productName, ',')");
        assertThat(productListOrdered, instanceOf(StringExpression.class));
    }

    @Test
    void testCollectionOperations() {
        EClass category = findBase("Category");
        createExpression(category, "self=>products!sort(p | p.unitPrice)!tail()!memberOf(self=>products)");

        Expression products = createExpression(category, "self.products");
        assertThat(products, collectionOf("Product"));

        createExpression(category, "self=>products!count()");
        createExpression(category, "self=>products!join(p1 | p1.productName, ', ')!length()");
        createExpression(category, "self=>products!sort(e | e.unitPrice)!head()");
        Expression productListTail = createExpression(category, "self=>products!sort(e | e.unitPrice)!tail()");
        Expression cheapProducts = createExpression(category, "self=>products!filter(p | p.unitPrice < 2.0)");
        assertThat(cheapProducts, collectionOf("Product"));
        createExpression(category, "self=>products!filter(p | p.unitPrice < 2.0)!count()");

        createExpression(category, "self=>products!min(p | p.unitPrice)");
        createExpression(category, "self=>products!max(p | p.unitPrice)");
        createExpression(category, "self=>products!sum(p | p.unitPrice)");
        createExpression(category, "self=>products!avg(p | p.unitPrice)");
        createExpression(category, "self=>products!min(p | p.quantityPerUnit)");
        createExpression(category, "self=>products!max(p | p.quantityPerUnit)");
        createExpression(category, "self=>products!sum(p | p.quantityPerUnit)");
        createExpression(category, "self=>products!avg(p | p.quantityPerUnit)");
        createExpression(category, "self=>products!filter(p | p.unitPrice < 2.0)!avg(p | p.unitPrice)");

        Expression productHead = createExpression(category, "self=>products!sort(p | p.unitPrice)!head()");
        assertThat(productHead, objectOf("Product"));
        createExpression(category, "self=>products!sort(p | p.unitPrice)!tail()");

        Expression containsProduct = createExpression(category, "self=>products!contains(self=>products!sort(p | p.unitPrice)!tail())");
        assertThat(containsProduct, instanceOf(LogicalExpression.class));
    }

    @Test
    void testObjectSelectorToFilterExpressions() {
        EClass category = findBase("Category");
        Expression headDescExpression = createExpression(category, "self=>products!head(p | p.unitPrice DESC).unitPrice");
        assertThat(headDescExpression.toString(), is("self=>products!filter(_iterator_1 | (_iterator_1.unitPrice == self=>products!max(_iterator_1 | _iterator_1.unitPrice)))!any().unitPrice"));
        Expression headAscExpression = createExpression(category, "self=>products!head(p | p.unitPrice).unitPrice");
        assertThat(headAscExpression.toString(), is("self=>products!filter(_iterator_2 | (_iterator_2.unitPrice == self=>products!min(_iterator_2 | _iterator_2.unitPrice)))!any().unitPrice"));
        Expression headImmutableSetExpression = createExpression(category, "demo::entities::Product!head(p | p.unitPrice).unitPrice");
        assertThat(headImmutableSetExpression.toString(), is("demo::entities::Product!filter(_iterator_3 | (_iterator_3.unitPrice == demo::entities::Product!min(_iterator_3 | _iterator_3.unitPrice)))!any().unitPrice"));

        Expression tailDescExpression = createExpression(category, "self=>products!tail(p | p.unitPrice DESC).unitPrice");
        assertThat(tailDescExpression.toString(), is("self=>products!filter(_iterator_4 | (_iterator_4.unitPrice == self=>products!min(_iterator_4 | _iterator_4.unitPrice)))!any().unitPrice"));
        Expression tailAscExpression = createExpression(category, "self=>products!tail(p | p.unitPrice).unitPrice");
        assertThat(tailAscExpression.toString(), is("self=>products!filter(_iterator_5 | (_iterator_5.unitPrice == self=>products!max(_iterator_5 | _iterator_5.unitPrice)))!any().unitPrice"));

        Expression headsDescExpression = createExpression(category, "self=>products!heads(p | p.unitPrice DESC)");
        assertThat(headsDescExpression.toString(), is("self=>products!filter(_iterator_6 | (_iterator_6.unitPrice == self=>products!max(_iterator_6 | _iterator_6.unitPrice)))"));
        Expression headsAscExpression = createExpression(category, "self=>products!heads(p | p.unitPrice)");
        assertThat(headsAscExpression.toString(), is("self=>products!filter(_iterator_7 | (_iterator_7.unitPrice == self=>products!min(_iterator_7 | _iterator_7.unitPrice)))"));

        Expression tailsDescExpression = createExpression(category, "self=>products!tails(p | p.unitPrice DESC)");
        assertThat(tailsDescExpression.toString(), is("self=>products!filter(_iterator_8 | (_iterator_8.unitPrice == self=>products!min(_iterator_8 | _iterator_8.unitPrice)))"));
        Expression tailsAscExpression = createExpression(category, "self=>products!tails(p | p.unitPrice)");
        assertThat(tailsAscExpression.toString(), is("self=>products!filter(_iterator_9 | (_iterator_9.unitPrice == self=>products!max(_iterator_9 | _iterator_9.unitPrice)))"));
    }

    @Test
    void testObjectSelectorToFilterExpressionsWithShortenedNames() {
        EClass category = findBase("Category");
        Expression headImmutableSetExpression = createExpression(category, "Product!head(p | p.unitPrice).unitPrice");
        assertThat(headImmutableSetExpression.toString(), is("demo::entities::Product!filter(_iterator_1 | (_iterator_1.unitPrice == demo::entities::Product!min(_iterator_1 | _iterator_1.unitPrice)))!any().unitPrice"));
    }

    /*
     * See JNG-1700 for details.
     * The simple resolution for tallesStudent would be:
     * self.classes.students!filter(s | (s.height == self=>classes=>students!max(s | s.height)))!any()
     * But we need to first get all students of all classes:
     * self.classes.students
     * And choose those for which the following is true:
     * there is a class in self.classes, where the student is the tallest, ie.:
     * self.classes.students!filter(s | (s.height == self=>classes=>students!max(s | s.height)))!any()
     * self.classes.students!filter(_s | self.classes!exists( _c | _c.students!filter(s | (s.height == _c.students!max(s | s.height)))!any() == _s))
     */
    @Test
    void testDerivedHeadTail() {
        EClass school = findBase("School");
        Expression expression = createExpression(school, "self.classes.tallestStudent");
        assertThat(expression.toString(), is("self=>classes=>students!filter(_iterator_2 | self=>classes!exists(_iterator_3 | (_iterator_3=>students!filter(_iterator_1 | (_iterator_1.height == _iterator_3=>students!max(_iterator_1 | _iterator_1.height)))!any() equal _iterator_2)))"));
        Expression expression2 = createExpression(school, "self.classes.tallestStudents");
        assertThat(expression2.toString(), is("self=>classes=>students!filter(_iterator_5 | self=>classes!exists(_iterator_6 | (_iterator_6=>students!filter(_iterator_4 | (_iterator_4.height == _iterator_6=>students!max(_iterator_4 | _iterator_4.height))) contains _iterator_5)))"));
        Expression expression3 = createExpression(school, "self.classes.tallestStudent.mother!avg(m | m.height)");
        assertThat(expression3.toString(), is("self=>classes=>students!filter(_iterator_8 | self=>classes!exists(_iterator_9 | (_iterator_9=>students!filter(_iterator_7 | (_iterator_7.height == _iterator_9=>students!max(_iterator_7 | _iterator_7.height)))!any() equal _iterator_8)))=>parents!filter(_iterator_11 | self=>classes=>students!filter(_iterator_8 | self=>classes!exists(_iterator_9 | (_iterator_9=>students!filter(_iterator_7 | (_iterator_7.height == _iterator_9=>students!max(_iterator_7 | _iterator_7.height)))!any() equal _iterator_8)))!exists(_iterator_12 | (_iterator_12=>parents!filter(_iterator_10 | (_iterator_10.gender equal schools::Gender#FEMALE))!any() equal _iterator_11)))!avg(_iterator_13 | _iterator_13.height)"));
    }

    @Test
    void testCustomAttributes() {
        EClass category = findBase("Category");
    }

    @Test
    void testParenNavigation() {
        EClass order = findBase("Order");
        Expression expression = createExpression(order, "(self.shipper).companyName");
    }

    @Test
    void testStringOperations() {
        EClass category = findBase("Category");
        createExpression(category, "self.categoryName < 'c'");
        createExpression(category, "self.categoryName > 'c'");
        createExpression(category, "self.categoryName <= 'c'");
        createExpression(category, "self.categoryName >= 'c'");
        createExpression(category, "self.categoryName == 'c'");
        createExpression(category, "self.categoryName != 'c'");

        // Concatenate
        createExpression("'a'+'b'");
        createExpression("demo::entities::Order!any().shipper.companyName + demo::entities::Order!any().shipper.companyName");
        createExpression("'_' + demo::entities::Order!any().shipper.companyName");
    }

    @Test
    void testLambdaScoping() {
        EClass category = findBase("Category");
        createExpression(category, "self=>products!sort(p | p.unitPrice)!head() == self=>products!sort(p | p.unitPrice)!tail()");
        assertThrows(JqlExpressionBuildException.class, () -> createExpression(category, "self=>products!sort(p | p.unitPrice)!head() == self=>products!sort(q | p.unitPrice)!tail()"));
    }

    @Test
    void testHeadDefault() {
        createExpression("demo::entities::Category!any()=>products!sort()!head()");
    }

    @Test
    void testHeadLambda() {
        createExpression("demo::entities::Category!any()=>products!sort(p | p.unitPrice)!head()");
        createExpression("demo::entities::Category!any()=>products!head(p | p.unitPrice) != demo::entities::Category!any()=>products!tail(p | p.unitPrice)");
        createExpression("demo::entities::Category!any()=>products!sort(p | p.unitPrice)!head() != demo::entities::Category!any()=>products!sort(p | p.unitPrice)!tail()");
    }

    @Test
    void testHeadToFilter() {
        Expression expression = createExpression("demo::entities::Category!any()=>products!head(p | p.unitPrice).category");
    }

    @Test
    void testObjectOperations() {
        createExpression("not demo::entities::OrderDetail!any().product!kindof(demo::entities::Product)");
        createExpression("demo::entities::OrderDetail!any().product!typeof(demo::entities::Product)");

        EClass order = findBase("Order");
        createExpression(order, "self->customer!asType(demo::entities::Individual)!filter(c | c.firstName == 'joe')");
        ObjectFilterExpression objectFilter = (ObjectFilterExpression) createExpression(order, "self.shipper!filter(s | s.companyName == 'DHL')");
        assertThat(objectFilter.getObjectExpression().getIteratorVariableName(), is("_iterator_2"));

    }

    @Test
    void testObjectOperationsWithShortenedNames() {
        EClass order = findBase("Order");
        createExpression(order, "self->customer!asType(Individual)!filter(c | c.firstName == 'joe')");
        ObjectFilterExpression objectFilter = (ObjectFilterExpression) createExpression(order, "self.shipper!filter(s | s.companyName == 'DHL')");
        assertThat(objectFilter.getObjectExpression().getIteratorVariableName(), is("_iterator_2"));

    }

    @Test
    void testChainedObjectFilter() {
        EClass order = findBase("Order");
        ObjectFilterExpression chainedObjectFilter = (ObjectFilterExpression) createExpression(order, "self.shipper!filter(s | s.companyName == 'DHL')!filter(c | c.phone!isDefined()).territory!filter(t | t.shipper.phone == t.shipper.phone)");
    }

    @Test
    void testUnaryOperations() {
        createExpression("-1");
        createExpression("-1.0");
        createExpression("not true");
    }

    @Test
    void testEnums() {
        EClass order = findBase("Order");
        assertThrows(JqlExpressionBuildException.class, () -> createExpression(order, "self.shipAddress.country == Countries#AT"));

        createExpression("1 < 2 ? demo::types::Countries#AT : demo::types::Countries#RO");
        createExpression("demo::types::Countries#AT == demo::types::Countries#RO");

        assertThat(createExpression("demo::types::Countries#HU!asString()"), instanceOf(StringExpression.class));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> createExpression("demo::types::Countries#UK"));
        assertTrue(exception.getMessage().contains("Unknown literal: UK"));
    }

    @Test
    void testEnumTypeDifference() {
        assertThrows(IllegalArgumentException.class, () -> createExpression("true ? demo::types::Countries#AT : schools::Gender#MALE"));
        assertThrows(IllegalArgumentException.class, () -> createExpression("demo::types::Countries#AT == schools::Gender#MALE"));
    }

    @Test
    void testMeasures() {
        createExpression("5[demo::measures::Time#min]");
        MeasuredDecimal unitNoFqName = (MeasuredDecimal) createExpression("1.23[min]");
        assertThat(unitNoFqName.getUnitName(), is("minute"));
        assertThat(unitNoFqName.getMeasure().getNamespace(), is("demo::measures"));
        assertThat(unitNoFqName.getMeasure().getName(), is("Time"));
        Expression measuredIntDiv = createExpression("2[min] / 1");
        assertTrue(modelAdapter.isMeasured((NumericExpression) measuredIntDiv));
        Expression notMeasuredDecimalRatio = createExpression("2.0[min] / 1[min]");
        assertFalse(modelAdapter.isMeasured((NumericExpression) notMeasuredDecimalRatio));
        Expression measuredDecimalRatio = createExpression("2.0[demo::measures::Velocity#m/s] / 1[s]");
        assertTrue(modelAdapter.isMeasured((NumericExpression) measuredDecimalRatio));

        assertThat(createExpression("2[hour]!asString()"), instanceOf(StringExpression.class));
    }

    @Test
    void testNumericFunctions() {
        Expression roundedConstant = createExpression("1.2!round()");
        assertThat(roundedConstant, instanceOf(IntegerExpression.class));
        Expression roundedAttribute = createExpression("(demo::entities::Product!any().unitPrice)!round()");
        assertThat(roundedAttribute, instanceOf(IntegerExpression.class));

        assertThat(createExpression("1.5!asString()"), instanceOf(StringExpression.class));
        assertThat(createExpression("100!asString()"), instanceOf(StringExpression.class));
    }

    @Test
    void testSelectorFunctions() {
        EClass category = findBase("Category");
        Expression expression = createExpression("demo::entities::Product!sort()");
        assertThat(expression, instanceOf(SortExpression.class));
        expression = createExpression("demo::entities::Product!sort()!head()");
        assertThat(expression, instanceOf(ObjectSelectorExpression.class));
        expression = createExpression("demo::entities::Product!sort()!head().weight");
        assertThat(expression, instanceOf(AttributeSelector.class));
        expression = createExpression(category, "self.products!sort()!head().weight");
        assertThat(expression, instanceOf(AttributeSelector.class));
    }

    @Test
    void testStringFunctions() {
        EClass order = findBase("Order");

        // LowerCase
        createExpression(order, "self.shipper.companyName!lowerCase()");

        // UpperCase
        createExpression(order, "self.shipper.companyName!upperCase()");

        // Length
        Expression shipperNameLength = createExpression(order, "self.shipper.companyName!length()");
        assertThat(shipperNameLength, instanceOf(NumericExpression.class));
        createExpression(order, "self.shipper.companyName!lowerCase()!length() > 0");

        // SubString
        createExpression(order, "self.shipper.companyName!substring(1, 4)!length() > 0");
        createExpression(order, "self.shipper.companyName!substring(1, self.shipper.companyName!length()-1)");

        // Position
        createExpression(order, "self.shipper.companyName!position('a') > 0");
        createExpression(order, "self.shipper.companyName!position(self.shipper.companyName) > 0");

        // Replace
        createExpression(order, "self.shipper.companyName!replace('\\n', '')");
        createExpression(order, "self.shipper.companyName!replace('$', self.shipper.companyName)");

        // Trim
        createExpression(order, "self.shipper.companyName!trim()");

        // First
        createExpression(order, "self.shipper.companyName!first(5)");

        // Last
        createExpression(order, "self.shipper.companyName!last(1)");

        // Matches
        createExpression(order, "self.shipper.companyName!matches('blackbelt\\\\.hu')");

        // Like
        createExpression(order, "self.shipper.companyName!like('%Kft')");
        createExpression(order, "self.shipper.companyName!ilike('%kft')");

        assertThat(createExpression("true!asString()"), instanceOf(StringExpression.class));
    }

    @Test
    public void testDefined() {
        EClass order = findBase("Order");
        createExpression(order, "self.shipper!isUndefined()");
        createExpression(order, "self.shipper.companyName!isUndefined()");
        createExpression(order, "not self.shipper!isUndefined()");
        createExpression(order, "self!isDefined()");
    }

//    TODO @Test
//    public void testSequences() {
//        createExpression("demo::GlobalSequence!next()");
//        createExpression("demo::GlobalSequence!current()");
//    }

    @Test
    public void testSpawnOperator() {
        EClass order = findBase("Order");
        assertThrows(UnsupportedOperationException.class, () -> createExpression(order, "self.shipper as demo::entities::Shipper"));
    }

    @Test
    public void testSpawnOperatorWithShortenedNames() {
        EClass order = findBase("Order");
        assertThrows(UnsupportedOperationException.class, () -> createExpression(order, "self.shipper as Shipper"));
    }

    @Test
    public void testEnvironmentVariables() {
        createExpression("demo::types::Timestamp!getVariable('SYSTEM', 'current_timestamp')");
        createExpression("demo::types::Time!getVariable('SYSTEM', 'current_time')");
    }

    @Test
    public void testExternalVariablesAreNotAllowed() {
        EClass order = findBase("Order");
        assertThrows(JqlExpressionBuildException.class, () -> createExpression(order, "demo::entities::Order!filter(o | o == self)"));
    }

    @Test
    public void testExternalVariablesAreNotAllowedWithShortenedNames() {
        EClass order = findBase("Order");
        assertThrows(JqlExpressionBuildException.class, () -> createExpression(order, "Order!filter(o | o == self)"));
    }

    @Test
    public void test002() {
        EClass customer = findBase("Customer");
        createExpression(customer, "self.addresses");

        createExpression("-1.5!round() < 1.2 and demo::entities::Order!sort()!head()!kindOf(demo::entities::InternationalOrder)");
        createExpression("demo::entities::Product!filter(p | p.discounted)!count() > 10 ? 1.2 : 8.7");
        // select categories, where the category has more than 10 products
        createExpression("demo::entities::Category!filter(c | demo::entities::Product!filter(p | p.category == p.category)!count() > 10)");

        createExpression("true ? 8[dkg]+12[g] : 2[g] + 4[g] + demo::entities::Product!sort()!head().weight");

        createExpression("(2 + 4) * 8[kg] * 60[kilometrePerHour] / 3[s]");
        createExpression("9[mm]/(1/45[cm])");
        createExpression("9[mg] < 2[kg]");
        createExpression("`2019-01-02T03:04:05.678+01:00` + 102[s]");
        Expression timeStampAddition = createExpression("demo::entities::Order!sort()!head().orderDate - 3[day]");
        assertThat(timeStampAddition, instanceOf(TimestampExpression.class));
        createExpression("`2019-01-02T03:04:05.678+01:00`!elapsedTimeFrom(`2019-01-30T15:57:08.123+01:00`)");

        Expression customerExpression = createExpression(
        		"demo::entities::Order!filter("
        				+ "o | o=>orderDetails->product!contains("
        					+ "demo::entities::Product!filter("
        						+ "p | p.productName == 'Lenovo B51')!sort()!head()))"
        		+ "!asCollection("
        			+ "demo::entities::InternationalOrder)"
        		+ "!filter(io | io.exciseTax > 1/2 + io=>orderDetails!sum("
        			+ "iod | iod.unitPrice))"
        		+ "!sort(iof | iof.freight, iof=>orderDetails!count() DESC)"
        		+ "!head()->customer"
        		+ "!filter(c | "
        			+ "c=>addresses!sort()!head()"
        			+ "!asType(demo::entities::InternationalAddress).country == demo::types::Countries#RO and c=>addresses!sort()!head().postalCode!matches('11%'))=>addresses"
        		);
        assertThat(customerExpression, instanceOf(CollectionNavigationFromObjectExpression.class));
    }

    @Test
    public void test003() {
        createExpression("demo::entities::Employee!filter(e | e.lastName == 'Gipsz' and e.firstName == 'Jakab')!sort()!head()=>orders!sort(o | o.orderDate DESC)");
        createExpression("demo::entities::Order=>orderDetails!filter(od | od.product.category.picture!isDefined())");
        createExpression("demo::entities::Order!any()=>orderDetails!sum(od | od.quantity * od.unitPrice * (1 - od.discount))");
    }

    @Test
    public void testKindOf() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!kindOf(schools::Student)"));
        assertTrue(exception.getMessage().contains("Invalid kindof function call: schools.School cannot be casted to schools.Student"));

        // #2 valid - reverse
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!kindOf(schools::Person)"));

        // #3 valid - different
        assertDoesNotThrow(() -> createExpression(findBase("Person"), "self!kindOf(schools::Student)"));

        // #4 valid - same
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!kindOf(schools::Student)"));
    }

    @Test
    public void testKindOfWithShortenedNames() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!kindOf(Student)"));
        assertTrue(exception.getMessage().contains("Invalid kindof function call: schools.School cannot be casted to schools.Student"));

        // #2 valid - reverse
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!kindOf(Person)"));

        // #3 valid - different
        assertDoesNotThrow(() -> createExpression(findBase("Person"), "self!kindOf(Student)"));

        // #4 valid - same
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!kindOf(Student)"));
    }

    @Test
    public void testTypeOf() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!typeOf(schools::Student)"));
        assertTrue(exception.getMessage().contains("Invalid typeof function call: schools.School cannot be casted to schools.Student"));

        // #2 valid - reverse
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!typeOf(schools::Person)"));

        // #3 valid - different
        assertDoesNotThrow(() -> createExpression(findBase("Person"), "self!typeOf(schools::Student)"));

        // #4 valid - same
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!typeOf(schools::Student)"));
    }

    @Test
    public void testTypeOfWithShortenedNames() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!typeOf(Student)"));
        assertTrue(exception.getMessage().contains("Invalid typeof function call: schools.School cannot be casted to schools.Student"));

        // #2 valid - reverse
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!typeOf(Person)"));

        // #3 valid - different
        assertDoesNotThrow(() -> createExpression(findBase("Person"), "self!typeOf(Student)"));

        // #4 valid - same
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!typeOf(Student)"));
    }

    @Test
    public void testAsType() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!asType(schools::Student)"));
        assertTrue(exception.getMessage().contains("Invalid astype function call: schools.School cannot be casted to schools.Student"));

        // #2 valid - reverse
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!asType(schools::Person)"));

        // #3 valid - same
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!asType(schools::Student)"));

        // #4 valid
        assertDoesNotThrow(() -> createExpression(findBase("Person"), "self!asType(schools::Student)"));
    }

    @Test
    public void testAsTypeWithShortenedNames() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!asType(Student)"));
        assertTrue(exception.getMessage().contains("Invalid astype function call: schools.School cannot be casted to schools.Student"));

        // #2 valid - reverse
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!asType(Person)"));

        // #3 valid - same
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self!asType(Student)"));

        // #4 valid
        assertDoesNotThrow(() -> createExpression(findBase("Person"), "self!asType(Student)"));
    }

    @Test
    public void testContainerValidation() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!container(schools::Class)"));
        assertTrue(exception.getMessage().contains("schools.Class type is not a container type of schools.School"));

        // #2 invalid - same
        JqlExpressionBuildException exception2 =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("Class"), "self!container(schools::Class)"));
        assertTrue(exception2.getMessage().contains("schools.Class type is not a container type of schools.Class"));

        // #3 valid
        assertDoesNotThrow(() -> createExpression(findBase("Class"), "self!container(schools::School)"));
    }

    @Test
    public void testContainerValidationWithShortenedNames() {
        // #1 invalid - different
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self!container(Class)"));
        assertTrue(exception.getMessage().contains("schools.Class type is not a container type of schools.School"));

        // #2 invalid - same
        JqlExpressionBuildException exception2 =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("Class"), "self!container(Class)"));
        assertTrue(exception2.getMessage().contains("schools.Class type is not a container type of schools.Class"));

        // #3 valid
        assertDoesNotThrow(() -> createExpression(findBase("Class"), "self!container(School)"));
    }

    @Test
    public void testContains() {
        // #1 invalid - collection's- and parameter's type are not compatible
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self.classes!contains(schools::Person!any())"));
        assertTrue(exception.getMessage().contains("Types of collection 'Class' and object 'Person' are not compatible"));

        // #2 valid - collection's- and parameter's type are the same
        assertDoesNotThrow(() -> createExpression(findBase("School"), "self.classes!contains(schools::Class!any())"));

        // #3 valid - collection's type is supertype of parameter's type
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self.parents!contains(schools::Student!any())"));

        // #4 valid - parameter's type is supertype of collection's type
        assertDoesNotThrow(() -> createExpression(findBase("Class"), "self.students!contains(schools::Person!any())"));
    }

    @Test
    public void testContainsWithShortenedNames() {
        // #1 invalid - collection's- and parameter's type are not compatible
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self.classes!contains(Person!any())"));
        assertTrue(exception.getMessage().contains("Types of collection 'Class' and object 'Person' are not compatible"));

        // #2 valid - collection's- and parameter's type are the same
        assertDoesNotThrow(() -> createExpression(findBase("School"), "self.classes!contains(Class!any())"));

        // #3 valid - collection's type is supertype of parameter's type
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "self.parents!contains(Student!any())"));

        // #4 valid - parameter's type is supertype of collection's type
        assertDoesNotThrow(() -> createExpression(findBase("Class"), "self.students!contains(Person!any())"));
    }

    @Test
    public void testMemberOf() {
        // #1 invalid - object's- and parameter's type are not compatible
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "schools::Person!any()!memberOf(self.classes)"));
        assertTrue(exception.getMessage().contains("Types of collection 'Person' and object 'Class' are not compatible"));

        // #2 valid - object's- and parameter's type are the same
        assertDoesNotThrow(() -> createExpression(findBase("School"), "schools::Class!any()!memberOf(self.classes)"));

        // #3 valid - object's type is supertype of parameter's type
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "schools::Student!any()!memberOf(self.parents)"));

        // #4 valid - parameter's type is supertype of object's type
        assertDoesNotThrow(() -> createExpression(findBase("Class"), "schools::Person!any()!memberOf(self.students)"));
    }

    @Test
    public void testMemberOfWithShortenedNames() {
        // #1 invalid - object's- and parameter's type are not compatible
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "Person!any()!memberOf(self.classes)"));
        assertTrue(exception.getMessage().contains("Types of collection 'Person' and object 'Class' are not compatible"));

        // #2 valid - object's- and parameter's type are the same
        assertDoesNotThrow(() -> createExpression(findBase("School"), "Class!any()!memberOf(self.classes)"));

        // #3 valid - object's type is supertype of parameter's type
        assertDoesNotThrow(() -> createExpression(findBase("Student"), "Student!any()!memberOf(self.parents)"));

        // #4 valid - parameter's type is supertype of object's type
        assertDoesNotThrow(() -> createExpression(findBase("Class"), "Person!any()!memberOf(self.students)"));
    }

    @Test
    public void testAsCollection() {
        // #1 invalid - incompatible
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self.classes!asCollection(schools::Student)"));
        assertTrue(exception.getMessage().contains("Invalid astype function call: schools.Class cannot be casted to schools.Student"));

        // #2 valid - same type
        assertDoesNotThrow(() -> createExpression(findBase("School"), "self.classes!asCollection(schools::Class)"));

        // #3 valid
        assertDoesNotThrow(() -> findBase("Student"), "self.parents!asCollection(schools::Student)");
    }

    @Test
    public void testAsCollectionWithShortenedNames() {
        // #1 invalid - incompatible
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () ->
                        createExpression(findBase("School"), "self.classes!asCollection(Student)"));
        assertTrue(exception.getMessage().contains("Invalid astype function call: schools.Class cannot be casted to schools.Student"));

        // #2 invalid - same type
        assertDoesNotThrow(() -> createExpression(findBase("School"), "self.classes!asCollection(Class)"));

        // #3 valid
        assertDoesNotThrow(() -> findBase("Student"), "self.parents!asCollection(Student)");
    }

    private static Stream<String> testValidEqualsWithBooleanExpressionsSource() {
        List<String> booleanExpressions = List.of("true", "(1 == 1)", "self!isDefined()");
        List<String> operators = List.of("==", "!=");
        List<String> expressions = new ArrayList<>();
        for (String left : booleanExpressions) {
            for (String right : booleanExpressions) {
                for (String operator : operators) {
                    expressions.add(String.format("%s %s %s", left, operator, right));
                }
            }
        }
        return expressions.stream();
    }

    @ParameterizedTest
    @MethodSource("testValidEqualsWithBooleanExpressionsSource")
    public void testValidEqualsWithBooleanExpressions(String expression) {
        EClass school = findBase("School");
        assertThat(school, notNullValue());
        assertDoesNotThrow(() -> createExpression(school, expression));
    }

    private static Stream<String> testInvalidEqualsWithBooleanExpressionsSource() {
        List<String> expressions = new ArrayList<>();
        for (String expression : List.of("1", "'apple'", "schools::Student!count()")) {
            for (String operator : List.of("==", "!=")) {
                expressions.add(String.format("true %s %s", operator, expression));
                expressions.add(String.format("%s %s true", expression, operator));
            }
        }
        return expressions.stream();
    }

    @ParameterizedTest
    @MethodSource("testInvalidEqualsWithBooleanExpressionsSource")
    public void testInvalidEqualsWithBooleanExpressions(String expression) {
        assertThrows(UnsupportedOperationException.class, () -> createExpression(expression));
    }

    private static Stream<String> testUsingNonFQNamesSource() {
        return Stream.of(
                "Person",
                "Person!any()",
                "Person!filter(e | Person!any().height == e.height)",
                "Person!filter(Person | schools::Person!any().height == Person.height)",
                "Person!any()!asType(Student)",
                "Person!any()!kindOf(Student)",
                "Person!any()!typeOf(Student)",
                "Person!asCollection(Student)",
                "Class!any()!container(School)"
        );
    }

    @ParameterizedTest
    @MethodSource("testUsingNonFQNamesSource")
    public void testUsingNonFQNames(final String script) {
        EClass person = findBase("Person");
        assertThat(person, notNullValue());

        assertDoesNotThrow(() -> createExpression(person, script));
    }

    private static Stream<Entry<String, String>> testInvalidUsingNonFQNamesSource() {
        return Stream.of(
                Map.entry("person", "Unknown symbol: person"),
                Map.entry("person!any()", "Unknown symbol: person"),
                Map.entry("Person!filter(e | person!any().height == e.height)", "Unknown symbol: person"),
                Map.entry("Person!any()!asType(student)", "Type not found: schools::student"),
                Map.entry("Person!any()!kindOf(student)", "Type not found: schools::student"),
                Map.entry("Person!any()!typeOf(student)", "Type not found: schools::student"),
                Map.entry("Person!asCollection(student)", "Type not found: schools::student"),
                Map.entry("Class!any()!container(student)", "Type not found: schools::student"),
                Map.entry("Person!filter(e | schools::student!any().height == e.height)", "Type not found: schools::student")
        );
    }

    @ParameterizedTest
    @MethodSource("testInvalidUsingNonFQNamesSource")
    public void testInvalidUsingNonFQNames(final Entry<String, String> scriptEntry) {
        EClass person = findBase("Person");
        assertThat(person, notNullValue());

        Exception exception = assertThrows(Exception.class, () -> createExpression(person, scriptEntry.getKey()));
        assertThat(exception.getMessage(), containsString(scriptEntry.getValue()));
    }

    @Test
    public void testAttributeSelector() {
        assertDoesNotThrow(() -> createExpression("schools::Person!any().height"));
        JqlExpressionBuildException exception =
                assertThrows(JqlExpressionBuildException.class, () -> createExpression("schools::Person.height"));
        assertThat(exception.getMessage(), containsString(INVALID_ATTRIBUTE_SELECTOR));
    }

}

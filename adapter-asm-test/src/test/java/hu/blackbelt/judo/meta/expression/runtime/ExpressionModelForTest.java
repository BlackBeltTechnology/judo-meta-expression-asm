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

import static hu.blackbelt.judo.meta.expression.collection.util.builder.CollectionBuilders.*;
import static hu.blackbelt.judo.meta.expression.custom.util.builder.CustomBuilders.*;
import static hu.blackbelt.judo.meta.expression.constant.util.builder.ConstantBuilders.*;
import static hu.blackbelt.judo.meta.expression.temporal.util.builder.TemporalBuilders.*;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.*;
import static hu.blackbelt.judo.meta.expression.logical.util.builder.LogicalBuilders.*;
import static hu.blackbelt.judo.meta.expression.numeric.util.builder.NumericBuilders.*;
import static hu.blackbelt.judo.meta.expression.enumeration.util.builder.EnumerationBuilders.*;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.*;
import static hu.blackbelt.judo.meta.expression.object.util.builder.ObjectBuilders.newObjectVariableReferenceBuilder;
import static hu.blackbelt.judo.meta.expression.string.util.builder.StringBuilders.newStringAttributeBuilder;
import static hu.blackbelt.judo.meta.expression.temporal.util.builder.TemporalBuilders.newTimestampAttributeBuilder;
import static hu.blackbelt.judo.meta.expression.util.builder.ExpressionBuilders.newTypeNameBuilder;

import java.math.BigInteger;
import java.time.OffsetDateTime;

import org.eclipse.emf.common.util.URI;

import java.math.BigDecimal;
import java.time.ZoneOffset;

import hu.blackbelt.judo.meta.expression.StringExpression;
import hu.blackbelt.judo.meta.expression.TypeName;
import hu.blackbelt.judo.meta.expression.collection.CollectionFilterExpression;
import hu.blackbelt.judo.meta.expression.collection.CollectionNavigationFromObjectExpression;
import hu.blackbelt.judo.meta.expression.collection.SortExpression;
import hu.blackbelt.judo.meta.expression.constant.Instance;
import hu.blackbelt.judo.meta.expression.logical.ContainsExpression;
import hu.blackbelt.judo.meta.expression.logical.DecimalComparison;
import hu.blackbelt.judo.meta.expression.logical.KleeneExpression;
import hu.blackbelt.judo.meta.expression.numeric.DecimalAggregatedExpression;
import hu.blackbelt.judo.meta.expression.numeric.DecimalArithmeticExpression;
import hu.blackbelt.judo.meta.expression.numeric.DecimalAttribute;
import hu.blackbelt.judo.meta.expression.numeric.DecimalSwitchExpression;
import hu.blackbelt.judo.meta.expression.numeric.IntegerAttribute;
import hu.blackbelt.judo.meta.expression.object.ContainerExpression;
import hu.blackbelt.judo.meta.expression.object.ObjectNavigationExpression;
import hu.blackbelt.judo.meta.expression.operator.DecimalAggregator;
import hu.blackbelt.judo.meta.expression.operator.DecimalOperator;
import hu.blackbelt.judo.meta.expression.operator.LogicalOperator;
import hu.blackbelt.judo.meta.expression.operator.NumericComparator;
import hu.blackbelt.judo.meta.expression.operator.ObjectComparator;
import hu.blackbelt.judo.meta.expression.operator.StringComparator;
import hu.blackbelt.judo.meta.expression.operator.TemporalOperator;
import hu.blackbelt.judo.meta.expression.string.StringAttribute;
import hu.blackbelt.judo.meta.expression.support.ExpressionModelResourceSupport;
import hu.blackbelt.judo.meta.expression.temporal.TimestampAdditionExpression;
import hu.blackbelt.judo.meta.expression.temporal.TimestampAttribute;
import hu.blackbelt.judo.meta.expression.temporal.TimestampDifferenceExpression;
import hu.blackbelt.judo.meta.expression.variable.ObjectVariable;

public class ExpressionModelForTest {

    public static ExpressionModel createExpressionModel() {

        final ExpressionModelResourceSupport expressionModelResourceSupport = ExpressionModelResourceSupport.expressionModelResourceSupportBuilder()
                .uri(URI.createURI("expression:test"))
                .build();

        final TypeName order = newTypeNameBuilder().withNamespace("demo::entities").withName("Order").build();
        final TypeName customerType = newTypeNameBuilder().withNamespace("demo::entities").withName("Customer").build();
        final TypeName orderDetail = newTypeNameBuilder().withNamespace("demo::entities").withName("OrderDetail").build();
        final TypeName intAddress = newTypeNameBuilder().withNamespace("demo::entities").withName("InternationalAddress").build();
        final TypeName intOrder = newTypeNameBuilder().withNamespace("demo::entities").withName("InternationalOrder").build();
        final TypeName product = newTypeNameBuilder().withNamespace("demo::entities").withName("Product").build();
        final TypeName category = newTypeNameBuilder().withNamespace("demo::entities").withName("Category").build();
        final TypeName employee = newTypeNameBuilder().withNamespace("demo::entities").withName("Employee").build();

        expressionModelResourceSupport.addContent(customerType);
        expressionModelResourceSupport.addContent(order);
        expressionModelResourceSupport.addContent(orderDetail);
        expressionModelResourceSupport.addContent(intAddress);
        expressionModelResourceSupport.addContent(intOrder);
        expressionModelResourceSupport.addContent(product);
        expressionModelResourceSupport.addContent(category);
        expressionModelResourceSupport.addContent(employee);

        Instance oIterator = newInstanceBuilder().withName("oIterator").withElementName(order).build();
        Instance pIterator = newInstanceBuilder().withName("pIterator").withElementName(product).build();
        Instance ioIterator = newInstanceBuilder().withName("ioIterator").withElementName(intOrder).build();

        ContainsExpression condition = newContainsExpressionBuilder()
        .withCollectionExpression(newObjectNavigationFromCollectionExpressionBuilder().withReferenceName("product")
                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("orderDetails")
                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(oIterator).build()).build()).build())
        .withObjectExpression(newObjectSelectorExpressionBuilder()
                .withCollectionExpression(newSortExpressionBuilder()
                        .withCollectionExpression(newCollectionFilterExpressionBuilder()
                                .withCollectionExpression(newImmutableCollectionBuilder().withElementName(product)
                                        .withIteratorVariable(pIterator).build())
                                .withCondition(newStringComparisonBuilder().withOperator(StringComparator.EQUAL)
                                        .withLeft(newStringAttributeBuilder().withAttributeName("productName")
                                                .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(pIterator).build()).build())
                                        .withRight(newStringConstantBuilder().withValue("Lenovo B51").build())
                                        .build())
                                .build())
                        .build())
                .build())
        .build();

        Instance ioOrderDetailsIterator = newInstanceBuilder().withName("ioOrderDetailsIterator").withElementName(orderDetail).build();

        ObjectNavigationExpression customer = newObjectNavigationExpressionBuilder().withReferenceName("customer")
                .withIteratorVariable(newInstanceBuilder().withName("c").withElementName(customerType).build())
                .withObjectExpression(newObjectSelectorExpressionBuilder()
                        .withCollectionExpression(newSortExpressionBuilder()
                                .withCollectionExpression(newCollectionFilterExpressionBuilder()
                                        .withCollectionExpression(newCastCollectionBuilder().withElementName(intOrder)
                                                .withIteratorVariable(ioIterator)
                                                .withCollectionExpression(newCollectionFilterExpressionBuilder()
                                                        .withCollectionExpression(newImmutableCollectionBuilder().withElementName(order)
                                                                .withIteratorVariable(oIterator).build())
                                                        .withCondition(condition).build()).build())
                                        .withCondition(newDecimalComparisonBuilder().withOperator(NumericComparator.GREATER_THAN)
                                                .withLeft(newDecimalAttributeBuilder().withAttributeName("exciseTax")
                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(oIterator).build()).build())
                                                .withRight(newDecimalArithmeticExpressionBuilder()
                                                        .withLeft(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.DIVIDE)
                                                                .withLeft(newIntegerConstantBuilder().withValue(BigInteger.valueOf(2)).build())
                                                                .withRight(newIntegerConstantBuilder().withValue(BigInteger.valueOf(1)).build())
                                                                .build())
                                                        .withRight(newDecimalAggregatedExpressionBuilder().withOperator(DecimalAggregator.SUM)
                                                                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("orderDetails")
                                                                        .withIteratorVariable(ioOrderDetailsIterator)
                                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(ioIterator).build()).build())
                                                                .withExpression(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.MULTIPLY)
                                                                        .withLeft(newDecimalAttributeBuilder().withAttributeName("unitPrice")
                                                                                .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(ioOrderDetailsIterator).build()).build())
                                                                        .withRight(newIntegerAttributeBuilder().withAttributeName("quantity")
                                                                                .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(ioOrderDetailsIterator).build()).build())
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .withOrderBy(newOrderByItemBuilder().withExpression(newDecimalAttributeBuilder().withAttributeName("freight")
                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(ioIterator).build())
                                        .build()))
                                .withOrderBy(newOrderByItemBuilder().withDescending(true)
                                        .withExpression(newCountExpressionBuilder()
                                                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder()
                                                        .withReferenceName("orderDetails")
                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(ioIterator).build()).build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        CollectionNavigationFromObjectExpression addresses = newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("addresses")
                .withIteratorVariable(newInstanceBuilder().withName("c").withElementName(customerType).build())
                .withObjectExpression(newObjectFilterExpressionBuilder()
                        .withObjectExpression(customer)
                        .withCondition(newKleeneExpressionBuilder().withOperator(LogicalOperator.AND)
                                .withLeft(newMatchesBuilder()
                                        .withExpression(newStringAttributeBuilder().withAttributeName("postalCode")
                                                .withObjectExpression(newObjectSelectorExpressionBuilder()
                                                        .withCollectionExpression(newSortExpressionBuilder()
                                                                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("addresses")
                                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(customer.getIteratorVariable()).build())
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .withPattern(newStringConstantBuilder().withValue("11%").build())
                                        .build())
                                .withRight(newEnumerationComparisonBuilder().withOperator(ObjectComparator.EQUAL)
                                        .withLeft(newEnumerationAttributeBuilder().withAttributeName("country")
                                                .withObjectExpression(newCastObjectBuilder().withElementName(intAddress)
                                                        .withObjectExpression(newObjectSelectorExpressionBuilder()
                                                                .withCollectionExpression(newSortExpressionBuilder()
                                                                        .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("addresses")
                                                                                .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(customer.getIteratorVariable()).build())
                                                                                .build())
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .withRight(newLiteralBuilder().withValue("RO").build())
                                        .build())
                                .build())
                        .build())
                .build();

        KleeneExpression and = newKleeneExpressionBuilder().withOperator(LogicalOperator.AND)
                .withLeft(newDecimalComparisonBuilder()
                        .withLeft(newIntegerRoundExpressionBuilder()
                                .withExpression(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(-1.5)).build())
                                .build())
                        .withRight(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(1.2)).build())
                        .build())
                .withRight(newInstanceOfExpressionBuilder().withElementName(intOrder)
                        .withObjectExpression(newObjectSelectorExpressionBuilder()
                                .withCollectionExpression(newSortExpressionBuilder()
                                        .withCollectionExpression(newImmutableCollectionBuilder().withElementName(order).build())
                                        .build())
                                .build())
                        .build())
                .build();

        Instance prIterator = newInstanceBuilder().withName("prIterator").withElementName(product).build();

        DecimalSwitchExpression decimalSwitch = newDecimalSwitchExpressionBuilder()
                .withDefaultExpression(newIntegerConstantBuilder().withValue(BigInteger.valueOf(50)).build())
                .withCases(newSwitchCaseBuilder()
                        .withCondition(newBooleanConstantBuilder().withValue(true).build())
                        .withExpression(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(1.2)).build())
                        .build())
                .withCases(newSwitchCaseBuilder()
                        .withCondition(newIntegerComparisonBuilder().withOperator(NumericComparator.GREATER_THAN)
                                .withLeft(newCountExpressionBuilder()
                                        .withCollectionExpression(newCollectionFilterExpressionBuilder()
                                                .withCollectionExpression(newImmutableCollectionBuilder().withElementName(product)
                                                        .withIteratorVariable(prIterator)
                                                        .build())
                                                .withCondition(newLogicalAttributeBuilder().withAttributeName("discounted")
                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(prIterator).build())
                                                        .build())
                                                .build())
                                        .build())
                                .withRight(newIntegerConstantBuilder().withValue(BigInteger.valueOf(10)).build())
                                .build())
                        .withExpression(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(1.2)).build())
                        .build())
                .build();

        Instance catIterator = newInstanceBuilder().withName("catIterator").withElementName(category).build();
        Instance prodIterator = newInstanceBuilder().withName("prodIterator").withElementName(product).build();

        CollectionFilterExpression catCollectionFilter = newCollectionFilterExpressionBuilder()
                .withCollectionExpression(newImmutableCollectionBuilder().withElementName(category)
                        .withIteratorVariable(catIterator).build())
                .withCondition(newIntegerComparisonBuilder().withOperator(NumericComparator.GREATER_THAN)
                        .withLeft(newCountExpressionBuilder()
                                .withCollectionExpression(newCollectionFilterExpressionBuilder()
                                        .withCollectionExpression(newImmutableCollectionBuilder().withElementName(product)
                                                .withIteratorVariable(prodIterator).build())
                                        .withCondition(newObjectComparisonBuilder()
                                                .withLeft(newObjectNavigationExpressionBuilder().withReferenceName("category")
                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(prodIterator).build())
                                                        .build())
                                                .withRight(newObjectVariableReferenceBuilder().withVariable(catIterator).build())
                                                .build())
                                        .build())
                                .build())
                        .withRight(newIntegerConstantBuilder().withValue(BigInteger.valueOf(10)).build())
                        .build())
                .build();

        DecimalArithmeticExpression decimalArithmetic = newDecimalArithmeticExpressionBuilder()
                .withLeft(newDecimalSwitchExpressionBuilder()
                        .withCases(newSwitchCaseBuilder().withCondition(newBooleanConstantBuilder().withValue(true).build())
                                .withExpression(newDecimalArithmeticExpressionBuilder()
                                        .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(8)).withUnitName("dkg").build())
                                        .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(12)).withUnitName("g").build())
                                        .build())
                                .build())
                        .withDefaultExpression(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(2)).withUnitName("g").build())
                        .build())
                .withRight(newDecimalArithmeticExpressionBuilder()
                        .withLeft(newDecimalAttributeBuilder().withAttributeName("weight")
                                .withObjectExpression(newObjectSelectorExpressionBuilder()
                                        .withCollectionExpression(newSortExpressionBuilder()
                                                .withCollectionExpression(newImmutableCollectionBuilder().withElementName(product).build())
                                                .build())
                                        .build())
                                .build())
                        .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(4)).withUnitName("g").build())
                        .build())
                .build();

        DecimalArithmeticExpression decimalArithmetic2 = newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.MULTIPLY)
                .withLeft(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.MULTIPLY)
                        .withLeft(newIntegerArithmeticExpressionBuilder()
                                .withLeft(newIntegerConstantBuilder().withValue(BigInteger.valueOf(2)).build())
                                .withRight(newIntegerConstantBuilder().withValue(BigInteger.valueOf(2)).build())
                                .build())
                        .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(8)).withUnitName("kg").build())
                        .build())
                .withRight(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.DIVIDE)
                        .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(60)).withUnitName("km/h").build())
                        .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(3)).withUnitName("s").build())
                        .build())
                .build();

        DecimalArithmeticExpression decimalArithmetic3 = newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.DIVIDE)
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(9)).withUnitName("mm").build())
                .withRight(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.DIVIDE)
                        .withLeft(newDecimalConstantBuilder().withValue(BigDecimal.valueOf(1)).build())
                        .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(45)).withUnitName("cm").build())
                        .build())
                .build();

        DecimalComparison decimalComp = newDecimalComparisonBuilder()
                .withLeft(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(9)).withUnitName("mg").build())
                .withRight(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(2)).withUnitName("kg").build())
                .build();

        TimestampAdditionExpression tsAddExpr1 = newTimestampAdditionExpressionBuilder()
        		.withTimestamp(newTimestampConstantBuilder().withValue(OffsetDateTime.parse("2019-01-02T03:04:05.678+01:00").atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()).build())
        		.withDuration(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(102)).withUnitName("s").build())
        		.build();
        
        TimestampAdditionExpression tsAddExpr2 = newTimestampAdditionExpressionBuilder().withOperator(TemporalOperator.SUBSTRACT)
                .withTimestamp(newTimestampAttributeBuilder().withAttributeName("orderDate")
                        .withObjectExpression(newObjectSelectorExpressionBuilder()
                                .withCollectionExpression(newSortExpressionBuilder()
                                        .withCollectionExpression(newImmutableCollectionBuilder().withElementName(order).build())
                                        .build())
                                .build())
                        .build())
                .withDuration(newMeasuredDecimalBuilder().withValue(BigDecimal.valueOf(3)).withUnitName("day").build())
                .build();

        TimestampDifferenceExpression tsDiffExpr = newTimestampDifferenceExpressionBuilder()
        		.withStartTimestamp(newTimestampConstantBuilder().withValue(OffsetDateTime.parse("2019-01-02T03:04:05.678+01:00").atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()).build())
        		.withEndTimestamp(newTimestampConstantBuilder().withValue(OffsetDateTime.parse("2019-01-30T15:57:08.123+01:00").atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()).build())
        		.build();
        
        ContainerExpression contExpr = newContainerExpressionBuilder().withElementName(order)
                .withObjectExpression(newInstanceBuilder().withName("self").withElementName(orderDetail).build())
                .build();

        expressionModelResourceSupport.addContent(addresses);
        expressionModelResourceSupport.addContent(and);
        expressionModelResourceSupport.addContent(decimalSwitch);
        expressionModelResourceSupport.addContent(catCollectionFilter);
        expressionModelResourceSupport.addContent(decimalArithmetic);
        expressionModelResourceSupport.addContent(decimalArithmetic2);
        expressionModelResourceSupport.addContent(decimalArithmetic3);
        expressionModelResourceSupport.addContent(decimalComp);
        expressionModelResourceSupport.addContent(tsAddExpr1);
        expressionModelResourceSupport.addContent(tsAddExpr2);
        expressionModelResourceSupport.addContent(tsDiffExpr);
        expressionModelResourceSupport.addContent(contExpr);

        ObjectVariable orderVar = newInstanceBuilder()
                .withElementName(order)
                .withName("self")
                .build();

        TimestampAttribute orderDate = newTimestampAttributeBuilder()
                .withObjectExpression(newObjectVariableReferenceBuilder()
                        .withVariable(orderVar)
                        .build())
                .withAttributeName("orderDate")
                .build();

        StringExpression shipperName = newStringAttributeBuilder()
                .withObjectExpression(newObjectNavigationExpressionBuilder()
                        .withObjectExpression(newObjectVariableReferenceBuilder()
                                .withVariable(orderVar)
                                .build())
                        .withReferenceName("shipper")
                        .build())
                .withAttributeName("companyName")
                .build();

        CollectionNavigationFromObjectExpression orderDetails = newCollectionNavigationFromObjectExpressionBuilder()
                .withObjectExpression(newObjectVariableReferenceBuilder()
                        .withVariable(orderVar)
                        .build())
                .withReferenceName("orderDetails")
                .build();

        expressionModelResourceSupport.addContent(orderVar);
        expressionModelResourceSupport.addContent(orderDate);
        expressionModelResourceSupport.addContent(shipperName);
        expressionModelResourceSupport.addContent(orderDetails);

        Instance eIterator = newInstanceBuilder().withName("eIterator").withElementName(employee).build();
        Instance oIterator2 = newInstanceBuilder().withName("oIterator").withElementName(order).build();

        SortExpression sortExpr = newSortExpressionBuilder()
                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder()
                        .withReferenceName("orders")
                        .withIteratorVariable(oIterator2)
                        .withObjectExpression(newObjectSelectorExpressionBuilder()
                                .withCollectionExpression(newSortExpressionBuilder()
                                        .withCollectionExpression(newCollectionFilterExpressionBuilder()
                                                .withCollectionExpression(
                                                        newImmutableCollectionBuilder().withElementName(employee)
                                                        .withIteratorVariable(eIterator).build())
                                                .withCondition(newKleeneExpressionBuilder().withOperator(LogicalOperator.AND)
                                                        .withLeft(newStringComparisonBuilder().withOperator(StringComparator.EQUAL)
                                                                .withLeft(newStringAttributeBuilder().withAttributeName("lastName")
                                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(eIterator).build()).build())
                                                                .withRight(newStringConstantBuilder().withValue("Gipsz").build())
                                                                .build())
                                                        .withRight(newStringComparisonBuilder().withOperator(StringComparator.EQUAL)
                                                                .withLeft(newStringAttributeBuilder().withAttributeName("firstName")
                                                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(eIterator).build())
                                                                        .build())
                                                                .withRight(newStringConstantBuilder().withValue("Jakab").build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .withOrderBy(newOrderByItemBuilder().withDescending(true)
                        .withExpression(newTimestampAttributeBuilder().withAttributeName("orderDate")
                                .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(oIterator2).build())
                                .build())
                        .build())
                .build();

        TimestampAttribute orderDate2 = newTimestampAttributeBuilder().withAttributeName("orderDate")
                .withObjectExpression(newInstanceBuilder().withElementName(order).build())
                .build();

        TimestampAttribute shippedDate = newTimestampAttributeBuilder().withAttributeName("shippedDate")
                .withObjectExpression(newInstanceBuilder().withElementName(order).build())
                .build();

        StringAttribute companyName = newStringAttributeBuilder().withAttributeName("companyName")
                .withObjectExpression(newObjectNavigationExpressionBuilder().withReferenceName("shipper")
                        .withObjectExpression(newInstanceBuilder().withElementName(order).build())
                        .build())
                .build();

        expressionModelResourceSupport.addContent(orderDate2);
        expressionModelResourceSupport.addContent(shippedDate);
        expressionModelResourceSupport.addContent(companyName);

        Instance odOrderDetailsIterator = newInstanceBuilder().withName("odOrderDetailsIterator").withElementName(orderDetail).build();

        CollectionFilterExpression filterExpr = newCollectionFilterExpressionBuilder()
                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("orderDetails")
                        .withIteratorVariable(odOrderDetailsIterator)
                        .withObjectExpression(newInstanceBuilder().withElementName(order).build()).build())
                .withCondition(newUndefinedComparisonBuilder().withExpression(newCustomAttributeBuilder().withAttributeName("picture")
                        .withObjectExpression(newObjectNavigationExpressionBuilder().withReferenceName("category")
                                .withObjectExpression(newObjectNavigationExpressionBuilder().withReferenceName("product")
                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(odOrderDetailsIterator).build())
                                        .build())
                                .build())
                        .build())
                        .build())
                .build();

        CollectionNavigationFromObjectExpression orderDetails2 = newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("orderDetails")
                .withObjectExpression(newInstanceBuilder().withElementName(order).build())
                .build();

        StringAttribute productName = newStringAttributeBuilder().withAttributeName("productName")
                .withObjectExpression(newObjectNavigationExpressionBuilder().withReferenceName("product")
                        .withObjectExpression(newInstanceBuilder().withElementName(orderDetail).build())
                        .build())
                .build();

        StringAttribute categoryName = newStringAttributeBuilder().withAttributeName("categoryName")
                .withObjectExpression(newObjectNavigationExpressionBuilder().withReferenceName("category")
                        .withObjectExpression(newObjectNavigationExpressionBuilder().withReferenceName("product")
                                .withObjectExpression(newInstanceBuilder().withElementName(orderDetail).build()).build())
                        .build())
                .build();

        expressionModelResourceSupport.addContent(filterExpr);
        expressionModelResourceSupport.addContent(orderDetails2);
        expressionModelResourceSupport.addContent(productName);
        expressionModelResourceSupport.addContent(categoryName);

        DecimalAttribute unitPrice = newDecimalAttributeBuilder().withAttributeName("unitPrice")
                .withObjectExpression(newInstanceBuilder().withElementName(orderDetail).build()).build();
        IntegerAttribute quantity = newIntegerAttributeBuilder().withAttributeName("quantity")
                .withObjectExpression(newInstanceBuilder().withElementName(orderDetail).build()).build();
        DecimalAttribute discount = newDecimalAttributeBuilder().withAttributeName("discount")
                .withObjectExpression(newInstanceBuilder().withElementName(orderDetail).build()).build();

        expressionModelResourceSupport.addContent(unitPrice);
        expressionModelResourceSupport.addContent(quantity);
        expressionModelResourceSupport.addContent(discount);

        Instance odOrderDetailsIterator2 = newInstanceBuilder().withName("odOrderDetailsIterator2").withElementName(orderDetail).build();
        Instance odOrderDetailsIterator3 = newInstanceBuilder().withName("odOrderDetailsIterator3").withElementName(orderDetail).build();

        DecimalAggregatedExpression decimalAggrExpr = newDecimalAggregatedExpressionBuilder().withOperator(DecimalAggregator.SUM)
                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("orderDetails")
                        .withIteratorVariable(odOrderDetailsIterator2)
                        .withObjectExpression(newInstanceBuilder().withElementName(order).build()).build())
                .withExpression(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.MULTIPLY)
                        .withLeft(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.MULTIPLY)
                                .withLeft(newIntegerAttributeBuilder().withAttributeName("quantity")
                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(odOrderDetailsIterator2).build())
                                        .build())
                                .withRight(newDecimalAttributeBuilder().withAttributeName("unitPrice")
                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(odOrderDetailsIterator2).build())
                                        .build())
                                .build())
                        .withRight(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.SUBSTRACT)
                                .withLeft(newIntegerConstantBuilder().withValue(BigInteger.valueOf(1)).build())
                                .withRight(newDecimalAttributeBuilder().withAttributeName("discount")
                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(odOrderDetailsIterator2).build())
                                        .build())
                                .build())
                        .build())
                .build();

        DecimalAggregatedExpression decimalAggrExpr2 = newDecimalAggregatedExpressionBuilder().withOperator(DecimalAggregator.SUM)
                .withCollectionExpression(newCollectionNavigationFromObjectExpressionBuilder().withReferenceName("orderDetails")
                        .withIteratorVariable(odOrderDetailsIterator3)
                        .withObjectExpression(newInstanceBuilder().withElementName(order).build()).build())
                .withExpression(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.MULTIPLY)
                        .withLeft(newDecimalArithmeticExpressionBuilder().withOperator(DecimalOperator.MULTIPLY)
                                .withLeft(newIntegerAttributeBuilder().withAttributeName("quantity")
                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(odOrderDetailsIterator3).build())
                                        .build())
                                .withRight(newDecimalAttributeBuilder().withAttributeName("unitPrice")
                                        .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(odOrderDetailsIterator3).build())
                                        .build())
                                .build())
                        .withRight(newDecimalAttributeBuilder().withAttributeName("discount")
                                .withObjectExpression(newObjectVariableReferenceBuilder().withVariable(odOrderDetailsIterator2).build())
                                .build())
                        .build())
                .build();

        expressionModelResourceSupport.addContent(sortExpr);
        expressionModelResourceSupport.addContent(decimalAggrExpr);
        expressionModelResourceSupport.addContent(decimalAggrExpr2);

        ExpressionModel expressionModel = ExpressionModel.buildExpressionModel()
                .name("expression")
                .expressionModelResourceSupport(expressionModelResourceSupport)
                .build();

        return expressionModel;
    }
}

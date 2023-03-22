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
import hu.blackbelt.epsilon.runtime.execution.exceptions.EvlScriptExecutionException;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import hu.blackbelt.epsilon.runtime.execution.impl.Slf4jLog;
import hu.blackbelt.judo.meta.asm.runtime.*;
import hu.blackbelt.judo.meta.expression.adapters.asm.AsmModelAdapter;
import hu.blackbelt.judo.meta.measure.*;
import hu.blackbelt.judo.meta.measure.runtime.MeasureEpsilonValidator;
import hu.blackbelt.judo.meta.measure.runtime.MeasureModel;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;

import java.math.BigDecimal;
import java.util.Collections;

import static hu.blackbelt.judo.meta.asm.runtime.AsmModel.buildAsmModel;
import static hu.blackbelt.judo.meta.measure.runtime.MeasureModel.buildMeasureModel;
import static hu.blackbelt.judo.meta.measure.util.builder.MeasureBuilders.*;
import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class ExecutionContextOnAsmTest {
    
    AsmModel asmModel;
    MeasureModel measureModel;
    AsmModelAdapter modelAdapter;
    AsmUtils asmUtils;
    
    protected EDataType doubleType;
    protected EDataType integerType;
    
    void setUp() throws Exception {
        asmModel = buildAsmModel()
                .uri(URI.createURI("urn:asm.judo-meta-asm"))
                .build();
        
        asmUtils = new AsmUtils(asmModel.getResourceSet());
        populateAsmModel();

        measureModel = buildMeasureModel()
                .name(asmModel.getName())
                .build();
        
        populateMeasureModel();
        
        assertTrue(asmModel.isValid(), () -> { log.info(asmModel.getDiagnosticsAsString()); return "ASM model is invalid"; });
        assertTrue(measureModel.isValid(), () -> { log.info(measureModel.getDiagnosticsAsString()); return "Measure model is invalid"; });
        runEpsilonOnMeasure();
        runEpsilonOnAsm();
        
        modelAdapter = new AsmModelAdapter(asmModel.getResourceSet(), measureModel.getResourceSet());
    }
    
    protected void populateAsmModel() {
        //enum
        EEnum countriesEnum = newEEnumBuilder().withName("Countries").withELiterals(newEEnumLiteralBuilder().withLiteral("HU").withName("HU").withValue(0).build(),
                newEEnumLiteralBuilder().withLiteral("AT").withName("AT").withValue(1).build(),
                newEEnumLiteralBuilder().withLiteral("RO").withName("RO").withValue(2).build(),
                newEEnumLiteralBuilder().withLiteral("SK").withName("SK").withValue(3).build()).build();
        
        //types
        EDataType timestamp = newEDataTypeBuilder().withName("Timestamp").withInstanceClassName("java.time.LocalDateTime").build();
        EDataType time = newEDataTypeBuilder().withName("Time").withInstanceClassName("java.time.LocalTime").build();
        EDataType stringType = newEDataTypeBuilder().withName("String").withInstanceClassName("java.lang.String").build();
        doubleType = newEDataTypeBuilder().withName("Double").withInstanceClassName("java.lang.Double").build();
        integerType = newEDataTypeBuilder().withName("Integer").withInstanceClassName("java.lang.Integer").build();
        EDataType binary = newEDataTypeBuilder().withName("Binary").withInstanceClassName("java.lang.Object").build();
        EDataType timeStoredInMonths = newEDataTypeBuilder().withName("TimeStoredInMonths").withInstanceClassName("java.lang.Integer").build();
        EDataType timeStoredInSeconds = newEDataTypeBuilder().withName("TimeStoredInSeconds").withInstanceClassName("java.lang.Double").build();
        EDataType dateType = newEDataTypeBuilder().withName("Date").withInstanceClassName("java.time.LocalDate").build();
        EDataType phoneType = newEDataTypeBuilder().withName("Phone").withInstanceClassName("java.lang.String").build();
        EDataType booleanType = newEDataTypeBuilder().withName("Boolean").withInstanceClassName("java.lang.Boolean").build();
        EDataType massStoredInKilograms = newEDataTypeBuilder().withName("MassStoredInKilograms").withInstanceClassName("java.lang.Double").build();
        
        //attributes
        EAttribute orderDate = newEAttributeBuilder().withName("orderDate").withEType(timestamp).build();
        EAttribute deliveryFrom = newEAttributeBuilder().withName("deliveryFrom").withEType(time).build();
        EAttribute deliveryTo = newEAttributeBuilder().withName("deliveryTo").withEType(time).build();
        EAttribute orderDateAnnotated = newEAttributeBuilder().withName("orderDate").withEType(timestamp).build();
        EAttribute companyName = newEAttributeBuilder().withName("companyName").withEType(stringType).build();
        EAttribute exciseTax = newEAttributeBuilder().withName("exciseTax").withEType(doubleType).build();
        EAttribute customsDescription = newEAttributeBuilder().withName("customsDescription").withEType(stringType).build();
        EAttribute productName = newEAttributeBuilder().withName("productName").withEType(stringType).build();
        EAttribute productNameForOrderDetail = newEAttributeBuilder().withName("productName")
                .withDerived(true).withEType(stringType).build();
        EAttribute unitPrice = newEAttributeBuilder().withName("unitPrice").withEType(doubleType).build();
        EAttribute categoryName = newEAttributeBuilder().withName("categoryName").withEType(stringType).build();
        EAttribute unitPriceOrderDetail = newEAttributeBuilder().withName("unitPrice").withEType(doubleType).build();
        EAttribute quantity = newEAttributeBuilder().withName("quantity").withEType(integerType).build();
        EAttribute discount = newEAttributeBuilder().withName("discount").withEType(doubleType).build();
        EAttribute country = newEAttributeBuilder().withName("country").withEType(countriesEnum).build();
        EAttribute picture = newEAttributeBuilder().withName("picture").withEType(binary).build();
        EAttribute quantityPerUnit = newEAttributeBuilder().withName("quantityPerUnit").withEType(integerType).build();
        EAttribute firstName = newEAttributeBuilder().withName("firstName").withEType(stringType).build();
        EAttribute firstNameEmployee = newEAttributeBuilder().withName("firstName").withEType(stringType).build();
        EAttribute lastNameEmployee = newEAttributeBuilder().withName("lastName").withEType(stringType).build();
        EAttribute phone = newEAttributeBuilder().withName("phone").withEType(phoneType).build();
        EAttribute discounted = newEAttributeBuilder().withName("discounted").withEType(booleanType).build();
        EAttribute weight = newEAttributeBuilder().withName("weight").withEType(massStoredInKilograms).build();
        EAttribute freight = newEAttributeBuilder().withName("freight").withEType(doubleType).build();
        EAttribute price = newEAttributeBuilder().withName("price").withEType(doubleType).build();
        EAttribute postalCode = newEAttributeBuilder().withName("postalCode").withEType(phoneType).build();
        EAttribute shipperName = newEAttributeBuilder().withName("shipperName").withEType(stringType).withDerived(true).build();
        EAttribute shipperNameMapped = newEAttributeBuilder().withName("shipperName").withEType(stringType).build();
        EAttribute totalNumberOfOrders = newEAttributeBuilder().withName("totalNumberOfOrders").withEType(integerType).build();

        EOperation getAllOrders = newEOperationBuilder().withName("getAllOrders").build();

        //relations
        EReference orderDetails = newEReferenceBuilder().withName("orderDetails").withContainment(true).withLowerBound(0).withUpperBound(-1).build();
        EReference productRef = newEReferenceBuilder().withName("product").withLowerBound(1).withUpperBound(1).build();
        EReference categoryRef = newEReferenceBuilder().withName("category").withLowerBound(1).withUpperBound(1).build();
        EReference productsRef = newEReferenceBuilder().withName("products").withLowerBound(0).withUpperBound(-1).build();
        EReference categories = newEReferenceBuilder().withName("categories").withLowerBound(0).withUpperBound(-1).withDerived(true).build();
        EReference ordersRef = newEReferenceBuilder().withName("orders").withLowerBound(0).withUpperBound(-1).build();
        EReference employeeRef = newEReferenceBuilder().withName("employee").withLowerBound(0).withUpperBound(1).build();
        EReference shipperOrdersRef = newEReferenceBuilder().withName("shipperOrders").withLowerBound(0).withUpperBound(-1).build();
        EReference shipperRef = newEReferenceBuilder().withName("shipper").withLowerBound(0).withUpperBound(1).build();
        EReference ordersCustomer = newEReferenceBuilder().withName("orders").withLowerBound(0).withUpperBound(-1).build();
        EReference addressesCustomer = newEReferenceBuilder().withName("addresses").withLowerBound(0).withUpperBound(-1)
                .withContainment(true).build();
        EReference customerOrder = newEReferenceBuilder().withName("customer").withLowerBound(0).withUpperBound(1).build();
        EReference owner = newEReferenceBuilder().withName("owner").withLowerBound(0).withUpperBound(1).build();
        EReference categoryEmployee = newEReferenceBuilder().withName("category").withLowerBound(0).withUpperBound(-1).build();
        EReference territoryRef = newEReferenceBuilder().withName("territory").withLowerBound(0).withUpperBound(1).build();
        EReference shipperTerritory = newEReferenceBuilder().withName("shipper").withLowerBound(0).withUpperBound(1).build();
        EReference shipAddress = newEReferenceBuilder().withName("shipAddress").withLowerBound(0).withUpperBound(1).build();
        EReference items = newEReferenceBuilder().withName("items").build();
        EReference ordersAssignedToEmployee = newEReferenceBuilder().withName("ordersAssignedToEmployee").withDerived(true)
                .withLowerBound(0).withUpperBound(-1).build();
        EReference cheapestCategoryProductCategory = newEReferenceBuilder().withName("cheapestCategoryProductCategory")
                .withDerived(true).withLowerBound(0).withUpperBound(1).build();

        
        //classes
        EClass order = newEClassBuilder().withName("Order")
                .withEStructuralFeatures(orderDate, deliveryFrom, deliveryTo, orderDetails, categories, employeeRef, shipperRef, customerOrder, shipAddress, freight, shipperName).build();
        EClass orderDetail = newEClassBuilder().withName("OrderDetail").withEStructuralFeatures(productNameForOrderDetail,productRef,unitPriceOrderDetail,quantity,discount,price).build();
        EClass product = newEClassBuilder().withName("Product").withEStructuralFeatures(categoryRef,productName,unitPrice,quantityPerUnit,discounted,weight).build();
        EClass category = newEClassBuilder().withName("Category").withEStructuralFeatures(productsRef, categoryName, picture, owner, cheapestCategoryProductCategory).build();
        EClass employee = newEClassBuilder().withName("Employee").withEStructuralFeatures(ordersRef,categoryEmployee,firstNameEmployee,lastNameEmployee).build();
        EClass internationalOrder = newEClassBuilder().withName("InternationalOrder").withEStructuralFeatures(exciseTax,customsDescription)
                .withESuperTypes(order).build();
        EClass customer = newEClassBuilder().withName("Customer").withEStructuralFeatures(ordersCustomer,addressesCustomer).build();
        EClass address = newEClassBuilder().withName("Address").withEStructuralFeatures(postalCode).build();
        EClass internationalAddress = newEClassBuilder().withName("InternationalAddress")
                .withESuperTypes(address).withEStructuralFeatures(country).build();
        EClass company = newEClassBuilder().withName("Company").withESuperTypes(customer).build();
        EClass shipper = newEClassBuilder().withName("Shipper").withEStructuralFeatures(companyName,shipperOrdersRef,phone,territoryRef)
                .withESuperTypes(company).build();
        EClass onlineOrder = newEClassBuilder().withName("OnlineOrder")
                .withESuperTypes(order).build();
        EClass individual = newEClassBuilder().withName("Individual").withEStructuralFeatures(firstName)
                .withESuperTypes(customer).build();
        EClass supplier = newEClassBuilder().withName("Supplier")
                .withESuperTypes(company).build();
        EClass territory = newEClassBuilder().withName("Territory").withEStructuralFeatures(shipperTerritory).build();
        
        EClass orderInfo = newEClassBuilder().withName("OrderInfo").withEStructuralFeatures(orderDateAnnotated,items).build();
        EClass orderItem = newEClassBuilder().withName("OrderItem").build();
        EClass productInfo = newEClassBuilder().withName("ProductInfo").build();
        EClass internationalOrderInfo = newEClassBuilder().withName("InternationalOrderInfo").withEStructuralFeatures(shipperNameMapped).build();
        
        EClass __static = newEClassBuilder().withName("__Static").withEStructuralFeatures(totalNumberOfOrders).build();
        EClass unboundServices = newEClassBuilder().withName("__UnboundServices").withEOperations(getAllOrders).build();
        EClass internalAP = newEClassBuilder().withName("InternalAP").withEStructuralFeatures(ordersAssignedToEmployee).build();
        
        //set types of relations
        useEReference(orderDetails).withEType(orderDetail).build();
        useEReference(productRef).withEType(product).build();
        useEReference(categoryRef).withEType(category).withEOpposite(productsRef).build();
        useEReference(productsRef).withEType(product).withEOpposite(categoryRef).build();
        useEReference(categories).withEType(category).build();
        useEReference(ordersRef).withEType(order).withEOpposite(employeeRef).build();
        useEReference(employeeRef).withEType(employee).withEOpposite(ordersRef).build();
        useEReference(shipperOrdersRef).withEType(order).withEOpposite(shipperRef).build();
        useEReference(shipperRef).withEType(shipper).withEOpposite(shipperOrdersRef).build();
        useEReference(addressesCustomer).withEType(address).build();
        useEReference(ordersCustomer).withEType(order).withEOpposite(customerOrder).build();
        useEReference(customerOrder).withEType(customer).withEOpposite(ordersCustomer).build();
        useEReference(owner).withEType(employee).withEOpposite(categoryEmployee).build();
        useEReference(categoryEmployee).withEType(category).withEOpposite(owner).build();
        useEReference(shipperTerritory).withEType(shipper).withEOpposite(territoryRef).build();
        useEReference(territoryRef).withEType(territory).withEOpposite(shipperTerritory).build();
        useEReference(shipAddress).withEType(address).build();
        useEReference(items).withEType(orderItem).build();
        useEReference(ordersAssignedToEmployee).withEType(order).build();
        useEReference(cheapestCategoryProductCategory).withEType(category).build();

        
        //packages
        EPackage demo = newEPackageBuilder().withName("demo").withNsURI("http://blackbelt.hu/judo/northwind/northwind/demo")
                .withNsPrefix("runtimenorthwindNorthwindDemo").build();
        EPackage services = newEPackageBuilder().withName("services").withNsURI("http://blackbelt.hu/judo/northwind/northwind/services")
                .withEClassifiers(orderInfo,orderItem,internationalOrderInfo,__static,unboundServices,internalAP,productInfo).withNsPrefix("runtimenorthwindNorthwindServices").build();
        EPackage entities = newEPackageBuilder().withName("entities")
                .withEClassifiers(order,
                        orderDetail,product,category,employee,
                        shipper,internationalOrder,customer,address,
                        internationalAddress,company,onlineOrder,individual,supplier,territory)
                .withNsURI("http://blackbelt.hu/judo/northwind/northwind/entities")
                .withNsPrefix("runtimenorthwindNorthwindEntities").build();
        EPackage types = newEPackageBuilder().withName("types")
                .withEClassifiers(timestamp,time,stringType,doubleType,integerType,binary,dateType,countriesEnum,phoneType,booleanType)
                .withNsURI("http://blackbelt.hu/judo/northwind/northwind/types")
                .withNsPrefix("runtimenorthwindNorthwindTypes").build();
        EPackage measured = newEPackageBuilder().withName("measured").withEClassifiers(timeStoredInMonths,timeStoredInSeconds,massStoredInKilograms)
                .withNsURI("http://blackbelt.hu/judo/northwind/demo/types/measured")
                .withNsPrefix("runtimenorthwindDemoTypesMeasured").build();
        EPackage measures = newEPackageBuilder().withName("measures")
                .withNsURI("http://blackbelt.hu/judo/northwind/demo/measures")
                .withNsPrefix("runtimenorthwindDemoMeasures").build();
        
        //packages again
        useEPackage(demo).withESubpackages(services,entities,types,measures).build();
        useEPackage(types).withESubpackages(measured).build();
        
        asmModel.addContent(demo);
        
        //annotations
        EAnnotation orderAnnotation = AsmUtils.getExtensionAnnotationByName(order, "entity", true).get();
        orderAnnotation.getDetails().put("value", "true");
        EAnnotation orderDetailAnnotation = AsmUtils.getExtensionAnnotationByName(orderDetail, "entity", true).get();
        orderDetailAnnotation.getDetails().put("value", "true");
        EAnnotation productAnnotation = AsmUtils.getExtensionAnnotationByName(product, "entity", true).get();
        productAnnotation.getDetails().put("value", "true");
        EAnnotation categoryAnnotation = AsmUtils.getExtensionAnnotationByName(category, "entity", true).get();
        categoryAnnotation.getDetails().put("value", "true");
        EAnnotation employeeAnnotation = AsmUtils.getExtensionAnnotationByName(employee, "entity", true).get();
        employeeAnnotation.getDetails().put("value", "true");
        EAnnotation shipperAnnotation = AsmUtils.getExtensionAnnotationByName(shipper, "entity", true).get();
        shipperAnnotation.getDetails().put("value", "true");
        EAnnotation intOrderAnnotation = AsmUtils.getExtensionAnnotationByName(internationalOrder, "entity", true).get();
        intOrderAnnotation.getDetails().put("value", "true");
        EAnnotation addressAnnotation = AsmUtils.getExtensionAnnotationByName(address, "entity", true).get();
        addressAnnotation.getDetails().put("value", "true");
        EAnnotation customerAnnotation = AsmUtils.getExtensionAnnotationByName(customer, "entity", true).get();
        customerAnnotation.getDetails().put("value", "true");
        EAnnotation intAddrAnnotation = AsmUtils.getExtensionAnnotationByName(internationalAddress, "entity", true).get();
        intAddrAnnotation.getDetails().put("value", "true");
        EAnnotation companyAnnotation = AsmUtils.getExtensionAnnotationByName(company, "entity", true).get();
        companyAnnotation.getDetails().put("value", "true");
        EAnnotation onlineOrderAnnotation = AsmUtils.getExtensionAnnotationByName(onlineOrder, "entity", true).get();
        onlineOrderAnnotation.getDetails().put("value", "true");
        EAnnotation individaulAnnotation = AsmUtils.getExtensionAnnotationByName(individual, "entity", true).get();
        individaulAnnotation.getDetails().put("value", "true");
        EAnnotation supplierAnnotation = AsmUtils.getExtensionAnnotationByName(supplier, "entity", true).get();
        supplierAnnotation.getDetails().put("value", "true");
        EAnnotation territoryAnnotation = AsmUtils.getExtensionAnnotationByName(territory, "entity", true).get();
        territoryAnnotation.getDetails().put("value", "true");
        EAnnotation weightAnnotation = AsmUtils.getExtensionAnnotationByName(weight, "constraints", true).get();
        weightAnnotation.getDetails().put("precision", "15");
        weightAnnotation.getDetails().put("scale", "4");
        weightAnnotation.getDetails().put("measure", "demo.measures.Mass");
        weightAnnotation.getDetails().put("unit", "kilogram");
        EAnnotation attributeAnnotation = AsmUtils.getExtensionAnnotationByName(orderDateAnnotated, "binding", true).get();
        attributeAnnotation.getDetails().put("value", orderDate.getName());
        EAnnotation itemsAnnotation = AsmUtils.getExtensionAnnotationByName(items, "binding", true).get();
        itemsAnnotation.getDetails().put("value", orderDetails.getName());
        
        EAnnotation annotationOrderInfo = AsmUtils.getExtensionAnnotationByName(orderInfo, "mappedEntityType", true).get();
        annotationOrderInfo.getDetails().put("value", AsmUtils.getClassifierFQName(order));
        EAnnotation annotationOrderItem = AsmUtils.getExtensionAnnotationByName(orderItem, "mappedEntityType", true).get();
        annotationOrderItem.getDetails().put("value", AsmUtils.getClassifierFQName(orderDetail));
        EAnnotation annotationInternationalOrderInfo = AsmUtils.getExtensionAnnotationByName(internationalOrderInfo, "mappedEntityType", true).get();
        annotationInternationalOrderInfo.getDetails().put("value", AsmUtils.getClassifierFQName(internationalOrder));
        EAnnotation annotationProductInfo = AsmUtils.getExtensionAnnotationByName(productInfo, "mappedEntityType", true).get();
        annotationProductInfo.getDetails().put("value", AsmUtils.getClassifierFQName(product));
        EAnnotation apAnnotation = AsmUtils.getExtensionAnnotationByName(internalAP, "accessPoint", true).get();
        apAnnotation.getDetails().put("value", "true");
        EAnnotation shipperNameAnnotation = AsmUtils.getExtensionAnnotationByName(shipperNameMapped, "binding", true).get();
        shipperNameAnnotation.getDetails().put("value", shipperName.getName());
        EAnnotation shipperNameMappedConstraintAnnotation = AsmUtils.getExtensionAnnotationByName(shipperNameMapped, "constraints", true).get();
        shipperNameMappedConstraintAnnotation.getDetails().put("maxLength", "255");
        EAnnotation operationAnnotation = AsmUtils.getExtensionAnnotationByName(getAllOrders, "exposedBy", true).get();
        operationAnnotation.getDetails().put("value", AsmUtils.getClassifierFQName(internalAP));
        
        EAnnotation getterAnnotationForOrdersAssignedToEmployee = AsmUtils.getExtensionAnnotationByName(ordersAssignedToEmployee, "expression", true).get();
        getterAnnotationForOrdersAssignedToEmployee.getDetails().put("getter", "demo::entities::Employee.orders");
        getterAnnotationForOrdersAssignedToEmployee.getDetails().put("getter.dialect", "JQL");
        
        EAnnotation getterAnnotationForShipperName = AsmUtils.getExtensionAnnotationByName(shipperName, "expression", true).get();
        getterAnnotationForShipperName.getDetails().put("getter", "self.shipper.companyName");
        getterAnnotationForShipperName.getDetails().put("getter.dialect", "JQL");
        
        EAnnotation getterAnnotationForCategories = AsmUtils.getExtensionAnnotationByName(categories, "expression", true).get();
        getterAnnotationForCategories.getDetails().put("getter", "self.orderDetails.product.category");
        getterAnnotationForCategories.getDetails().put("getter.dialect", "JQL");
        
        EAnnotation getterAnnotationForProductName = AsmUtils.getExtensionAnnotationByName(productNameForOrderDetail, "expression", true).get();
        getterAnnotationForProductName.getDetails().put("getter", "self.product.productName");
        getterAnnotationForProductName.getDetails().put("getter.dialect", "JQL");
        
        EAnnotation getterAnnotationForCheapestCategoryProduct = AsmUtils.getExtensionAnnotationByName(cheapestCategoryProductCategory, "expression", true).get();
        getterAnnotationForCheapestCategoryProduct.getDetails().put("getter", "self.products!head(p | p.unitPrice).category");
        getterAnnotationForCheapestCategoryProduct.getDetails().put("getter.dialect", "JQL");

    }
    
    protected void populateMeasureModel() {
        
        BaseMeasure time = newBaseMeasureBuilder().withName("Time").withNamespace("demo::measures").withUnits(
                newDurationUnitBuilder().withName("nanosecond").withSymbol("ns").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0E+9)).withType(DurationType.NANOSECOND).build(),
                newDurationUnitBuilder().withName("microsecond").withSymbol("μs").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1000000.0)).withType(DurationType.MICROSECOND).build(),
                newDurationUnitBuilder().withName("millisecond").withSymbol("ms").withRateDividend(new BigDecimal(0.001)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.MILLISECOND).build(),
                newDurationUnitBuilder().withName("second").withSymbol("s").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.SECOND).build(),
                newDurationUnitBuilder().withName("minute").withSymbol("min").withRateDividend(new BigDecimal(60.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                newDurationUnitBuilder().withName("hour").withSymbol("h").withRateDividend(new BigDecimal(3600.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.HOUR).build(),
                newDurationUnitBuilder().withName("day").withSymbol("").withRateDividend(new BigDecimal(86400.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.DAY).build(),
                newDurationUnitBuilder().withName("week").withSymbol("").withRateDividend(new BigDecimal(604800.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.WEEK).build(),
                newDurationUnitBuilder().withName("halfDay").withSymbol("").withRateDividend(new BigDecimal(43200.0)).withRateDivisor(new BigDecimal(1.0)).build())
            .build();
        
        BaseMeasure monthBasedTime = newBaseMeasureBuilder().withName("MonthBasedTime").withNamespace("demo::measures").withUnits(
                newDurationUnitBuilder().withName("month").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.MONTH).build(),
                newDurationUnitBuilder().withName("year").withRateDividend(new BigDecimal(12.0)).withRateDivisor(new BigDecimal(1.0)).withType(DurationType.YEAR).build())
            .build();
        
        BaseMeasure mass = newBaseMeasureBuilder().withName("Mass").withNamespace("demo::measures").withUnits(
                newUnitBuilder().withName("milligram").withSymbol("mg").withRateDividend(new BigDecimal(0.0000010)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("gram").withSymbol("g").withRateDividend(new BigDecimal(0.001)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("dekagram").withSymbol("dkg").withRateDividend(new BigDecimal(0.01)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("kilogram").withSymbol("kg").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("quintal").withSymbol("q").withRateDividend(new BigDecimal(100.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("tonne").withSymbol("t").withRateDividend(new BigDecimal(1000.0)).withRateDivisor(new BigDecimal(1.0)).build())
            .build();
        
        BaseMeasure length = newBaseMeasureBuilder().withName("Length").withNamespace("demo::measures").withUnits(
                newUnitBuilder().withName("nanometre").withSymbol("nm").withRateDividend(new BigDecimal(1.0E-9)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("micrometre").withSymbol("μm").withRateDividend(new BigDecimal(0.0000010)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("millimetre").withSymbol("mm").withRateDividend(new BigDecimal(0.001)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("centimetre").withSymbol("cm").withRateDividend(new BigDecimal(0.01)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("decimetre").withSymbol("dm").withRateDividend(new BigDecimal(0.1)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("metre").withSymbol("m").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("kilometre").withSymbol("km").withRateDividend(new BigDecimal(1000.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("inch").withSymbol("&quot;").withRateDividend(new BigDecimal(0.0254)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("foot").withSymbol("ft").withRateDividend(new BigDecimal(0.3048)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("mile").withSymbol("mi").withRateDividend(new BigDecimal(1609.344)).withRateDivisor(new BigDecimal(1.0)).build())
            .build();
        
        DerivedMeasure velocity = newDerivedMeasureBuilder().withName("Velocity").withNamespace("demo::measures").withUnits(
                newUnitBuilder().withName("kilometrePerHour").withSymbol("km/h").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(3.6)).build(),
                newUnitBuilder().withName("metrePerSecond").withSymbol("m/s").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(1).withBaseMeasure(length).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(-1).withBaseMeasure(time).build())
            .build();
        
        DerivedMeasure area = newDerivedMeasureBuilder().withName("Area").withNamespace("demo::measures").withUnits(
                newUnitBuilder().withName("squareMillimetre").withSymbol("mm²").withRateDividend(new BigDecimal(0.0000010)).withRateDivisor(new BigDecimal(3.6)).build(),
                newUnitBuilder().withName("squareCentimetre").withSymbol("cm²").withRateDividend(new BigDecimal(0.00010)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("squareDecimetre").withSymbol("dm²").withRateDividend(new BigDecimal(0.01)).withRateDivisor(new BigDecimal(3.6)).build(),
                newUnitBuilder().withName("squareMetre").withSymbol("m²").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("hectare").withSymbol("ha").withRateDividend(new BigDecimal(10000.0)).withRateDivisor(new BigDecimal(1.0)).build(),
                newUnitBuilder().withName("squareKilometre").withSymbol("km²").withRateDividend(new BigDecimal(1000000.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(2).withBaseMeasure(length).build())
            .build();
        
        DerivedMeasure force = newDerivedMeasureBuilder().withName("Force").withNamespace("demo::measures").withUnits(
                newUnitBuilder().withName("newton").withSymbol("N").withRateDividend(new BigDecimal(1.0)).withRateDivisor(new BigDecimal(1.0)).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(-2).withBaseMeasure(time).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(1).withBaseMeasure(mass).build())
                .withTerms(newBaseMeasureTermBuilder().withExponent(1).withBaseMeasure(length).build())
            .build();
        
        measureModel.addContent(time);
        measureModel.addContent(mass);
        measureModel.addContent(length);
        measureModel.addContent(velocity);
        measureModel.addContent(area);
        measureModel.addContent(force);
        measureModel.addContent(monthBasedTime);
    }
    
    private void runEpsilonOnMeasure() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            MeasureEpsilonValidator.validateMeasure(bufferedLog,
                    measureModel,
                    MeasureEpsilonValidator.calculateMeasureValidationScriptURI(),
                    Collections.emptyList(),
                    Collections.emptyList());
        } catch (EvlScriptExecutionException ex) {
            log.error("EVL failed", ex);
            log.error("\u001B[31m - unexpected errors: {}\u001B[0m", ex.getUnexpectedErrors());
            log.error("\u001B[33m - unexpected warnings: {}\u001B[0m", ex.getUnexpectedWarnings());
            throw ex;
        }
    }
    
    private void runEpsilonOnAsm() throws Exception {
        try (Log bufferedLog = new BufferedSlf4jLogger(log)) {
            AsmEpsilonValidator.validateAsm(bufferedLog,
                    asmModel,
                    AsmEpsilonValidator.calculateAsmValidationScriptURI(),
                    Collections.emptyList(),
                    Collections.emptyList());
        } catch (EvlScriptExecutionException ex) {
            log.error("EVL failed", ex);
            log.error("\u001B[31m - unexpected errors: {}\u001B[0m", ex.getUnexpectedErrors());
            log.error("\u001B[33m - unexpected warnings: {}\u001B[0m", ex.getUnexpectedWarnings());
            throw ex;
        }
    }
}

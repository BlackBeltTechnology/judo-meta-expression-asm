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

import static org.eclipse.emf.ecore.util.builder.EcoreBuilders.newEAttributeBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import hu.blackbelt.judo.meta.expression.ExecutionContextOnAsmTest;

public class AsmMeasureSupportTest extends ExecutionContextOnAsmTest {

    private AsmModelAdapter modelAdapter;
    private EClass product;
    private EDataType doubleType;

    @BeforeEach
    public void setUp() throws Exception {
    	super.setUp();
        modelAdapter = new AsmModelAdapter(asmModel.getResourceSet(), measureModel.getResourceSet());
        
        product = asmUtils.all(EClass.class).filter(c -> c.getName().equals("Product")).findAny().get();
        doubleType = asmUtils.all(EDataType.class).filter(t -> t.getName().equals("Double")).findAny().get();
    }

    @AfterEach
    public void tearDown() {
        measureModel = null;
        product = null;
        modelAdapter = null;
    }

    @Test
    public void testGetUnitOfNonMeasuredAttribute() {
        assertFalse(modelAdapter.getUnit(product, "discount").isPresent());
    }

    @Test
    public void testGetUnitOfMeasuredAttribute() {
        assertTrue(modelAdapter.getUnit(product, "weight").isPresent());
        assertTrue(modelAdapter.getUnit(product, "height").isPresent());
    }

    @Test
    public void testGetUnitOfAttributeWithUnknownUnit() {
        product.getEStructuralFeatures().addAll(ImmutableList.of(
                newEAttributeBuilder().withName("vat").withEType(doubleType).build(),
                newEAttributeBuilder().withName("netWeight").withEType(doubleType).build(),
                newEAttributeBuilder().withName("grossWeight").withEType(doubleType).build(),
                newEAttributeBuilder().withName("width").withEType(doubleType).build()
        ));

        asmUtils.addExtensionAnnotationDetails(product.getEStructuralFeature("vat"), "constraints", ImmutableMap.of("unit", "EUR"));
        asmUtils.addExtensionAnnotationDetails(product.getEStructuralFeature("netWeight"), "constraints", ImmutableMap.of("unit", "kg", "measure", "demo::measures.Length"));
        asmUtils.addExtensionAnnotationDetails(product.getEStructuralFeature("grossWeight"), "constraints", ImmutableMap.of("unit", "kg", "measure", "Length"));
        asmUtils.addExtensionAnnotationDetails(product.getEStructuralFeature("width"), "constraints", ImmutableMap.of("unit", "m", "measure", "measures::Length"));

        assertFalse(modelAdapter.getUnit(product, "vat").isPresent());          // EUR is not defined as unit
        assertFalse(modelAdapter.getUnit(product, "netWeight").isPresent());    // unit belongs to another measure
        assertFalse(modelAdapter.getUnit(product, "grossWeight").isPresent());  // measure name is not matching expected pattern
        assertFalse(modelAdapter.getUnit(product, "width").isPresent());        // measure name is invalid
    }

    @Test
    public void testGetUnitOfNonNumericAttribute() {
        assertFalse(modelAdapter.getUnit(product, "url").isPresent());          // attribute is not numeric
    }

    @Test
    public void getGetUnitOfNonExistingAttribute() {
        assertFalse(modelAdapter.getUnit(product, "width").isPresent());        // attribute is not defined
        assertFalse(modelAdapter.getUnit(product, "unitPrice").isPresent());    // annotation is added without 'unit' key
    }
}

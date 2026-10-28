package hu.blackbelt.judo.meta.expression.adapters.asm.validation;

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

import hu.blackbelt.judo.meta.expression.ExecutionContextOnAsmTest;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModel;
import hu.blackbelt.judo.meta.expression.runtime.ExpressionModelForTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Performance comparison test for EVL vs Java (Zeta) validation.
 *
 * <p>This test compares the execution time of both validators on the same model.
 * It is tagged with "performance" so it can be excluded from regular test runs.</p>
 *
 * <p>Run with: {@code mvn test -Dgroups=performance}</p>
 *
 * <p>Or enable via environment variable: {@code PERFORMANCE_TESTS=true mvn test}</p>
 */
@Slf4j
@Tag("performance")
@EnabledIfEnvironmentVariable(named = "PERFORMANCE_TESTS", matches = "true", disabledReason = "Performance tests disabled by default")
public class ExpressionAsmValidationPerformanceTest extends AbstractExpressionAsmValidationTest {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASURED_ITERATIONS = 10;

    private final ExecutionContextOnAsmTest context = new ExecutionContextOnAsmTest();

    @BeforeEach
    public void setUp() throws Exception {
        context.setUp();
        setModelAdapter(context.asmModel, context.measureModel);
        expressionModel = ExpressionModelForTest.createExpressionModel();
        log.info(expressionModel.getDiagnosticsAsString());
        assertTrue(expressionModel.isValid());
    }

    @Test
    void compareValidatorPerformance() throws Exception {
        assumeTrue(isJavaValidatorAvailable(), "Java validator not available, skipping performance comparison");

        log.info("=== Performance Comparison: EVL vs Java (Zeta) Validation ===");
        log.info("Warmup iterations: {}, Measured iterations: {}", WARMUP_ITERATIONS, MEASURED_ITERATIONS);

        // Warmup EVL
        log.info("Warming up EVL validator...");
        this.validatorType = ValidatorType.EVL;
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runValidation();
        }

        // Warmup Java
        log.info("Warming up Java validator...");
        this.validatorType = ValidatorType.JAVA;
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runValidation();
        }

        // Measure EVL
        log.info("Measuring EVL validator...");
        this.validatorType = ValidatorType.EVL;
        long evlStart = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            runValidation();
        }
        long evlEnd = System.nanoTime();
        double evlAvgMs = (evlEnd - evlStart) / 1_000_000.0 / MEASURED_ITERATIONS;

        // Measure Java
        log.info("Measuring Java validator...");
        this.validatorType = ValidatorType.JAVA;
        long javaStart = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            runValidation();
        }
        long javaEnd = System.nanoTime();
        double javaAvgMs = (javaEnd - javaStart) / 1_000_000.0 / MEASURED_ITERATIONS;

        // Report results
        log.info("=== Results ===");
        log.info("EVL average:  {:.2f} ms", evlAvgMs);
        log.info("Java average: {:.2f} ms", javaAvgMs);
        log.info("Speedup: {:.2f}x", evlAvgMs / javaAvgMs);

        // Java should generally be faster or comparable
        // We don't assert this as it's just informational
        log.info("Performance test completed successfully");
    }

    @Test
    void evlValidationBenchmark() throws Exception {
        log.info("=== EVL Validation Benchmark ===");

        this.validatorType = ValidatorType.EVL;

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runValidation();
        }

        // Measure
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            long start = System.nanoTime();
            runValidation();
            long elapsed = System.nanoTime() - start;
            totalTime += elapsed;
            minTime = Math.min(minTime, elapsed);
            maxTime = Math.max(maxTime, elapsed);
        }

        double avgMs = totalTime / 1_000_000.0 / MEASURED_ITERATIONS;
        double minMs = minTime / 1_000_000.0;
        double maxMs = maxTime / 1_000_000.0;

        log.info("EVL Validation - Avg: {:.2f} ms, Min: {:.2f} ms, Max: {:.2f} ms", avgMs, minMs, maxMs);
    }

    @Test
    void javaValidationBenchmark() throws Exception {
        assumeTrue(isJavaValidatorAvailable(), "Java validator not available");

        log.info("=== Java (Zeta) Validation Benchmark ===");

        this.validatorType = ValidatorType.JAVA;

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            runValidation();
        }

        // Measure
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;

        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            long start = System.nanoTime();
            runValidation();
            long elapsed = System.nanoTime() - start;
            totalTime += elapsed;
            minTime = Math.min(minTime, elapsed);
            maxTime = Math.max(maxTime, elapsed);
        }

        double avgMs = totalTime / 1_000_000.0 / MEASURED_ITERATIONS;
        double minMs = minTime / 1_000_000.0;
        double maxMs = maxTime / 1_000_000.0;

        log.info("Java Validation - Avg: {:.2f} ms, Min: {:.2f} ms, Max: {:.2f} ms", avgMs, minMs, maxMs);
    }
}

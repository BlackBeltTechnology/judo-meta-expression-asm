# Change: Add Zeta Dual Validation Framework Integration

## Why
The judo-meta-expression-asm module currently relies on EVL (Epsilon Validation Language) for expression validation via the ExpressionValidatorOnAsm class. To align with the judo-meta-esm module pattern and leverage the performance benefits of Java-based validation, we need to integrate the Zeta validation framework to run both EVL and Java validation in parallel. This enables:
- Faster validation execution through parallel Java validation
- Consistent validation architecture across JUDO metamodel projects
- Performance testing and benchmarking between EVL and Java validators

## What Changes
- Add judo-zeta dependency (SNAPSHOT version) to pom.xml with proper version property
- Extend ExpressionValidatorOnAsm to support dual validation (EVL + Java)
- Create parameterized tests in adapter-asm-test that run both validation engines
- Add Zeta bundles to OSGi/Karaf integration tests (osgi-itest)
- Create performance tests with synthetic models matching rackinspect characteristics (~70 entities, ~1000 attributes, ~400 relations)
- Update documentation to reference Zeta validation framework
- Convert AsciiDoc files (except pages/) to Markdown with PlantUML to Mermaid diagrams

## Impact
- Affected specs: zeta-adapter-integration (new capability)
- Affected code:
  - `pom.xml` - Add judo-zeta-version property and dependencies
  - `adapter-asm/pom.xml` - Add Zeta dependencies
  - `adapter-asm-test/pom.xml` - Add Zeta test dependencies
  - `adapter-asm/src/main/java/.../ExpressionValidatorOnAsm.java` - Extend for dual validation
  - `adapter-asm-test/src/test/java/` - New parameterized tests
  - `osgi-itest/src/test/resources/test-features.xml` - Add Zeta bundles
  - `README.adoc` - Convert to README.md
  - `CONTRIBUTING.adoc` - Convert to CONTRIBUTING.md
  - `docs/` - Update documentation references

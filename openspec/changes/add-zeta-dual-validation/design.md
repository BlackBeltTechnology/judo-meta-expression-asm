## Context

The judo-meta-expression project provides a comprehensive validation framework with both EVL and Java (Zeta) validators. The expression-asm adapter module needs to integrate this dual validation capability following the same pattern established in judo-meta-esm.

**Key Stakeholders:**
- JUDO platform developers using expression validation
- Runtime systems depending on consistent validation behavior
- Performance-sensitive deployments benefiting from Java validation

**Constraints:**
- Must not break existing EVL-only validation usage
- Java validation must produce identical results to EVL
- OSGi/Karaf compatibility required
- Expression validation is delegated to judo-meta-expression module (no new validators here)

## Goals / Non-Goals

**Goals:**
- Integrate Zeta validation framework as a dependency
- Enable dual validation execution (EVL and Java in parallel for testing)
- Create parameterized tests using ValidatorType enum pattern from ESM
- Add performance benchmarks with realistic model sizes
- Ensure OSGi/Karaf deployment works with Zeta bundles
- Modernize documentation format (AsciiDoc to Markdown)

**Non-Goals:**
- Do not implement new validation rules (use existing expression validators)
- Do not copy Zeta framework code (use as dependency)
- Do not modify expression metamodel
- Do not change EVL validation files (they remain authoritative)

## Decisions

### Decision 1: Use Expression Validator as Dependency
**What:** Import and use the ExpressionZetaValidator from judo-meta-expression rather than implementing validators here.

**Why:** The expression-asm module is an adapter layer. All validation rules are defined in judo-meta-expression. This module only needs to wire up the adapter and execute validation.

**Alternatives considered:**
- Copy validators locally: Rejected - violates DRY, creates maintenance burden
- Create ASM-specific validators: Rejected - validation is expression-model level, not ASM-specific

### Decision 2: Parameterized Test Pattern
**What:** Use JUnit 5 @ParameterizedTest with @EnumSource(ValidatorType.class) following ESM pattern.

**Why:** Ensures both EVL and Java validators are tested with identical test cases and expectations.

**Pattern:**
```java
public enum ValidatorType {
    EVL("EVL (Epsilon Validation Language)"),
    JAVA("Java Validation Framework (Zeta)");
}

@ParameterizedTest(name = "testCase [{0}]")
@EnumSource(ValidatorType.class)
void testCase(ValidatorType type) {
    // Run validation with specified engine
}
```

### Decision 3: Performance Test Model Characteristics
**What:** Generate synthetic ASM models matching rackinspect characteristics:
- 60-100 entity types (EClasses)
- 8-10 attributes per entity
- 5-6 relations per entity
- 20-30% derived expressions
- Mix of containment and reference relations

**Why:** Provides realistic performance benchmarks comparable to production models.

### Decision 4: Documentation Modernization
**What:** Convert AsciiDoc to Markdown, PlantUML to Mermaid.

**Why:** Better tooling support, GitHub rendering, AI assistant compatibility.

**Scope:** All .adoc files except those under /pages directory.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Performance regression in Zeta vs EVL | Performance tests will quantify differences; both engines remain available |
| Breaking existing API | ExpressionValidatorOnAsm maintains backward-compatible methods |
| OSGi resolution issues with Zeta | Test thoroughly in osgi-itest; follow ESM bundle patterns |
| Documentation conversion errors | Manual review of converted diagrams and formatting |

## Migration Plan

1. **Phase 1: Dependencies** - Add Zeta dependencies to all relevant pom.xml files
2. **Phase 2: Adapter Extension** - Extend ExpressionValidatorOnAsm for dual validation
3. **Phase 3: Test Infrastructure** - Create ValidatorType enum and parameterized test base
4. **Phase 4: Test Migration** - Convert existing tests to parameterized format
5. **Phase 5: Performance Tests** - Add benchmark tests with synthetic models
6. **Phase 6: OSGi Integration** - Update test-features.xml with Zeta bundles
7. **Phase 7: Documentation** - Convert AsciiDoc to Markdown

**Rollback:** Each phase can be independently reverted. The existing EVL-only path remains functional throughout.

## Open Questions

None - all clarifications received:
- Use expression model EVL (no EVL files in this project)
- Adapter integration only (no new validators)
- Latest SNAPSHOT for Zeta version
- Extend/integrate ExpressionValidatorOnEsm pattern

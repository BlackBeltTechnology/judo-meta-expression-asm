# osgi-itest Specification

## Purpose

Validates that the adapter-asm and builder-jql-asm OSGi bundles load correctly, resolve their dependencies, and function properly within an Apache Karaf OSGi runtime container.

## Architecture

Integration tests use the Pax Exam framework with JUnit 4 compatibility layer to:

1. Boot an Apache Karaf container
2. Install the project's OSGi bundles and their transitive dependencies
3. Run test methods inside the OSGi container, verifying bundle activation and service availability

## Requirements

### Requirement: Bundle Activation

All project OSGi bundles SHALL activate successfully in an Apache Karaf runtime environment.

#### Scenario: Adapter bundle loads
- **GIVEN** an Apache Karaf container with all required dependency bundles installed
- **WHEN** the `hu.blackbelt.judo.meta.expression.model.adapter.asm` bundle is installed
- **THEN** the bundle reaches ACTIVE state without errors

#### Scenario: Builder bundle loads
- **GIVEN** an Apache Karaf container with all required dependency bundles installed (including the adapter bundle)
- **WHEN** the `hu.blackbelt.judo.meta.expression.builder.jql.asm` bundle is installed
- **THEN** the bundle reaches ACTIVE state without errors

### Requirement: Package Wiring

All `Import-Package` declarations in bundle manifests SHALL resolve correctly against the installed bundles in the Karaf container.

#### Scenario: Adapter imports resolve
- **GIVEN** the adapter-asm bundle installed in Karaf
- **WHEN** the OSGi resolver wires the bundle's imports
- **THEN** all packages listed in `adapter-asm/META-INF/MANIFEST.MF` Import-Package are satisfied

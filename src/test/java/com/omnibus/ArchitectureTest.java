package com.Omnibus;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing Hexagonal Architecture dependency rules:
 * - domain → nothing (no Spring, no adapter, no infrastructure)
 * - application → domain only
 * - adapter → application + domain (not infrastructure directly)
 */
@Tag("unit")
@AnalyzeClasses(packages = "com.Omnibus", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // ---- Domain must not depend on anything outside domain ----
    @ArchTest
    static final ArchRule domain_should_not_depend_on_application =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_use_spring =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..");

    // ---- Application must not depend on adapters or infrastructure ----
    @ArchTest
    static final ArchRule application_should_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..");

    // ---- Adapters must not depend on infrastructure ----
    @ArchTest
    static final ArchRule adapter_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..adapter..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");
}

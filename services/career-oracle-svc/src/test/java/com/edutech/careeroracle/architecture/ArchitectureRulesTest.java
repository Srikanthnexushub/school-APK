package com.edutech.careeroracle.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.careeroracle");
    }

    @Test
    void domainShouldNotDependOnApplication() {
        // Check only domain.model, domain.event, domain.service, domain.port.out
        // domain.port.in is intentionally excluded — it bridges domain and application
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "..domain.model..",
                        "..domain.event..",
                        "..domain.service..",
                        "..domain.port.out.."
                )
                .should().dependOnClassesThat()
                .resideInAPackage("..application..");

        rule.check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");

        rule.check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");

        rule.check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnApi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..api..");

        rule.check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnApi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..api..");

        rule.check(importedClasses);
    }
}

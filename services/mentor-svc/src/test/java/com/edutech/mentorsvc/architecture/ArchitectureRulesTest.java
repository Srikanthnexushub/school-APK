package com.edutech.mentorsvc.architecture;

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
    static void loadClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.mentorsvc");
    }

    @Test
    void domainShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.edutech.mentorsvc.domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.edutech.mentorsvc.infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnApplication() {
        // Check only domain.model, domain.event, domain.service, domain.port.out
        // domain.port.in is intentionally excluded — it bridges domain and application
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "com.edutech.mentorsvc.domain.model",
                        "com.edutech.mentorsvc.domain.event",
                        "com.edutech.mentorsvc.domain.port.out"
                )
                .should().dependOnClassesThat()
                .resideInAPackage("com.edutech.mentorsvc.application..");
        rule.check(importedClasses);
    }

    @Test
    void domainShouldNotDependOnApi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.edutech.mentorsvc.domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.edutech.mentorsvc.api..");
        rule.check(importedClasses);
    }

    @Test
    void apiShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.edutech.mentorsvc.api..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.edutech.mentorsvc.infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    void infrastructurePersistenceAdaptersShouldImplementOutPorts() {
        // The predicate evaluates the INTERFACE class — check if the interface resides in domain.port.out
        ArchRule rule = classes()
                .that().resideInAPackage("com.edutech.mentorsvc.infrastructure.persistence")
                .and().haveSimpleNameEndingWith("PersistenceAdapter")
                .should().implement(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                "a domain port out interface",
                                c -> c.getPackageName()
                                        .startsWith("com.edutech.mentorsvc.domain.port.out")
                        )
                );
        rule.check(importedClasses);
    }
}

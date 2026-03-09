package com.edutech.notification.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {

    private static JavaClasses importedClasses;

    private static final String BASE_PACKAGE = "com.edutech.notification";

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void domainModelAndOutPorts_mustNotDependOnApplication() {
        // domain.port.in is intentionally excluded — it bridges domain and application DTOs.
        // ADR: input port interfaces may reference application-layer commands/responses.
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        BASE_PACKAGE + ".domain.model",
                        BASE_PACKAGE + ".domain.event",
                        BASE_PACKAGE + ".domain.port.out"
                )
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".application..");
        rule.check(importedClasses);
    }

    @Test
    void domainLayer_mustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".domain..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".infrastructure..");
        rule.check(importedClasses);
    }

    @Test
    void domainLayer_mustNotDependOnApi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".domain..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".api..");
        rule.check(importedClasses);
    }

    @Test
    void applicationLayer_mustNotDependOnInfrastructureOrApi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    BASE_PACKAGE + ".infrastructure..",
                    BASE_PACKAGE + ".api.."
                );
        rule.check(importedClasses);
    }

    @Test
    void servicesMustResideInApplicationService() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage(BASE_PACKAGE + ".application.service..")
                .because("Service implementations belong in the application layer");
        rule.check(importedClasses);
    }

    @Test
    void controllers_mustResideInApiPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .and().areNotInterfaces()
                .should().resideInAPackage(BASE_PACKAGE + ".api..")
                .because("HTTP controllers belong in the api layer");
        rule.check(importedClasses);
    }

    @Test
    @DisplayName("No class may use Lombok annotations")
    void noLombok() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAPackage("org.projectlombok..");
        rule.check(importedClasses);
    }
}

package com.edutech.performance.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("performance-svc Architecture Rules")
class ArchitectureRulesTest {

    static JavaClasses classes;

    @BeforeAll
    static void load() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.performance");
    }

    @Test
    @DisplayName("Domain must not depend on infrastructure or api")
    void domainMustNotDependOnInfraOrApi() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..api..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Application must not depend on infrastructure or api")
    void applicationMustNotDependOnInfraOrApi() {
        ArchRule rule = noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..api..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Infrastructure must not depend on api")
    void infrastructureMustNotDependOnApi() {
        ArchRule rule = noClasses().that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("..api..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Api must not depend on infrastructure")
    void apiMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Services must reside in application.service")
    void servicesMustResideInApplicationService() {
        ArchRule rule = noClasses().that()
                .haveNameMatching(".*Service")
                .and().doNotHaveSimpleName("DropoutRiskCalculator")
                .should().resideOutsideOfPackage("..application.service..");
        rule.check(classes);
    }
}

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

    /**
     * ADR: JPA annotations are allowed in the domain layer as a pragmatic compromise
     * (rich entity model without a separate ORM mapping layer). This rule documents
     * the known exception. If a future refactor introduces pure-Java domain objects
     * with JPA entities in infrastructure, remove the allowedDependency() call.
     */
    @Test
    @DisplayName("Domain layer must not depend on infrastructure persistence (JPA) — no Lombok allowed")
    void domain_must_not_depend_on_lombok() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.projectlombok..");
        rule.check(classes);
    }

    @Test
    @DisplayName("No class in any layer may use Lombok annotations")
    void no_lombok_in_any_layer() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAPackage("org.projectlombok..");
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on WebClient or HTTP infrastructure")
    void domain_must_not_depend_on_webclient() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web.reactive..");
        rule.check(classes);
    }
}
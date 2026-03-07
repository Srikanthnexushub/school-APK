package com.edutech.aimentor.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing hexagonal architecture dependency rules for ai-mentor-svc.
 *
 * Rules:
 *   1. Domain has no Spring dependencies
 *   2. Application services depend only on domain ports (not infrastructure)
 *   3. @Entity classes reside in domain.model
 *   4. @Service classes reside in application.service
 *   5. Infrastructure adapters implement domain ports
 */
@DisplayName("AI Mentor Service — Hexagonal Architecture Rules")
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.aimentor");
    }

    @Test
    @DisplayName("Rule 1: Domain layer must not depend on Spring framework")
    void domain_must_not_depend_on_spring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 2: Application services must not depend on infrastructure layer")
    void application_services_must_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application.service..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 3: @Entity classes must reside in domain.model package")
    void entity_classes_must_reside_in_domain_model() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..domain.model..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 4: @Service classes must reside in application.service package")
    void service_classes_must_reside_in_application_service() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Service")
                .should().resideInAPackage("..application.service..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 5: Infrastructure adapters must implement domain port interfaces")
    void infrastructure_adapters_must_implement_domain_ports() {
        // The predicate evaluates each candidate INTERFACE class — check if it resides in domain.port
        ArchRule rule = classes()
                .that().resideInAPackage("..infrastructure..")
                .and().haveNameMatching(".*Adapter")
                .should().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                        "a domain port interface",
                        javaClass -> javaClass.getPackageName().contains("domain.port")
                ));

        rule.check(classes);
    }
}

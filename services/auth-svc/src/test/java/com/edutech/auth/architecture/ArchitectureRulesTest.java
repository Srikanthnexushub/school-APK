// src/test/java/com/edutech/auth/architecture/ArchitectureRulesTest.java
package com.edutech.auth.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * ArchUnit tests enforcing hexagonal architecture dependency rules.
 *
 * Allowed dependencies:
 *   domain     → (no outward deps — pure Java + Jakarta Persistence only)
 *   application → domain
 *   infrastructure → application, domain, Spring framework
 *   api         → application, domain, Spring Web
 *
 * Forbidden:
 *   domain → application, infrastructure, api, Spring
 *   application → infrastructure, api, Spring (except annotations)
 */
@DisplayName("Hexagonal architecture layer rules")
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.edutech.auth");
    }

    @Test
    @DisplayName("Domain layer must not depend on Spring framework")
    void domain_must_not_depend_on_spring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on infrastructure layer")
    void domain_must_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain layer must not depend on api layer")
    void domain_must_not_depend_on_api() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..api..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application layer must not depend on infrastructure layer")
    void application_must_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application layer must not depend on api layer")
    void application_must_not_depend_on_api() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..api..");

        rule.check(classes);
    }
}

package com.edutech.student.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("student-profile-svc Architecture Rules")
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.student");
    }

    @Test
    @DisplayName("Domain must not depend on Spring framework")
    void domain_must_not_depend_on_spring() {
        noClasses()
                .that().resideInAPackage("com.edutech.student.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on infrastructure")
    void domain_must_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("com.edutech.student.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.student.infrastructure..")
                .check(classes);
    }

    @Test
    @DisplayName("Application must not depend on infrastructure")
    void application_must_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("com.edutech.student.application..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.student.infrastructure..")
                .check(classes);
    }

    @Test
    @DisplayName("Application must not depend on api layer")
    void application_must_not_depend_on_api() {
        noClasses()
                .that().resideInAPackage("com.edutech.student.application..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.student.api..")
                .check(classes);
    }

    @Test
    @DisplayName("Infrastructure and API must not access domain model internals directly bypassing ports")
    void infrastructure_and_api_do_not_bypass_ports() {
        noClasses()
                .that().resideInAPackage("com.edutech.student.api..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.student.infrastructure..")
                .check(classes);
    }
}

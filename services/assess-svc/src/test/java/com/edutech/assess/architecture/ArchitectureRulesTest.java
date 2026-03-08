// src/test/java/com/edutech/assess/architecture/ArchitectureRulesTest.java
package com.edutech.assess.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import com.tngtech.archunit.lang.ArchRule;

@DisplayName("assess-svc Architecture Rules")
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.assess");
    }

    @Test
    @DisplayName("Domain must not depend on Spring")
    void domain_must_not_depend_on_spring() {
        noClasses().that().resideInAPackage("com.edutech.assess.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on infrastructure")
    void domain_must_not_depend_on_infrastructure() {
        noClasses().that().resideInAPackage("com.edutech.assess.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.assess.infrastructure..")
                .check(classes);
    }

    @Test
    @DisplayName("Domain must not depend on api")
    void domain_must_not_depend_on_api() {
        noClasses().that().resideInAPackage("com.edutech.assess.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.assess.api..")
                .check(classes);
    }

    @Test
    @DisplayName("Application must not depend on infrastructure")
    void application_must_not_depend_on_infrastructure() {
        noClasses().that().resideInAPackage("com.edutech.assess.application..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.assess.infrastructure..")
                .check(classes);
    }

    @Test
    @DisplayName("Application must not depend on api")
    void application_must_not_depend_on_api() {
        noClasses().that().resideInAPackage("com.edutech.assess.application..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.assess.api..")
                .check(classes);
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
    @DisplayName("Domain layer must not import pgvector or JDBC types")
    void domain_must_not_import_pgvector() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.pgvector..", "java.sql..");
        rule.check(classes);
    }
}
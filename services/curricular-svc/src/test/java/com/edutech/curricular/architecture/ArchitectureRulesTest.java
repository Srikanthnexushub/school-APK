package com.edutech.curricular.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("curricular-svc Architecture Rules")
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.edutech.curricular");
    }

    /**
     * Domain output ports must not depend on Spring.
     * Domain model uses @Entity (JPA) intentionally — that is the codebase pattern.
     */
    @Test
    @DisplayName("Domain output ports must not depend on Spring")
    void domain_ports_out_must_not_depend_on_spring() {
        noClasses().that()
            .resideInAPackage("com.edutech.curricular.domain.port.out..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .check(classes);
    }

    /**
     * Domain must not depend on infrastructure adapters.
     */
    @Test
    @DisplayName("Domain must not depend on infrastructure adapters")
    void domain_must_not_depend_on_infrastructure() {
        noClasses().that()
            .resideInAPackage("com.edutech.curricular.domain..")
            .should().dependOnClassesThat().resideInAPackage("com.edutech.curricular.infrastructure.adapter..")
            .check(classes);
    }

    /**
     * Application services must not depend on infrastructure security config.
     */
    @Test
    @DisplayName("Application must not depend on infrastructure security config")
    void application_must_not_depend_on_security_config() {
        noClasses().that()
            .resideInAPackage("com.edutech.curricular.application.service..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.edutech.curricular.infrastructure.config..")
            .check(classes);
    }
}

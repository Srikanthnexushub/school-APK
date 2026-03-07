package com.edutech.studentgateway.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Student Gateway Architecture Rules")
class ArchitectureRulesTest {

    static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.studentgateway");
    }

    @Test
    @DisplayName("Rule 1: @ConfigurationProperties classes reside in config package")
    void configPropertiesInConfigPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.boot.context.properties.ConfigurationProperties.class)
                .should().resideInAPackage("..config..")
                .because("Configuration properties must live in the config package");
        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 2: GlobalFilter implementations reside in security or filter package")
    void globalFiltersInSecurityOrFilterPackage() {
        ArchRule rule = classes()
                .that().implement(org.springframework.cloud.gateway.filter.GlobalFilter.class)
                .should().resideInAnyPackage("..security..", "..filter..")
                .because("GlobalFilter implementations must live in security or filter package");
        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 3: No JPA annotations anywhere in the gateway")
    void noJpaAnnotations() {
        ArchRule rule = noClasses()
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .because("Student gateway has no database — no JPA entities allowed");
        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 4: No @Service annotation — only @Component or @Configuration allowed")
    void noServiceAnnotation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.edutech.studentgateway..")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .because("Gateway uses @Component and @Configuration, not @Service");
        rule.check(classes);
    }

    @Test
    @DisplayName("Rule 5: Filter classes must not access security internals directly")
    void filterPackageDoesNotAccessSecurityInternals() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..filter..")
                .should().accessClassesThat().resideInAPackage("..security..")
                .because("filter package classes must only use security classes via Spring DI, not direct access");
        rule.check(classes);
    }
}

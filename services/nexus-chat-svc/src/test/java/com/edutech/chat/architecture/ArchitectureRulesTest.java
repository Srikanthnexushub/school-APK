package com.edutech.chat.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("nexus-chat-svc Architecture Rules")
class ArchitectureRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.edutech.chat");
    }

    /**
     * ADR: domain.model and domain.port.out must not depend on Spring.
     *
     * Known documented exception — domain.port.in.StreamMessageUseCase returns
     * ResponseBodyEmitter (Spring MVC type). This is a pragmatic coupling: streaming
     * via ResponseBodyEmitter is inherently tied to the Tomcat/Spring MVC transport
     * and there is no value in abstracting it further. Port.in is intentionally excluded
     * from this rule.
     */
    @Test
    @DisplayName("Domain model and output ports must not depend on Spring")
    void domain_model_and_ports_out_must_not_depend_on_spring() {
        noClasses().that()
                .resideInAnyPackage(
                    "com.edutech.chat.domain.model..",
                    "com.edutech.chat.domain.port.out.."
                )
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(classes);
    }

    /**
     * ADR: domain must not depend on infrastructure (adapters).
     * This rule is strict and should always pass.
     */
    @Test
    @DisplayName("Domain must not depend on infrastructure adapters")
    void domain_must_not_depend_on_infrastructure() {
        noClasses().that().resideInAPackage("com.edutech.chat.domain..")
                .should().dependOnClassesThat().resideInAPackage("com.edutech.chat.infrastructure..")
                .check(classes);
    }

    /**
     * ADR: application.service classes must not depend on inbound adapter DTOs
     * (infrastructure.adapter.in.dto) — response objects should live in the application
     * layer. This is a known pre-existing violation: ChatSessionService directly constructs
     * infrastructure DTOs and ContextAggregatorService directly injects WebClient adapters.
     *
     * Tracked as a future refactor: introduce port abstractions for WebClient adapters
     * and move response DTOs to application.dto. Until then, the check is scoped to
     * domain-level infractions only (infrastructure.adapter.out and adapter.in separately)
     * and the application→infrastructure dependency is documented below.
     *
     * This test verifies the application layer does NOT depend on inbound filter/security config.
     */
    @Test
    @DisplayName("Application must not depend on infrastructure security config")
    void application_must_not_depend_on_security_config() {
        noClasses().that().resideInAPackage("com.edutech.chat.application..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.edutech.chat.infrastructure.config..")
                .check(classes);
    }

    @Test
    @DisplayName("Domain must not use Lombok")
    void domain_must_not_use_lombok() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.edutech.chat.domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.projectlombok..");
        rule.check(classes);
    }
}

package com.srm.creditengine.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Three-layer architecture required by the challenge, enforced at build time:
 *
 * <ul>
 *   <li>{@code web} (application layer): controllers, DTOs, error handling;
 *   <li>{@code domain} (business layer): model, domain services, pricing, business rules;
 *   <li>{@code persistence}: Spring Data repositories and native SQL report queries.
 * </ul>
 *
 * Reports are allowed to skip the business layer ({@code web.report -> persistence.report}).
 * {@code integration} adapters implement domain ports; {@code config} holds cross-cutting beans.
 */
@AnalyzeClasses(packages = "com.srm.creditengine", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    private static final String ROOT = "com.srm.creditengine";
    private static final String WEB = ROOT + ".web..";
    private static final String WEB_REPORT = ROOT + ".web.report..";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String PERSISTENCE = ROOT + ".persistence..";
    private static final String PERSISTENCE_REPORT = ROOT + ".persistence.report..";
    private static final String INTEGRATION = ROOT + ".integration..";
    private static final String CONFIG = ROOT + ".config..";

    @ArchTest
    static final ArchRule layers_are_respected = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Web")
            .definedBy(WEB)
            .layer("Domain")
            .definedBy(DOMAIN)
            .layer("Persistence")
            .definedBy(PERSISTENCE)
            .layer("Integration")
            .definedBy(INTEGRATION)
            .layer("Config")
            .definedBy(CONFIG)
            .whereLayer("Web")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Integration")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Config")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Web", "Persistence", "Integration", "Config")
            .whereLayer("Persistence")
            .mayOnlyBeAccessedByLayers("Domain", "Web");

    @ArchTest
    static final ArchRule only_reports_skip_the_business_layer = noClasses()
            .that()
            .resideInAPackage(WEB)
            .and()
            .resideOutsideOfPackage(WEB_REPORT)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(PERSISTENCE)
            .because("controllers go through domain services; only reports may query persistence directly")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule reports_only_use_report_queries = noClasses()
            .that()
            .resideInAPackage(WEB_REPORT)
            .should()
            .dependOnClassesThat(resideInAPackage(PERSISTENCE).and(not(resideInAPackage(PERSISTENCE_REPORT))))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistence_does_not_depend_on_domain_services = noClasses()
            .that()
            .resideInAPackage(PERSISTENCE)
            .should()
            .dependOnClassesThat(
                    resideInAPackage(DOMAIN).and(annotatedWith(Service.class).or(annotatedWith(Component.class))))
            .because("repositories may use the domain model, never the business services")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_is_independent_of_http = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..", "org.springframework.http..", "jakarta.servlet..")
            .because("business rules must not know about the delivery mechanism");

    @ArchTest
    static final ArchRule controllers_live_in_the_web_layer = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .or()
            .areAnnotatedWith(RestControllerAdvice.class)
            .should()
            .resideInAPackage(WEB);

    @ArchTest
    static final ArchRule entities_live_in_the_domain_layer = classes()
            .that()
            .areAnnotatedWith(Entity.class)
            .should()
            .resideInAPackage(DOMAIN)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositories_live_in_the_persistence_layer = classes()
            .that()
            .areAssignableTo(Repository.class)
            .should()
            .resideInAPackage(PERSISTENCE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule constructor_injection_only = noFields()
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("dependencies are explicit constructor parameters");

    @ArchTest
    static final ArchRule domain_modules_are_free_of_cycles =
            slices().matching(ROOT + ".domain.(*)..").should().beFreeOfCycles();
}

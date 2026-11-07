package com.paulrule.mathapi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.paulrule.mathapi.MathApiApplication;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;

/** Enforces the dependency rules in docs/STANDARDS.md §2. */
@AnalyzeClasses(packages = "com.paulrule.mathapi", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    private static final String DOMAIN = "com.paulrule.mathapi.domain..";
    private static final String APPLICATION = "com.paulrule.mathapi.application..";
    private static final String WEB = "com.paulrule.mathapi.adapter.in.web..";
    private static final String ADAPTER = "com.paulrule.mathapi.adapter..";
    private static final String CONFIG = "com.paulrule.mathapi.config..";

    @ArchTest
    public static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = classes()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("java..", DOMAIN)
            .because("the domain is plain Java (STANDARDS.md §2)");

    @ArchTest
    public static final ArchRule APPLICATION_IS_NOT_WEB_AWARE = noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(ADAPTER, CONFIG, "org.springframework.web..", "org.springframework.http..")
            .because("use cases must not know about adapters or HTTP (STANDARDS.md §2)");

    @ArchTest
    public static final ArchRule CONFIGURATION_ONLY_IN_CONFIG = noClasses()
            .that()
            .resideOutsideOfPackage(CONFIG)
            .and()
            .doNotBelongToAnyOf(MathApiApplication.class)
            .should()
            .beMetaAnnotatedWith(Configuration.class)
            .because("Spring wiring lives in config (STANDARDS.md §2)");

    @ArchTest
    public static final ArchRule LAYERS_POINT_INWARDS = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain")
            .definedBy(DOMAIN)
            .layer("Application")
            .definedBy(APPLICATION)
            .layer("Web")
            .definedBy(WEB)
            .layer("Config")
            .definedBy(CONFIG)
            .whereLayer("Web")
            .mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Web", "Config")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Web", "Config")
            .whereLayer("Config")
            .mayNotBeAccessedByAnyLayer();
}

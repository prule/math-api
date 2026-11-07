package com.paulrule.mathapi.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paulrule.mathapi.domain.fixture.UsesSpring;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Proves the domain rule rejects a framework import; guards against rules that silently match nothing. */
class ArchitectureRulesAreLiveTest {

    @Test
    void domainRuleRejectsSpringImport() {
        var classes = new ClassFileImporter().importClasses(UsesSpring.class);

        assertThatThrownBy(() -> ArchitectureTest.DOMAIN_IS_FRAMEWORK_FREE.check(classes))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("org.springframework.util.StringUtils");
    }
}

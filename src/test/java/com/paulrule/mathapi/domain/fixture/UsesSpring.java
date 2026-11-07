package com.paulrule.mathapi.domain.fixture;

import org.springframework.util.StringUtils;

/** Test-only fixture: a "domain" class that breaks the framework-free rule, so the rule can be shown to bite. */
public class UsesSpring {

    public boolean blank(String s) {
        return !StringUtils.hasText(s);
    }
}

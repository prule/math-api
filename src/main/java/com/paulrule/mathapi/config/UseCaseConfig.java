package com.paulrule.mathapi.config;

import com.paulrule.mathapi.application.PerformCalculation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the framework-free use cases as Spring beans. */
@Configuration
public class UseCaseConfig {

    @Bean
    PerformCalculation performCalculation() {
        return new PerformCalculation();
    }
}

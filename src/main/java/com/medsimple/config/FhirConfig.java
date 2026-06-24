package com.medsimple.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A single shared {@link FhirContext} for R4. Creating a FhirContext is expensive (it scans the
 * resource model), so it must be a singleton — HAPI's own guidance. Used to serialize the lab
 * requisition ServiceRequest to FHIR JSON.
 */
@Configuration
public class FhirConfig {

    @Bean
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}

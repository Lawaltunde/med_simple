package com.medsimple.config;

import com.medsimple.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessRuleViolation_maps_to_409_with_message() {
        ResponseEntity<?> response = handler.handleInvalidState(
                new BusinessRuleException("Episode must be assigned to a physician before prescribing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().toString())
                .contains("INVALID_STATE")
                .contains("must be assigned to a physician");
    }

    @Test
    void unknownRoute_maps_to_404_not_500() {
        ResponseEntity<?> response = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/does-not-exist", "api/v1/does-not-exist"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().toString()).contains("NOT_FOUND");
    }
}

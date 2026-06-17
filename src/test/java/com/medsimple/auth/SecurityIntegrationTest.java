package com.medsimple.auth;

import com.medsimple.auth.jwt.JwtService;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Exercises the real Spring Security filter chain over HTTP against the running app.
// Every assertion is rejected at the filter / method-security / validation layer,
// so none reach the (table-less H2) database.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @LocalServerPort int port;
    @Autowired JwtService jwtService;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private String tokenFor(UserRole role) {
        User user = User.builder()
                .id(UUID.randomUUID()).email(role + "@example.com").role(role)
                .firstName("T").lastName("U").build();
        return jwtService.issueAccessToken(user);
    }

    @Test
    void protectedEndpoint_noToken_returns401() {
        HttpStatusCode status = client().get()
                .uri("/api/v1/users/{id}/notifications", UUID.randomUUID())
                .exchange((req, res) -> res.getStatusCode());
        assertThat(status.value()).isEqualTo(401);
    }

    @Test
    void protectedEndpoint_invalidToken_returns401() {
        HttpStatusCode status = client().get()
                .uri("/api/v1/users/{id}/notifications", UUID.randomUUID())
                .header("Authorization", "Bearer not.a.real.token")
                .exchange((req, res) -> res.getStatusCode());
        assertThat(status.value()).isEqualTo(401);
    }

    @Test
    void physicianOnlyEndpoint_patientToken_returns403() {
        String body = "{\"episodeId\":\"" + UUID.randomUUID() + "\",\"physicianId\":\"" + UUID.randomUUID() + "\"}";
        HttpStatusCode status = client().post()
                .uri("/api/v1/care-plans")
                .header("Authorization", "Bearer " + tokenFor(UserRole.PATIENT))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((req, res) -> res.getStatusCode());
        assertThat(status.value()).isEqualTo(403);
    }

    @Test
    void loginEndpoint_isPublic_reachesValidation() {
        // permitAll -> reaches the controller and fails validation (400), not blocked (401).
        HttpStatusCode status = client().post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .exchange((req, res) -> res.getStatusCode());
        assertThat(status.value()).isEqualTo(400);
    }

    @Test
    void patientAccessingAnotherPatientsEpisodes_returns403() {
        // A valid PATIENT token whose id is not the path patientId -> ownership gate denies
        // before any DB access. This is the IDOR fix exercised through the real filter chain.
        HttpStatusCode status = client().get()
                .uri("/api/v1/patients/{id}/episodes", UUID.randomUUID())
                .header("Authorization", "Bearer " + tokenFor(UserRole.PATIENT))
                .exchange((req, res) -> res.getStatusCode());
        assertThat(status.value()).isEqualTo(403);
    }

    @Test
    void refresh_withoutCookie_returns401() {
        // No refresh cookie present -> rejected before any DB access.
        HttpStatusCode status = client().post()
                .uri("/api/v1/auth/refresh")
                .exchange((req, res) -> res.getStatusCode());
        assertThat(status.value()).isEqualTo(401);
    }

    @Test
    void logout_withoutCookie_returns204AndClearsCookie() {
        client().post()
                .uri("/api/v1/auth/logout")
                .exchange((req, res) -> {
                    assertThat(res.getStatusCode().value()).isEqualTo(204);
                    // Always emits a cleared (max-age 0) refresh cookie.
                    String setCookie = res.getHeaders().getFirst("Set-Cookie");
                    assertThat(setCookie).contains("ms_refresh=").contains("Max-Age=0");
                    return null;
                });
    }

    @Test
    void patientSelfRegistration_isPublic() {
        HttpStatusCode status = client().post()
                .uri("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .exchange((req, res) -> res.getStatusCode());
        assertThat(status.value()).isEqualTo(400);
    }
}

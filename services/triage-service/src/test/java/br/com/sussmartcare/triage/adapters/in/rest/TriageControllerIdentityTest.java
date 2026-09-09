package br.com.sussmartcare.triage.adapters.in.rest;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.triage.application.TriageApplicationService;
import br.com.sussmartcare.triage.domain.ClinicalPriority;
import br.com.sussmartcare.triage.domain.Triage;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TriageControllerIdentityTest {

  @Test
  void decisionUsesAuthenticatedJwtSubjectAsProfessionalId() {

    TriageApplicationService app =
        mock(TriageApplicationService.class);

    TriageController controller =
        new TriageController(app);

    UUID triageId =
        UUID.randomUUID();

    UUID authenticatedProfessionalId =
        UUID.randomUUID();

    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject(
                authenticatedProfessionalId.toString())
            .build();

    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(jwt);

    Triage expected =
        mock(Triage.class);

    when(
        app.confirm(
            triageId,
            ClinicalPriority.HIGH,
            authenticatedProfessionalId))
        .thenReturn(expected);

    Triage actual =
        controller.decision(
            triageId,
            new TriageController.DecisionRequest(
                ClinicalPriority.HIGH),
            authentication);

    assertSame(expected, actual);

    verify(app)
        .confirm(
            triageId,
            ClinicalPriority.HIGH,
            authenticatedProfessionalId);
  }
}
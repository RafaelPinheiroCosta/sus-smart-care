package br.com.sussmartcare.patientregistry.adapters.in.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import br.com.sussmartcare.patientregistry.application.PatientAccessApplicationService;
import br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException;
import br.com.sussmartcare.patientregistry.application.PatientCommunicationProfileApplicationService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PatientCommunicationProfileControllerAuthorizationTest {

  private PatientCommunicationProfileApplicationService app;
  private PatientAccessApplicationService access;
  private PatientCommunicationProfileController controller;

  @BeforeEach
  void setUp() {

    app =
        mock(
            PatientCommunicationProfileApplicationService.class);

    access =
        mock(
            PatientAccessApplicationService.class);

    controller =
        new PatientCommunicationProfileController(
            app,
            access);
  }

  @Test
  void patientCannotReadCommunicationProfileOfUnrelatedPatient() {

    UUID userId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    when(
        access.decide(
            patientId,
            userId,
            Set.of("PATIENT")))
        .thenReturn(
            new PatientAccessApplicationService.AccessDecision(
                false,
                "NO_ACTIVE_RELATIONSHIP"));

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            controller.get(
                patientId,
                authentication(
                    userId,
                    "PATIENT")));

    verify(
        app,
        never())
        .get(patientId);
  }

  @Test
  void selfPatientCanReadOwnCommunicationProfile() {

    UUID userId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    when(
        access.decide(
            patientId,
            userId,
            Set.of("PATIENT")))
        .thenReturn(
            new PatientAccessApplicationService.AccessDecision(
                true,
                "SELF"));

    assertDoesNotThrow(
        () ->
            controller.get(
                patientId,
                authentication(
                    userId,
                    "PATIENT")));

    verify(app)
        .get(patientId);
  }

  private JwtAuthenticationToken authentication(
      UUID userId,
      String... roles) {

    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header(
                "alg",
                "none")
            .subject(
                userId.toString())
            .build();

    var authorities =
        java.util.Arrays
            .stream(roles)
            .map(
                role ->
                    new SimpleGrantedAuthority(
                        "ROLE_" + role))
            .toList();

    return new JwtAuthenticationToken(
        jwt,
        authorities);
  }
}
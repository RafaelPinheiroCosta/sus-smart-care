package br.com.sussmartcare.patientregistry.adapters.in.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.patientregistry.application.PatientAccessApplicationService;
import br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException;
import br.com.sussmartcare.patientregistry.application.PatientCommunicationProfileApplicationService;
import br.com.sussmartcare.patientregistry.domain.QueueCallMode;
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

    app = mock(
        PatientCommunicationProfileApplicationService.class);

    access = mock(
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

    deny(
        patientId,
        userId,
        "PATIENT");

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            controller.get(
                patientId,
                authentication(
                    userId,
                    "PATIENT")));

    verify(app, never())
        .get(patientId);
  }

  @Test
  void selfPatientCanReadOwnCommunicationProfile() {

    UUID userId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    allow(
        patientId,
        userId,
        "PATIENT");

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

  @Test
  void patientCannotChangeCommunicationProfileOfUnrelatedPatient() {

    UUID userId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    deny(
        patientId,
        userId,
        "PATIENT");

    var request =
        new PatientCommunicationProfileController
            .UpdateCommunicationProfileRequest(
                false,
                QueueCallMode.DISPLAY_AND_VERBAL);

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            controller.upsert(
                patientId,
                request,
                authentication(
                    userId,
                    "PATIENT")));

    verify(app, never())
        .upsert(
            patientId,
            false,
            QueueCallMode.DISPLAY_AND_VERBAL);
  }

  @Test
  void selfPatientCanChangeOwnCommunicationProfile() {

    UUID userId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    allow(
        patientId,
        userId,
        "PATIENT");

    var request =
        new PatientCommunicationProfileController
            .UpdateCommunicationProfileRequest(
                false,
                QueueCallMode.DISPLAY_AND_VERBAL);

    assertDoesNotThrow(
        () ->
            controller.upsert(
                patientId,
                request,
                authentication(
                    userId,
                    "PATIENT")));

    verify(app)
        .upsert(
            patientId,
            false,
            QueueCallMode.DISPLAY_AND_VERBAL);
  }

  private void allow(
      UUID patientId,
      UUID userId,
      String role) {

    when(
        access.decide(
            patientId,
            userId,
            Set.of(role)))
        .thenReturn(
            new PatientAccessApplicationService.AccessDecision(
                true,
                "AUTHORIZED"));
  }

  private void deny(
      UUID patientId,
      UUID userId,
      String role) {

    when(
        access.decide(
            patientId,
            userId,
            Set.of(role)))
        .thenReturn(
            new PatientAccessApplicationService.AccessDecision(
                false,
                "NO_ACTIVE_RELATIONSHIP"));
  }

  private JwtAuthenticationToken authentication(
      UUID userId,
      String... roles) {

    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject(userId.toString())
            .build();

    var authorities =
        java.util.Arrays
            .stream(roles)
            .map(role ->
                new SimpleGrantedAuthority(
                    "ROLE_" + role))
            .toList();

    return new JwtAuthenticationToken(
        jwt,
        authorities);
  }
}
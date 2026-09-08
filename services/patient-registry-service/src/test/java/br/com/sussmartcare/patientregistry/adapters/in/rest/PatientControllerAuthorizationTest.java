package br.com.sussmartcare.patientregistry.adapters.in.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.sussmartcare.patientregistry.application.PatientAccessApplicationService;
import br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException;
import br.com.sussmartcare.patientregistry.application.PatientApplicationService;
import br.com.sussmartcare.patientregistry.domain.IdentifierType;
import br.com.sussmartcare.patientregistry.domain.RelationshipType;
import br.com.sussmartcare.patientregistry.domain.RepresentativeRelationship;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PatientControllerAuthorizationTest {

  private PatientApplicationService app;
  private PatientAccessApplicationService access;
  private PatientController controller;

  @BeforeEach
  void setUp() {

    app = mock(PatientApplicationService.class);
    access = mock(PatientAccessApplicationService.class);

    controller =
        new PatientController(
            app,
            access);
  }

  @Test
  void patientCannotReadIdentifiersOfUnrelatedPatient() {

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
            controller.identifiers(
                patientId,
                authentication(
                    userId,
                    "PATIENT")));

    verify(
        app,
        never())
        .identifiers(any());
  }

  @Test
  void representativeCannotReadRelationshipsOfUnrelatedPatient() {

    UUID userId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    when(
        access.decide(
            patientId,
            userId,
            Set.of("REPRESENTATIVE")))
        .thenReturn(
            new PatientAccessApplicationService.AccessDecision(
                false,
                "NO_ACTIVE_RELATIONSHIP"));

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            controller.representatives(
                patientId,
                authentication(
                    userId,
                    "REPRESENTATIVE")));

    verify(
        app,
        never())
        .representatives(any());
  }

  @Test
  void patientCannotRevokeRelationshipOfAnotherPatient() {

    UUID userId = UUID.randomUUID();
    UUID otherPatientId = UUID.randomUUID();

    RepresentativeRelationship relationship =
        new RepresentativeRelationship(
            otherPatientId,
            UUID.randomUUID(),
            RelationshipType.PARENT);

    when(
        app.relationship(
            relationship.getId()))
        .thenReturn(
            relationship);

    when(
        access.decide(
            otherPatientId,
            userId,
            Set.of("PATIENT")))
        .thenReturn(
            new PatientAccessApplicationService.AccessDecision(
                false,
                "NO_ACTIVE_RELATIONSHIP"));

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            controller.revoke(
                relationship.getId(),
                authentication(
                    userId,
                    "PATIENT")));

    verify(
        app,
        never())
        .revoke(any());
  }

  @Test
  void representativeCannotCreateUnlinkedPatient() {

    UUID userId = UUID.randomUUID();

    var request =
        new PatientController.CreatePatientRequest(
            "Paciente Indevido",
            java.time.LocalDate.of(1990, 1, 1),
            IdentifierType.OTHER,
            "REPRESENTATIVE-CREATE");

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            controller.create(
                request,
                authentication(
                    userId,
                    "REPRESENTATIVE")));

    verify(
        app,
        never())
        .create(
            any(),
            any(),
            any(),
            any());

    verify(
        app,
        never())
        .createForSelf(
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void patientCannotSearchPatientsByIdentifier() {

    UUID userId = UUID.randomUUID();

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            controller.byIdentifier(
                IdentifierType.CPF,
                "12345678900",
                authentication(
                    userId,
                    "PATIENT")));

    verify(
        app,
        never())
        .findByIdentifier(
            any(),
            any());
  }

  @Test
  void selfPatientCanReadOwnIdentifiers() {

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

    when(
        app.identifiers(
            patientId))
        .thenReturn(
            List.of());

    assertDoesNotThrow(
        () ->
            controller.identifiers(
                patientId,
                authentication(
                    userId,
                    "PATIENT")));

    verify(app)
        .identifiers(
            patientId);
  }

  @Test
  void operatorCanReadPatientIdentifiers() {

    UUID userId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    when(
        access.decide(
            patientId,
            userId,
            Set.of("OPERATOR")))
        .thenReturn(
            new PatientAccessApplicationService.AccessDecision(
                true,
                "PRIVILEGED_ROLE"));

    when(
        app.identifiers(
            patientId))
        .thenReturn(
            List.of());

    assertDoesNotThrow(
        () ->
            controller.identifiers(
                patientId,
                authentication(
                    userId,
                    "OPERATOR")));

    verify(app)
        .identifiers(
            patientId);
  }

  @Test
  void operatorCanSearchByIdentifier() {

    UUID userId = UUID.randomUUID();

    assertDoesNotThrow(
        () ->
            controller.byIdentifier(
                IdentifierType.CPF,
                "12345678900",
                authentication(
                    userId,
                    "OPERATOR")));

    verify(app)
        .findByIdentifier(
            IdentifierType.CPF,
            "12345678900");
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
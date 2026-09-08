package br.com.sussmartcare.patientregistry.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.sussmartcare.patientregistry.domain.*;
import br.com.sussmartcare.patientregistry.infrastructure.outbox.EventOutbox;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PatientApplicationServiceSelfTest {

  private PatientRepository patients;
  private PatientIdentifierRepository identifiers;
  private RepresentativeRelationshipRepository representatives;
  private EventOutbox outbox;
  private PatientApplicationService service;

  @BeforeEach
  void setUp() {

    patients = mock(PatientRepository.class);
    identifiers = mock(PatientIdentifierRepository.class);
    representatives =
        mock(RepresentativeRelationshipRepository.class);
    outbox = mock(EventOutbox.class);

    service =
        new PatientApplicationService(
            patients,
            identifiers,
            representatives,
            outbox);
  }

  @Test
  void createForSelfCreatesPatientAndSelfRelationship() {

    UUID userId = UUID.randomUUID();

    when(
        representatives
            .findByRepresentativeUserIdAndTypeAndActiveTrue(
                userId,
                RelationshipType.SELF))
        .thenReturn(Optional.empty());

    when(
        representatives
            .findByPatientIdAndTypeAndActiveTrue(
                any(UUID.class),
                eq(RelationshipType.SELF)))
        .thenReturn(Optional.empty());

    when(
        identifiers.findByTypeAndValue(
            IdentifierType.CPF,
            "12345678901"))
        .thenReturn(Optional.empty());

    when(patients.save(any(Patient.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(
        representatives.save(
            any(RepresentativeRelationship.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Patient patient =
        service.createForSelf(
            "Paciente Digital",
            LocalDate.of(1990, 1, 1),
            IdentifierType.CPF,
            "123.456.789-01",
            userId);

    ArgumentCaptor<RepresentativeRelationship> captor =
        ArgumentCaptor.forClass(
            RepresentativeRelationship.class);

    verify(representatives)
        .save(captor.capture());

    RepresentativeRelationship self =
        captor.getValue();

    assertEquals(
        patient.getId(),
        self.getPatientId());

    assertEquals(
        userId,
        self.getRepresentativeUserId());

    assertEquals(
        RelationshipType.SELF,
        self.getType());

    assertTrue(self.isActive());

    verify(outbox)
        .append(
            eq("patient-registered"),
            eq(patient.getId().toString()),
            any());

    verify(outbox)
        .append(
            eq("patient-self-linked"),
            eq(patient.getId().toString()),
            any());
  }

  @Test
  void createForSelfRejectsAccountThatAlreadyHasActiveSelf() {

    UUID userId = UUID.randomUUID();

    RepresentativeRelationship existing =
        new RepresentativeRelationship(
            UUID.randomUUID(),
            userId,
            RelationshipType.SELF);

    when(
        representatives
            .findByRepresentativeUserIdAndTypeAndActiveTrue(
                userId,
                RelationshipType.SELF))
        .thenReturn(Optional.of(existing));

    assertThrows(
        IllegalStateException.class,
        () ->
            service.createForSelf(
                "Outro Paciente",
                LocalDate.of(1990, 1, 1),
                null,
                null,
                userId));

    verifyNoInteractions(
        patients,
        identifiers,
        outbox);
  }

  @Test
  void linkSelfRejectsPatientThatAlreadyHasActiveSelf() {

    UUID patientId = UUID.randomUUID();
    UUID existingUser = UUID.randomUUID();
    UUID newUser = UUID.randomUUID();

    Patient patient =
        new Patient(
            patientId,
            "Paciente",
            LocalDate.of(1990, 1, 1),
            IdentityStatus.CONFIRMED);

    RepresentativeRelationship existing =
        new RepresentativeRelationship(
            patientId,
            existingUser,
            RelationshipType.SELF);

    when(patients.findById(patientId))
        .thenReturn(Optional.of(patient));

    when(
        representatives
            .findByPatientIdAndTypeAndActiveTrue(
                patientId,
                RelationshipType.SELF))
        .thenReturn(Optional.of(existing));

    assertThrows(
        IllegalStateException.class,
        () ->
            service.linkSelf(
                patientId,
                newUser));

    verify(
        representatives,
        never())
        .save(any());
  }

  @Test
  void linkSelfRejectsAccountThatAlreadyBelongsToAnotherPatient() {

    UUID patientId = UUID.randomUUID();
    UUID otherPatientId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Patient patient =
        new Patient(
            patientId,
            "Paciente",
            LocalDate.of(1990, 1, 1),
            IdentityStatus.CONFIRMED);

    RepresentativeRelationship existing =
        new RepresentativeRelationship(
            otherPatientId,
            userId,
            RelationshipType.SELF);

    when(patients.findById(patientId))
        .thenReturn(Optional.of(patient));

    when(
        representatives
            .findByPatientIdAndTypeAndActiveTrue(
                patientId,
                RelationshipType.SELF))
        .thenReturn(Optional.empty());

    when(
        representatives
            .findByRepresentativeUserIdAndTypeAndActiveTrue(
                userId,
                RelationshipType.SELF))
        .thenReturn(Optional.of(existing));

    assertThrows(
        IllegalStateException.class,
        () ->
            service.linkSelf(
                patientId,
                userId));

    verify(
        representatives,
        never())
        .save(any());
  }

  @Test
  void regularLinkCannotCreateSelfRelationship() {

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.link(
                UUID.randomUUID(),
                UUID.randomUUID(),
                RelationshipType.SELF));

    verifyNoInteractions(
        patients,
        representatives,
        outbox);
  }

  @Test
  void representativeRevokeCannotRevokeSelfRelationship() {

    RepresentativeRelationship self =
        new RepresentativeRelationship(
            UUID.randomUUID(),
            UUID.randomUUID(),
            RelationshipType.SELF);

    when(
        representatives.findById(
            self.getId()))
        .thenReturn(Optional.of(self));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.revoke(
                self.getId()));

    assertTrue(self.isActive());

    verify(
        representatives,
        never())
        .save(any());
  }
}
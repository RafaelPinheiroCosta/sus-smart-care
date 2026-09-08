package br.com.sussmartcare.patientregistry.application;

import br.com.sussmartcare.patientregistry.domain.*;
import br.com.sussmartcare.patientregistry.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientApplicationService {

  private final PatientRepository patients;
  private final PatientIdentifierRepository identifiers;
  private final RepresentativeRelationshipRepository representatives;
  private final EventOutbox outbox;

  public PatientApplicationService(
      PatientRepository patients,
      PatientIdentifierRepository identifiers,
      RepresentativeRelationshipRepository representatives,
      EventOutbox outbox) {

    this.patients = patients;
    this.identifiers = identifiers;
    this.representatives = representatives;
    this.outbox = outbox;
  }

  @Transactional
  public Patient create(
      String name,
      LocalDate birthDate,
      IdentifierType type,
      String value) {

    return createInternal(
        name,
        birthDate,
        type,
        value);
  }

  @Transactional
  public Patient createForSelf(
      String name,
      LocalDate birthDate,
      IdentifierType type,
      String value,
      UUID userId) {

    if (representatives
        .findByRepresentativeUserIdAndTypeAndActiveTrue(
            userId,
            RelationshipType.SELF)
        .isPresent()) {

      throw new IllegalStateException(
          "Conta ja possui um paciente SELF ativo");
    }

    Patient patient =
        createInternal(
            name,
            birthDate,
            type,
            value);

    linkSelfInternal(
        patient.getId(),
        userId);

    return patient;
  }

  @Transactional
  public RepresentativeRelationship linkSelf(
      UUID patientId,
      UUID userId) {

    get(patientId);

    return linkSelfInternal(
        patientId,
        userId);
  }

  private Patient createInternal(
      String name,
      LocalDate birthDate,
      IdentifierType type,
      String value) {

    String normalizedValue =
        normalizeIdentifier(value);

    if (normalizedValue != null && type == null) {
      throw new IllegalArgumentException(
          "Tipo do identificador e obrigatorio quando ha valor");
    }

    if (normalizedValue != null
        && identifiers
            .findByTypeAndValue(type, normalizedValue)
            .isPresent()) {

      throw new IllegalArgumentException(
          "Identificador ja associado a outro paciente");
    }

    Patient patient =
        patients.save(
            new Patient(
                UUID.randomUUID(),
                name,
                birthDate,
                IdentityStatus.CONFIRMED));

    if (normalizedValue != null) {
      identifiers.save(
          new PatientIdentifier(
              patient.getId(),
              type,
              normalizedValue));
    }

    outbox.append(
        "patient-registered",
        patient.getId().toString(),
        profile(patient));

    return patient;
  }

  private RepresentativeRelationship linkSelfInternal(
      UUID patientId,
      UUID userId) {

    if (representatives
        .findByPatientIdAndTypeAndActiveTrue(
            patientId,
            RelationshipType.SELF)
        .isPresent()) {

      throw new IllegalStateException(
          "Paciente ja possui uma conta SELF ativa");
    }

    if (representatives
        .findByRepresentativeUserIdAndTypeAndActiveTrue(
            userId,
            RelationshipType.SELF)
        .isPresent()) {

      throw new IllegalStateException(
          "Conta ja possui um paciente SELF ativo");
    }

    RepresentativeRelationship relationship =
        representatives.save(
            new RepresentativeRelationship(
                patientId,
                userId,
                RelationshipType.SELF));

    outbox.append(
        "patient-self-linked",
        patientId.toString(),
        new RepresentativeLinked(
            patientId,
            userId,
            RelationshipType.SELF.name(),
            relationship.getValidFrom()));

    return relationship;
  }

  @Transactional
  public Patient provisional(String description) {

    Patient patient =
        patients.save(
            Patient.provisional(description));

    outbox.append(
        "patient-provisional-created",
        patient.getId().toString(),
        profile(patient));

    return patient;
  }

  public Patient get(UUID id) {

    return patients.findById(id)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "Paciente nao encontrado"));
  }

  public Patient findByIdentifier(
      IdentifierType type,
      String value) {

    String normalized =
        normalizeIdentifier(value);

    if (normalized == null) {
      throw new IllegalArgumentException(
          "Valor do identificador e obrigatorio");
    }

    PatientIdentifier identifier =
        identifiers.findByTypeAndValue(
            type,
            normalized)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "Paciente nao encontrado para o identificador informado"));

    return get(identifier.getPatientId());
  }

  public List<PatientIdentifier> identifiers(
      UUID patientId) {

    get(patientId);

    return identifiers.findByPatientId(
        patientId);
  }

  @Transactional
  public RepresentativeRelationship link(
      UUID patientId,
      UUID userId,
      RelationshipType type) {

    if (type == RelationshipType.SELF) {
      throw new IllegalArgumentException(
          "Vinculo SELF somente pode ser criado pelo fluxo de identidade");
    }

    get(patientId);

    RepresentativeRelationship relationship =
        representatives.save(
            new RepresentativeRelationship(
                patientId,
                userId,
                type));

    outbox.append(
        "patient-representative-linked",
        patientId.toString(),
        new RepresentativeLinked(
            patientId,
            userId,
            type.name(),
            relationship.getValidFrom()));

    return relationship;
  }

  public RepresentativeRelationship relationship(UUID id) {

    return representatives.findById(id)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "Vinculo nao encontrado"));
  }
  @Transactional
  public void revoke(UUID id) {

    RepresentativeRelationship relationship =
        representatives.findById(id)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "Vinculo nao encontrado"));

    if (relationship.getType() == RelationshipType.SELF) {
      throw new IllegalArgumentException(
          "Vinculo SELF nao pode ser revogado pelo fluxo de representacao");
    }

    relationship.revoke();
    representatives.save(relationship);

    outbox.append(
        "patient-representative-revoked",
        relationship.getPatientId().toString(),
        new RepresentativeRevoked(
            relationship.getPatientId(),
            relationship.getRepresentativeUserId(),
            relationship.getId(),
            relationship.getValidUntil()));
  }

  @Transactional
  public Patient resolve(
      UUID id,
      String name,
      LocalDate birthDate) {

    Patient patient = get(id);

    patient.confirmIdentity(
        name,
        birthDate);

    patients.save(patient);

    outbox.append(
        "patient-identity-resolved",
        patient.getId().toString(),
        profile(patient));

    return patient;
  }

  @Transactional
  public Patient merge(
      UUID source,
      UUID target) {

    Patient canonical = get(target);
    Patient sourcePatient = get(source);

    sourcePatient.mergeInto(target);
    patients.save(sourcePatient);

    outbox.append(
        "patient-profile-updated",
        source.toString(),
        profile(sourcePatient));

    outbox.append(
        "patient-records-merged",
        source.toString(),
        new PatientMerged(
            source,
            target,
            Instant.now()));

    outbox.append(
        "patient-profile-updated",
        target.toString(),
        profile(canonical));

    return sourcePatient;
  }

  public List<RepresentativeRelationship> representatives(
      UUID patientId) {

    get(patientId);

    return representatives
        .findByPatientIdAndActiveTrue(
            patientId);
  }

  private String normalizeIdentifier(
      String value) {

    if (value == null || value.isBlank()) {
      return null;
    }

    return value
        .trim()
        .replaceAll("[.\\-/\\s]", "");
  }

  private PatientProfile profile(
      Patient patient) {

    return new PatientProfile(
        patient.getId(),
        patient.getFullName(),
        patient.getBirthDate(),
        patient.getIdentityStatus().name(),
        patient.getMergedIntoPatientId(),
        Instant.now());
  }

  public record PatientProfile(
      UUID patientId,
      String fullName,
      LocalDate birthDate,
      String identityStatus,
      UUID mergedIntoPatientId,
      Instant updatedAt) {}

  public record RepresentativeLinked(
      UUID patientId,
      UUID representativeUserId,
      String type,
      Instant validFrom) {}

  public record RepresentativeRevoked(
      UUID patientId,
      UUID representativeUserId,
      UUID relationshipId,
      Instant revokedAt) {}

  public record PatientMerged(
      UUID sourcePatientId,
      UUID canonicalPatientId,
      Instant occurredAt) {}
}
package br.com.sussmartcare.patientregistry.application;

import br.com.sussmartcare.patientregistry.domain.PatientRepository;
import br.com.sussmartcare.patientregistry.domain.RelationshipType;
import br.com.sussmartcare.patientregistry.domain.RepresentativeRelationship;
import br.com.sussmartcare.patientregistry.domain.RepresentativeRelationshipRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PatientAccessApplicationService {

  private static final Set<String> PRIVILEGED_ROLES = Set.of(
      "OPERATOR",
      "TRIAGE_NURSE",
      "DOCTOR",
      "AMBULANCE_TEAM",
      "ADMIN");

  private final PatientRepository patients;
  private final RepresentativeRelationshipRepository relationships;

  public PatientAccessApplicationService(
      PatientRepository patients,
      RepresentativeRelationshipRepository relationships) {
    this.patients = patients;
    this.relationships = relationships;
  }

  public AccessDecision decide(
      UUID patientId,
      UUID userId,
      Set<String> roles) {

    patients.findById(patientId)
        .orElseThrow(() -> new IllegalArgumentException("Paciente nao encontrado"));

    if (roles.stream().anyMatch(PRIVILEGED_ROLES::contains)) {
      return new AccessDecision(true, "PRIVILEGED_ROLE");
    }

    List<RepresentativeRelationship> activeRelationships =
        relationships.findByPatientIdAndRepresentativeUserIdAndActiveTrue(
            patientId,
            userId);

    boolean selfAccess =
        roles.contains("PATIENT")
            && activeRelationships.stream()
                .anyMatch(r -> r.getType() == RelationshipType.SELF);

    if (selfAccess) {
      return new AccessDecision(true, "SELF");
    }

    boolean representativeAccess =
        roles.contains("REPRESENTATIVE")
            && activeRelationships.stream()
                .anyMatch(r -> r.getType() != RelationshipType.SELF);

    if (representativeAccess) {
      return new AccessDecision(true, "ACTIVE_REPRESENTATION");
    }

    return new AccessDecision(false, "NO_ACTIVE_RELATIONSHIP");
  }

  public record AccessDecision(
      boolean allowed,
      String reason) {}
}
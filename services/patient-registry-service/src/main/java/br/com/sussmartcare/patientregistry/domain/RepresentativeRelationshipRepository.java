package br.com.sussmartcare.patientregistry.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepresentativeRelationshipRepository {

  RepresentativeRelationship save(
      RepresentativeRelationship relationship);

  List<RepresentativeRelationship>
      findByPatientIdAndActiveTrue(UUID patientId);

  List<RepresentativeRelationship>
      findByPatientIdAndRepresentativeUserIdAndActiveTrue(
          UUID patientId,
          UUID representativeUserId);

  Optional<RepresentativeRelationship>
      findByPatientIdAndTypeAndActiveTrue(
          UUID patientId,
          RelationshipType type);

  Optional<RepresentativeRelationship>
      findByRepresentativeUserIdAndTypeAndActiveTrue(
          UUID representativeUserId,
          RelationshipType type);

  Optional<RepresentativeRelationship> findById(UUID id);
}
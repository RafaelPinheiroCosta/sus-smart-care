package br.com.sussmartcare.patientregistry.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientIdentifierRepository {
  PatientIdentifier save(PatientIdentifier identifier);
  Optional<PatientIdentifier> findByTypeAndValue(IdentifierType type, String value);
  List<PatientIdentifier> findByPatientId(UUID patientId);
}

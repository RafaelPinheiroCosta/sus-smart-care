package br.com.sussmartcare.patientregistry.infrastructure;

import br.com.sussmartcare.patientregistry.domain.IdentifierType;
import br.com.sussmartcare.patientregistry.domain.PatientIdentifier;
import br.com.sussmartcare.patientregistry.domain.PatientIdentifierRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPatientIdentifierRepository
    extends PatientIdentifierRepository, JpaRepository<PatientIdentifier, UUID> {
  Optional<PatientIdentifier> findByTypeAndValue(IdentifierType type, String value);
  List<PatientIdentifier> findByPatientId(UUID patientId);
}

package br.com.sussmartcare.patientregistry.infrastructure;

import br.com.sussmartcare.patientregistry.domain.PatientCommunicationProfile;
import br.com.sussmartcare.patientregistry.domain.PatientCommunicationProfileRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPatientCommunicationProfileRepository
    extends PatientCommunicationProfileRepository,
        JpaRepository<PatientCommunicationProfile, UUID> {}
package br.com.sussmartcare.patientregistry.domain;

import java.util.Optional;
import java.util.UUID;

public interface PatientCommunicationProfileRepository {

  PatientCommunicationProfile save(PatientCommunicationProfile profile);

  Optional<PatientCommunicationProfile> findById(UUID patientId);
}
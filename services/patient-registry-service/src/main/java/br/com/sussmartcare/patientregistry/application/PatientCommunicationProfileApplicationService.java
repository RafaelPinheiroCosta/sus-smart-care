package br.com.sussmartcare.patientregistry.application;

import br.com.sussmartcare.patientregistry.domain.PatientCommunicationProfile;
import br.com.sussmartcare.patientregistry.domain.PatientCommunicationProfileRepository;
import br.com.sussmartcare.patientregistry.domain.PatientRepository;
import br.com.sussmartcare.patientregistry.domain.QueueCallMode;
import br.com.sussmartcare.patientregistry.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientCommunicationProfileApplicationService {

  private final PatientRepository patients;
  private final PatientCommunicationProfileRepository profiles;
  private final EventOutbox outbox;

  public PatientCommunicationProfileApplicationService(
      PatientRepository patients,
      PatientCommunicationProfileRepository profiles,
      EventOutbox outbox) {

    this.patients = patients;
    this.profiles = profiles;
    this.outbox = outbox;
  }

  @Transactional
  public PatientCommunicationProfile upsert(
      UUID patientId,
      boolean hasSmartphone,
      QueueCallMode queueCallMode) {

    ensurePatientExists(patientId);

    PatientCommunicationProfile profile = profiles.findById(patientId)
        .map(existing -> {
          existing.update(hasSmartphone, queueCallMode);
          return existing;
        })
        .orElseGet(() ->
            new PatientCommunicationProfile(
                patientId,
                hasSmartphone,
                queueCallMode));

    PatientCommunicationProfile saved = profiles.save(profile);

    outbox.append(
        "patient-communication-profile-updated",
        patientId.toString(),
        new PatientCommunicationProfileUpdated(
            saved.getPatientId(),
            saved.isHasSmartphone(),
            saved.getQueueCallMode().name(),
            saved.getUpdatedAt()));

    return saved;
  }

  public PatientCommunicationProfile get(UUID patientId) {
    ensurePatientExists(patientId);

    return profiles.findById(patientId)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Perfil de comunicacao do paciente nao encontrado"));
  }

  private void ensurePatientExists(UUID patientId) {
    patients.findById(patientId)
        .orElseThrow(() ->
            new IllegalArgumentException("Paciente nao encontrado"));
  }

  public record PatientCommunicationProfileUpdated(
      UUID patientId,
      boolean hasSmartphone,
      String queueCallMode,
      Instant updatedAt) {}
}
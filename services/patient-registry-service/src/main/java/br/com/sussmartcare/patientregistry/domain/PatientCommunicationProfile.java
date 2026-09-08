package br.com.sussmartcare.patientregistry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "patient_communication_profiles")
public class PatientCommunicationProfile {

  @Id
  private UUID patientId;

  @Column(nullable = false)
  private boolean hasSmartphone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private QueueCallMode queueCallMode;

  @Column(nullable = false)
  private Instant updatedAt;

  protected PatientCommunicationProfile() {}

  public PatientCommunicationProfile(
      UUID patientId,
      boolean hasSmartphone,
      QueueCallMode queueCallMode) {

    this.patientId = Objects.requireNonNull(patientId);
    update(hasSmartphone, queueCallMode);
  }

  public void update(
      boolean hasSmartphone,
      QueueCallMode queueCallMode) {

    QueueCallMode validatedMode = Objects.requireNonNull(queueCallMode);

    if (!hasSmartphone && validatedMode == QueueCallMode.MOBILE) {
      throw new IllegalArgumentException(
          "Paciente sem smartphone nao pode utilizar MOBILE como modo de chamada");
    }

    this.hasSmartphone = hasSmartphone;
    this.queueCallMode = validatedMode;
    this.updatedAt = Instant.now();
  }

  public UUID getPatientId() {
    return patientId;
  }

  public boolean isHasSmartphone() {
    return hasSmartphone;
  }

  public QueueCallMode getQueueCallMode() {
    return queueCallMode;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
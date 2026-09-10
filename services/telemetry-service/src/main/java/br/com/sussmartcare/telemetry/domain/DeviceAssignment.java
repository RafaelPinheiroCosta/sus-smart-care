package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "device_assignments")
public class DeviceAssignment {

  @Id
  private UUID id;

  @Column(name = "device_id", nullable = false, updatable = false)
  private UUID deviceId;

  @Column(name = "telemetry_session_id", nullable = false, updatable = false)
  private UUID telemetrySessionId;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  protected DeviceAssignment() {}

  public DeviceAssignment(
      UUID deviceId,
      UUID telemetrySessionId) {

    this.id = UUID.randomUUID();
    this.deviceId =
        Objects.requireNonNull(deviceId, "Device id is required");
    this.telemetrySessionId =
        Objects.requireNonNull(
            telemetrySessionId,
            "Telemetry session id is required");
    this.startedAt = Instant.now();
  }

  public void close() {
    if (!isActive()) {
      throw new IllegalStateException("Device assignment is already closed");
    }
    endedAt = Instant.now();
  }

  public boolean isActive() {
    return endedAt == null;
  }

  public UUID getId() { return id; }
  public UUID getDeviceId() { return deviceId; }
  public UUID getTelemetrySessionId() { return telemetrySessionId; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getEndedAt() { return endedAt; }
}

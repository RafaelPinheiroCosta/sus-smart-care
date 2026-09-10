package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telemetry_anomalies")
public class TelemetryAnomaly {

  @Id
  private UUID id;

  @Column(name = "session_id", nullable = false)
  private UUID sessionId;

  @Column(name = "device_id", nullable = false)
  private UUID deviceId;

  @Column(name = "metric_type", nullable = false, length = 120)
  private String type;

  @Column(nullable = false)
  private double value;

  @Column(length = 40)
  private String unit;

  @Column(name = "measured_at", nullable = false)
  private Instant measuredAt;

  @Column(name = "detected_at", nullable = false)
  private Instant detectedAt;

  @Column(nullable = false, length = 300)
  private String reason;

  protected TelemetryAnomaly() {
  }

  public TelemetryAnomaly(
      UUID sessionId,
      UUID deviceId,
      String type,
      double value,
      String unit,
      Instant measuredAt,
      String reason) {

    this.id = UUID.randomUUID();
    this.sessionId = sessionId;
    this.deviceId = deviceId;
    this.type = type;
    this.value = value;
    this.unit = unit;
    this.measuredAt = measuredAt;
    this.detectedAt = Instant.now();
    this.reason = reason;
  }

  public UUID getId() {
    return id;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public UUID getDeviceId() {
    return deviceId;
  }

  public String getType() {
    return type;
  }

  public double getValue() {
    return value;
  }

  public String getUnit() {
    return unit;
  }

  public Instant getMeasuredAt() {
    return measuredAt;
  }

  public Instant getDetectedAt() {
    return detectedAt;
  }

  public String getReason() {
    return reason;
  }
}

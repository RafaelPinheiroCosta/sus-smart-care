package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telemetry_aggregates")
public class TelemetryAggregate {

  @Id
  private UUID id;

  @Column(name = "session_id", nullable = false)
  private UUID sessionId;

  @Column(name = "metric_type", nullable = false, length = 120)
  private String type;

  @Column(name = "sample_count", nullable = false)
  private long sampleCount;

  @Column(name = "minimum_value", nullable = false)
  private double minimumValue;

  @Column(name = "maximum_value", nullable = false)
  private double maximumValue;

  @Column(name = "average_value", nullable = false)
  private double averageValue;

  @Column(length = 40)
  private String unit;

  @Column(name = "last_measured_at", nullable = false)
  private Instant lastMeasuredAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected TelemetryAggregate() {
  }

  public TelemetryAggregate(
      UUID sessionId,
      String type,
      double value,
      String unit,
      Instant measuredAt) {

    this.id = UUID.randomUUID();
    this.sessionId = sessionId;
    this.type = type;
    this.sampleCount = 1;
    this.minimumValue = value;
    this.maximumValue = value;
    this.averageValue = value;
    this.unit = unit;
    this.lastMeasuredAt = measuredAt;
    this.updatedAt = Instant.now();
  }

  public void apply(
      double value,
      String unit,
      Instant measuredAt) {

    double total =
        averageValue * sampleCount;

    sampleCount++;

    minimumValue =
        Math.min(
            minimumValue,
            value);

    maximumValue =
        Math.max(
            maximumValue,
            value);

    averageValue =
        (total + value) /
        sampleCount;

    if (unit != null &&
        !unit.isBlank()) {

      this.unit = unit;
    }

    lastMeasuredAt = measuredAt;
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public String getType() {
    return type;
  }

  public long getSampleCount() {
    return sampleCount;
  }

  public double getMinimumValue() {
    return minimumValue;
  }

  public double getMaximumValue() {
    return maximumValue;
  }

  public double getAverageValue() {
    return averageValue;
  }

  public String getUnit() {
    return unit;
  }

  public Instant getLastMeasuredAt() {
    return lastMeasuredAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}

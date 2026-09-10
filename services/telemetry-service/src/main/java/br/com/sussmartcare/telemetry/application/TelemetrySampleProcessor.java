package br.com.sussmartcare.telemetry.application;

import br.com.sussmartcare.telemetry.domain.*;
import br.com.sussmartcare.telemetry.infrastructure.*;
import br.com.sussmartcare.telemetry.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetrySampleProcessor {

  private final TelemetrySessionRepository sessions;
  private final DeviceRepository devices;
  private final DeviceAssignmentRepository assignments;
  private final BiometricObservationRepository observations;
  private final TelemetryAggregateRepository aggregates;
  private final TelemetryAnomalyRepository anomalies;
  private final RedisRollingTelemetryStore rollingStore;
  private final TelemetryAnomalyPolicy anomalyPolicy;
  private final EventOutbox outbox;
  private final int aggregatePublishEvery;

  public TelemetrySampleProcessor(
      TelemetrySessionRepository sessions,
      DeviceRepository devices,
      DeviceAssignmentRepository assignments,
      BiometricObservationRepository observations,
      TelemetryAggregateRepository aggregates,
      TelemetryAnomalyRepository anomalies,
      RedisRollingTelemetryStore rollingStore,
      TelemetryAnomalyPolicy anomalyPolicy,
      EventOutbox outbox,
      @Value("${TELEMETRY_AGGREGATE_PUBLISH_EVERY:10}")
      int aggregatePublishEvery) {

    this.sessions = sessions;
    this.devices = devices;
    this.assignments = assignments;
    this.observations = observations;
    this.aggregates = aggregates;
    this.anomalies = anomalies;
    this.rollingStore = rollingStore;
    this.anomalyPolicy = anomalyPolicy;
    this.outbox = outbox;
    this.aggregatePublishEvery =
        Math.max(
            1,
            aggregatePublishEvery);
  }

  @Transactional
  public Result process(
      UUID sessionId,
      UUID deviceId,
      String type,
      Double value,
      String unit,
      Instant measuredAt) {

    if (value == null) {
      throw new IllegalArgumentException(
          "Observation value is required");
    }

    TelemetrySession session =
        sessions.findById(sessionId)
            .orElseThrow(
                () ->
                    new TelemetryNotFoundException(
                        "Telemetry session not found: " +
                        sessionId));

    if (!session.isActive()) {
      throw new TelemetryConflictException(
          "Telemetry session is closed");
    }

    MedicalDevice device =
        devices.findById(deviceId)
            .orElseThrow(
                () ->
                    new TelemetryNotFoundException(
                        "Device not found: " +
                        deviceId));

    if (!device.isOperational()) {
      throw new TelemetryConflictException(
          "Device is not active");
    }

    DeviceAssignment assignment =
        assignments
            .findByDeviceIdAndEndedAtIsNull(
                deviceId)
            .orElseThrow(
                () ->
                    new TelemetryConflictException(
                        "Device has no active assignment"));

    if (!assignment
        .getTelemetrySessionId()
        .equals(sessionId)) {

      throw new TelemetryConflictException(
          "Device is assigned to another telemetry session");
    }

    String normalizedType =
        normalizeType(type);

    String normalizedUnit =
        normalizeUnit(unit);

    Instant actualMeasuredAt =
        measuredAt == null
            ? Instant.now()
            : measuredAt;

    if (session.getMode() ==
        TelemetryMode.CONTINUOUS) {

      return processContinuous(
          session,
          device,
          normalizedType,
          value,
          normalizedUnit,
          actualMeasuredAt);
    }

    return processSpot(
        session,
        device,
        normalizedType,
        value,
        normalizedUnit,
        actualMeasuredAt);
  }

  private Result processSpot(
      TelemetrySession session,
      MedicalDevice device,
      String type,
      double value,
      String unit,
      Instant measuredAt) {

    BiometricObservation observation =
        observations.save(
            new BiometricObservation(
                session.getId(),
                device.getId(),
                type,
                value,
                unit,
                measuredAt,
                session.getSourceContext()));

    outbox.append(
        "biometric-observation-received",
        session.getId().toString(),
        new SpotObservationEvent(
            observation.getId(),
            session.getPatientId(),
            session.getVisitId(),
            session.getId(),
            device.getId(),
            type,
            value,
            unit,
            measuredAt,
            session.getSourceContext()));

    return new Result(
        ProcessingStatus.SPOT_PERSISTED,
        observation.getId(),
        null,
        null);
  }

  private Result processContinuous(
      TelemetrySession session,
      MedicalDevice device,
      String type,
      double value,
      String unit,
      Instant measuredAt) {

    TelemetryAggregate aggregate =
        aggregates
            .findBySessionIdAndType(
                session.getId(),
                type)
            .orElse(null);

    if (aggregate == null) {

      aggregate =
          new TelemetryAggregate(
              session.getId(),
              type,
              value,
              unit,
              measuredAt);
    }
    else {

      aggregate.apply(
          value,
          unit,
          measuredAt);
    }

    aggregate =
        aggregates.save(
            aggregate);

    rollingStore.append(
        session.getId(),
        type,
        device.getId(),
        value,
        unit,
        measuredAt);

    UUID anomalyId = null;

    var anomalyReason =
        anomalyPolicy.evaluate(
            type,
            value);

    if (anomalyReason.isPresent()) {

      TelemetryAnomaly anomaly =
          anomalies.save(
              new TelemetryAnomaly(
                  session.getId(),
                  device.getId(),
                  type,
                  value,
                  unit,
                  measuredAt,
                  anomalyReason.get()));

      anomalyId =
          anomaly.getId();

      outbox.append(
          "telemetry-anomaly-detected",
          session.getId().toString(),
          new AnomalyEvent(
              anomaly.getId(),
              session.getPatientId(),
              session.getVisitId(),
              session.getId(),
              device.getId(),
              type,
              value,
              unit,
              measuredAt,
              anomaly.getDetectedAt(),
              anomaly.getReason(),
              session.getSourceContext()));
    }

    if (
        aggregate.getSampleCount() %
        aggregatePublishEvery ==
        0
    ) {

      outbox.append(
          "telemetry-aggregate-updated",
          session.getId() +
              ":" +
              type,
          new AggregateEvent(
              session.getPatientId(),
              session.getVisitId(),
              session.getId(),
              type,
              aggregate.getSampleCount(),
              aggregate.getMinimumValue(),
              aggregate.getMaximumValue(),
              aggregate.getAverageValue(),
              aggregate.getUnit(),
              aggregate.getLastMeasuredAt(),
              session.getSourceContext()));
    }

    return new Result(
        anomalyId == null
            ? ProcessingStatus.CONTINUOUS_AGGREGATED
            : ProcessingStatus.ANOMALY_DETECTED,
        null,
        anomalyId,
        aggregate.getSampleCount());
  }

  private String normalizeType(
      String value) {

    if (value == null ||
        value.trim().isEmpty()) {

      throw new IllegalArgumentException(
          "Observation type is required");
    }

    return value
        .trim()
        .toUpperCase(Locale.ROOT)
        .replace(' ', '_');
  }

  private String normalizeUnit(
      String value) {

    if (value == null ||
        value.trim().isEmpty()) {

      return null;
    }

    return value.trim();
  }

  public enum ProcessingStatus {
    SPOT_PERSISTED,
    CONTINUOUS_AGGREGATED,
    ANOMALY_DETECTED
  }

  public record Result(
      ProcessingStatus status,
      UUID observationId,
      UUID anomalyId,
      Long aggregateCount) {
  }

  public record SpotObservationEvent(
      UUID observationId,
      UUID patientId,
      UUID visitId,
      UUID sessionId,
      UUID deviceId,
      String type,
      double value,
      String unit,
      Instant measuredAt,
      String sourceContext) {
  }

  public record AggregateEvent(
      UUID patientId,
      UUID visitId,
      UUID sessionId,
      String type,
      long count,
      double minimum,
      double maximum,
      double average,
      String unit,
      Instant lastMeasuredAt,
      String sourceContext) {
  }

  public record AnomalyEvent(
      UUID anomalyId,
      UUID patientId,
      UUID visitId,
      UUID sessionId,
      UUID deviceId,
      String type,
      double value,
      String unit,
      Instant measuredAt,
      Instant detectedAt,
      String reason,
      String sourceContext) {
  }
}

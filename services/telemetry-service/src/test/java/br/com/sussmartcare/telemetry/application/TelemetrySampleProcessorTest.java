package br.com.sussmartcare.telemetry.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.sussmartcare.telemetry.domain.*;
import br.com.sussmartcare.telemetry.infrastructure.*;
import br.com.sussmartcare.telemetry.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetrySampleProcessorTest {

  @Mock TelemetrySessionRepository sessions;
  @Mock DeviceRepository devices;
  @Mock DeviceAssignmentRepository assignments;
  @Mock BiometricObservationRepository observations;
  @Mock TelemetryAggregateRepository aggregates;
  @Mock TelemetryAnomalyRepository anomalies;
  @Mock RedisRollingTelemetryStore rollingStore;
  @Mock TelemetryAnomalyPolicy anomalyPolicy;
  @Mock EventOutbox outbox;

  TelemetrySampleProcessor processor;

  @BeforeEach
  void setUp() {

    processor =
        new TelemetrySampleProcessor(
            sessions,
            devices,
            assignments,
            observations,
            aggregates,
            anomalies,
            rollingStore,
            anomalyPolicy,
            outbox,
            10);
  }

  @Test
  void spotPersistsRawObservation() {

    Fixture fixture =
        fixture(
            TelemetryMode.SPOT);

    when(
        observations.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    var result =
        processor.process(
            fixture.session.getId(),
            fixture.device.getId(),
            "HEART_RATE",
            80.0,
            "bpm",
            Instant.now());

    assertEquals(
        TelemetrySampleProcessor
            .ProcessingStatus
            .SPOT_PERSISTED,
        result.status());

    verify(observations)
        .save(any());

    verify(outbox)
        .append(
            eq("biometric-observation-received"),
            anyString(),
            any());

    verifyNoInteractions(
        rollingStore,
        aggregates,
        anomalies);
  }

  @Test
  void continuousNormalSampleUsesAggregateAndRedis() {

    Fixture fixture =
        fixture(
            TelemetryMode.CONTINUOUS);

    when(
        aggregates.findBySessionIdAndType(
            fixture.session.getId(),
            "HEART_RATE"))
        .thenReturn(
            Optional.empty());

    when(
        aggregates.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    when(
        anomalyPolicy.evaluate(
            "HEART_RATE",
            80.0))
        .thenReturn(
            Optional.empty());

    var result =
        processor.process(
            fixture.session.getId(),
            fixture.device.getId(),
            "HEART_RATE",
            80.0,
            "bpm",
            Instant.now());

    assertEquals(
        TelemetrySampleProcessor
            .ProcessingStatus
            .CONTINUOUS_AGGREGATED,
        result.status());

    verifyNoInteractions(
        observations);

    verify(rollingStore)
        .append(
            eq(fixture.session.getId()),
            eq("HEART_RATE"),
            eq(fixture.device.getId()),
            eq(80.0),
            eq("bpm"),
            any());

    verify(aggregates)
        .save(any());
  }

  @Test
  void anomalyIsPersistedAndPublishedImmediately() {

    Fixture fixture =
        fixture(
            TelemetryMode.CONTINUOUS);

    when(
        aggregates.findBySessionIdAndType(
            fixture.session.getId(),
            "SPO2"))
        .thenReturn(
            Optional.empty());

    when(
        aggregates.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    when(
        anomalyPolicy.evaluate(
            "SPO2",
            85.0))
        .thenReturn(
            Optional.of(
                "Value below threshold"));

    when(
        anomalies.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    var result =
        processor.process(
            fixture.session.getId(),
            fixture.device.getId(),
            "SPO2",
            85.0,
            "%",
            Instant.now());

    assertEquals(
        TelemetrySampleProcessor
            .ProcessingStatus
            .ANOMALY_DETECTED,
        result.status());

    assertNotNull(
        result.anomalyId());

    verify(anomalies)
        .save(any());

    verify(outbox)
        .append(
            eq("telemetry-anomaly-detected"),
            anyString(),
            any());
  }

  private Fixture fixture(
      TelemetryMode mode) {

    TelemetrySession session =
        new TelemetrySession(
            UUID.randomUUID(),
            null,
            null,
            "TRIAGE",
            mode);

    MedicalDevice device =
        new MedicalDevice(
            "TEST-DEVICE-" +
            UUID.randomUUID(),
            "MONITOR",
            null,
            null);

    DeviceAssignment assignment =
        new DeviceAssignment(
            device.getId(),
            session.getId());

    when(
        sessions.findById(
            session.getId()))
        .thenReturn(
            Optional.of(session));

    when(
        devices.findById(
            device.getId()))
        .thenReturn(
            Optional.of(device));

    when(
        assignments
            .findByDeviceIdAndEndedAtIsNull(
                device.getId()))
        .thenReturn(
            Optional.of(assignment));

    return new Fixture(
        session,
        device);
  }

  private record Fixture(
      TelemetrySession session,
      MedicalDevice device) {
  }
}

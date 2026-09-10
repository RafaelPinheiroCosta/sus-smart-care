package br.com.sussmartcare.telemetry.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.sussmartcare.telemetry.adapters.in.mqtt.MqttTelemetryPayloadParser;
import br.com.sussmartcare.telemetry.domain.DeviceAssignment;
import br.com.sussmartcare.telemetry.domain.MedicalDevice;
import br.com.sussmartcare.telemetry.infrastructure.DeviceAssignmentRepository;
import br.com.sussmartcare.telemetry.infrastructure.DeviceRepository;
import br.com.sussmartcare.telemetry.infrastructure.MqttIngestionMessageRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MqttIngestionServiceTest {

  @Mock
  DeviceRepository devices;

  @Mock
  DeviceAssignmentRepository assignments;

  @Mock
  MqttIngestionMessageRepository messages;

  @Mock
  TelemetryApplicationService telemetry;

  MqttIngestionService service;

  @BeforeEach
  void setUp() {

    service =
        new MqttIngestionService(
            devices,
            assignments,
            messages,
            telemetry);
  }

  @Test
  void activeAssignedDeviceCanIngest() {

    MedicalDevice device =
        new MedicalDevice(
            "MQTT-01",
            "MONITOR",
            null,
            null);

    UUID sessionId =
        UUID.randomUUID();

    DeviceAssignment assignment =
        new DeviceAssignment(
            device.getId(),
            sessionId);

    UUID messageId =
        UUID.randomUUID();

    when(
        devices.findByExternalId("MQTT-01"))
        .thenReturn(
            Optional.of(device));

    when(
        assignments
            .findByDeviceIdAndEndedAtIsNull(
                device.getId()))
        .thenReturn(
            Optional.of(assignment));

    var result =
        service.ingest(
            "MQTT-01",
            new MqttTelemetryPayloadParser.Payload(
                messageId,
                1,
                "HEART_RATE",
                80.0,
                "bpm",
                Instant.now()));

    assertEquals(
        MqttIngestionService.Status.ACCEPTED,
        result.status());

    verify(telemetry)
        .ingest(
            eq(sessionId),
            eq(device.getId()),
            eq("HEART_RATE"),
            eq(80.0),
            eq("bpm"),
            any());
  }

  @Test
  void repeatedMessageIdIsDuplicate() {

    MedicalDevice device =
        new MedicalDevice(
            "MQTT-02",
            "MONITOR",
            null,
            null);

    UUID messageId =
        UUID.randomUUID();

    when(
        devices.findByExternalId("MQTT-02"))
        .thenReturn(
            Optional.of(device));

    when(
        messages.existsByDeviceIdAndMessageId(
            device.getId(),
            messageId))
        .thenReturn(true);

    var result =
        service.ingest(
            "MQTT-02",
            new MqttTelemetryPayloadParser.Payload(
                messageId,
                2,
                "SPO2",
                98.0,
                "%",
                Instant.now()));

    assertEquals(
        MqttIngestionService.Status.DUPLICATE,
        result.status());

    verifyNoInteractions(
        assignments,
        telemetry);
  }

  @Test
  void repeatedSequenceIsDuplicate() {

    MedicalDevice device =
        new MedicalDevice(
            "MQTT-03",
            "MONITOR",
            null,
            null);

    when(
        devices.findByExternalId("MQTT-03"))
        .thenReturn(
            Optional.of(device));

    when(
        messages.existsByDeviceIdAndSequenceNumber(
            device.getId(),
            3))
        .thenReturn(true);

    var result =
        service.ingest(
            "MQTT-03",
            new MqttTelemetryPayloadParser.Payload(
                UUID.randomUUID(),
                3,
                "TEMPERATURE",
                36.5,
                "C",
                Instant.now()));

    assertEquals(
        MqttIngestionService.Status.DUPLICATE,
        result.status());

    verifyNoInteractions(
        assignments,
        telemetry);
  }
}

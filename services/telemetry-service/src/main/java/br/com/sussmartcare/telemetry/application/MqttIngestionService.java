package br.com.sussmartcare.telemetry.application;

import br.com.sussmartcare.telemetry.adapters.in.mqtt.MqttTelemetryPayloadParser.Payload;
import br.com.sussmartcare.telemetry.domain.DeviceAssignment;
import br.com.sussmartcare.telemetry.domain.MedicalDevice;
import br.com.sussmartcare.telemetry.domain.MqttIngestionMessage;
import br.com.sussmartcare.telemetry.infrastructure.DeviceAssignmentRepository;
import br.com.sussmartcare.telemetry.infrastructure.DeviceRepository;
import br.com.sussmartcare.telemetry.infrastructure.MqttIngestionMessageRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MqttIngestionService {

  private final DeviceRepository devices;
  private final DeviceAssignmentRepository assignments;
  private final MqttIngestionMessageRepository messages;
  private final TelemetrySampleProcessor processor;

  public MqttIngestionService(
      DeviceRepository devices,
      DeviceAssignmentRepository assignments,
      MqttIngestionMessageRepository messages,
      TelemetrySampleProcessor processor) {

    this.devices = devices;
    this.assignments = assignments;
    this.messages = messages;
    this.processor = processor;
  }

  @Transactional
  public Result ingest(
      String externalId,
      Payload payload) {

    MedicalDevice device =
        devices
            .findByExternalId(
                requireText(
                    externalId,
                    "Device external id is required"))
            .orElseThrow(
                () ->
                    new TelemetryNotFoundException(
                        "Device not found: " +
                        externalId));

    if (
        messages.existsByDeviceIdAndMessageId(
            device.getId(),
            payload.messageId())
    ) {

      return duplicate(
          payload);
    }

    if (
        messages.existsByDeviceIdAndSequenceNumber(
            device.getId(),
            payload.sequence())
    ) {

      return duplicate(
          payload);
    }

    if (!device.isOperational()) {

      throw new TelemetryConflictException(
          "Device is not active: " +
          externalId);
    }

    DeviceAssignment assignment =
        assignments
            .findByDeviceIdAndEndedAtIsNull(
                device.getId())
            .orElseThrow(
                () ->
                    new TelemetryConflictException(
                        "Device has no active telemetry assignment"));

    messages.save(
        new MqttIngestionMessage(
            device.getId(),
            payload.messageId(),
            payload.sequence()));

    processor.process(
        assignment.getTelemetrySessionId(),
        device.getId(),
        payload.type(),
        payload.value(),
        payload.unit(),
        payload.measuredAt());

    return new Result(
        Status.ACCEPTED,
        payload.messageId(),
        payload.sequence(),
        Instant.now());
  }

  private Result duplicate(
      Payload payload) {

    return new Result(
        Status.DUPLICATE,
        payload.messageId(),
        payload.sequence(),
        Instant.now());
  }

  private String requireText(
      String value,
      String message) {

    if (value == null ||
        value.trim().isEmpty()) {

      throw new IllegalArgumentException(
          message);
    }

    return value.trim();
  }

  public enum Status {
    ACCEPTED,
    DUPLICATE
  }

  public record Result(
      Status status,
      java.util.UUID messageId,
      long sequence,
      Instant receivedAt) {
  }
}

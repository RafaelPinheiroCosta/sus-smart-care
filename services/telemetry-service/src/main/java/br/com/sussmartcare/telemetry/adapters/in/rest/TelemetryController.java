package br.com.sussmartcare.telemetry.adapters.in.rest;

import br.com.sussmartcare.telemetry.application.*;
import br.com.sussmartcare.telemetry.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TelemetryController {

  private final TelemetryApplicationService app;
  private final TelemetrySummaryService summaries;

  public TelemetryController(
      TelemetryApplicationService app,
      TelemetrySummaryService summaries) {
    this.app = app;
    this.summaries = summaries;
  }

  public record DeviceRequest(
      @NotBlank String externalId,
      @NotBlank String deviceType,
      String manufacturer,
      String model) {}

  public record DeviceStatusRequest(
      @NotNull DeviceStatus status) {}

  public record PlacementRequest(
      @NotNull DevicePlacementType placementType,
      UUID facilityId,
      UUID zoneId,
      String ambulanceId) {}

  public record SessionRequest(
      @NotNull UUID patientId,
      UUID visitId,
      UUID preHospitalEncounterId,
      @NotBlank String sourceContext) {}

  public record AssignmentRequest(
      @NotNull UUID deviceId) {}

  public record ObservationRequest(
      @NotNull UUID deviceId,
      @NotBlank String type,
      @NotNull Double value,
      String unit,
      Instant measuredAt) {}

  @PostMapping("/devices")
  public ResponseEntity<MedicalDevice> register(
      @Valid @RequestBody DeviceRequest request) {
    return ResponseEntity.status(201).body(
        app.register(
            request.externalId(),
            request.deviceType(),
            request.manufacturer(),
            request.model()));
  }

  @GetMapping("/devices")
  public List<MedicalDevice> devices() {
    return app.listDevices();
  }

  @GetMapping("/devices/{id}")
  public MedicalDevice device(@PathVariable UUID id) {
    return app.getDevice(id);
  }

  @PatchMapping("/devices/{id}/status")
  public MedicalDevice status(
      @PathVariable UUID id,
      @Valid @RequestBody DeviceStatusRequest request) {
    return app.updateDeviceStatus(id, request.status());
  }

  @PostMapping("/devices/{id}/placements")
  public ResponseEntity<DevicePlacement> place(
      @PathVariable UUID id,
      @Valid @RequestBody PlacementRequest request) {
    return ResponseEntity.status(201).body(
        app.place(
            id,
            request.placementType(),
            request.facilityId(),
            request.zoneId(),
            request.ambulanceId()));
  }

  @GetMapping("/devices/{id}/placement")
  public DevicePlacement placement(@PathVariable UUID id) {
    return app.activePlacement(id);
  }

  @PostMapping("/device-placements/{id}/end")
  public DevicePlacement endPlacement(@PathVariable UUID id) {
    return app.endPlacement(id);
  }

  @PostMapping("/telemetry/sessions")
  public ResponseEntity<TelemetrySession> session(
      @Valid @RequestBody SessionRequest request) {
    return ResponseEntity.status(201).body(
        app.start(
            request.patientId(),
            request.visitId(),
            request.preHospitalEncounterId(),
            request.sourceContext()));
  }

  @GetMapping("/telemetry/sessions/{id}")
  public TelemetrySession session(@PathVariable UUID id) {
    return app.getSession(id);
  }

  @PostMapping("/telemetry/sessions/{id}/end")
  public TelemetrySession endSession(@PathVariable UUID id) {
    return app.endSession(id);
  }

  @PostMapping("/telemetry/sessions/{id}/assignments")
  public ResponseEntity<DeviceAssignment> assignment(
      @PathVariable UUID id,
      @Valid @RequestBody AssignmentRequest request) {
    return ResponseEntity.status(201).body(
        app.assign(id, request.deviceId()));
  }

  @PostMapping("/device-assignments/{id}/end")
  public DeviceAssignment endAssignment(@PathVariable UUID id) {
    return app.endAssignment(id);
  }

  @PostMapping("/telemetry/sessions/{id}/observations")
  public ResponseEntity<BiometricObservation> observation(
      @PathVariable UUID id,
      @Valid @RequestBody ObservationRequest request) {
    return ResponseEntity.status(202).body(
        app.ingest(
            id,
            request.deviceId(),
            request.type(),
            request.value(),
            request.unit(),
            request.measuredAt()));
  }

  @GetMapping("/telemetry/sessions/{sessionId}/summary")
  public TelemetrySummaryService.Summary summary(
      @PathVariable UUID sessionId,
      @RequestParam String type) {
    return summaries.summarize(
        sessionId,
        type.trim().toUpperCase().replace(' ', '_'));
  }
}

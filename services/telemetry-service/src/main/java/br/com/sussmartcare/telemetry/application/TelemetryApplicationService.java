package br.com.sussmartcare.telemetry.application;

import br.com.sussmartcare.telemetry.domain.*;
import br.com.sussmartcare.telemetry.infrastructure.*;
import br.com.sussmartcare.telemetry.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryApplicationService {

  private final DeviceRepository devices;
  private final TelemetrySessionRepository sessions;
  private final BiometricObservationRepository observations;
  private final DevicePlacementRepository placements;
  private final DeviceAssignmentRepository assignments;
  private final EventOutbox outbox;

  public TelemetryApplicationService(
      DeviceRepository devices,
      TelemetrySessionRepository sessions,
      BiometricObservationRepository observations,
      DevicePlacementRepository placements,
      DeviceAssignmentRepository assignments,
      EventOutbox outbox) {

    this.devices = devices;
    this.sessions = sessions;
    this.observations = observations;
    this.placements = placements;
    this.assignments = assignments;
    this.outbox = outbox;
  }

  @Transactional
  public MedicalDevice register(
      String externalId,
      String type,
      String manufacturer,
      String model) {

    String normalized = requireText(externalId, "External id is required");

    if (devices.findByExternalId(normalized).isPresent()) {
      throw new TelemetryConflictException(
          "Device already registered: " + normalized);
    }

    return devices.save(
        new MedicalDevice(
            normalized,
            type,
            manufacturer,
            model));
  }

  @Transactional(readOnly = true)
  public List<MedicalDevice> listDevices() {
    return devices.findAll().stream()
        .sorted(Comparator.comparing(MedicalDevice::getExternalId))
        .toList();
  }

  @Transactional(readOnly = true)
  public MedicalDevice getDevice(UUID id) {
    return devices.findById(id)
        .orElseThrow(() ->
            new TelemetryNotFoundException(
                "Device not found: " + id));
  }

  @Transactional
  public MedicalDevice updateDeviceStatus(
      UUID id,
      DeviceStatus status) {

    if (status == null) {
      throw new IllegalArgumentException("Device status is required");
    }

    MedicalDevice device = getDevice(id);

    if (status != DeviceStatus.ACTIVE &&
        assignments.findByDeviceIdAndEndedAtIsNull(id).isPresent()) {
      throw new TelemetryConflictException(
          "Device has an active telemetry assignment");
    }

    device.changeStatus(status);
    return devices.save(device);
  }

  @Transactional
  public DevicePlacement place(
      UUID deviceId,
      DevicePlacementType type,
      UUID facilityId,
      UUID zoneId,
      String ambulanceId) {

    MedicalDevice device = getDevice(deviceId);

    if (!device.isOperational()) {
      throw new TelemetryConflictException(
          "Device is not active: " + deviceId);
    }

    if (placements.findByDeviceIdAndEndedAtIsNull(deviceId).isPresent()) {
      throw new TelemetryConflictException(
          "Device already has an active placement");
    }

    return placements.save(
        new DevicePlacement(
            deviceId,
            type,
            facilityId,
            zoneId,
            ambulanceId));
  }

  @Transactional(readOnly = true)
  public DevicePlacement activePlacement(UUID deviceId) {

    getDevice(deviceId);

    return placements
        .findByDeviceIdAndEndedAtIsNull(deviceId)
        .orElseThrow(() ->
            new TelemetryNotFoundException(
                "Active placement not found for device: " + deviceId));
  }

  @Transactional
  public DevicePlacement endPlacement(UUID placementId) {

    DevicePlacement placement =
        placements.findById(placementId)
            .orElseThrow(() ->
                new TelemetryNotFoundException(
                    "Device placement not found: " + placementId));

    if (assignments
        .findByDeviceIdAndEndedAtIsNull(placement.getDeviceId())
        .isPresent()) {
      throw new TelemetryConflictException(
          "Cannot end placement while device is assigned");
    }

    placement.close();
    return placements.save(placement);
  }

  @Transactional
  public TelemetrySession start(
      UUID patient,
      UUID visit,
      UUID preHospital,
      String context) {

    return sessions.save(
        new TelemetrySession(
            patient,
            visit,
            preHospital,
            context));
  }

  @Transactional(readOnly = true)
  public TelemetrySession getSession(UUID id) {

    return sessions.findById(id)
        .orElseThrow(() ->
            new TelemetryNotFoundException(
                "Telemetry session not found: " + id));
  }

  @Transactional
  public TelemetrySession endSession(UUID id) {

    TelemetrySession session = getSession(id);

    for (DeviceAssignment assignment :
        assignments.findByTelemetrySessionIdAndEndedAtIsNull(id)) {

      assignment.close();
      assignments.save(assignment);
    }

    session.end();
    return sessions.save(session);
  }

  @Transactional
  public DeviceAssignment assign(
      UUID sessionId,
      UUID deviceId) {

    TelemetrySession session = getSession(sessionId);

    if (!session.isActive()) {
      throw new TelemetryConflictException(
          "Telemetry session is closed");
    }

    MedicalDevice device = getDevice(deviceId);

    if (!device.isOperational()) {
      throw new TelemetryConflictException(
          "Device is not active");
    }

    if (placements
        .findByDeviceIdAndEndedAtIsNull(deviceId)
        .isEmpty()) {
      throw new TelemetryConflictException(
          "Device has no active placement");
    }

    if (assignments
        .findByDeviceIdAndEndedAtIsNull(deviceId)
        .isPresent()) {
      throw new TelemetryConflictException(
          "Device already has an active assignment");
    }

    return assignments.save(
        new DeviceAssignment(
            deviceId,
            sessionId));
  }

  @Transactional
  public DeviceAssignment endAssignment(UUID assignmentId) {

    DeviceAssignment assignment =
        assignments.findById(assignmentId)
            .orElseThrow(() ->
                new TelemetryNotFoundException(
                    "Device assignment not found: " + assignmentId));

    assignment.close();
    return assignments.save(assignment);
  }

  @Transactional
  public BiometricObservation ingest(
      UUID sessionId,
      UUID deviceId,
      String type,
      Double value,
      String unit,
      Instant measuredAt) {

    TelemetrySession session = getSession(sessionId);

    if (!session.isActive()) {
      throw new TelemetryConflictException(
          "Telemetry session is closed");
    }

    MedicalDevice device = getDevice(deviceId);

    if (!device.isOperational()) {
      throw new TelemetryConflictException(
          "Device is not active");
    }

    DeviceAssignment assignment =
        assignments
            .findByDeviceIdAndEndedAtIsNull(deviceId)
            .orElseThrow(() ->
                new TelemetryConflictException(
                    "Device has no active assignment"));

    if (!assignment.getTelemetrySessionId().equals(sessionId)) {
      throw new TelemetryConflictException(
          "Device is assigned to another telemetry session");
    }

    if (value == null) {
      throw new IllegalArgumentException(
          "Observation value is required");
    }

    BiometricObservation observation =
        observations.save(
            new BiometricObservation(
                sessionId,
                deviceId,
                normalize(type),
                value,
                normalizeUnit(unit),
                measuredAt == null
                    ? Instant.now()
                    : measuredAt,
                session.getSourceContext()));

    outbox.append(
        "biometric-observation-received",
        sessionId.toString(),
        new ObservationEvent(
            observation.getId(),
            session.getPatientId(),
            session.getVisitId(),
            observation.getType(),
            observation.getValue(),
            observation.getUnit(),
            observation.getMeasuredAt(),
            session.getSourceContext()));

    return observation;
  }

  private String normalize(String value) {
    return requireText(value, "Observation type is required")
        .toUpperCase()
        .replace(' ', '_');
  }

  private String normalizeUnit(String value) {
    return value == null ? null : value.trim();
  }

  private String requireText(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  public record ObservationEvent(
      UUID observationId,
      UUID patientId,
      UUID visitId,
      String type,
      Double value,
      String unit,
      Instant measuredAt,
      String sourceContext) {}
}

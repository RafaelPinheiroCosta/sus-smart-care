package br.com.sussmartcare.telemetry.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeviceModelDomainTest {

  @Test
  void revokedDeviceIsTerminal() {

    MedicalDevice device =
        new MedicalDevice(
            "MONITOR-01",
            "MULTIPARAMETER",
            "ACME",
            "M1");

    device.changeStatus(DeviceStatus.REVOKED);

    assertThrows(
        IllegalStateException.class,
        () -> device.changeStatus(DeviceStatus.ACTIVE));
  }

  @Test
  void careZonePlacementRequiresFacilityAndZone() {

    assertThrows(
        IllegalArgumentException.class,
        () -> new DevicePlacement(
            UUID.randomUUID(),
            DevicePlacementType.CARE_ZONE,
            UUID.randomUUID(),
            null,
            null));
  }

  @Test
  void assignmentHasTemporalLifecycle() {

    DeviceAssignment assignment =
        new DeviceAssignment(
            UUID.randomUUID(),
            UUID.randomUUID());

    assertTrue(assignment.isActive());

    assignment.close();

    assertFalse(assignment.isActive());
  }

  @Test
  void telemetrySessionHasTemporalLifecycle() {

    TelemetrySession session =
        new TelemetrySession(
            UUID.randomUUID(),
            null,
            null,
            "TRIAGE");

    assertTrue(session.isActive());

    session.end();

    assertFalse(session.isActive());
  }
}

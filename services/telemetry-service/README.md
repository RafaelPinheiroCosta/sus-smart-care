# Telemetry Service

Owns medical-device inventory, placement, patient-session assignment
and biometric telemetry.

## Stage 3 device lifecycle

MedicalDevice
  -> DevicePlacement
  -> DeviceAssignment
  -> TelemetrySession
  -> BiometricObservation

A device must be ACTIVE, have an active operational placement and have
an active assignment to the exact telemetry session before an observation
is accepted.

Placements are temporal and may represent:

- FACILITY
- CARE_ZONE
- AMBULANCE

Assignments are also temporal. Several devices may belong to the same
telemetry session, but one physical device can have only one active
assignment at a time.

REVOKED is terminal.

The device model intentionally keeps deviceType as text so new biometric
equipment can be onboarded without changing the domain schema.

MQTT transport and per-device message identity are implemented in Stage 4.

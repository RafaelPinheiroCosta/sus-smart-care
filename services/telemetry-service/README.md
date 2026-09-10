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

## MQTT

Local development uses Eclipse Mosquitto on port 1883.

Telemetry topic:

`sus/v1/devices/{deviceExternalId}/telemetry`

Acknowledgement topic:

`sus/v1/devices/{deviceExternalId}/ack`

QoS 1 is used.

The MQTT payload contains only device measurement information:

- messageId
- sequence
- type
- value
- unit
- measuredAt

It must not contain patientId, visitId, sessionId, CPF or CNS.

The telemetry service resolves the active patient session from the temporal
DeviceAssignment created by the clinical workflow.

messageId and sequence are persisted and deduplicated, making QoS 1
redelivery idempotent.

The local broker uses a development-only credential. Production deployment
is expected to use TLS and per-device authentication/ACLs through the
managed MQTT broker profile, such as EMQX.

package br.com.sussmartcare.presence.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "presence_tracking_sessions")
public class PresenceTrackingSession {

  @Id
  private UUID id;

  @Column(
      name = "visit_id",
      nullable = false)
  private UUID visitId;

  @Column(
      name = "patient_id",
      nullable = false)
  private UUID patientId;

  @Column(
      name = "facility_id",
      nullable = false)
  private UUID facilityId;

  @JsonIgnore
  @Column(
      name = "tracking_token_hash",
      nullable = false,
      unique = true,
      length = 64)
  private String trackingTokenHash;

  @Column(
      name = "inside_facility",
      nullable = false)
  private boolean insideFacility;

  @Column(
      name = "current_zone_id",
      length = 100)
  private String currentZoneId;

  @Column(
      name = "zone_entered_at")
  private Instant zoneEnteredAt;

  @Column(
      name = "last_signal_at")
  private Instant lastSignalAt;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "last_source_type",
      length = 20)
  private PresenceSourceType lastSourceType;

  @Column(
      name = "last_gateway_external_id",
      length = 100)
  private String lastGatewayExternalId;

  @Column(
      name = "started_at",
      nullable = false,
      updatable = false)
  private Instant startedAt;

  @Column(
      name = "ended_at")
  private Instant endedAt;

  @Version
  private long version;

  protected PresenceTrackingSession() {
  }

  public PresenceTrackingSession(
      UUID visitId,
      UUID patientId,
      UUID facilityId,
      String trackingTokenHash) {

    if (visitId == null ||
        patientId == null ||
        facilityId == null) {

      throw new IllegalArgumentException(
          "Visit, patient and facility are required");
    }

    if (trackingTokenHash == null ||
        trackingTokenHash.length() != 64) {

      throw new IllegalArgumentException(
          "Tracking token hash is invalid");
    }

    this.id = UUID.randomUUID();
    this.visitId = visitId;
    this.patientId = patientId;
    this.facilityId = facilityId;
    this.trackingTokenHash = trackingTokenHash;
    this.insideFacility = false;
    this.startedAt = Instant.now();
  }

  public Transition apply(
      PresenceState state,
      String zoneId,
      PresenceSourceType source,
      String gatewayExternalId,
      Instant occurredAt) {

    if (!isActive()) {
      throw new IllegalStateException(
          "Tracking session is closed");
    }

    if (state == null ||
        source == null) {

      throw new IllegalArgumentException(
          "Presence state and source are required");
    }

    Instant actual =
        occurredAt == null
            ? Instant.now()
            : occurredAt;

    if (lastSignalAt != null &&
        actual.isBefore(lastSignalAt)) {

      throw new IllegalStateException(
          "Out-of-order presence signal");
    }

    String normalizedZone = null;

    if (state == PresenceState.INSIDE) {

      if (zoneId == null ||
          zoneId.trim().isEmpty()) {

        throw new IllegalArgumentException(
            "Zone is required while inside facility");
      }

      normalizedZone = zoneId.trim();
    }

    PresenceState previousState =
        insideFacility
            ? PresenceState.INSIDE
            : PresenceState.OUTSIDE;

    String previousZone =
        currentZoneId;

    boolean changed =
        previousState != state ||
        !java.util.Objects.equals(
            previousZone,
            normalizedZone);

    if (state == PresenceState.INSIDE) {

      if (!java.util.Objects.equals(
          currentZoneId,
          normalizedZone)) {

        zoneEnteredAt = actual;
      }

      insideFacility = true;
      currentZoneId = normalizedZone;
    }
    else {

      insideFacility = false;
      currentZoneId = null;
      zoneEnteredAt = null;
    }

    lastSignalAt = actual;
    lastSourceType = source;
    lastGatewayExternalId = gatewayExternalId;

    return new Transition(
        previousState,
        previousZone,
        state,
        currentZoneId,
        changed,
        actual);
  }

  public void end() {

    if (!isActive()) {
      throw new IllegalStateException(
          "Tracking session is already closed");
    }

    endedAt = Instant.now();
  }

  public boolean isActive() {
    return endedAt == null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getVisitId() {
    return visitId;
  }

  public UUID getPatientId() {
    return patientId;
  }

  public UUID getFacilityId() {
    return facilityId;
  }

  public boolean isInsideFacility() {
    return insideFacility;
  }

  public String getCurrentZoneId() {
    return currentZoneId;
  }

  public Instant getZoneEnteredAt() {
    return zoneEnteredAt;
  }

  public Instant getLastSignalAt() {
    return lastSignalAt;
  }

  public PresenceSourceType getLastSourceType() {
    return lastSourceType;
  }

  public String getLastGatewayExternalId() {
    return lastGatewayExternalId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public record Transition(
      PresenceState previousState,
      String previousZoneId,
      PresenceState state,
      String zoneId,
      boolean changed,
      Instant occurredAt) {
  }
}

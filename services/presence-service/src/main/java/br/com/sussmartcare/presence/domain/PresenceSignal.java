package br.com.sussmartcare.presence.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "presence_signals")
public class PresenceSignal {

  @Id
  private UUID id;

  @Column(
      name = "tracking_session_id",
      nullable = false)
  private UUID trackingSessionId;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "source_type",
      nullable = false,
      length = 20)
  private PresenceSourceType sourceType;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "presence_state",
      nullable = false,
      length = 20)
  private PresenceState state;

  @Column(
      name = "zone_id",
      length = 100)
  private String zoneId;

  @Column(
      name = "gateway_external_id",
      length = 100)
  private String gatewayExternalId;

  @Column(
      name = "occurred_at",
      nullable = false)
  private Instant occurredAt;

  @Column(
      name = "received_at",
      nullable = false)
  private Instant receivedAt;

  protected PresenceSignal() {
  }

  public PresenceSignal(
      UUID signalId,
      UUID trackingSessionId,
      PresenceSourceType sourceType,
      PresenceState state,
      String zoneId,
      String gatewayExternalId,
      Instant occurredAt) {

    if (signalId == null ||
        trackingSessionId == null) {

      throw new IllegalArgumentException(
          "Signal and tracking session ids are required");
    }

    this.id = signalId;
    this.trackingSessionId = trackingSessionId;
    this.sourceType = sourceType;
    this.state = state;
    this.zoneId = zoneId;
    this.gatewayExternalId = gatewayExternalId;
    this.occurredAt =
        occurredAt == null
            ? Instant.now()
            : occurredAt;
    this.receivedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getTrackingSessionId() {
    return trackingSessionId;
  }

  public PresenceSourceType getSourceType() {
    return sourceType;
  }

  public PresenceState getState() {
    return state;
  }

  public String getZoneId() {
    return zoneId;
  }

  public String getGatewayExternalId() {
    return gatewayExternalId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}

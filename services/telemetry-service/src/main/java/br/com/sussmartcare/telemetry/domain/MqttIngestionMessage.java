package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mqtt_ingestion_messages")
public class MqttIngestionMessage {

  @Id
  private UUID id;

  @Column(
      name = "device_id",
      nullable = false,
      updatable = false)
  private UUID deviceId;

  @Column(
      name = "message_id",
      nullable = false,
      updatable = false)
  private UUID messageId;

  @Column(
      name = "sequence_number",
      nullable = false,
      updatable = false)
  private long sequenceNumber;

  @Column(
      name = "received_at",
      nullable = false,
      updatable = false)
  private Instant receivedAt;

  protected MqttIngestionMessage() {
  }

  public MqttIngestionMessage(
      UUID deviceId,
      UUID messageId,
      long sequenceNumber) {

    if (deviceId == null) {
      throw new IllegalArgumentException(
          "Device id is required");
    }

    if (messageId == null) {
      throw new IllegalArgumentException(
          "Message id is required");
    }

    if (sequenceNumber < 0) {
      throw new IllegalArgumentException(
          "Sequence must be non-negative");
    }

    this.id = UUID.randomUUID();
    this.deviceId = deviceId;
    this.messageId = messageId;
    this.sequenceNumber = sequenceNumber;
    this.receivedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getDeviceId() {
    return deviceId;
  }

  public UUID getMessageId() {
    return messageId;
  }

  public long getSequenceNumber() {
    return sequenceNumber;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}

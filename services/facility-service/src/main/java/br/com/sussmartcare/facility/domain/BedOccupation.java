package br.com.sussmartcare.facility.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "bed_occupations")
public class BedOccupation {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID bedId;

  @Column(nullable = false)
  private UUID visitId;

  @Column(nullable = false)
  private Instant startedAt;

  private Instant endedAt;

  protected BedOccupation() {}

  public BedOccupation(
      UUID bedId,
      UUID visitId) {

    this.id = UUID.randomUUID();
    this.bedId = Objects.requireNonNull(bedId, "bedId");
    this.visitId = Objects.requireNonNull(visitId, "visitId");
    this.startedAt = Instant.now();
  }

  public void close() {

    if (endedAt != null) {
      throw new IllegalStateException(
          "Bed occupation is already closed");
    }

    endedAt = Instant.now();
  }

  public boolean isActive() {
    return endedAt == null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getBedId() {
    return bedId;
  }

  public UUID getVisitId() {
    return visitId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }
}

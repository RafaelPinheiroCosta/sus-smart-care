package br.com.sussmartcare.presence.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PresenceTrackingDomainTest {

  private PresenceTrackingSession session() {

    return new PresenceTrackingSession(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "a".repeat(64));
  }

  @Test
  void supportsAllPlannedSources() {

    assertEquals(
        7,
        PresenceSourceType.values().length);

    assertTrue(
        PresenceSourceType.BLE
            .requiresGateway());

    assertTrue(
        PresenceSourceType.UWB
            .requiresGateway());

    assertFalse(
        PresenceSourceType.MANUAL
            .requiresGateway());

    assertFalse(
        PresenceSourceType.SYSTEM
            .requiresGateway());
  }

  @Test
  void insideSignalUpdatesCurrentZone() {

    var session =
        session();

    var transition =
        session.apply(
            PresenceState.INSIDE,
            "TRIAGE-A",
            PresenceSourceType.BLE,
            "BLE-01",
            Instant.now());

    assertTrue(
        transition.changed());

    assertTrue(
        session.isInsideFacility());

    assertEquals(
        "TRIAGE-A",
        session.getCurrentZoneId());
  }

  @Test
  void movingZoneChangesZoneEntryTime() {

    var session =
        session();

    Instant first =
        Instant.parse(
            "2026-09-10T10:00:00Z");

    Instant second =
        first.plusSeconds(60);

    session.apply(
        PresenceState.INSIDE,
        "TRIAGE-A",
        PresenceSourceType.BLE,
        "BLE-01",
        first);

    session.apply(
        PresenceState.INSIDE,
        "WAITING-A",
        PresenceSourceType.UWB,
        "UWB-01",
        second);

    assertEquals(
        second,
        session.getZoneEnteredAt());
  }

  @Test
  void outOfOrderSignalIsRejected() {

    var session =
        session();

    Instant now =
        Instant.now();

    session.apply(
        PresenceState.INSIDE,
        "TRIAGE-A",
        PresenceSourceType.BLE,
        "BLE-01",
        now);

    assertThrows(
        IllegalStateException.class,
        () ->
            session.apply(
                PresenceState.OUTSIDE,
                null,
                PresenceSourceType.SYSTEM,
                null,
                now.minusSeconds(10)));
  }
}

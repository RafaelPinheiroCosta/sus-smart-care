package br.com.sussmartcare.telemetry.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelemetryModeDomainTest {

  @Test
  void legacyConstructorDefaultsToSpot() {

    TelemetrySession session =
        new TelemetrySession(
            UUID.randomUUID(),
            null,
            null,
            "TRIAGE");

    assertEquals(
        TelemetryMode.SPOT,
        session.getMode());
  }

  @Test
  void continuousModeCanBeSelected() {

    TelemetrySession session =
        new TelemetrySession(
            UUID.randomUUID(),
            null,
            null,
            "TRIAGE",
            TelemetryMode.CONTINUOUS);

    assertEquals(
        TelemetryMode.CONTINUOUS,
        session.getMode());
  }
}

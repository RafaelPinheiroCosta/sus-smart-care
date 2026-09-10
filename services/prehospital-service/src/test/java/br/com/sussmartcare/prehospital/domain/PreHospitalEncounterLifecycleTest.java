package br.com.sussmartcare.prehospital.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PreHospitalEncounterLifecycleTest {

  @Test
  void encounterStartsEnRoute() {

    PreHospitalEncounter encounter =
        newEncounter();

    assertEquals(
        PreHospitalEncounterStatus.EN_ROUTE,
        encounter.getStatus());
  }

  @Test
  void encounterCanArrive() {

    PreHospitalEncounter encounter =
        newEncounter();

    encounter.markArrived();

    assertEquals(
        PreHospitalEncounterStatus.ARRIVED,
        encounter.getStatus());

    assertNotNull(
        encounter.getArrivedAt());
  }

  @Test
  void encounterCanBeCancelled() {

    PreHospitalEncounter encounter =
        newEncounter();

    encounter.cancel(
        "Vehicle failure");

    assertEquals(
        PreHospitalEncounterStatus.CANCELLED,
        encounter.getStatus());

    assertEquals(
        "Vehicle failure",
        encounter.getCancellationReason());

    assertNotNull(
        encounter.getCancelledAt());
  }

  @Test
  void terminalEncounterCannotTransitionAgain() {

    PreHospitalEncounter encounter =
        newEncounter();

    encounter.markArrived();

    assertThrows(
        IllegalStateException.class,
        () ->
            encounter.cancel(
                "Invalid transition"));

    assertThrows(
        IllegalStateException.class,
        () ->
            encounter.updateEta(
                Instant.now()));
  }

  private PreHospitalEncounter newEncounter() {

    return new PreHospitalEncounter(
        UUID.randomUUID(),
        null,
        "SAMU-LIFE-01",
        UUID.randomUUID(),
        Instant.now().plusSeconds(600));
  }
}

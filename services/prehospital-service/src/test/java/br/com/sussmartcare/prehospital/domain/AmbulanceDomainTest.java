package br.com.sussmartcare.prehospital.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AmbulanceDomainTest {

  @Test
  void newAmbulanceStartsOperational() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-USA-01",
            "USA 01");

    assertEquals(
        AmbulanceOperationalStatus.ACTIVE,
        ambulance.getOperationalStatus());

    assertTrue(
        ambulance.isOperational());
  }

  @Test
  void ambulanceCanChangeOperationalState() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-USB-02",
            "USB 02");

    ambulance.putInMaintenance();

    assertEquals(
        AmbulanceOperationalStatus.MAINTENANCE,
        ambulance.getOperationalStatus());

    assertFalse(
        ambulance.isOperational());

    ambulance.activate();

    assertTrue(
        ambulance.isOperational());

    ambulance.putOutOfService();

    assertEquals(
        AmbulanceOperationalStatus.OUT_OF_SERVICE,
        ambulance.getOperationalStatus());
  }

  @Test
  void coverageHasTemporalLifecycle() {

    AmbulanceCoverage coverage =
        new AmbulanceCoverage(
            "SAMU-USA-03",
            UUID.randomUUID());

    assertTrue(
        coverage.isActive());

    coverage.close();

    assertFalse(
        coverage.isActive());
  }

  @Test
  void closedCoverageCannotBeClosedAgain() {

    AmbulanceCoverage coverage =
        new AmbulanceCoverage(
            "SAMU-USB-04",
            UUID.randomUUID());

    coverage.close();

    assertThrows(
        IllegalStateException.class,
        coverage::close);
  }
}

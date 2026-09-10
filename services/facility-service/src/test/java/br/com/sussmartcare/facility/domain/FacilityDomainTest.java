package br.com.sussmartcare.facility.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FacilityDomainTest {

  @Test
  void facilityStartsActive() {

    HealthFacility facility =
        new HealthFacility(
            "UPA-01",
            "UPA Central",
            FacilityType.UPA);

    assertNotNull(facility.getId());
    assertTrue(facility.isActive());
    assertEquals("UPA-01", facility.getCode());
  }

  @Test
  void zoneBelongsToFacility() {

    UUID facilityId = UUID.randomUUID();

    CareZone zone =
        new CareZone(
            facilityId,
            "TRIAGE-01",
            "Triagem",
            CareZoneType.TRIAGE);

    assertEquals(facilityId, zone.getFacilityId());
    assertTrue(zone.isActive());
  }

  @Test
  void bedOperationalStateIsIndependentFromOccupation() {

    Bed bed =
        new Bed(
            UUID.randomUUID(),
            "OBS-01",
            BedType.BED);

    assertTrue(bed.isOperational());

    bed.putInMaintenance();

    assertFalse(bed.isOperational());
    assertEquals(
        BedOperationalStatus.MAINTENANCE,
        bed.getOperationalStatus());

    bed.activate();

    assertTrue(bed.isOperational());
  }

  @Test
  void occupationIsTemporalAndCannotCloseTwice() {

    BedOccupation occupation =
        new BedOccupation(
            UUID.randomUUID(),
            UUID.randomUUID());

    assertTrue(occupation.isActive());

    occupation.close();

    assertFalse(occupation.isActive());
    assertNotNull(occupation.getEndedAt());

    assertThrows(
        IllegalStateException.class,
        occupation::close);
  }

  @Test
  void facilityRejectsBlankCode() {

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new HealthFacility(
                " ",
                "UPA Central",
                FacilityType.UPA));
  }
}

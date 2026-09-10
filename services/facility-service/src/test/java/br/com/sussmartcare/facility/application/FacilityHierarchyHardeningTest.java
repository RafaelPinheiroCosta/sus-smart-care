package br.com.sussmartcare.facility.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.sussmartcare.facility.domain.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacilityHierarchyHardeningTest {

  HealthFacilityRepository facilities;
  CareZoneRepository zones;
  BedRepository beds;
  BedOccupationRepository occupations;
  FacilityApplicationService service;

  @BeforeEach
  void setUp() {

    facilities =
        mock(
            HealthFacilityRepository.class);

    zones =
        mock(
            CareZoneRepository.class);

    beds =
        mock(
            BedRepository.class);

    occupations =
        mock(
            BedOccupationRepository.class);

    service =
        new FacilityApplicationService(
            facilities,
            zones,
            beds,
            occupations);
  }

  @Test
  void cannotActivateZoneWhenFacilityInactive() {

    HealthFacility facility =
        new HealthFacility(
            "FAC-01",
            "Facility",
            FacilityType.HOSPITAL);

    facility.deactivate();

    CareZone zone =
        new CareZone(
            facility.getId(),
            "ZONE-01",
            "Observation",
            CareZoneType.OBSERVATION);

    zone.deactivate();

    when(
        zones.findById(
            zone.getId()))
        .thenReturn(
            Optional.of(zone));

    when(
        facilities.findById(
            facility.getId()))
        .thenReturn(
            Optional.of(facility));

    assertThrows(
        FacilityConflictException.class,
        () ->
            service.setZoneActive(
                zone.getId(),
                true));

    verify(
        zones,
        never())
        .save(any());
  }

  @Test
  void cannotActivateBedWhenZoneInactive() {

    HealthFacility facility =
        new HealthFacility(
            "FAC-02",
            "Facility",
            FacilityType.HOSPITAL);

    CareZone zone =
        new CareZone(
            facility.getId(),
            "ZONE-02",
            "Observation",
            CareZoneType.OBSERVATION);

    zone.deactivate();

    Bed bed =
        new Bed(
            zone.getId(),
            "BED-02",
            BedType.BED);

    when(
        beds.findById(
            bed.getId()))
        .thenReturn(
            Optional.of(bed));

    when(
        zones.findById(
            zone.getId()))
        .thenReturn(
            Optional.of(zone));

    assertThrows(
        FacilityConflictException.class,
        () ->
            service.setBedOperationalStatus(
                bed.getId(),
                BedOperationalStatus.ACTIVE));
  }

  @Test
  void cannotActivateBedWhenFacilityInactive() {

    HealthFacility facility =
        new HealthFacility(
            "FAC-03",
            "Facility",
            FacilityType.HOSPITAL);

    facility.deactivate();

    CareZone zone =
        new CareZone(
            facility.getId(),
            "ZONE-03",
            "Observation",
            CareZoneType.OBSERVATION);

    Bed bed =
        new Bed(
            zone.getId(),
            "BED-03",
            BedType.BED);

    when(
        beds.findById(
            bed.getId()))
        .thenReturn(
            Optional.of(bed));

    when(
        zones.findById(
            zone.getId()))
        .thenReturn(
            Optional.of(zone));

    when(
        facilities.findById(
            facility.getId()))
        .thenReturn(
            Optional.of(facility));

    assertThrows(
        FacilityConflictException.class,
        () ->
            service.setBedOperationalStatus(
                bed.getId(),
                BedOperationalStatus.ACTIVE));
  }

  @Test
  void cannotOccupyBedWhenZoneInactive() {

    HealthFacility facility =
        new HealthFacility(
            "FAC-04",
            "Facility",
            FacilityType.HOSPITAL);

    CareZone zone =
        new CareZone(
            facility.getId(),
            "ZONE-04",
            "Observation",
            CareZoneType.OBSERVATION);

    zone.deactivate();

    Bed bed =
        new Bed(
            zone.getId(),
            "BED-04",
            BedType.BED);

    when(
        beds.findById(
            bed.getId()))
        .thenReturn(
            Optional.of(bed));

    when(
        zones.findById(
            zone.getId()))
        .thenReturn(
            Optional.of(zone));

    assertThrows(
        FacilityConflictException.class,
        () ->
            service.occupyBed(
                bed.getId(),
                UUID.randomUUID()));

    verifyNoInteractions(
        occupations);
  }

  @Test
  void cannotOccupyBedWhenFacilityInactive() {

    HealthFacility facility =
        new HealthFacility(
            "FAC-05",
            "Facility",
            FacilityType.HOSPITAL);

    facility.deactivate();

    CareZone zone =
        new CareZone(
            facility.getId(),
            "ZONE-05",
            "Observation",
            CareZoneType.OBSERVATION);

    Bed bed =
        new Bed(
            zone.getId(),
            "BED-05",
            BedType.BED);

    when(
        beds.findById(
            bed.getId()))
        .thenReturn(
            Optional.of(bed));

    when(
        zones.findById(
            zone.getId()))
        .thenReturn(
            Optional.of(zone));

    when(
        facilities.findById(
            facility.getId()))
        .thenReturn(
            Optional.of(facility));

    assertThrows(
        FacilityConflictException.class,
        () ->
            service.occupyBed(
                bed.getId(),
                UUID.randomUUID()));

    verifyNoInteractions(
        occupations);
  }
}

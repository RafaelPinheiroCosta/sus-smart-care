package br.com.sussmartcare.facility.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.facility.domain.Bed;
import br.com.sussmartcare.facility.domain.BedOccupationRepository;
import br.com.sussmartcare.facility.domain.BedOperationalStatus;
import br.com.sussmartcare.facility.domain.BedRepository;
import br.com.sussmartcare.facility.domain.BedType;
import br.com.sussmartcare.facility.domain.CareZone;
import br.com.sussmartcare.facility.domain.CareZoneRepository;
import br.com.sussmartcare.facility.domain.CareZoneType;
import br.com.sussmartcare.facility.domain.FacilityType;
import br.com.sussmartcare.facility.domain.HealthFacility;
import br.com.sussmartcare.facility.domain.HealthFacilityRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacilityStateApplicationServiceTest {

  @Mock
  HealthFacilityRepository facilityRepository;

  @Mock
  CareZoneRepository zoneRepository;

  @Mock
  BedRepository bedRepository;

  @Mock
  BedOccupationRepository occupationRepository;

  @InjectMocks
  FacilityApplicationService service;

  @Test
  void deactivatesFacilityAndPersistsState() {

    HealthFacility facility =
        new HealthFacility(
            "UPA-01",
            "UPA Central",
            FacilityType.UPA);

    when(facilityRepository.findById(
        facility.getId()))
        .thenReturn(Optional.of(facility));

    when(facilityRepository.save(
        any(HealthFacility.class)))
        .thenAnswer(invocation ->
            invocation.getArgument(0));

    HealthFacility result =
        service.setFacilityActive(
            facility.getId(),
            false);

    assertFalse(result.isActive());

    verify(facilityRepository)
        .save(facility);
  }

  @Test
  void deactivatesZoneAndPersistsState() {

    CareZone zone =
        new CareZone(
            UUID.randomUUID(),
            "OBS-01",
            "Observation",
            CareZoneType.OBSERVATION);

    when(zoneRepository.findById(
        zone.getId()))
        .thenReturn(Optional.of(zone));

    when(zoneRepository.save(
        any(CareZone.class)))
        .thenAnswer(invocation ->
            invocation.getArgument(0));

    CareZone result =
        service.setZoneActive(
            zone.getId(),
            false);

    assertFalse(result.isActive());

    verify(zoneRepository)
        .save(zone);
  }

  @Test
  void putsBedInMaintenanceAndPersistsState() {

    Bed bed =
        new Bed(
            UUID.randomUUID(),
            "BED-01",
            BedType.BED);

    when(bedRepository.findById(
        bed.getId()))
        .thenReturn(Optional.of(bed));

    when(bedRepository.save(
        any(Bed.class)))
        .thenAnswer(invocation ->
            invocation.getArgument(0));

    Bed result =
        service.setBedOperationalStatus(
            bed.getId(),
            BedOperationalStatus.MAINTENANCE);

    assertEquals(
        BedOperationalStatus.MAINTENANCE,
        result.getOperationalStatus());

    verify(bedRepository)
        .save(bed);
  }
}

package br.com.sussmartcare.prehospital.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.prehospital.domain.Ambulance;
import br.com.sussmartcare.prehospital.domain.AmbulanceCoverage;
import br.com.sussmartcare.prehospital.domain.AmbulanceOperationalStatus;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceCoverageRepository;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmbulanceApplicationServiceTest {

  @Mock
  AmbulanceRepository ambulanceRepository;

  @Mock
  AmbulanceCoverageRepository
      coverageRepository;

  AmbulanceApplicationService service;

  @BeforeEach
  void setUp() {

    service =
        new AmbulanceApplicationService(
            ambulanceRepository,
            coverageRepository);
  }

  @Test
  void duplicateAmbulanceIsRejected() {

    when(
        ambulanceRepository.existsById(
            "SAMU-01"))
        .thenReturn(true);

    assertThrows(
        PreHospitalConflictException.class,
        () ->
            service.create(
                "SAMU-01",
                "USA 01"));
  }

  @Test
  void maintenanceAmbulanceCannotReceiveCoverage() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-02",
            "USA 02");

    ambulance.putInMaintenance();

    when(
        ambulanceRepository.findById(
            "SAMU-02"))
        .thenReturn(
            Optional.of(ambulance));

    assertThrows(
        PreHospitalConflictException.class,
        () ->
            service.assignCoverage(
                "SAMU-02",
                UUID.randomUUID()));
  }

  @Test
  void ambulanceCanReceiveCoverage() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-03",
            "USA 03");

    UUID facilityId =
        UUID.randomUUID();

    when(
        ambulanceRepository.findById(
            "SAMU-03"))
        .thenReturn(
            Optional.of(ambulance));

    when(
        coverageRepository
            .findByAmbulanceIdAndEndedAtIsNull(
                "SAMU-03"))
        .thenReturn(Optional.empty());

    when(
        coverageRepository.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    AmbulanceCoverage coverage =
        service.assignCoverage(
            "SAMU-03",
            facilityId);

    assertTrue(
        coverage.isActive());

    assertEquals(
        facilityId,
        coverage.getFacilityId());
  }

  @Test
  void operationalStatusCanBeChanged() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-04",
            "USA 04");

    when(
        ambulanceRepository.findById(
            "SAMU-04"))
        .thenReturn(
            Optional.of(ambulance));

    when(
        ambulanceRepository.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    Ambulance updated =
        service.updateOperationalStatus(
            "SAMU-04",
            AmbulanceOperationalStatus.OUT_OF_SERVICE);

    assertEquals(
        AmbulanceOperationalStatus.OUT_OF_SERVICE,
        updated.getOperationalStatus());
  }
}

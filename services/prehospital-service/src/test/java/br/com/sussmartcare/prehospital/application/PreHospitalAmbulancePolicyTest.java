package br.com.sussmartcare.prehospital.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.prehospital.domain.Ambulance;
import br.com.sussmartcare.prehospital.domain.AmbulanceCoverage;
import br.com.sussmartcare.prehospital.domain.PreHospitalEncounter;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceCoverageRepository;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceRepository;
import br.com.sussmartcare.prehospital.infrastructure.PreHospitalRepository;
import br.com.sussmartcare.prehospital.infrastructure.outbox.EventOutbox;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreHospitalAmbulancePolicyTest {

  @Mock
  PreHospitalRepository encounterRepository;

  @Mock
  EventOutbox outbox;

  @Mock
  AmbulanceRepository ambulanceRepository;

  @Mock
  AmbulanceCoverageRepository coverageRepository;

  PreHospitalApplicationService service;

  @BeforeEach
  void setUp() {

    service =
        new PreHospitalApplicationService(
            encounterRepository,
            outbox,
            ambulanceRepository,
            coverageRepository);
  }

  @Test
  void unknownAmbulanceCannotOpenEncounter() {

    when(
        ambulanceRepository.findById(
            "SAMU-10"))
        .thenReturn(Optional.empty());

    assertThrows(
        PreHospitalNotFoundException.class,
        () ->
            service.create(
                UUID.randomUUID(),
                null,
                "SAMU-10",
                UUID.randomUUID(),
                Instant.now()));
  }

  @Test
  void unavailableAmbulanceCannotOpenEncounter() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-11",
            "USA 11");

    ambulance.putOutOfService();

    when(
        ambulanceRepository.findById(
            "SAMU-11"))
        .thenReturn(
            Optional.of(ambulance));

    assertThrows(
        PreHospitalConflictException.class,
        () ->
            service.create(
                UUID.randomUUID(),
                null,
                "SAMU-11",
                UUID.randomUUID(),
                Instant.now()));
  }

  @Test
  void ambulanceWithoutCoverageCannotOpenEncounter() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-12",
            "USA 12");

    when(
        ambulanceRepository.findById(
            "SAMU-12"))
        .thenReturn(
            Optional.of(ambulance));

    when(
        coverageRepository
            .findByAmbulanceIdAndEndedAtIsNull(
                "SAMU-12"))
        .thenReturn(Optional.empty());

    assertThrows(
        PreHospitalConflictException.class,
        () ->
            service.create(
                UUID.randomUUID(),
                null,
                "SAMU-12",
                UUID.randomUUID(),
                Instant.now()));
  }

  @Test
  void operationalCoveredAmbulanceCanOpenEncounter() {

    Ambulance ambulance =
        new Ambulance(
            "SAMU-13",
            "USA 13");

    UUID patientId =
        UUID.randomUUID();

    UUID facilityId =
        UUID.randomUUID();

    Instant eta =
        Instant.now().plusSeconds(600);

    AmbulanceCoverage coverage =
        new AmbulanceCoverage(
            "SAMU-13",
            UUID.randomUUID());

    when(
        ambulanceRepository.findById(
            "SAMU-13"))
        .thenReturn(
            Optional.of(ambulance));

    when(
        coverageRepository
            .findByAmbulanceIdAndEndedAtIsNull(
                "SAMU-13"))
        .thenReturn(
            Optional.of(coverage));

    when(
        encounterRepository.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    PreHospitalEncounter encounter =
        service.create(
            patientId,
            null,
            "SAMU-13",
            facilityId,
            eta);

    assertEquals(
        "SAMU-13",
        encounter.getAmbulanceId());

    verify(outbox)
        .append(
            eq("pre-hospital-encounter-created"),
            anyString(),
            any());
  }
}

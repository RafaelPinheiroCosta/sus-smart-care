package br.com.sussmartcare.prehospital.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.prehospital.domain.PreHospitalEncounter;
import br.com.sussmartcare.prehospital.domain.PreHospitalEncounterStatus;
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
class PreHospitalLifecycleApplicationServiceTest {

  @Mock
  PreHospitalRepository repository;

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
            repository,
            outbox,
            ambulanceRepository,
            coverageRepository);
  }

  @Test
  void arrivalChangesStateAndPublishesEvent() {

    PreHospitalEncounter encounter =
        newEncounter();

    when(
        repository.findById(
            encounter.getId()))
        .thenReturn(
            Optional.of(encounter));

    when(repository.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    PreHospitalEncounter result =
        service.arrive(
            encounter.getId());

    assertEquals(
        PreHospitalEncounterStatus.ARRIVED,
        result.getStatus());

    assertNotNull(
        result.getArrivedAt());

    verify(outbox)
        .append(
            eq("pre-hospital-encounter-arrived"),
            eq(encounter.getId().toString()),
            any());
  }

  @Test
  void cancellationChangesStateAndPublishesEvent() {

    PreHospitalEncounter encounter =
        newEncounter();

    when(
        repository.findById(
            encounter.getId()))
        .thenReturn(
            Optional.of(encounter));

    when(repository.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    PreHospitalEncounter result =
        service.cancel(
            encounter.getId(),
            "Mechanical failure");

    assertEquals(
        PreHospitalEncounterStatus.CANCELLED,
        result.getStatus());

    assertEquals(
        "Mechanical failure",
        result.getCancellationReason());

    verify(outbox)
        .append(
            eq("pre-hospital-encounter-cancelled"),
            eq(encounter.getId().toString()),
            any());
  }

  @Test
  void secondTerminalTransitionIsRejected() {

    PreHospitalEncounter encounter =
        newEncounter();

    encounter.markArrived();

    when(
        repository.findById(
            encounter.getId()))
        .thenReturn(
            Optional.of(encounter));

    assertThrows(
        IllegalStateException.class,
        () ->
            service.cancel(
                encounter.getId(),
                "Invalid transition"));
  }

  private PreHospitalEncounter newEncounter() {

    return new PreHospitalEncounter(
        UUID.randomUUID(),
        null,
        "SAMU-LIFE-02",
        UUID.randomUUID(),
        Instant.now().plusSeconds(600));
  }
}

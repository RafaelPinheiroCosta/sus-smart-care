package br.com.sussmartcare.presence.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.presence.domain.PresenceEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PresenceAccessApplicationServiceTest {

  @Test
  void recordAllowsMatchingPatientFromAccessibleVisit() {

    PresenceApplicationService presence =
        mock(PresenceApplicationService.class);

    VisitAccessPort visitAccess =
        mock(VisitAccessPort.class);

    PresenceAccessApplicationService service =
        new PresenceAccessApplicationService(
            presence,
            visitAccess);

    UUID visitId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    PresenceEvent expected =
        mock(PresenceEvent.class);

    when(visitAccess.assertCurrentActorCanAccess(visitId))
        .thenReturn(patientId);

    when(presence.record(
        visitId,
        patientId,
        "ENTERED_FACILITY",
        "RECEPTION",
        "APP",
        null))
        .thenReturn(expected);

    PresenceEvent actual =
        service.record(
            visitId,
            patientId,
            "ENTERED_FACILITY",
            "RECEPTION",
            "APP",
            null);

    assertSame(expected, actual);
  }

  @Test
  void recordRejectsPatientThatDoesNotBelongToVisit() {

    PresenceApplicationService presence =
        mock(PresenceApplicationService.class);

    VisitAccessPort visitAccess =
        mock(VisitAccessPort.class);

    PresenceAccessApplicationService service =
        new PresenceAccessApplicationService(
            presence,
            visitAccess);

    UUID visitId = UUID.randomUUID();
    UUID requestPatientId = UUID.randomUUID();
    UUID realPatientId = UUID.randomUUID();

    when(visitAccess.assertCurrentActorCanAccess(visitId))
        .thenReturn(realPatientId);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.record(
            visitId,
            requestPatientId,
            "ENTERED_FACILITY",
            "RECEPTION",
            "APP",
            Instant.now()));

    verify(
        presence,
        never())
        .record(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void historyDoesNotReadEventsWhenAccessIsDenied() {

    PresenceApplicationService presence =
        mock(PresenceApplicationService.class);

    VisitAccessPort visitAccess =
        mock(VisitAccessPort.class);

    PresenceAccessApplicationService service =
        new PresenceAccessApplicationService(
            presence,
            visitAccess);

    UUID visitId = UUID.randomUUID();

    doThrow(
        new VisitAccessDeniedException("denied"))
        .when(visitAccess)
        .assertCurrentActorCanAccess(visitId);

    assertThrows(
        VisitAccessDeniedException.class,
        () -> service.history(visitId));

    verify(
        presence,
        never())
        .history(visitId);
  }

  @Test
  void historyReadsEventsAfterAuthorization() {

    PresenceApplicationService presence =
        mock(PresenceApplicationService.class);

    VisitAccessPort visitAccess =
        mock(VisitAccessPort.class);

    PresenceAccessApplicationService service =
        new PresenceAccessApplicationService(
            presence,
            visitAccess);

    UUID visitId = UUID.randomUUID();
    UUID patientId = UUID.randomUUID();

    List<PresenceEvent> expected =
        List.of(mock(PresenceEvent.class));

    when(visitAccess.assertCurrentActorCanAccess(visitId))
        .thenReturn(patientId);

    when(presence.history(visitId))
        .thenReturn(expected);

    List<PresenceEvent> actual =
        service.history(visitId);

    assertSame(expected, actual);
  }
}
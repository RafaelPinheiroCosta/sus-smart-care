package br.com.sussmartcare.notification.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.notification.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationAccessApplicationServiceTest {

  @Test
  void historyValidatesPatientAccessBeforeReading() {

    NotificationApplicationService notification =
        mock(NotificationApplicationService.class);

    PatientAccessPort patientAccess =
        mock(PatientAccessPort.class);

    NotificationAccessApplicationService service =
        new NotificationAccessApplicationService(
            notification,
            patientAccess);

    UUID patientId =
        UUID.randomUUID();

    List<Notification> expected =
        List.of(
            mock(Notification.class));

    when(
        notification.history(patientId))
        .thenReturn(expected);

    List<Notification> actual =
        service.history(patientId);

    assertSame(
        expected,
        actual);

    verify(patientAccess)
        .assertCurrentActorCanAccess(patientId);

    verify(notification)
        .history(patientId);
  }

  @Test
  void historyDoesNotReadWhenAccessIsDenied() {

    NotificationApplicationService notification =
        mock(NotificationApplicationService.class);

    PatientAccessPort patientAccess =
        mock(PatientAccessPort.class);

    NotificationAccessApplicationService service =
        new NotificationAccessApplicationService(
            notification,
            patientAccess);

    UUID patientId =
        UUID.randomUUID();

    doThrow(
        new PatientAccessDeniedException("denied"))
        .when(patientAccess)
        .assertCurrentActorCanAccess(patientId);

    assertThrows(
        PatientAccessDeniedException.class,
        () ->
            service.history(patientId));

    verify(notification, never())
        .history(patientId);
  }
}
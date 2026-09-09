package br.com.sussmartcare.notification.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.notification.domain.Notification;
import br.com.sussmartcare.notification.domain.NotificationPreference;
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

    UUID patientId = UUID.randomUUID();

    List<Notification> expected =
        List.of(mock(Notification.class));

    when(notification.history(patientId))
        .thenReturn(expected);

    List<Notification> actual =
        service.history(patientId);

    assertSame(expected, actual);

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

    UUID patientId = UUID.randomUUID();

    doThrow(
        new PatientAccessDeniedException("denied"))
        .when(patientAccess)
        .assertCurrentActorCanAccess(patientId);

    assertThrows(
        PatientAccessDeniedException.class,
        () -> service.history(patientId));

    verify(notification, never())
        .history(patientId);
  }

  @Test
  void preferenceValidatesAccessBeforeSaving() {

    NotificationApplicationService notification =
        mock(NotificationApplicationService.class);

    PatientAccessPort patientAccess =
        mock(PatientAccessPort.class);

    NotificationAccessApplicationService service =
        new NotificationAccessApplicationService(
            notification,
            patientAccess);

    UUID patientId = UUID.randomUUID();

    NotificationPreference expected =
        mock(NotificationPreference.class);

    when(notification.preference(
        patientId,
        true,
        "PUSH",
        "DISPLAY"))
        .thenReturn(expected);

    NotificationPreference actual =
        service.preference(
            patientId,
            true,
            "PUSH",
            "DISPLAY");

    assertSame(expected, actual);

    verify(patientAccess)
        .assertCurrentActorCanAccess(patientId);

    verify(notification)
        .preference(
            patientId,
            true,
            "PUSH",
            "DISPLAY");
  }

  @Test
  void preferenceDoesNotSaveWhenAccessIsDenied() {

    NotificationApplicationService notification =
        mock(NotificationApplicationService.class);

    PatientAccessPort patientAccess =
        mock(PatientAccessPort.class);

    NotificationAccessApplicationService service =
        new NotificationAccessApplicationService(
            notification,
            patientAccess);

    UUID patientId = UUID.randomUUID();

    doThrow(
        new PatientAccessDeniedException("denied"))
        .when(patientAccess)
        .assertCurrentActorCanAccess(patientId);

    assertThrows(
        PatientAccessDeniedException.class,
        () -> service.preference(
            patientId,
            true,
            "PUSH",
            "DISPLAY"));

    verify(notification, never())
        .preference(
            patientId,
            true,
            "PUSH",
            "DISPLAY");
  }
}
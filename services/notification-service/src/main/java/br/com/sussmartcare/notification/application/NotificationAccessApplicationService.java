package br.com.sussmartcare.notification.application;

import br.com.sussmartcare.notification.domain.Notification;
import br.com.sussmartcare.notification.domain.NotificationPreference;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationAccessApplicationService {

  private final NotificationApplicationService notification;
  private final PatientAccessPort patientAccess;

  public NotificationAccessApplicationService(
      NotificationApplicationService notification,
      PatientAccessPort patientAccess) {

    this.notification = notification;
    this.patientAccess = patientAccess;
  }

  public NotificationPreference preference(
      UUID patientId,
      boolean hasSmartphone,
      String preferredChannel,
      String fallbackChannel) {

    patientAccess.assertCurrentActorCanAccess(patientId);

    return notification.preference(
        patientId,
        hasSmartphone,
        preferredChannel,
        fallbackChannel);
  }

  public List<Notification> history(UUID patientId) {

    patientAccess.assertCurrentActorCanAccess(patientId);

    return notification.history(patientId);
  }
}
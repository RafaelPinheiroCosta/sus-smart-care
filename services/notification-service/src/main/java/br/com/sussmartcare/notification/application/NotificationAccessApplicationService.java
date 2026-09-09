package br.com.sussmartcare.notification.application;

import br.com.sussmartcare.notification.domain.Notification;
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

  public List<Notification> history(UUID patientId) {

    patientAccess.assertCurrentActorCanAccess(patientId);

    return notification.history(patientId);
  }
}
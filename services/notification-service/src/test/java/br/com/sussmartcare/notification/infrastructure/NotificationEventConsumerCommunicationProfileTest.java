package br.com.sussmartcare.notification.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.notification.application.NotificationApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationEventConsumerCommunicationProfileTest {

  @Test
  void projectsDisplayAndVerbalPreferenceFromRegistryEvent()
      throws Exception {

    ProcessedEventRepository processed =
        mock(ProcessedEventRepository.class);

    VisitPatientLinkRepository links =
        mock(VisitPatientLinkRepository.class);

    PendingVisitNotificationRepository pending =
        mock(PendingVisitNotificationRepository.class);

    NotificationApplicationService app =
        mock(NotificationApplicationService.class);

    NotificationEventConsumer consumer =
        new NotificationEventConsumer(
            new ObjectMapper(),
            processed,
            links,
            pending,
            app);

    UUID eventId =
        UUID.randomUUID();

    UUID patientId =
        UUID.randomUUID();

    when(processed.existsById(eventId))
        .thenReturn(false);

    String raw =
        """
        {
          "eventId": "%s",
          "data": {
            "patientId": "%s",
            "hasSmartphone": false,
            "queueCallMode": "DISPLAY_AND_VERBAL"
          }
        }
        """.formatted(
            eventId,
            patientId);

    consumer.communicationProfile(raw);

    verify(app)
        .preference(
            patientId,
            false,
            "DISPLAY",
            "STAFF_ASSISTED");

    verify(processed)
        .save(any(ProcessedEvent.class));
  }
}
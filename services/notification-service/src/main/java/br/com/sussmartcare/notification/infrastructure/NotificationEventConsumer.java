package br.com.sussmartcare.notification.infrastructure;

import br.com.sussmartcare.notification.application.NotificationApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationEventConsumer {

  private final ObjectMapper mapper;
  private final ProcessedEventRepository processed;
  private final VisitPatientLinkRepository links;
  private final PendingVisitNotificationRepository pending;
  private final NotificationApplicationService app;

  public NotificationEventConsumer(
      ObjectMapper mapper,
      ProcessedEventRepository processed,
      VisitPatientLinkRepository links,
      PendingVisitNotificationRepository pending,
      NotificationApplicationService app) {

    this.mapper = mapper;
    this.processed = processed;
    this.links = links;
    this.pending = pending;
    this.app = app;
  }

  private Envelope envelope(String raw)
      throws Exception {

    var root =
        mapper.readTree(raw);

    UUID eventId =
        UUID.fromString(
            root.path("eventId").asText());

    if (processed.existsById(eventId)) {
      return null;
    }

    return new Envelope(
        eventId,
        root.path("data"));
  }

  @KafkaListener(
      topics = "patient-communication-profile-updated",
      groupId = "notification-communication-profile")
  @Transactional
  public void communicationProfile(
      String raw)
      throws Exception {

    var envelope =
        envelope(raw);

    if (envelope == null) {
      return;
    }

    var data =
        envelope.data();

    UUID patientId =
        UUID.fromString(
            data.path("patientId").asText());

    boolean hasSmartphone =
        data.path("hasSmartphone")
            .asBoolean(false);

    String mode =
        data.path("queueCallMode")
            .asText();

    switch (mode) {

      case "MOBILE" ->
          app.preference(
              patientId,
              hasSmartphone,
              "PUSH",
              null);

      case "DISPLAY" ->
          app.preference(
              patientId,
              hasSmartphone,
              "DISPLAY",
              null);

      case "VERBAL" ->
          app.preference(
              patientId,
              hasSmartphone,
              "STAFF_ASSISTED",
              null);

      case "DISPLAY_AND_VERBAL" ->
          app.preference(
              patientId,
              hasSmartphone,
              "DISPLAY",
              "STAFF_ASSISTED");

      default ->
          throw new IllegalArgumentException(
              "QueueCallMode nao suportado: " + mode);
    }

    processed.save(
        new ProcessedEvent(
            envelope.eventId(),
            "notification-communication-profile"));
  }

  @KafkaListener(
      topics = "pre-visit-created",
      groupId = "notification-visit-map")
  @Transactional
  public void visit(String raw)
      throws Exception {

    var envelope = envelope(raw);

    if (envelope == null) {
      return;
    }

    var data = envelope.data();

    UUID visitId =
        UUID.fromString(
            data.path("visitId").asText());

    UUID patientId =
        UUID.fromString(
            data.path("patientId").asText());

    links.save(
        new VisitPatientLink(
            visitId,
            patientId));

    for (var item :
        pending.findByVisitIdOrderByCreatedAtAsc(
            visitId)) {

      app.sendAuto(
          patientId,
          visitId,
          item.getType(),
          item.getMessage());

      pending.delete(item);
    }

    processed.save(
        new ProcessedEvent(
            envelope.eventId(),
            "notification-visit-map"));
  }

  @KafkaListener(
      topics = "patient-records-merged",
      groupId = "notification-patient-merge")
  @Transactional
  public void merge(String raw)
      throws Exception {

    var envelope = envelope(raw);

    if (envelope == null) {
      return;
    }

    var data = envelope.data();

    UUID source =
        UUID.fromString(
            data.path("sourcePatientId").asText());

    UUID target =
        UUID.fromString(
            data.path("canonicalPatientId").asText());

    for (var link :
        links.findByPatientId(source)) {

      link.reassignPatient(target);
      links.save(link);
    }

    processed.save(
        new ProcessedEvent(
            envelope.eventId(),
            "notification-patient-merge"));
  }

  @KafkaListener(
      topics = "queue-return-required",
      groupId = "notification-return")
  @Transactional
  public void returnRequired(String raw)
      throws Exception {

    var envelope = envelope(raw);

    if (envelope == null) {
      return;
    }

    var data = envelope.data();

    UUID visitId =
        UUID.fromString(
            data.path("visitId").asText());

    String message =
        "Senha "
            + data.path("displayCode").asText("")
            + ": aproxime-se da unidade; sua chamada esta proxima. "
            + "Consulte a estimativa atual.";

    dispatchOrPend(
        envelope.eventId(),
        visitId,
        "RETURN_REQUIRED",
        message);

    processed.save(
        new ProcessedEvent(
            envelope.eventId(),
            "notification-return"));
  }

  @KafkaListener(
      topics = "patient-called",
      groupId = "notification-called")
  @Transactional
  public void called(String raw)
      throws Exception {

    var envelope = envelope(raw);

    if (envelope == null) {
      return;
    }

    var data = envelope.data();

    UUID visitId =
        UUID.fromString(
            data.path("visitId").asText());

    String message =
        "Senha "
            + data.path("displayCode").asText("")
            + ": seu atendimento foi chamado. "
            + "Dirija-se ao ponto indicado pela unidade.";

    dispatchOrPend(
        envelope.eventId(),
        visitId,
        "PATIENT_CALLED",
        message);

    processed.save(
        new ProcessedEvent(
            envelope.eventId(),
            "notification-called"));
  }

  @KafkaListener(
      topics = "pre-arrival-alert-created",
      groupId = "notification-prearrival")
  @Transactional
  public void prearrival(String raw)
      throws Exception {

    var envelope = envelope(raw);

    if (envelope == null) {
      return;
    }

    var data = envelope.data();

    UUID patientId =
        UUID.fromString(
            data.path("patientId").asText());

    app.send(
        patientId,
        null,
        "PRE_ARRIVAL_ALERT",
        "STAFF_CONSOLE",
        "Paciente pre-hospitalar a caminho: risco="
            + data.path("riskLevel").asText()
            + " ETA="
            + data.path("eta").asText());

    processed.save(
        new ProcessedEvent(
            envelope.eventId(),
            "notification-prearrival"));
  }

  private void dispatchOrPend(
      UUID eventId,
      UUID visitId,
      String type,
      String message) {

    var link =
        links.findById(visitId);

    if (link.isPresent()) {

      app.sendAuto(
          link.get().getPatientId(),
          visitId,
          type,
          message);

      return;
    }

    pending.save(
        new PendingVisitNotification(
            eventId,
            visitId,
            type,
            message));
  }

  private record Envelope(
      UUID eventId,
      JsonNode data) {}
}
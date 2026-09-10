package br.com.sussmartcare.clinicalquery.application;

import br.com.sussmartcare.clinicalquery.domain.ClinicalLatestObservationView;
import br.com.sussmartcare.clinicalquery.domain.ClinicalPatientView;
import br.com.sussmartcare.clinicalquery.domain.PatientProfileView;
import br.com.sussmartcare.clinicalquery.infrastructure.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProjectionConsumer {

  private final ClinicalPatientViewRepository views;
  private final PatientProfileViewRepository profiles;
  private final ClinicalLatestObservationViewRepository observations;
  private final ProcessedEventRepository processedEvents;
  private final ClinicalPatientViewCache cache;
  private final ObjectMapper mapper;

  public ProjectionConsumer(
      ClinicalPatientViewRepository views,
      PatientProfileViewRepository profiles,
      ClinicalLatestObservationViewRepository observations,
      ProcessedEventRepository processedEvents,
      ClinicalPatientViewCache cache,
      ObjectMapper mapper) {

    this.views = views;
    this.profiles = profiles;
    this.observations = observations;
    this.processedEvents = processedEvents;
    this.cache = cache;
    this.mapper = mapper;
  }

  private ClinicalPatientView view(
      UUID visitId) {

    /*
     * Multiple event topics can target the same visit concurrently.
     * Ensure the projection row exists first and then lock it until the
     * current transaction commits.
     */
    views.ensureExists(
        visitId);

    return views
        .findByVisitIdForUpdate(
            visitId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Clinical projection could not be initialized"));
  }

  private void process(
      String raw,
      String consumer,
      Consumer<JsonNode> handler)
      throws Exception {

    JsonNode envelope =
        mapper.readTree(raw);

    UUID eventId =
        UUID.fromString(
            envelope
                .path("eventId")
                .asText());

    if (
        processedEvents.existsById(
            eventId)
    ) {

      return;
    }

    handler.accept(
        envelope.path("data"));

    processedEvents.save(
        new ProcessedEvent(
            eventId,
            consumer));
  }

  private void save(
      ClinicalPatientView view) {

    views.save(view);
    refreshCache(view);
  }

  private void refreshCache(
      ClinicalPatientView view) {

    PatientProfileView profile =
        view.getPatientId() == null
            ? null
            : profiles
                .findById(
                    view.getPatientId())
                .orElse(null);

    cache.put(
        ClinicalPatientViewSnapshot.from(
            view,
            profile,
            observations
                .findByVisitIdOrderByTypeAsc(
                    view.getVisitId())));
  }

  @KafkaListener(
      topics = {
          "patient-registered",
          "patient-provisional-created",
          "patient-identity-resolved",
          "patient-profile-updated"
      },
      groupId = "clinical-query-patient-profile")
  @Transactional
  public void patientProfile(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-patient-profile",
        data -> {

          UUID patientId =
              UUID.fromString(
                  data.path("patientId")
                      .asText());

          var existing =
              profiles
                  .findById(patientId)
                  .orElse(null);

          LocalDate birthDate =
              data.hasNonNull("birthDate")
                  ? LocalDate.parse(
                      data.path("birthDate")
                          .asText())
                  : null;

          UUID mergedInto =
              uuidOrNull(
                  data,
                  "mergedIntoPatientId");

          Instant updatedAt =
              instantOrNull(
                  data,
                  "updatedAt");

          if (updatedAt == null) {
            updatedAt = Instant.now();
          }

          if (existing == null) {

            profiles.save(
                new PatientProfileView(
                    patientId,
                    data.path("fullName")
                        .asText(),
                    birthDate,
                    data.path("identityStatus")
                        .asText(),
                    mergedInto,
                    updatedAt));

            return;
          }

          existing.update(
              data.path("fullName")
                  .asText(),
              birthDate,
              data.path("identityStatus")
                  .asText(),
              mergedInto,
              updatedAt);

          profiles.save(existing);

          views.findByPatientId(
                  patientId)
              .forEach(
                  this::refreshCache);
        });
  }

  @KafkaListener(
      topics = "patient-records-merged",
      groupId = "clinical-query-patient-merge")
  @Transactional
  public void patientMerged(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-patient-merge",
        data -> {

          UUID source =
              UUID.fromString(
                  data.path("sourcePatientId")
                      .asText());

          UUID canonical =
              UUID.fromString(
                  data.path("canonicalPatientId")
                      .asText());

          for (
              var view :
                  views.findByPatientId(source)
          ) {

            view.reassignPatient(
                canonical);

            save(view);
          }
        });
  }

  @KafkaListener(
      topics = "pre-visit-created",
      groupId = "clinical-query-visit")
  @Transactional
  public void visit(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-visit",
        data -> {

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.visit(
              UUID.fromString(
                  data.path("patientId")
                      .asText()),
              "PRE_ARRIVAL");

          save(view);
        });
  }

  @KafkaListener(
      topics = "pre-anamnesis-recorded",
      groupId = "clinical-query-anamnesis")
  @Transactional
  public void anamnesis(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-anamnesis",
        data -> {

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.anamnesis(
              data.path("text")
                  .asText());

          save(view);
        });
  }

  @KafkaListener(
      topics = "patient-checked-in",
      groupId = "clinical-query-checkin")
  @Transactional
  public void checkin(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-checkin",
        data -> {

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.visit(
              UUID.fromString(
                  data.path("patientId")
                      .asText()),
              "WAITING_TRIAGE");

          save(view);
        });
  }

  @KafkaListener(
      topics = "biometric-observation-received",
      groupId = "clinical-query-biometric")
  @Transactional
  public void biometric(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-biometric",
        data -> {

          if (!data.hasNonNull("visitId")) {
            return;
          }

          UUID visitId =
              UUID.fromString(
                  data.path("visitId")
                      .asText());

          UUID patientId =
              UUID.fromString(
                  data.path("patientId")
                      .asText());

          String type =
              data.path("type")
                  .asText();

          Double value =
              data.path("value")
                  .asDouble();

          String unit =
              textOrNull(
                  data,
                  "unit");

          Instant measuredAt =
              instantOrNull(
                  data,
                  "measuredAt");

          Instant effectiveMeasuredAt =
              measuredAt == null
                  ? Instant.now()
                  : measuredAt;

          var latest =
              observations
                  .findByVisitIdAndType(
                      visitId,
                      type)
                  .orElseGet(
                      () ->
                          new ClinicalLatestObservationView(
                              visitId,
                              type,
                              value,
                              unit,
                              effectiveMeasuredAt));

          latest.updateIfNewer(
              value,
              unit,
              effectiveMeasuredAt);

          observations.save(
              latest);

          var view =
              view(visitId);

          view.biometric(
              patientId,
              type,
              value,
              unit);

          save(view);
        });
  }

  @KafkaListener(
      topics = "triage-assessment-generated",
      groupId = "clinical-query-assessment")
  @Transactional
  public void assessment(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-assessment",
        data -> {

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.assessment(
              data.path("recommendation")
                  .asText(),
              data.path("confidence")
                  .asDouble(),
              data.path("reasoning")
                  .asText(),
              data.path("modelName")
                  .asText(),
              data.path("modelVersion")
                  .asText());

          save(view);
        });
  }

  @KafkaListener(
      topics = "clinical-priority-confirmed",
      groupId = "clinical-query-priority")
  @Transactional
  public void priority(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-priority",
        data -> {

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.priority(
              data.path("priority")
                  .asText());

          save(view);
        });
  }

  @KafkaListener(
      topics = "patient-presence-changed",
      groupId = "clinical-query-presence")
  @Transactional
  public void presence(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-presence",
        data -> {

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.presence(
              data.path("eventType")
                  .asText());

          save(view);
        });
  }

  @KafkaListener(
      topics = "queue-position-estimated",
      groupId = "clinical-query-queue")
  @Transactional
  public void queue(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-queue",
        data -> {

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.queue(
              data.path("estimatedPosition")
                  .asInt(),
              data.path("estimatedMinutes")
                  .asInt(),
              data.path("status")
                  .asText());

          save(view);
        });
  }

  @KafkaListener(
      topics = "pre-arrival-alert-created",
      groupId = "clinical-query-prearrival")
  @Transactional
  public void prearrival(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-prearrival",
        data -> {

          if (!data.hasNonNull("visitId")) {
            return;
          }

          var view =
              view(
                  UUID.fromString(
                      data.path("visitId")
                          .asText()));

          view.preArrival(
              UUID.fromString(
                  data.path("patientId")
                      .asText()),
              data.path("riskLevel")
                  .asText(),
              instantOrNull(
                  data,
                  "eta"));

          save(view);
        });
  }

  @KafkaListener(
      topics = {
          "journey-triage-started",
          "journey-triage-completed",
          "journey-queued",
          "journey-patient-called",
          "journey-service-started",
          "journey-next-stage-required",
          "patient-discharged",
          "patient-transferred",
          "journey-cancelled"
      },
      groupId = "clinical-query-journey-lifecycle")
  @Transactional
  public void journeyLifecycle(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-journey-lifecycle",
        data -> {

          UUID visitId =
              UUID.fromString(
                  data.path("visitId")
                      .asText());

          var view =
              view(visitId);

          view.journey(
              uuidOrNull(
                  data,
                  "patientId"),
              textOrNull(
                  data,
                  "status"),
              textOrNull(
                  data,
                  "currentStage"),
              textOrNull(
                  data,
                  "outcome"),
              uuidOrNull(
                  data,
                  "transferFacilityId"),
              textOrNull(
                  data,
                  "note"),
              instantOrNull(
                  data,
                  "occurredAt"));

          save(view);
        });
  }

  @KafkaListener(
      topics = "telemetry-aggregate-updated",
      groupId = "clinical-query-telemetry-aggregate")
  @Transactional
  public void telemetryAggregate(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-telemetry-aggregate",
        data -> {

          if (!data.hasNonNull("visitId")) {
            return;
          }

          UUID visitId =
              UUID.fromString(
                  data.path("visitId")
                      .asText());

          var view =
              view(visitId);

          view.aggregate(
              uuidOrNull(
                  data,
                  "patientId"),
              textOrNull(
                  data,
                  "type"),
              data.path("count")
                  .asLong(),
              data.path("minimum")
                  .asDouble(),
              data.path("maximum")
                  .asDouble(),
              data.path("average")
                  .asDouble(),
              textOrNull(
                  data,
                  "unit"),
              instantOrNull(
                  data,
                  "lastMeasuredAt"));

          save(view);
        });
  }

  @KafkaListener(
      topics = "telemetry-anomaly-detected",
      groupId = "clinical-query-telemetry-anomaly")
  @Transactional
  public void telemetryAnomaly(
      String raw)
      throws Exception {

    process(
        raw,
        "clinical-query-telemetry-anomaly",
        data -> {

          if (!data.hasNonNull("visitId")) {
            return;
          }

          UUID visitId =
              UUID.fromString(
                  data.path("visitId")
                      .asText());

          var view =
              view(visitId);

          view.anomaly(
              uuidOrNull(
                  data,
                  "patientId"),
              textOrNull(
                  data,
                  "type"),
              data.path("value")
                  .asDouble(),
              textOrNull(
                  data,
                  "unit"),
              textOrNull(
                  data,
                  "reason"),
              instantOrNull(
                  data,
                  "detectedAt"));

          save(view);
        });
  }

  private UUID uuidOrNull(
      JsonNode data,
      String field) {

    if (!data.hasNonNull(field)) {
      return null;
    }

    String value =
        data.path(field)
            .asText();

    if (value == null ||
        value.isBlank()) {

      return null;
    }

    return UUID.fromString(value);
  }

  private String textOrNull(
      JsonNode data,
      String field) {

    if (!data.hasNonNull(field)) {
      return null;
    }

    String value =
        data.path(field)
            .asText();

    if (value == null ||
        value.isBlank()) {

      return null;
    }

    return value;
  }

  private Instant instantOrNull(
      JsonNode data,
      String field) {

    String value =
        textOrNull(
            data,
            field);

    if (value == null) {
      return null;
    }

    return Instant.parse(value);
  }
}

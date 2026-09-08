package br.com.sussmartcare.clinicalquery.application;

import br.com.sussmartcare.clinicalquery.domain.ClinicalLatestObservationView;
import br.com.sussmartcare.clinicalquery.domain.ClinicalPatientView;
import br.com.sussmartcare.clinicalquery.domain.PatientProfileView;
import br.com.sussmartcare.clinicalquery.infrastructure.ClinicalLatestObservationViewRepository;
import br.com.sussmartcare.clinicalquery.infrastructure.ClinicalPatientViewCache;
import br.com.sussmartcare.clinicalquery.infrastructure.ClinicalPatientViewRepository;
import br.com.sussmartcare.clinicalquery.infrastructure.PatientProfileViewRepository;
import br.com.sussmartcare.clinicalquery.infrastructure.ProcessedEvent;
import br.com.sussmartcare.clinicalquery.infrastructure.ProcessedEventRepository;
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

  private ClinicalPatientView view(UUID visitId) {
    return views.findById(visitId).orElseGet(() -> new ClinicalPatientView(visitId));
  }

  private void process(String raw, String consumer, Consumer<JsonNode> handler) throws Exception {
    JsonNode envelope = mapper.readTree(raw);
    UUID eventId = UUID.fromString(envelope.path("eventId").asText());
    if (processedEvents.existsById(eventId)) {
      return;
    }
    handler.accept(envelope.path("data"));
    processedEvents.save(new ProcessedEvent(eventId, consumer));
  }

  private void save(ClinicalPatientView view) {
    views.save(view);
    refreshCache(view);
  }

  private void refreshCache(ClinicalPatientView view) {
    PatientProfileView profile = view.getPatientId() == null
        ? null
        : profiles.findById(view.getPatientId()).orElse(null);
    cache.put(ClinicalPatientViewSnapshot.from(
        view,
        profile,
        observations.findByVisitIdOrderByTypeAsc(view.getVisitId())));
  }

  @KafkaListener(
      topics = {"patient-registered", "patient-provisional-created", "patient-identity-resolved", "patient-profile-updated"},
      groupId = "clinical-query-patient-profile")
  @Transactional
  public void patientProfile(String raw) throws Exception {
    process(raw, "clinical-query-patient-profile", data -> {
      UUID patientId = UUID.fromString(data.path("patientId").asText());
      var existing = profiles.findById(patientId).orElse(null);
      LocalDate birthDate = data.hasNonNull("birthDate")
          ? LocalDate.parse(data.path("birthDate").asText())
          : null;
      UUID mergedInto = data.hasNonNull("mergedIntoPatientId")
          ? UUID.fromString(data.path("mergedIntoPatientId").asText())
          : null;
      Instant updatedAt = data.hasNonNull("updatedAt")
          ? Instant.parse(data.path("updatedAt").asText())
          : Instant.now();

      if (existing == null) {
        profiles.save(new PatientProfileView(
            patientId,
            data.path("fullName").asText(),
            birthDate,
            data.path("identityStatus").asText(),
            mergedInto,
            updatedAt));
      } else {
        existing.update(
            data.path("fullName").asText(),
            birthDate,
            data.path("identityStatus").asText(),
            mergedInto,
            updatedAt);
        profiles.save(existing);
      }
      views.findByPatientId(patientId).forEach(this::refreshCache);
    });
  }

  @KafkaListener(topics = "patient-records-merged", groupId = "clinical-query-patient-merge")
  @Transactional
  public void patientMerged(String raw) throws Exception {
    process(raw, "clinical-query-patient-merge", data -> {
      UUID source = UUID.fromString(data.path("sourcePatientId").asText());
      UUID canonical = UUID.fromString(data.path("canonicalPatientId").asText());
      for (var view : views.findByPatientId(source)) {
        view.reassignPatient(canonical);
        save(view);
      }
    });
  }

  @KafkaListener(topics = "pre-visit-created", groupId = "clinical-query-visit")
  @Transactional
  public void visit(String raw) throws Exception {
    process(raw, "clinical-query-visit", data -> {
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.visit(UUID.fromString(data.path("patientId").asText()), "PRE_ARRIVAL");
      save(view);
    });
  }

  @KafkaListener(topics = "pre-anamnesis-recorded", groupId = "clinical-query-anamnesis")
  @Transactional
  public void anamnesis(String raw) throws Exception {
    process(raw, "clinical-query-anamnesis", data -> {
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.anamnesis(data.path("text").asText());
      save(view);
    });
  }

  @KafkaListener(topics = "patient-checked-in", groupId = "clinical-query-checkin")
  @Transactional
  public void checkin(String raw) throws Exception {
    process(raw, "clinical-query-checkin", data -> {
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.visit(UUID.fromString(data.path("patientId").asText()), "WAITING_TRIAGE");
      save(view);
    });
  }

  @KafkaListener(topics = "biometric-observation-received", groupId = "clinical-query-biometric")
  @Transactional
  public void biometric(String raw) throws Exception {
    process(raw, "clinical-query-biometric", data -> {
      if (!data.hasNonNull("visitId")) {
        return;
      }
      UUID visitId = UUID.fromString(data.path("visitId").asText());
      UUID patientId = UUID.fromString(data.path("patientId").asText());
      String type = data.path("type").asText();
      Double value = data.path("value").asDouble();
      String unit = data.path("unit").isNull() ? null : data.path("unit").asText();
      Instant measuredAt = data.hasNonNull("measuredAt")
          ? Instant.parse(data.path("measuredAt").asText())
          : Instant.now();

      var latest = observations.findByVisitIdAndType(visitId, type)
          .orElseGet(() -> new ClinicalLatestObservationView(visitId, type, value, unit, measuredAt));
      latest.updateIfNewer(value, unit, measuredAt);
      observations.save(latest);

      var view = view(visitId);
      view.biometric(patientId, type, value, unit);
      save(view);
    });
  }

  @KafkaListener(topics = "triage-assessment-generated", groupId = "clinical-query-assessment")
  @Transactional
  public void assessment(String raw) throws Exception {
    process(raw, "clinical-query-assessment", data -> {
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.assessment(
          data.path("recommendation").asText(),
          data.path("confidence").asDouble(),
          data.path("reasoning").asText(),
          data.path("modelName").asText(),
          data.path("modelVersion").asText());
      save(view);
    });
  }

  @KafkaListener(topics = "clinical-priority-confirmed", groupId = "clinical-query-priority")
  @Transactional
  public void priority(String raw) throws Exception {
    process(raw, "clinical-query-priority", data -> {
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.priority(data.path("priority").asText());
      save(view);
    });
  }

  @KafkaListener(topics = "patient-presence-changed", groupId = "clinical-query-presence")
  @Transactional
  public void presence(String raw) throws Exception {
    process(raw, "clinical-query-presence", data -> {
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.presence(data.path("eventType").asText());
      save(view);
    });
  }

  @KafkaListener(topics = "queue-position-estimated", groupId = "clinical-query-queue")
  @Transactional
  public void queue(String raw) throws Exception {
    process(raw, "clinical-query-queue", data -> {
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.queue(
          data.path("estimatedPosition").asInt(),
          data.path("estimatedMinutes").asInt(),
          data.path("status").asText());
      save(view);
    });
  }

  @KafkaListener(topics = "pre-arrival-alert-created", groupId = "clinical-query-prearrival")
  @Transactional
  public void prearrival(String raw) throws Exception {
    process(raw, "clinical-query-prearrival", data -> {
      if (!data.hasNonNull("visitId")) {
        return;
      }
      var view = view(UUID.fromString(data.path("visitId").asText()));
      view.preArrival(
          UUID.fromString(data.path("patientId").asText()),
          data.path("riskLevel").asText(),
          data.hasNonNull("eta") ? Instant.parse(data.path("eta").asText()) : null);
      save(view);
    });
  }
}

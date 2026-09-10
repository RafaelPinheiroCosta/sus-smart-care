package br.com.sussmartcare.clinicalquery.domain;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sussmartcare.clinicalquery.application.ClinicalPatientViewSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalPatientViewProjectionTest {

  @Test
  void projectsTerminalJourneyAndContinuousTelemetry() {

    UUID visitId =
        UUID.randomUUID();

    UUID patientId =
        UUID.randomUUID();

    ClinicalPatientView view =
        new ClinicalPatientView(
            visitId);

    view.visit(
        patientId,
        "WAITING_TRIAGE");

    view.priority(
        "HIGH");

    view.journey(
        patientId,
        "COMPLETED",
        "IMAGING",
        "DISCHARGED",
        null,
        "Stable discharge",
        Instant.parse(
            "2026-09-10T10:00:00Z"));

    Instant aggregateAt =
        Instant.parse(
            "2026-09-10T10:01:00Z");

    view.aggregate(
        patientId,
        "HEART_RATE",
        10L,
        80.0,
        95.0,
        87.5,
        "bpm",
        aggregateAt);

    Instant anomalyAt =
        aggregateAt.plusSeconds(1);

    view.anomaly(
        patientId,
        "SPO2",
        85.0,
        "%",
        "Value below configured minimum",
        anomalyAt);

    var snapshot =
        ClinicalPatientViewSnapshot.from(
            view,
            null,
            List.of());

    assertEquals(
        "COMPLETED",
        snapshot.journeyStatus());

    assertEquals(
        "DISCHARGED",
        snapshot.journeyOutcome());

    assertEquals(
        "IMAGING",
        snapshot.journeyStage());

    assertEquals(
        "HIGH",
        snapshot.clinicalPriority());

    assertNotNull(
        snapshot.continuousTelemetry());

    assertEquals(
        10L,
        snapshot.continuousTelemetry()
            .count());

    assertEquals(
        "HEART_RATE",
        snapshot.continuousTelemetry()
            .type());

    assertNotNull(
        snapshot.latestAnomaly());

    assertEquals(
        "SPO2",
        snapshot.latestAnomaly()
            .type());
  }

  @Test
  void queueProjectionDoesNotRegressJourneyStatus() {

    UUID patientId =
        UUID.randomUUID();

    ClinicalPatientView view =
        new ClinicalPatientView(
            UUID.randomUUID());

    view.journey(
        patientId,
        "COMPLETED",
        "MEDICAL_CARE",
        "DISCHARGED",
        null,
        null,
        Instant.parse(
            "2026-09-10T10:10:00Z"));

    view.queue(
        1,
        5,
        "CALLED");

    assertEquals(
        "COMPLETED",
        view.getJourneyStatus());

    assertEquals(
        "CALLED",
        view.getQueueStatus());
  }

  @Test
  void oldJourneyEventCannotRegressTerminalProjection() {

    UUID patientId =
        UUID.randomUUID();

    ClinicalPatientView view =
        new ClinicalPatientView(
            UUID.randomUUID());

    view.journey(
        patientId,
        "COMPLETED",
        "MEDICAL_CARE",
        "DISCHARGED",
        null,
        "Stable discharge",
        Instant.parse(
            "2026-09-10T10:20:00Z"));

    /*
     * Simulates an older event from another Kafka topic arriving after
     * patient-discharged.
     */
    view.journey(
        patientId,
        "IN_SERVICE",
        "MEDICAL_CARE",
        null,
        null,
        null,
        Instant.parse(
            "2026-09-10T10:19:00Z"));

    assertEquals(
        "COMPLETED",
        view.getJourneyStatus());

    assertEquals(
        "DISCHARGED",
        view.getJourneyOutcome());

    assertEquals(
        "Stable discharge",
        view.getTerminalNote());
  }

  @Test
  void lateIntakeEventCannotRegressJourneyProjection() {

    UUID patientId =
        UUID.randomUUID();

    ClinicalPatientView view =
        new ClinicalPatientView(
            UUID.randomUUID());

    view.journey(
        patientId,
        "IN_SERVICE",
        "MEDICAL_CARE",
        null,
        null,
        null,
        Instant.parse(
            "2026-09-10T10:30:00Z"));

    view.visit(
        patientId,
        "WAITING_TRIAGE");

    assertEquals(
        "IN_SERVICE",
        view.getJourneyStatus());
  }
}

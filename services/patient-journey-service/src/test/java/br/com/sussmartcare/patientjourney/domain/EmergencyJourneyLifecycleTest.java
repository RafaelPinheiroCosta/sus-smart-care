package br.com.sussmartcare.patientjourney.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmergencyJourneyLifecycleTest {

  private Visit checkedIn() {

    Visit visit =
        Visit.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Channel.RECEPTION);

    visit.pullChanges();

    visit.checkIn();

    return visit;
  }

  private Visit inService() {

    Visit visit =
        checkedIn();

    visit.startTriage();
    visit.completeTriage();
    visit.queueForCare(
        "MEDICAL_CARE");
    visit.call();
    visit.startService();

    return visit;
  }

  @Test
  void completesEmergencyJourneyByDischarge() {

    Visit visit =
        inService();

    visit.discharge(
        "Clinical discharge");

    assertEquals(
        VisitStatus.COMPLETED,
        visit.getStatus());

    assertEquals(
        JourneyOutcome.DISCHARGED,
        visit.getOutcome());

    assertNotNull(
        visit.getCompletedAt());

    assertNull(
        visit.getTransferFacilityId());
  }

  @Test
  void completesEmergencyJourneyByTransfer() {

    Visit visit =
        inService();

    UUID target =
        UUID.randomUUID();

    visit.transfer(
        target,
        "Higher complexity");

    assertEquals(
        VisitStatus.COMPLETED,
        visit.getStatus());

    assertEquals(
        JourneyOutcome.TRANSFERRED,
        visit.getOutcome());

    assertEquals(
        target,
        visit.getTransferFacilityId());
  }

  @Test
  void supportsMultipleCareStages() {

    Visit visit =
        inService();

    visit.waitForNextStage(
        "IMAGING");

    assertEquals(
        VisitStatus.WAITING_NEXT_STAGE,
        visit.getStatus());

    assertEquals(
        "IMAGING",
        visit.getCurrentStage());

    visit.call();
    visit.startService();

    assertEquals(
        VisitStatus.IN_SERVICE,
        visit.getStatus());

    assertEquals(
        "IMAGING",
        visit.getCurrentStage());
  }

  @Test
  void cannotSkipTriage() {

    Visit visit =
        checkedIn();

    assertThrows(
        IllegalStateException.class,
        () ->
            visit.queueForCare(
                "MEDICAL_CARE"));
  }

  @Test
  void cannotMutateCompletedJourney() {

    Visit visit =
        inService();

    visit.discharge(
        null);

    assertThrows(
        IllegalStateException.class,
        visit::call);
  }

  @Test
  void cancellationIsTerminal() {

    Visit visit =
        checkedIn();

    visit.cancel(
        "Patient left facility");

    assertEquals(
        VisitStatus.CANCELLED,
        visit.getStatus());

    assertEquals(
        JourneyOutcome.CANCELLED,
        visit.getOutcome());

    assertNotNull(
        visit.getCompletedAt());

    assertThrows(
        IllegalStateException.class,
        visit::startTriage);
  }

  @Test
  void cannotTransferToSameFacility() {

    UUID facilityId =
        UUID.randomUUID();

    Visit visit =
        Visit.create(
            UUID.randomUUID(),
            facilityId,
            Channel.AMBULANCE);

    visit.checkIn();
    visit.startTriage();
    visit.completeTriage();
    visit.queueForCare(
        "EMERGENCY");
    visit.call();
    visit.startService();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            visit.transfer(
                facilityId,
                null));
  }
}

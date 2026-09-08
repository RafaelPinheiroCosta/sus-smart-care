package br.com.sussmartcare.queue.domain;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QueueEntryTest {
  @Test void leavingChangesReadinessButNotClinicalPriority(){
    var q=new QueueEntry(UUID.randomUUID(),UUID.randomUUID(),"HIGH");
    q.updatePresence(PresenceState.OUTSIDE_FACILITY);
    q.requestReturn(Duration.ofMinutes(20),Duration.ofMinutes(10));
    assertEquals("HIGH",q.getClinicalPriority());
    assertEquals(PresenceState.OUTSIDE_FACILITY,q.getPresence());
    assertEquals(QueueEntryStatus.RETURN_REQUIRED,q.getStatus());
  }
  @Test void cannotCallOutsidePatient(){
    var q=new QueueEntry(UUID.randomUUID(),UUID.randomUUID(),"LOW");
    q.updatePresence(PresenceState.OUTSIDE_FACILITY);
    assertThrows(IllegalStateException.class,q::call);
  }
}

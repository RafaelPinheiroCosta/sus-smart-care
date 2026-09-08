package br.com.sussmartcare.triage.domain;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class TriageTest {
  @Test void shouldRequireProfessionalConfirmation(){
    var t=Triage.start(UUID.randomUUID());
    UUID professional=UUID.randomUUID();
    t.confirm(ClinicalPriority.HIGH,professional);
    assertThat(t.getStatus()).isEqualTo(TriageStatus.CONFIRMED);
    assertThat(t.getFinalPriority()).isEqualTo(ClinicalPriority.HIGH);
    assertThat(t.getDecidedBy()).isEqualTo(professional);
  }
}

package br.com.sussmartcare.triage.domain;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TriageDecisionTest {
  @Test void professionalDecisionIsStoredSeparatelyFromAiRecommendation(){var t=Triage.start(UUID.randomUUID());t.assessment(ClinicalPriority.HIGH,0.82,"Demonstração de recomendação separada da decisão clínica","demo","1.0");t.confirm(ClinicalPriority.MEDIUM,UUID.randomUUID());assertEquals(ClinicalPriority.HIGH,t.getAiRecommendation());assertEquals(ClinicalPriority.MEDIUM,t.getFinalPriority());}
}

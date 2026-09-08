package br.com.sussmartcare.patientjourney.domain;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class VisitTest {
  @Test void shouldCheckInFromPreArrival(){var v=Visit.create(UUID.randomUUID(),UUID.randomUUID(),Channel.MOBILE);v.checkIn();assertThat(v.getStatus()).isEqualTo(VisitStatus.WAITING_TRIAGE);}
  @Test void shouldRejectSecondCheckIn(){var v=Visit.create(UUID.randomUUID(),UUID.randomUUID(),Channel.RECEPTION);v.checkIn();assertThatThrownBy(v::checkIn).isInstanceOf(IllegalStateException.class);}
}

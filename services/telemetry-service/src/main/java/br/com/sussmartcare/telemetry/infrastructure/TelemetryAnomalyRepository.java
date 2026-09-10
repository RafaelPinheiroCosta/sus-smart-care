package br.com.sussmartcare.telemetry.infrastructure;

import br.com.sussmartcare.telemetry.domain.TelemetryAnomaly;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryAnomalyRepository
    extends JpaRepository<TelemetryAnomaly, UUID> {

  List<TelemetryAnomaly>
      findBySessionIdOrderByDetectedAtDesc(
          UUID sessionId);
}

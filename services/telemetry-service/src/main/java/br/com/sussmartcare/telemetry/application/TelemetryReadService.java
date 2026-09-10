package br.com.sussmartcare.telemetry.application;

import br.com.sussmartcare.telemetry.domain.TelemetryAggregate;
import br.com.sussmartcare.telemetry.domain.TelemetryAnomaly;
import br.com.sussmartcare.telemetry.infrastructure.TelemetryAggregateRepository;
import br.com.sussmartcare.telemetry.infrastructure.TelemetryAnomalyRepository;
import br.com.sussmartcare.telemetry.infrastructure.TelemetrySessionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryReadService {

  private final TelemetrySessionRepository sessions;
  private final TelemetryAggregateRepository aggregates;
  private final TelemetryAnomalyRepository anomalies;

  public TelemetryReadService(
      TelemetrySessionRepository sessions,
      TelemetryAggregateRepository aggregates,
      TelemetryAnomalyRepository anomalies) {

    this.sessions = sessions;
    this.aggregates = aggregates;
    this.anomalies = anomalies;
  }

  @Transactional(readOnly = true)
  public List<TelemetryAggregate> aggregates(
      UUID sessionId) {

    requireSession(sessionId);

    return aggregates
        .findBySessionIdOrderByType(
            sessionId);
  }

  @Transactional(readOnly = true)
  public List<TelemetryAnomaly> anomalies(
      UUID sessionId) {

    requireSession(sessionId);

    return anomalies
        .findBySessionIdOrderByDetectedAtDesc(
            sessionId);
  }

  private void requireSession(
      UUID sessionId) {

    if (!sessions.existsById(sessionId)) {

      throw new TelemetryNotFoundException(
          "Telemetry session not found: " +
          sessionId);
    }
  }
}

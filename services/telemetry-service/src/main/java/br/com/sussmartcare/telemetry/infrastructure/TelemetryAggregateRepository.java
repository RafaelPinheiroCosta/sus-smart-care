package br.com.sussmartcare.telemetry.infrastructure;

import br.com.sussmartcare.telemetry.domain.TelemetryAggregate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryAggregateRepository
    extends JpaRepository<TelemetryAggregate, UUID> {

  Optional<TelemetryAggregate>
      findBySessionIdAndType(
          UUID sessionId,
          String type);

  List<TelemetryAggregate>
      findBySessionIdOrderByType(
          UUID sessionId);
}

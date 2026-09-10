package br.com.sussmartcare.presence.infrastructure;

import br.com.sussmartcare.presence.domain.PresenceTrackingSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceTrackingSessionRepository
    extends JpaRepository<PresenceTrackingSession, UUID> {

  Optional<PresenceTrackingSession>
      findByVisitIdAndEndedAtIsNull(
          UUID visitId);

  Optional<PresenceTrackingSession>
      findByTrackingTokenHash(
          String trackingTokenHash);

  List<PresenceTrackingSession>
      findByEndedAtIsNullAndInsideFacilityTrue();
}

package br.com.sussmartcare.presence.infrastructure;

import br.com.sussmartcare.presence.domain.PresenceGateway;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceGatewayRepository
    extends JpaRepository<PresenceGateway, UUID> {

  Optional<PresenceGateway>
      findByExternalId(
          String externalId);

  List<PresenceGateway>
      findAllByOrderByExternalIdAsc();
}

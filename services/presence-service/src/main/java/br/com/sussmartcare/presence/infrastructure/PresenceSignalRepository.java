package br.com.sussmartcare.presence.infrastructure;

import br.com.sussmartcare.presence.domain.PresenceSignal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceSignalRepository
    extends JpaRepository<PresenceSignal, UUID> {
}

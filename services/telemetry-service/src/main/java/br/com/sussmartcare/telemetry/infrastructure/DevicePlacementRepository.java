package br.com.sussmartcare.telemetry.infrastructure;

import br.com.sussmartcare.telemetry.domain.DevicePlacement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevicePlacementRepository
    extends JpaRepository<DevicePlacement, UUID> {

  Optional<DevicePlacement>
      findByDeviceIdAndEndedAtIsNull(UUID deviceId);
}

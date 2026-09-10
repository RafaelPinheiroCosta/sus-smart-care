package br.com.sussmartcare.telemetry.infrastructure;

import br.com.sussmartcare.telemetry.domain.MedicalDevice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository
    extends JpaRepository<MedicalDevice, UUID> {

  Optional<MedicalDevice> findByExternalId(String externalId);
}

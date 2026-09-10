package br.com.sussmartcare.facility.infrastructure;

import br.com.sussmartcare.facility.domain.CareZone;
import br.com.sussmartcare.facility.domain.CareZoneRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCareZoneRepository
    extends CareZoneRepository,
        JpaRepository<CareZone, UUID> {
}

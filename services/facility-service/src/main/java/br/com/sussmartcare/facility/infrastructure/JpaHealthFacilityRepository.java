package br.com.sussmartcare.facility.infrastructure;

import br.com.sussmartcare.facility.domain.HealthFacility;
import br.com.sussmartcare.facility.domain.HealthFacilityRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaHealthFacilityRepository
    extends HealthFacilityRepository,
        JpaRepository<HealthFacility, UUID> {
}

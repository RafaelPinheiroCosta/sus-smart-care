package br.com.sussmartcare.facility.infrastructure;

import br.com.sussmartcare.facility.domain.BedOccupation;
import br.com.sussmartcare.facility.domain.BedOccupationRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBedOccupationRepository
    extends BedOccupationRepository,
        JpaRepository<BedOccupation, UUID> {
}

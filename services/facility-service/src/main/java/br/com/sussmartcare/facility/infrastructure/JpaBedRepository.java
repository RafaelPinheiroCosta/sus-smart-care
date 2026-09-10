package br.com.sussmartcare.facility.infrastructure;

import br.com.sussmartcare.facility.domain.Bed;
import br.com.sussmartcare.facility.domain.BedRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBedRepository
    extends BedRepository,
        JpaRepository<Bed, UUID> {
}

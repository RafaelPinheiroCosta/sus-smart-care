package br.com.sussmartcare.prehospital.infrastructure;

import br.com.sussmartcare.prehospital.domain.AmbulanceCoverage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceCoverageRepository
    extends JpaRepository<AmbulanceCoverage, UUID> {

  Optional<AmbulanceCoverage>
      findByAmbulanceIdAndEndedAtIsNull(
          String ambulanceId);

  List<AmbulanceCoverage>
      findByFacilityIdAndEndedAtIsNull(
          UUID facilityId);
}

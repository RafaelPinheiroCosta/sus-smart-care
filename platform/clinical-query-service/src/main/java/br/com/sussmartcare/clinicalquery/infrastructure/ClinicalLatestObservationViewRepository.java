package br.com.sussmartcare.clinicalquery.infrastructure;

import br.com.sussmartcare.clinicalquery.domain.ClinicalLatestObservationView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalLatestObservationViewRepository
    extends JpaRepository<ClinicalLatestObservationView, UUID> {
  Optional<ClinicalLatestObservationView> findByVisitIdAndType(UUID visitId, String type);
  List<ClinicalLatestObservationView> findByVisitIdOrderByTypeAsc(UUID visitId);
}

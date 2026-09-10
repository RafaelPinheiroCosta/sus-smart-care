package br.com.sussmartcare.facility.domain;

import java.util.Optional;
import java.util.UUID;

public interface BedOccupationRepository {

  BedOccupation save(BedOccupation occupation);

  Optional<BedOccupation> findById(UUID id);

  Optional<BedOccupation> findByBedIdAndEndedAtIsNull(
      UUID bedId);

  Optional<BedOccupation> findByVisitIdAndEndedAtIsNull(
      UUID visitId);
}

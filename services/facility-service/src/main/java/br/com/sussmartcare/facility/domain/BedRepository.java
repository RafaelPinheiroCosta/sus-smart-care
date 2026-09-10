package br.com.sussmartcare.facility.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BedRepository {

  Bed save(Bed bed);

  Optional<Bed> findById(UUID id);

  Optional<Bed> findByZoneIdAndCode(
      UUID zoneId,
      String code);

  List<Bed> findByZoneId(UUID zoneId);
}

package br.com.sussmartcare.facility.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareZoneRepository {

  CareZone save(CareZone zone);

  Optional<CareZone> findById(UUID id);

  Optional<CareZone> findByFacilityIdAndCode(
      UUID facilityId,
      String code);

  List<CareZone> findByFacilityId(UUID facilityId);
}

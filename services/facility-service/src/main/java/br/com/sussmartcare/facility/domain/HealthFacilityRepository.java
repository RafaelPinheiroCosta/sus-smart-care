package br.com.sussmartcare.facility.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthFacilityRepository {

  HealthFacility save(HealthFacility facility);

  Optional<HealthFacility> findById(UUID id);

  Optional<HealthFacility> findByCode(String code);

  List<HealthFacility> findAll();
}

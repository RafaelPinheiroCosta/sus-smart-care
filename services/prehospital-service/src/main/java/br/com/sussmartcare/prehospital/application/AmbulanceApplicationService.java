package br.com.sussmartcare.prehospital.application;

import br.com.sussmartcare.prehospital.domain.Ambulance;
import br.com.sussmartcare.prehospital.domain.AmbulanceCoverage;
import br.com.sussmartcare.prehospital.domain.AmbulanceOperationalStatus;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceCoverageRepository;
import br.com.sussmartcare.prehospital.infrastructure.AmbulanceRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AmbulanceApplicationService {

  private final AmbulanceRepository
      ambulanceRepository;

  private final AmbulanceCoverageRepository
      coverageRepository;

  public AmbulanceApplicationService(
      AmbulanceRepository ambulanceRepository,
      AmbulanceCoverageRepository coverageRepository) {

    this.ambulanceRepository =
        ambulanceRepository;

    this.coverageRepository =
        coverageRepository;
  }

  @Transactional
  public Ambulance create(
      String id,
      String displayName) {

    String normalizedId =
        normalizeId(id);

    if (ambulanceRepository.existsById(
        normalizedId)) {

      throw new PreHospitalConflictException(
          "Ambulance already exists: " +
          normalizedId);
    }

    return ambulanceRepository.save(
        new Ambulance(
            normalizedId,
            displayName));
  }

  @Transactional(readOnly = true)
  public Ambulance get(
      String id) {

    return ambulanceRepository
        .findById(normalizeId(id))
        .orElseThrow(
            () ->
                new PreHospitalNotFoundException(
                    "Ambulance not found: " +
                    id));
  }

  @Transactional(readOnly = true)
  public List<Ambulance> list() {

    return ambulanceRepository
        .findAll()
        .stream()
        .sorted(
            Comparator.comparing(
                Ambulance::getId))
        .toList();
  }

  @Transactional
  public Ambulance updateOperationalStatus(
      String id,
      AmbulanceOperationalStatus status) {

    if (status == null) {
      throw new IllegalArgumentException(
          "Operational status is required");
    }

    Ambulance ambulance =
        get(id);

    switch (status) {

      case ACTIVE ->
          ambulance.activate();

      case MAINTENANCE ->
          ambulance.putInMaintenance();

      case OUT_OF_SERVICE ->
          ambulance.putOutOfService();
    }

    return ambulanceRepository.save(
        ambulance);
  }

  @Transactional
  public AmbulanceCoverage assignCoverage(
      String ambulanceId,
      UUID facilityId) {

    Ambulance ambulance =
        get(ambulanceId);

    if (!ambulance.isOperational()) {

      throw new PreHospitalConflictException(
          "Ambulance is not operational: " +
          ambulance.getId());
    }

    if (facilityId == null) {

      throw new IllegalArgumentException(
          "Facility id is required");
    }

    if (coverageRepository
        .findByAmbulanceIdAndEndedAtIsNull(
            ambulance.getId())
        .isPresent()) {

      throw new PreHospitalConflictException(
          "Ambulance already has active coverage: " +
          ambulance.getId());
    }

    return coverageRepository.save(
        new AmbulanceCoverage(
            ambulance.getId(),
            facilityId));
  }

  @Transactional(readOnly = true)
  public AmbulanceCoverage
      getActiveCoverage(
          String ambulanceId) {

    Ambulance ambulance =
        get(ambulanceId);

    return coverageRepository
        .findByAmbulanceIdAndEndedAtIsNull(
            ambulance.getId())
        .orElseThrow(
            () ->
                new PreHospitalNotFoundException(
                    "Active coverage not found for ambulance: " +
                    ambulance.getId()));
  }

  @Transactional
  public AmbulanceCoverage endCoverage(
      UUID coverageId) {

    AmbulanceCoverage coverage =
        coverageRepository
            .findById(coverageId)
            .orElseThrow(
                () ->
                    new PreHospitalNotFoundException(
                        "Ambulance coverage not found: " +
                        coverageId));

    if (!coverage.isActive()) {

      throw new PreHospitalConflictException(
          "Ambulance coverage is already closed: " +
          coverageId);
    }

    coverage.close();

    return coverageRepository.save(
        coverage);
  }

  private String normalizeId(
      String id) {

    if (id == null ||
        id.trim().isEmpty()) {

      throw new IllegalArgumentException(
          "Ambulance id is required");
    }

    return id.trim();
  }
}

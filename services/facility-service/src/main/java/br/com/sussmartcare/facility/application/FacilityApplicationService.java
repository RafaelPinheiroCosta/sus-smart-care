package br.com.sussmartcare.facility.application;

import br.com.sussmartcare.facility.domain.Bed;
import br.com.sussmartcare.facility.domain.BedOccupation;
import br.com.sussmartcare.facility.domain.BedOccupationRepository;
import br.com.sussmartcare.facility.domain.BedOperationalStatus;
import br.com.sussmartcare.facility.domain.BedRepository;
import br.com.sussmartcare.facility.domain.BedType;
import br.com.sussmartcare.facility.domain.CareZone;
import br.com.sussmartcare.facility.domain.CareZoneRepository;
import br.com.sussmartcare.facility.domain.CareZoneType;
import br.com.sussmartcare.facility.domain.FacilityType;
import br.com.sussmartcare.facility.domain.HealthFacility;
import br.com.sussmartcare.facility.domain.HealthFacilityRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityApplicationService {

  private final HealthFacilityRepository facilityRepository;
  private final CareZoneRepository zoneRepository;
  private final BedRepository bedRepository;
  private final BedOccupationRepository occupationRepository;

  public FacilityApplicationService(
      HealthFacilityRepository facilityRepository,
      CareZoneRepository zoneRepository,
      BedRepository bedRepository,
      BedOccupationRepository occupationRepository) {

    this.facilityRepository = facilityRepository;
    this.zoneRepository = zoneRepository;
    this.bedRepository = bedRepository;
    this.occupationRepository = occupationRepository;
  }

  @Transactional
  public HealthFacility createFacility(
      String code,
      String name,
      FacilityType type) {

    HealthFacility facility =
        new HealthFacility(code, name, type);

    facilityRepository
        .findByCode(facility.getCode())
        .ifPresent(existing -> {
          throw new FacilityConflictException(
              "Facility code already exists");
        });

    return facilityRepository.save(facility);
  }

  @Transactional(readOnly = true)
  public HealthFacility getFacility(UUID facilityId) {

    return facilityRepository.findById(facilityId)
        .orElseThrow(() ->
            new FacilityNotFoundException(
                "Health facility not found"));
  }

  @Transactional(readOnly = true)
  public List<HealthFacility> listFacilities() {
    return facilityRepository.findAll();
  }

  @Transactional
  public HealthFacility setFacilityActive(
      UUID facilityId,
      boolean active) {

    HealthFacility facility =
        getFacility(facilityId);

    if (active) {
      facility.activate();
    }

    if (!active) {
      facility.deactivate();
    }

    return facilityRepository.save(facility);
  }

  @Transactional
  public CareZone createZone(
      UUID facilityId,
      String code,
      String name,
      CareZoneType type) {

    HealthFacility facility = getFacility(facilityId);

    if (!facility.isActive()) {
      throw new FacilityConflictException(
          "Health facility is inactive");
    }

    CareZone zone =
        new CareZone(
            facilityId,
            code,
            name,
            type);

    zoneRepository
        .findByFacilityIdAndCode(
            facilityId,
            zone.getCode())
        .ifPresent(existing -> {
          throw new FacilityConflictException(
              "Care zone code already exists in facility");
        });

    return zoneRepository.save(zone);
  }

  @Transactional(readOnly = true)
  public CareZone getZone(UUID zoneId) {

    return zoneRepository.findById(zoneId)
        .orElseThrow(() ->
            new FacilityNotFoundException(
                "Care zone not found"));
  }

  @Transactional(readOnly = true)
  public List<CareZone> listZones(UUID facilityId) {

    getFacility(facilityId);

    return zoneRepository.findByFacilityId(facilityId);
  }

  @Transactional
  public CareZone setZoneActive(
      UUID zoneId,
      boolean active) {

    CareZone zone = getZone(zoneId);

    if (active) {

      HealthFacility facility =
          getFacility(
              zone.getFacilityId());

      if (!facility.isActive()) {

        throw new FacilityConflictException(
            "Health facility is inactive");
      }

      zone.activate();
    }

    if (!active) {
      zone.deactivate();
    }

    return zoneRepository.save(zone);
  }

  @Transactional
  public Bed createBed(
      UUID zoneId,
      String code,
      BedType type) {

    CareZone zone = getZone(zoneId);

    if (!zone.isActive()) {
      throw new FacilityConflictException(
          "Care zone is inactive");
    }

    Bed bed =
        new Bed(
            zoneId,
            code,
            type);

    bedRepository
        .findByZoneIdAndCode(
            zoneId,
            bed.getCode())
        .ifPresent(existing -> {
          throw new FacilityConflictException(
              "Bed code already exists in care zone");
        });

    return bedRepository.save(bed);
  }

  @Transactional(readOnly = true)
  public Bed getBed(UUID bedId) {

    return bedRepository.findById(bedId)
        .orElseThrow(() ->
            new FacilityNotFoundException(
                "Bed not found"));
  }

  @Transactional(readOnly = true)
  public List<Bed> listBeds(UUID zoneId) {

    getZone(zoneId);

    return bedRepository.findByZoneId(zoneId);
  }

  @Transactional
  public Bed setBedOperationalStatus(
      UUID bedId,
      BedOperationalStatus status) {

    Bed bed = getBed(bedId);

    Objects.requireNonNull(
        status,
        "status");

    if (status == BedOperationalStatus.ACTIVE) {
      assertBedHierarchyAvailable(bed);
    }

    switch (status) {

      case ACTIVE ->
          bed.activate();

      case MAINTENANCE ->
          bed.putInMaintenance();

      case OUT_OF_SERVICE ->
          bed.putOutOfService();
    }

    return bedRepository.save(bed);
  }

  @Transactional
  public BedOccupation occupyBed(
      UUID bedId,
      UUID visitId) {

    Bed bed = getBed(bedId);

    assertBedHierarchyAvailable(bed);

    if (!bed.isOperational()) {
      throw new FacilityConflictException(
          "Bed is not operational");
    }

    if (occupationRepository
        .findByBedIdAndEndedAtIsNull(bedId)
        .isPresent()) {

      throw new FacilityConflictException(
          "Bed is already occupied");
    }

    if (occupationRepository
        .findByVisitIdAndEndedAtIsNull(visitId)
        .isPresent()) {

      throw new FacilityConflictException(
          "Visit already has an active bed occupation");
    }

    return occupationRepository.save(
        new BedOccupation(bedId, visitId));
  }

  @Transactional(readOnly = true)
  public BedOccupation getActiveOccupation(
      UUID bedId) {

    getBed(bedId);

    return occupationRepository
        .findByBedIdAndEndedAtIsNull(bedId)
        .orElseThrow(() ->
            new FacilityNotFoundException(
                "Active bed occupation not found"));
  }

  private void assertBedHierarchyAvailable(
      Bed bed) {

    CareZone zone =
        getZone(
            bed.getZoneId());

    if (!zone.isActive()) {

      throw new FacilityConflictException(
          "Care zone is inactive");
    }

    HealthFacility facility =
        getFacility(
            zone.getFacilityId());

    if (!facility.isActive()) {

      throw new FacilityConflictException(
          "Health facility is inactive");
    }
  }

  @Transactional
  public BedOccupation releaseOccupation(
      UUID occupationId) {

    BedOccupation occupation =
        occupationRepository.findById(occupationId)
            .orElseThrow(() ->
                new FacilityNotFoundException(
                    "Bed occupation not found"));

    occupation.close();

    return occupationRepository.save(occupation);
  }
}

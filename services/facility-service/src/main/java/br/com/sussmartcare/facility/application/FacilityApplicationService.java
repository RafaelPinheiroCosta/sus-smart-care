package br.com.sussmartcare.facility.application;

import br.com.sussmartcare.facility.domain.Bed;
import br.com.sussmartcare.facility.domain.BedOccupation;
import br.com.sussmartcare.facility.domain.BedOccupationRepository;
import br.com.sussmartcare.facility.domain.BedRepository;
import br.com.sussmartcare.facility.domain.BedType;
import br.com.sussmartcare.facility.domain.CareZone;
import br.com.sussmartcare.facility.domain.CareZoneRepository;
import br.com.sussmartcare.facility.domain.CareZoneType;
import br.com.sussmartcare.facility.domain.FacilityType;
import br.com.sussmartcare.facility.domain.HealthFacility;
import br.com.sussmartcare.facility.domain.HealthFacilityRepository;
import java.util.List;
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

    facilityRepository.findByCode(code.trim())
        .ifPresent(existing -> {
          throw new IllegalStateException(
              "Facility code already exists");
        });

    return facilityRepository.save(
        new HealthFacility(code, name, type));
  }

  @Transactional(readOnly = true)
  public HealthFacility getFacility(UUID facilityId) {

    return facilityRepository.findById(facilityId)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Health facility not found"));
  }

  @Transactional(readOnly = true)
  public List<HealthFacility> listFacilities() {
    return facilityRepository.findAll();
  }

  @Transactional
  public CareZone createZone(
      UUID facilityId,
      String code,
      String name,
      CareZoneType type) {

    HealthFacility facility = getFacility(facilityId);

    if (!facility.isActive()) {
      throw new IllegalStateException(
          "Health facility is inactive");
    }

    zoneRepository
        .findByFacilityIdAndCode(facilityId, code.trim())
        .ifPresent(existing -> {
          throw new IllegalStateException(
              "Care zone code already exists in facility");
        });

    return zoneRepository.save(
        new CareZone(
            facilityId,
            code,
            name,
            type));
  }

  @Transactional(readOnly = true)
  public List<CareZone> listZones(UUID facilityId) {

    getFacility(facilityId);

    return zoneRepository.findByFacilityId(facilityId);
  }

  @Transactional
  public Bed createBed(
      UUID zoneId,
      String code,
      BedType type) {

    CareZone zone = zoneRepository.findById(zoneId)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Care zone not found"));

    if (!zone.isActive()) {
      throw new IllegalStateException(
          "Care zone is inactive");
    }


    bedRepository
        .findByZoneIdAndCode(zoneId, code.trim())
        .ifPresent(existing -> {
          throw new IllegalStateException(
              "Bed code already exists in care zone");
        });

    return bedRepository.save(
        new Bed(zoneId, code, type));
  }

  @Transactional(readOnly = true)
  public List<Bed> listBeds(UUID zoneId) {

    zoneRepository.findById(zoneId)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Care zone not found"));

    return bedRepository.findByZoneId(zoneId);
  }

  @Transactional
  public BedOccupation occupyBed(
      UUID bedId,
      UUID visitId) {

    Bed bed = bedRepository.findById(bedId)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "Bed not found"));

    if (!bed.isOperational()) {
      throw new IllegalStateException(
          "Bed is not operational");
    }

    if (occupationRepository
        .findByBedIdAndEndedAtIsNull(bedId)
        .isPresent()) {

      throw new IllegalStateException(
          "Bed is already occupied");
    }

    if (occupationRepository
        .findByVisitIdAndEndedAtIsNull(visitId)
        .isPresent()) {

      throw new IllegalStateException(
          "Visit already has an active bed occupation");
    }

    return occupationRepository.save(
        new BedOccupation(bedId, visitId));
  }

  @Transactional
  public BedOccupation releaseOccupation(
      UUID occupationId) {

    BedOccupation occupation =
        occupationRepository.findById(occupationId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Bed occupation not found"));

    occupation.close();

    return occupationRepository.save(occupation);
  }
}

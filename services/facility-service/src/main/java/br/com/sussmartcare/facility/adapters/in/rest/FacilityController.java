package br.com.sussmartcare.facility.adapters.in.rest;

import br.com.sussmartcare.facility.application.FacilityApplicationService;
import br.com.sussmartcare.facility.domain.Bed;
import br.com.sussmartcare.facility.domain.BedOccupation;
import br.com.sussmartcare.facility.domain.BedOperationalStatus;
import br.com.sussmartcare.facility.domain.BedType;
import br.com.sussmartcare.facility.domain.CareZone;
import br.com.sussmartcare.facility.domain.CareZoneType;
import br.com.sussmartcare.facility.domain.FacilityType;
import br.com.sussmartcare.facility.domain.HealthFacility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FacilityController {

  private final FacilityApplicationService service;

  public FacilityController(
      FacilityApplicationService service) {

    this.service = service;
  }

  @PostMapping("/facilities")
  ResponseEntity<FacilityResponse> createFacility(
      @Valid @RequestBody CreateFacilityRequest request) {

    HealthFacility facility =
        service.createFacility(
            request.code(),
            request.name(),
            request.type());

    return ResponseEntity
        .created(
            URI.create(
                "/api/v1/facilities/" +
                    facility.getId()))
        .body(FacilityResponse.from(facility));
  }

  @GetMapping("/facilities")
  List<FacilityResponse> listFacilities() {

    return service.listFacilities()
        .stream()
        .map(FacilityResponse::from)
        .toList();
  }

  @GetMapping("/facilities/{facilityId}")
  FacilityResponse getFacility(
      @PathVariable UUID facilityId) {

    return FacilityResponse.from(
        service.getFacility(facilityId));
  }

  @PatchMapping("/facilities/{facilityId}/status")
  FacilityResponse updateFacilityStatus(
      @PathVariable UUID facilityId,
      @Valid @RequestBody
      UpdateActivationRequest request) {

    return FacilityResponse.from(
        service.setFacilityActive(
            facilityId,
            request.active()));
  }

  @PostMapping("/facilities/{facilityId}/zones")
  ResponseEntity<CareZoneResponse> createZone(
      @PathVariable UUID facilityId,
      @Valid @RequestBody CreateZoneRequest request) {

    CareZone zone =
        service.createZone(
            facilityId,
            request.code(),
            request.name(),
            request.type());

    return ResponseEntity
        .created(
            URI.create(
                "/api/v1/zones/" +
                    zone.getId()))
        .body(CareZoneResponse.from(zone));
  }

  @GetMapping("/facilities/{facilityId}/zones")
  List<CareZoneResponse> listZones(
      @PathVariable UUID facilityId) {

    return service.listZones(facilityId)
        .stream()
        .map(CareZoneResponse::from)
        .toList();
  }

  @GetMapping("/zones/{zoneId}")
  CareZoneResponse getZone(
      @PathVariable UUID zoneId) {

    return CareZoneResponse.from(
        service.getZone(zoneId));
  }

  @PatchMapping("/zones/{zoneId}/status")
  CareZoneResponse updateZoneStatus(
      @PathVariable UUID zoneId,
      @Valid @RequestBody
      UpdateActivationRequest request) {

    return CareZoneResponse.from(
        service.setZoneActive(
            zoneId,
            request.active()));
  }

  @PostMapping("/zones/{zoneId}/beds")
  ResponseEntity<BedResponse> createBed(
      @PathVariable UUID zoneId,
      @Valid @RequestBody CreateBedRequest request) {

    Bed bed =
        service.createBed(
            zoneId,
            request.code(),
            request.type());

    return ResponseEntity
        .created(
            URI.create(
                "/api/v1/beds/" +
                    bed.getId()))
        .body(BedResponse.from(bed));
  }

  @GetMapping("/zones/{zoneId}/beds")
  List<BedResponse> listBeds(
      @PathVariable UUID zoneId) {

    return service.listBeds(zoneId)
        .stream()
        .map(BedResponse::from)
        .toList();
  }

  @GetMapping("/beds/{bedId}")
  BedResponse getBed(
      @PathVariable UUID bedId) {

    return BedResponse.from(
        service.getBed(bedId));
  }

  @PatchMapping("/beds/{bedId}/operational-status")
  BedResponse updateBedOperationalStatus(
      @PathVariable UUID bedId,
      @Valid @RequestBody
      UpdateBedOperationalStatusRequest request) {

    return BedResponse.from(
        service.setBedOperationalStatus(
            bedId,
            request.status()));
  }

  @PostMapping("/beds/{bedId}/occupations")
  ResponseEntity<BedOccupationResponse> occupyBed(
      @PathVariable UUID bedId,
      @Valid @RequestBody OccupyBedRequest request) {

    BedOccupation occupation =
        service.occupyBed(
            bedId,
            request.visitId());

    return ResponseEntity
        .created(
            URI.create(
                "/api/v1/bed-occupations/" +
                    occupation.getId()))
        .body(
            BedOccupationResponse.from(
                occupation));
  }

  @GetMapping("/beds/{bedId}/occupation")
  BedOccupationResponse getActiveOccupation(
      @PathVariable UUID bedId) {

    return BedOccupationResponse.from(
        service.getActiveOccupation(bedId));
  }

  @PostMapping(
      "/bed-occupations/{occupationId}/release")
  BedOccupationResponse releaseOccupation(
      @PathVariable UUID occupationId) {

    return BedOccupationResponse.from(
        service.releaseOccupation(occupationId));
  }

  public record CreateFacilityRequest(
      @NotBlank String code,
      @NotBlank String name,
      @NotNull FacilityType type) {
  }

  public record CreateZoneRequest(
      @NotBlank String code,
      @NotBlank String name,
      @NotNull CareZoneType type) {
  }

  public record CreateBedRequest(
      @NotBlank String code,
      @NotNull BedType type) {
  }

  public record OccupyBedRequest(
      @NotNull UUID visitId) {
  }

  public record UpdateActivationRequest(
      @NotNull Boolean active) {
  }

  public record UpdateBedOperationalStatusRequest(
      @NotNull BedOperationalStatus status) {
  }

  public record FacilityResponse(
      UUID id,
      String code,
      String name,
      FacilityType type,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {

    static FacilityResponse from(
        HealthFacility facility) {

      return new FacilityResponse(
          facility.getId(),
          facility.getCode(),
          facility.getName(),
          facility.getType(),
          facility.isActive(),
          facility.getCreatedAt(),
          facility.getUpdatedAt());
    }
  }

  public record CareZoneResponse(
      UUID id,
      UUID facilityId,
      String code,
      String name,
      CareZoneType type,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {

    static CareZoneResponse from(
        CareZone zone) {

      return new CareZoneResponse(
          zone.getId(),
          zone.getFacilityId(),
          zone.getCode(),
          zone.getName(),
          zone.getType(),
          zone.isActive(),
          zone.getCreatedAt(),
          zone.getUpdatedAt());
    }
  }

  public record BedResponse(
      UUID id,
      UUID zoneId,
      String code,
      BedType type,
      BedOperationalStatus operationalStatus,
      Instant createdAt,
      Instant updatedAt) {

    static BedResponse from(Bed bed) {

      return new BedResponse(
          bed.getId(),
          bed.getZoneId(),
          bed.getCode(),
          bed.getType(),
          bed.getOperationalStatus(),
          bed.getCreatedAt(),
          bed.getUpdatedAt());
    }
  }

  public record BedOccupationResponse(
      UUID id,
      UUID bedId,
      UUID visitId,
      Instant startedAt,
      Instant endedAt,
      boolean active) {

    static BedOccupationResponse from(
        BedOccupation occupation) {

      return new BedOccupationResponse(
          occupation.getId(),
          occupation.getBedId(),
          occupation.getVisitId(),
          occupation.getStartedAt(),
          occupation.getEndedAt(),
          occupation.isActive());
    }
  }
}

package br.com.sussmartcare.prehospital.adapters.in.rest;

import br.com.sussmartcare.prehospital.application.AmbulanceApplicationService;
import br.com.sussmartcare.prehospital.domain.Ambulance;
import br.com.sussmartcare.prehospital.domain.AmbulanceCoverage;
import br.com.sussmartcare.prehospital.domain.AmbulanceOperationalStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pre-hospital")
public class AmbulanceController {

  private final AmbulanceApplicationService app;

  public AmbulanceController(
      AmbulanceApplicationService app) {

    this.app = app;
  }

  public record CreateAmbulanceRequest(
      @NotBlank String id,
      @NotBlank String displayName) {
  }

  public record UpdateOperationalStatusRequest(
      @NotNull AmbulanceOperationalStatus status) {
  }

  public record AssignCoverageRequest(
      @NotNull UUID facilityId) {
  }

  @PostMapping("/ambulances")
  public ResponseEntity<Ambulance>
      create(
          @Valid
          @RequestBody
          CreateAmbulanceRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(
            app.create(
                request.id(),
                request.displayName()));
  }

  @GetMapping("/ambulances")
  public List<Ambulance> list() {
    return app.list();
  }

  @GetMapping("/ambulances/{ambulanceId}")
  public Ambulance get(
      @PathVariable
      String ambulanceId) {

    return app.get(ambulanceId);
  }

  @PatchMapping(
      "/ambulances/{ambulanceId}/operational-status")
  public Ambulance updateOperationalStatus(
      @PathVariable
      String ambulanceId,
      @Valid
      @RequestBody
      UpdateOperationalStatusRequest request) {

    return app.updateOperationalStatus(
        ambulanceId,
        request.status());
  }

  @PostMapping(
      "/ambulances/{ambulanceId}/coverages")
  public ResponseEntity<AmbulanceCoverage>
      assignCoverage(
          @PathVariable
          String ambulanceId,
          @Valid
          @RequestBody
          AssignCoverageRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(
            app.assignCoverage(
                ambulanceId,
                request.facilityId()));
  }

  @GetMapping(
      "/ambulances/{ambulanceId}/coverage")
  public AmbulanceCoverage
      getActiveCoverage(
          @PathVariable
          String ambulanceId) {

    return app.getActiveCoverage(
        ambulanceId);
  }

  @PostMapping(
      "/ambulance-coverages/{coverageId}/end")
  public AmbulanceCoverage endCoverage(
      @PathVariable
      UUID coverageId) {

    return app.endCoverage(
        coverageId);
  }
}

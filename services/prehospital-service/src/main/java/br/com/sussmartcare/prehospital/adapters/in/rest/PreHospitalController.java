package br.com.sussmartcare.prehospital.adapters.in.rest;

import br.com.sussmartcare.prehospital.application.PreHospitalApplicationService;
import br.com.sussmartcare.prehospital.domain.PreHospitalEncounter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pre-hospital")
public class PreHospitalController {

  private final PreHospitalApplicationService app;

  public PreHospitalController(
      PreHospitalApplicationService app) {

    this.app = app;
  }

  public record CreateRequest(
      @NotNull UUID patientId,
      UUID visitId,
      @NotBlank String ambulanceId,
      @NotNull UUID destinationFacilityId,
      Instant estimatedArrivalAt) {
  }

  public record EtaRequest(
      @NotNull Instant estimatedArrivalAt) {
  }

  public record RiskRequest(
      @NotBlank String riskLevel) {
  }

  public record CancelRequest(
      @NotBlank String reason) {
  }

  @PostMapping("/encounters")
  public ResponseEntity<PreHospitalEncounter>
      create(
          @Valid
          @RequestBody
          CreateRequest request) {

    return ResponseEntity
        .status(201)
        .body(
            app.create(
                request.patientId(),
                request.visitId(),
                request.ambulanceId(),
                request.destinationFacilityId(),
                request.estimatedArrivalAt()));
  }

  @GetMapping("/encounters/{id}")
  public PreHospitalEncounter get(
      @PathVariable UUID id) {

    return app.get(id);
  }

  @PutMapping("/encounters/{id}/eta")
  public PreHospitalEncounter eta(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      EtaRequest request) {

    return app.eta(
        id,
        request.estimatedArrivalAt());
  }

  @PutMapping("/encounters/{id}/risk")
  public PreHospitalEncounter risk(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      RiskRequest request) {

    return app.risk(
        id,
        request.riskLevel());
  }

  @PostMapping("/encounters/{id}/arrive")
  public PreHospitalEncounter arrive(
      @PathVariable UUID id) {

    return app.arrive(id);
  }

  @PostMapping("/encounters/{id}/cancel")
  public PreHospitalEncounter cancel(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      CancelRequest request) {

    return app.cancel(
        id,
        request.reason());
  }
}

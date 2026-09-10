package br.com.sussmartcare.patientjourney.adapters.in.rest;

import br.com.sussmartcare.patientjourney.application.VisitApplicationService;
import br.com.sussmartcare.patientjourney.domain.Channel;
import br.com.sussmartcare.patientjourney.domain.Visit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class VisitController {

  private final VisitApplicationService app;

  public VisitController(
      VisitApplicationService app) {

    this.app = app;
  }

  public record CreateVisitRequest(
      @NotNull UUID patientId,
      @NotNull UUID facilityId,
      @NotNull Channel channel) {
  }

  public record AnamnesisRequest(
      @NotBlank String text) {
  }

  public record StageRequest(
      @NotBlank String stage) {
  }

  public record NoteRequest(
      String note) {
  }

  public record TransferRequest(
      @NotNull UUID targetFacilityId,
      String note) {
  }

  public record CancelRequest(
      @NotBlank String reason) {
  }

  @PostMapping("/pre-visits")
  public ResponseEntity<Visit> create(
      @Valid
      @RequestBody
      CreateVisitRequest request) {

    return ResponseEntity
        .status(201)
        .body(
            app.create(
                request.patientId(),
                request.facilityId(),
                request.channel()));
  }

  @GetMapping("/visits/{id}")
  public Visit get(
      @PathVariable UUID id) {

    return app.get(id);
  }

  @PutMapping("/pre-visits/{id}/anamnesis")
  public Visit anamnesis(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      AnamnesisRequest request) {

    return app.anamnesis(
        id,
        request.text());
  }

  @PostMapping("/pre-visits/{id}/check-in")
  public Visit checkIn(
      @PathVariable UUID id) {

    return app.checkIn(id);
  }

  @PostMapping("/visits/{id}/triage/start")
  public Visit startTriage(
      @PathVariable UUID id) {

    return app.startTriage(id);
  }

  @PostMapping("/visits/{id}/triage/complete")
  public Visit completeTriage(
      @PathVariable UUID id) {

    return app.completeTriage(id);
  }

  @PostMapping("/visits/{id}/queue")
  public Visit queue(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      StageRequest request) {

    return app.queue(
        id,
        request.stage());
  }

  @PostMapping("/visits/{id}/call")
  public Visit call(
      @PathVariable UUID id) {

    return app.call(id);
  }

  @PostMapping("/visits/{id}/service/start")
  public Visit startService(
      @PathVariable UUID id) {

    return app.startService(id);
  }

  @PostMapping("/visits/{id}/next-stage")
  public Visit nextStage(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      StageRequest request) {

    return app.nextStage(
        id,
        request.stage());
  }

  @PostMapping("/visits/{id}/discharge")
  public Visit discharge(
      @PathVariable UUID id,
      @RequestBody(required = false)
      NoteRequest request) {

    return app.discharge(
        id,
        request == null
            ? null
            : request.note());
  }

  @PostMapping("/visits/{id}/transfer")
  public Visit transfer(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      TransferRequest request) {

    return app.transfer(
        id,
        request.targetFacilityId(),
        request.note());
  }

  @PostMapping("/visits/{id}/cancel")
  public Visit cancel(
      @PathVariable UUID id,
      @Valid
      @RequestBody
      CancelRequest request) {

    return app.cancel(
        id,
        request.reason());
  }
}

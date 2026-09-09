package br.com.sussmartcare.queue.adapters.in.rest;

import br.com.sussmartcare.queue.application.QueueAccessApplicationService;
import br.com.sussmartcare.queue.application.QueueApplicationService;
import br.com.sussmartcare.queue.application.WaitTimeEstimator;
import br.com.sussmartcare.queue.domain.QueueEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QueueController {

  private final QueueApplicationService app;
  private final QueueAccessApplicationService access;

  public QueueController(
      QueueApplicationService app,
      QueueAccessApplicationService access) {

    this.app = app;
    this.access = access;
  }

  public record EnterRequest(
      @NotNull UUID visitId,
      @NotNull UUID facilityId,
      @NotBlank String clinicalPriority) {}

  public record LeaveRequest(
      @Min(1) long expectedReturnMinutes,
      @Min(0) long graceMinutes) {}

  @PostMapping("/queue-entries")
  public ResponseEntity<QueueEntry> enter(
      @Valid @RequestBody EnterRequest request) {

    return ResponseEntity.status(201).body(
        app.enter(
            request.visitId(),
            request.facilityId(),
            request.clinicalPriority()));
  }

  @GetMapping("/queue-entries/by-visit/{visitId}")
  public ResponseEntity<QueueEntry> byVisit(
      @PathVariable UUID visitId) {

    try {
      return ResponseEntity.ok(access.getByVisit(visitId));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/queue-entries/{id}/leave")
  public QueueEntry leave(
      @PathVariable UUID id,
      @Valid @RequestBody LeaveRequest request) {

    return access.leave(
        id,
        request.expectedReturnMinutes(),
        request.graceMinutes());
  }

  @PostMapping("/queue-entries/{id}/return")
  public QueueEntry returned(@PathVariable UUID id) {
    return access.returned(id);
  }

  @PostMapping("/queue-entries/{id}/call")
  public QueueEntry call(@PathVariable UUID id) {
    return app.call(id);
  }

  @GetMapping("/queues/{facilityId}/view")
  public List<QueueApplicationService.QueueView> view(
      @PathVariable UUID facilityId) {

    return app.view(facilityId);
  }

  @GetMapping("/queues/{facilityId}/public-view")
  public List<QueueApplicationService.QueuePublicView> publicView(
      @PathVariable UUID facilityId) {

    return app.publicView(facilityId);
  }

  @GetMapping("/queues/{facilityId}/estimate")
  public WaitTimeEstimator.FacilityEstimate estimate(
      @PathVariable UUID facilityId) {

    return app.facilityEstimate(facilityId);
  }
}
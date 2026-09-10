package br.com.sussmartcare.presence.adapters.in.rest;

import br.com.sussmartcare.presence.application.PresenceAccessApplicationService;
import br.com.sussmartcare.presence.application.PresenceTrackingApplicationService;
import br.com.sussmartcare.presence.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/presence")
public class PresenceController {

  private final PresenceAccessApplicationService access;
  private final PresenceTrackingApplicationService tracking;

  public PresenceController(
      PresenceAccessApplicationService access,
      PresenceTrackingApplicationService tracking) {

    this.access = access;
    this.tracking = tracking;
  }

  public record PresenceRequest(
      @NotNull UUID visitId,
      @NotNull UUID patientId,
      @NotBlank String eventType,
      String zoneId,
      @NotBlank String source,
      Instant occurredAt) {
  }

  public record GatewayRequest(
      @NotBlank String externalId,
      @NotNull PresenceSourceType sourceType,
      @NotNull UUID facilityId,
      @NotBlank String zoneId) {
  }

  public record TrackingRequest(
      @NotNull UUID visitId,
      @NotNull UUID patientId,
      @NotNull UUID facilityId) {
  }

  public record TrackingCreated(
      PresenceTrackingSession session,
      String trackingToken) {
  }

  public record SignalRequest(
      @NotNull UUID signalId,
      @NotBlank String trackingToken,
      @NotNull PresenceSourceType sourceType,
      @NotNull PresenceState state,
      String zoneId,
      String gatewayExternalId,
      Instant occurredAt) {
  }

  @PostMapping("/events")
  public ResponseEntity<PresenceEvent> record(
      @Valid
      @RequestBody
      PresenceRequest request) {

    return ResponseEntity
        .status(201)
        .body(
            access.record(
                request.visitId(),
                request.patientId(),
                request.eventType(),
                request.zoneId(),
                request.source(),
                request.occurredAt()));
  }

  @GetMapping("/visits/{visitId}")
  public List<PresenceEvent> history(
      @PathVariable UUID visitId) {

    return access.history(
        visitId);
  }

  @PostMapping("/gateways")
  public ResponseEntity<PresenceGateway> gateway(
      @Valid
      @RequestBody
      GatewayRequest request) {

    return ResponseEntity
        .status(201)
        .body(
            tracking.registerGateway(
                request.externalId(),
                request.sourceType(),
                request.facilityId(),
                request.zoneId()));
  }

  @GetMapping("/gateways")
  public List<PresenceGateway> gateways() {

    return tracking.gateways();
  }

  @PostMapping("/tracking-sessions")
  public ResponseEntity<TrackingCreated> startTracking(
      @Valid
      @RequestBody
      TrackingRequest request) {

    var started =
        tracking.start(
            request.visitId(),
            request.patientId(),
            request.facilityId());

    return ResponseEntity
        .status(201)
        .body(
            new TrackingCreated(
                started.session(),
                started.trackingToken()));
  }

  @GetMapping("/tracking-sessions/{id}")
  public PresenceTrackingSession trackingSession(
      @PathVariable UUID id) {

    return tracking.get(id);
  }

  @PostMapping("/tracking-sessions/{id}/end")
  public PresenceTrackingSession endTracking(
      @PathVariable UUID id) {

    return tracking.end(id);
  }

  @PostMapping("/signals")
  public ResponseEntity<
      PresenceTrackingApplicationService.SignalResult>
      signal(
          @Valid
          @RequestBody
          SignalRequest request) {

    return ResponseEntity
        .accepted()
        .body(
            tracking.signal(
                request.signalId(),
                request.trackingToken(),
                request.sourceType(),
                request.state(),
                request.zoneId(),
                request.gatewayExternalId(),
                request.occurredAt()));
  }

  @GetMapping("/operations/zones")
  public List<
      PresenceTrackingApplicationService.ZoneLoad>
      zoneLoads() {

    return tracking.zoneLoads();
  }
}

package br.com.sussmartcare.presence.adapters.in.rest;

import br.com.sussmartcare.presence.application.PresenceAccessApplicationService;
import br.com.sussmartcare.presence.domain.PresenceEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
@RequestMapping("/api/v1/presence")
public class PresenceController {

  private final PresenceAccessApplicationService access;

  public PresenceController(
      PresenceAccessApplicationService access) {

    this.access = access;
  }

  public record PresenceRequest(
      @NotNull UUID visitId,
      @NotNull UUID patientId,
      @NotBlank String eventType,
      String zoneId,
      @NotBlank String source,
      Instant occurredAt) {}

  @PostMapping("/events")
  public ResponseEntity<PresenceEvent> record(
      @Valid @RequestBody PresenceRequest request) {

    return ResponseEntity.status(201).body(
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

    return access.history(visitId);
  }
}
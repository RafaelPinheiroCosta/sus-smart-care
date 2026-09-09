package br.com.sussmartcare.triage.adapters.in.rest;

import br.com.sussmartcare.triage.application.TriageApplicationService;
import br.com.sussmartcare.triage.domain.ClinicalPriority;
import br.com.sussmartcare.triage.domain.Triage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/triages")
public class TriageController {

  private final TriageApplicationService app;

  public TriageController(
      TriageApplicationService app) {

    this.app = app;
  }

  public record StartRequest(
      @NotNull UUID visitId) {}

  public record AssessmentRequest(
      @NotNull ClinicalPriority recommendation,
      @DecimalMin("0.0")
      @DecimalMax("1.0")
      double confidence,
      @NotBlank String reasoning) {}

  public record DecisionRequest(
      @NotNull ClinicalPriority priority) {}

  @PostMapping
  public ResponseEntity<Triage> start(
      @Valid @RequestBody StartRequest request) {

    return ResponseEntity
        .status(201)
        .body(
            app.start(
                request.visitId()));
  }

  @GetMapping("/{id}")
  public Triage get(
      @PathVariable UUID id) {

    return app.get(id);
  }

  @GetMapping("/by-visit/{visitId}")
  public ResponseEntity<Triage> byVisit(
      @PathVariable UUID visitId) {

    try {
      return ResponseEntity.ok(
          app.getByVisit(visitId));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/{id}/assessment")
  public Triage assess(
      @PathVariable UUID id,
      @Valid @RequestBody AssessmentRequest request) {

    return app.assess(
        id,
        request.recommendation(),
        request.confidence(),
        request.reasoning());
  }

  @PostMapping("/{id}/assessment/ai")
  public Triage assessWithAi(
      @PathVariable UUID id) {

    return app.assessWithAi(id);
  }

  @PostMapping("/{id}/decision")
  public Triage decision(
      @PathVariable UUID id,
      @Valid @RequestBody DecisionRequest request,
      JwtAuthenticationToken authentication) {

    UUID professionalId =
        UUID.fromString(
            authentication
                .getToken()
                .getSubject());

    return app.confirm(
        id,
        request.priority(),
        professionalId);
  }
}
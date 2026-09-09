package br.com.sussmartcare.patientregistry.adapters.in.rest;

import br.com.sussmartcare.patientregistry.application.PatientAccessApplicationService;
import br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException;
import br.com.sussmartcare.patientregistry.application.PatientCommunicationProfileApplicationService;
import br.com.sussmartcare.patientregistry.domain.PatientCommunicationProfile;
import br.com.sussmartcare.patientregistry.domain.QueueCallMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/communication-profile")
public class PatientCommunicationProfileController {

  private final PatientCommunicationProfileApplicationService app;
  private final PatientAccessApplicationService access;

  public PatientCommunicationProfileController(
      PatientCommunicationProfileApplicationService app,
      PatientAccessApplicationService access) {

    this.app = app;
    this.access = access;
  }

  public record UpdateCommunicationProfileRequest(
      @NotNull Boolean hasSmartphone,
      @NotNull QueueCallMode queueCallMode) {}

  @PutMapping
  public PatientCommunicationProfile upsert(
      @PathVariable UUID patientId,
      @Valid @RequestBody UpdateCommunicationProfileRequest request,
      JwtAuthenticationToken authentication) {

    assertAccess(
        patientId,
        authentication);

    return app.upsert(
        patientId,
        request.hasSmartphone(),
        request.queueCallMode());
  }

  @GetMapping
  public PatientCommunicationProfile get(
      @PathVariable UUID patientId,
      JwtAuthenticationToken authentication) {

    assertAccess(
        patientId,
        authentication);

    return app.get(patientId);
  }

  private void assertAccess(
      UUID patientId,
      JwtAuthenticationToken authentication) {

    UUID userId =
        UUID.fromString(
            authentication
                .getToken()
                .getSubject());

    Set<String> roles =
        authentication
            .getAuthorities()
            .stream()
            .map(authority -> authority.getAuthority())
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority ->
                authority.substring("ROLE_".length()))
            .collect(Collectors.toSet());

    var decision =
        access.decide(
            patientId,
            userId,
            roles);

    if (!decision.allowed()) {
      throw new PatientAccessDeniedException(
          "Usuario nao possui vinculo ativo com o paciente");
    }
  }
}
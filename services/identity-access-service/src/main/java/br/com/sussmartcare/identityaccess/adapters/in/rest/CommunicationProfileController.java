package br.com.sussmartcare.identityaccess.adapters.in.rest;

import br.com.sussmartcare.identityaccess.application.CommunicationProfileApplicationService;
import br.com.sussmartcare.identityaccess.domain.CommunicationProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/communication-profiles")
public class CommunicationProfileController {

  private final CommunicationProfileApplicationService app;

  public CommunicationProfileController(
      CommunicationProfileApplicationService app) {
    this.app = app;
  }

  public record Request(
      @NotNull UUID userId,
      boolean hasSmartphone,
      @NotBlank String preferredChannel) {}

  @PutMapping
  public CommunicationProfile save(
      @Valid @RequestBody Request request) {

    return app.save(
        request.userId(),
        request.hasSmartphone(),
        request.preferredChannel());
  }

  @GetMapping("/{userId}")
  public CommunicationProfile get(
      @PathVariable UUID userId) {

    return app.get(userId);
  }
}

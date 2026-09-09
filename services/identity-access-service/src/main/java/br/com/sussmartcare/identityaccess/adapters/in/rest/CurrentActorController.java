package br.com.sussmartcare.identityaccess.adapters.in.rest;

import br.com.sussmartcare.identityaccess.application.CurrentActorApplicationService;
import br.com.sussmartcare.identityaccess.domain.ActorIdentity;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class CurrentActorController {

  private final CurrentActorApplicationService app;

  public CurrentActorController(
      CurrentActorApplicationService app) {

    this.app = app;
  }

  @GetMapping("/me")
  public ActorIdentity me(
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

    return app.current(
        userId,
        roles);
  }
}
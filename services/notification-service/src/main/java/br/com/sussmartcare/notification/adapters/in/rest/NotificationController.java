package br.com.sussmartcare.notification.adapters.in.rest;

import br.com.sussmartcare.notification.application.NotificationAccessApplicationService;
import br.com.sussmartcare.notification.application.NotificationApplicationService;
import br.com.sussmartcare.notification.domain.Notification;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationApplicationService app;
  private final NotificationAccessApplicationService access;

  public NotificationController(
      NotificationApplicationService app,
      NotificationAccessApplicationService access) {

    this.app = app;
    this.access = access;
  }

  public record SendRequest(
      @NotNull UUID patientId,
      UUID visitId,
      @NotBlank String type,
      @NotBlank String channel,
      @NotBlank String message) {}

  @PostMapping
  public ResponseEntity<Notification> send(
      @Valid @RequestBody SendRequest request) {

    return ResponseEntity
        .status(202)
        .body(
            app.send(
                request.patientId(),
                request.visitId(),
                request.type(),
                request.channel(),
                request.message()));
  }

  @GetMapping("/patients/{patientId}")
  public List<Notification> history(
      @PathVariable UUID patientId) {

    return access.history(patientId);
  }
}
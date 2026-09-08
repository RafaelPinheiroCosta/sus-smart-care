package br.com.sussmartcare.patientregistry.adapters.in.rest;

import br.com.sussmartcare.patientregistry.application.PatientAccessApplicationService;
import br.com.sussmartcare.patientregistry.application.PatientApplicationService;
import br.com.sussmartcare.patientregistry.domain.IdentifierType;
import br.com.sussmartcare.patientregistry.domain.Patient;
import br.com.sussmartcare.patientregistry.domain.PatientIdentifier;
import br.com.sussmartcare.patientregistry.domain.RelationshipType;
import br.com.sussmartcare.patientregistry.domain.RepresentativeRelationship;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

  private static final Set<String> CREATE_AS_OPERATOR_ROLES =
      Set.of(
          "OPERATOR",
          "AMBULANCE_TEAM",
          "ADMIN");

  private static final Set<String> PRIVILEGED_PATIENT_READ_ROLES =
      Set.of(
          "OPERATOR",
          "TRIAGE_NURSE",
          "DOCTOR",
          "AMBULANCE_TEAM",
          "ADMIN");

  private static final Set<String> RELATIONSHIP_ADMIN_ROLES =
      Set.of(
          "OPERATOR",
          "ADMIN");
  private final PatientApplicationService app;
  private final PatientAccessApplicationService access;

  public PatientController(
      PatientApplicationService app,
      PatientAccessApplicationService access) {

    this.app = app;
    this.access = access;
  }

  public record CreatePatientRequest(
      @NotBlank String fullName,
      LocalDate birthDate,
      IdentifierType identifierType,
      String identifierValue) {}

  public record ProvisionalRequest(
      @NotBlank String description) {}

  public record ResolveRequest(
      @NotBlank String fullName,
      LocalDate birthDate) {}

  public record LinkRequest(
      @NotNull UUID representativeUserId,
      @NotNull RelationshipType type) {}

  public record SelfLinkRequest(
      @NotNull UUID userId) {}

  public record MergeRequest(
      @NotNull UUID canonicalPatientId) {}

  public record PatientAccessResponse(
      UUID patientId,
      UUID userId,
      boolean allowed,
      String reason) {}

  @PostMapping
  public ResponseEntity<Patient> create(
      @Valid @RequestBody CreatePatientRequest request,
      JwtAuthenticationToken authentication) {

    Actor actor = actor(authentication);

    boolean patientSelfRegistration =
        actor.roles().contains("PATIENT")
            && !hasAnyRole(
                actor,
                CREATE_AS_OPERATOR_ROLES);

    boolean privilegedCreation =
        hasAnyRole(
            actor,
            CREATE_AS_OPERATOR_ROLES);

    Patient patient;

    if (patientSelfRegistration) {

      patient =
          app.createForSelf(
              request.fullName(),
              request.birthDate(),
              request.identifierType(),
              request.identifierValue(),
              actor.userId());

    } else if (privilegedCreation) {

      patient =
          app.create(
              request.fullName(),
              request.birthDate(),
              request.identifierType(),
              request.identifierValue());

    } else {

      throw new br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException(
          "Perfil nao autorizado a criar paciente sem vinculo");
    }

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(patient);
  }
  @PostMapping("/provisional")
  public ResponseEntity<Patient> provisional(
      @Valid @RequestBody ProvisionalRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(
            app.provisional(
                request.description()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Patient> get(
      @PathVariable UUID id,
      JwtAuthenticationToken authentication) {

    requirePatientAccess(
        id,
        authentication);

    return ResponseEntity.ok(
        app.get(id));
  }
  @GetMapping("/{id}/access")
  public ResponseEntity<PatientAccessResponse> access(
      @PathVariable UUID id,
      JwtAuthenticationToken authentication) {

    Actor actor = actor(authentication);

    var decision =
        access.decide(
            id,
            actor.userId(),
            actor.roles());

    var response =
        new PatientAccessResponse(
            id,
            actor.userId(),
            decision.allowed(),
            decision.reason());

    if (!decision.allowed()) {
      return ResponseEntity
          .status(HttpStatus.FORBIDDEN)
          .body(response);
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/by-identifier")
  public Patient byIdentifier(
      @RequestParam IdentifierType type,
      @RequestParam String value,
      JwtAuthenticationToken authentication) {

    Actor actor = actor(authentication);

    if (!hasAnyRole(
        actor,
        PRIVILEGED_PATIENT_READ_ROLES)) {

      throw new br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException(
          "Busca de paciente por identificador exige perfil operacional");
    }

    return app.findByIdentifier(
        type,
        value);
  }
  @GetMapping("/{id}/identifiers")
  public List<PatientIdentifier> identifiers(
      @PathVariable UUID id,
      JwtAuthenticationToken authentication) {

    requirePatientAccess(
        id,
        authentication);

    return app.identifiers(id);
  }
  @PostMapping("/{id}/resolve-identity")
  public Patient resolve(
      @PathVariable UUID id,
      @Valid @RequestBody ResolveRequest request) {

    return app.resolve(
        id,
        request.fullName(),
        request.birthDate());
  }

  @PostMapping("/{id}/merge")
  public Patient merge(
      @PathVariable UUID id,
      @Valid @RequestBody MergeRequest request) {

    return app.merge(
        id,
        request.canonicalPatientId());
  }

  @PostMapping("/{id}/self-link")
  public ResponseEntity<RepresentativeRelationship> selfLink(
      @PathVariable UUID id,
      @Valid @RequestBody SelfLinkRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(
            app.linkSelf(
                id,
                request.userId()));
  }

  @PostMapping("/{id}/representatives")
  public ResponseEntity<RepresentativeRelationship> link(
      @PathVariable UUID id,
      @Valid @RequestBody LinkRequest request,
      JwtAuthenticationToken authentication) {

    if (request.type() == RelationshipType.SELF) {
      throw new IllegalArgumentException(
          "Vinculo SELF somente pode ser criado pelo fluxo de identidade");
    }

    Actor actor = actor(authentication);

    requireSelfOrRelationshipAdmin(
        id,
        actor);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(
            app.link(
                id,
                request.representativeUserId(),
                request.type()));
  }
  @GetMapping("/{id}/representatives")
  public List<RepresentativeRelationship> representatives(
      @PathVariable UUID id,
      JwtAuthenticationToken authentication) {

    requirePatientAccess(
        id,
        authentication);

    return app.representatives(id);
  }
  @DeleteMapping("/representatives/{relationshipId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(
      @PathVariable UUID relationshipId,
      JwtAuthenticationToken authentication) {

    RepresentativeRelationship relationship =
        app.relationship(
            relationshipId);

    Actor actor = actor(authentication);

    requireSelfOrRelationshipAdmin(
        relationship.getPatientId(),
        actor);

    app.revoke(
        relationshipId);
  }
  private void requirePatientAccess(
      UUID patientId,
      JwtAuthenticationToken authentication) {

    Actor actor =
        actor(authentication);

    var decision =
        access.decide(
            patientId,
            actor.userId(),
            actor.roles());

    if (!decision.allowed()) {
      throw new br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException(
          "Usuario nao possui vinculo ativo com o paciente");
    }
  }

  private void requireSelfOrRelationshipAdmin(
      UUID patientId,
      Actor actor) {

    if (hasAnyRole(
        actor,
        RELATIONSHIP_ADMIN_ROLES)) {
      return;
    }

    var decision =
        access.decide(
            patientId,
            actor.userId(),
            actor.roles());

    boolean ownPatient =
        actor.roles().contains("PATIENT")
            && decision.allowed()
            && "SELF".equals(
                decision.reason());

    if (!ownPatient) {
      throw new br.com.sussmartcare.patientregistry.application.PatientAccessDeniedException(
          "Somente o proprio paciente ou perfil administrativo pode alterar representacoes");
    }
  }

  private boolean hasAnyRole(
      Actor actor,
      Set<String> allowedRoles) {

    return actor.roles()
        .stream()
        .anyMatch(
            allowedRoles::contains);
  }
  private Actor actor(
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
            .map(
                authority ->
                    authority.getAuthority())
            .filter(
                authority ->
                    authority.startsWith("ROLE_"))
            .map(
                authority ->
                    authority.substring(
                        "ROLE_".length()))
            .collect(
                Collectors.toSet());

    return new Actor(
        userId,
        roles);
  }

  private record Actor(
      UUID userId,
      Set<String> roles) {}
}
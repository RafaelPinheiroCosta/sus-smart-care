package br.com.sussmartcare.presence.infrastructure.visitaccess;

import br.com.sussmartcare.presence.application.VisitAccessDeniedException;
import br.com.sussmartcare.presence.application.VisitAccessPort;
import br.com.sussmartcare.presence.application.VisitAccessUnavailableException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpVisitAccessAdapter implements VisitAccessPort {

  private final RestClient client;

  public HttpVisitAccessAdapter(
      @Value("${PATIENT_JOURNEY_URL:http://localhost:8082}")
      String baseUrl) {

    this.client = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
  }

  @Override
  public UUID assertCurrentActorCanAccess(UUID visitId) {

    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
      throw new VisitAccessDeniedException(
          "Usuario autenticado nao possui contexto JWT valido");
    }

    String accessToken =
        jwtAuthentication.getToken().getTokenValue();

    try {

      VisitResponse response =
          client.get()
              .uri("/api/v1/visits/{visitId}", visitId)
              .header(
                  HttpHeaders.AUTHORIZATION,
                  "Bearer " + accessToken)
              .retrieve()
              .body(VisitResponse.class);

      if (response == null || response.patientId() == null) {
        throw new VisitAccessUnavailableException(
            "Patient Journey retornou visita sem patientId",
            null);
      }

      return response.patientId();

    } catch (HttpClientErrorException exception) {

      if (exception.getStatusCode() == HttpStatus.FORBIDDEN
          || exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {

        throw new VisitAccessDeniedException(
            "Usuario nao possui acesso a visita");
      }

      if (exception.getStatusCode() == HttpStatus.BAD_REQUEST
          || exception.getStatusCode() == HttpStatus.NOT_FOUND) {

        throw new IllegalArgumentException(
            "Visita inexistente ou invalida");
      }

      throw new VisitAccessUnavailableException(
          "Falha ao validar acesso no Patient Journey",
          exception);

    } catch (RestClientException exception) {

      throw new VisitAccessUnavailableException(
          "Patient Journey indisponivel para validacao de acesso",
          exception);
    }
  }

  private record VisitResponse(
      UUID id,
      UUID patientId) {}
}
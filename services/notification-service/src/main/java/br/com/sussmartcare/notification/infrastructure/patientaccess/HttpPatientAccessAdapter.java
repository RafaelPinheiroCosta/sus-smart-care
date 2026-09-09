package br.com.sussmartcare.notification.infrastructure.patientaccess;

import br.com.sussmartcare.notification.application.PatientAccessDeniedException;
import br.com.sussmartcare.notification.application.PatientAccessPort;
import br.com.sussmartcare.notification.application.PatientAccessUnavailableException;
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
public class HttpPatientAccessAdapter implements PatientAccessPort {

  private final RestClient client;

  public HttpPatientAccessAdapter(
      @Value("${patient-registry.base-url:http://localhost:8081}")
      String baseUrl) {

    this.client = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
  }

  @Override
  public void assertCurrentActorCanAccess(UUID patientId) {

    Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
      throw new PatientAccessDeniedException(
          "Usuario autenticado nao possui contexto JWT valido");
    }

    String accessToken =
        jwtAuthentication.getToken().getTokenValue();

    try {

      client.get()
          .uri(
              "/api/v1/patients/{patientId}/access",
              patientId)
          .header(
              HttpHeaders.AUTHORIZATION,
              "Bearer " + accessToken)
          .retrieve()
          .toBodilessEntity();

    } catch (HttpClientErrorException exception) {

      if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
        throw new PatientAccessDeniedException(
            "Usuario nao possui vinculo ativo com o paciente");
      }

      if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        throw new PatientAccessDeniedException(
            "Credencial nao aceita para acesso ao paciente");
      }

      if (exception.getStatusCode() == HttpStatus.BAD_REQUEST
          || exception.getStatusCode() == HttpStatus.NOT_FOUND) {

        throw new IllegalArgumentException(
            "Paciente inexistente ou invalido");
      }

      throw new PatientAccessUnavailableException(
          "Falha ao validar acesso no Patient Registry",
          exception);

    } catch (RestClientException exception) {

      throw new PatientAccessUnavailableException(
          "Patient Registry indisponivel para validacao de acesso",
          exception);
    }
  }
}
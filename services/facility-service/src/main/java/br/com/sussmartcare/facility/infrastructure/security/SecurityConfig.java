package br.com.sussmartcare.facility.infrastructure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http) throws Exception {

    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/actuator/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**")
            .permitAll()

            .requestMatchers(
                HttpMethod.GET,
                "/api/v1/**")
            .hasAnyRole(
                "OPERATOR",
                "TRIAGE_NURSE",
                "DOCTOR",
                "AMBULANCE_TEAM",
                "ADMIN")

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/facilities",
                "/api/v1/facilities/*/zones",
                "/api/v1/zones/*/beds")
            .hasRole("ADMIN")

            .requestMatchers(
                HttpMethod.PATCH,
                "/api/v1/facilities/*/status",
                "/api/v1/zones/*/status",
                "/api/v1/beds/*/operational-status")
            .hasRole("ADMIN")

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/beds/*/occupations",
                "/api/v1/bed-occupations/*/release")
            .hasAnyRole(
                "OPERATOR",
                "TRIAGE_NURSE",
                "DOCTOR",
                "ADMIN")

            .anyRequest()
            .denyAll())
        .oauth2ResourceServer(
            oauth ->
                oauth.jwt(
                    jwt ->
                        jwt.jwtAuthenticationConverter(
                            jwtAuthenticationConverter())));

    return http.build();
  }

  private JwtAuthenticationConverter
      jwtAuthenticationConverter() {

    JwtGrantedAuthoritiesConverter scopeConverter =
        new JwtGrantedAuthoritiesConverter();

    JwtAuthenticationConverter converter =
        new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {

          Collection<GrantedAuthority> authorities =
              new ArrayList<>();

          Collection<GrantedAuthority> scopes =
              scopeConverter.convert(jwt);

          if (scopes != null) {
            authorities.addAll(scopes);
          }

          Map<String, Object> realmAccess =
              jwt.getClaim("realm_access");

          if (realmAccess != null
              && realmAccess.get("roles")
                  instanceof Collection<?> roles) {

            for (Object role : roles) {

              authorities.add(
                  new SimpleGrantedAuthority(
                      "ROLE_" + role));
            }
          }

          return authorities;
        });

    return converter;
  }
}

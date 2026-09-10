package br.com.sussmartcare.presence.infrastructure.security;

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
      HttpSecurity http)
      throws Exception {

    http
        .csrf(
            csrf ->
                csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth
                    .requestMatchers(
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**")
                    .permitAll()

                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/presence/gateways")
                    .hasRole("ADMIN")

                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/presence/gateways",
                        "/api/v1/presence/operations/**")
                    .hasAnyRole(
                        "OPERATOR",
                        "TRIAGE_NURSE",
                        "DOCTOR",
                        "ADMIN")

                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/presence/signals")
                    .hasAnyRole(
                        "PATIENT",
                        "REPRESENTATIVE",
                        "OPERATOR",
                        "TRIAGE_NURSE",
                        "DEVICE",
                        "ADMIN")

                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/presence/tracking-sessions",
                        "/api/v1/presence/tracking-sessions/*/end")
                    .hasAnyRole(
                        "PATIENT",
                        "REPRESENTATIVE",
                        "OPERATOR",
                        "TRIAGE_NURSE",
                        "ADMIN")

                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/presence/tracking-sessions/**")
                    .hasAnyRole(
                        "PATIENT",
                        "REPRESENTATIVE",
                        "OPERATOR",
                        "TRIAGE_NURSE",
                        "DOCTOR",
                        "ADMIN")

                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/presence/events")
                    .hasAnyRole(
                        "PATIENT",
                        "REPRESENTATIVE",
                        "OPERATOR",
                        "TRIAGE_NURSE",
                        "ADMIN")

                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/presence/visits/**")
                    .hasAnyRole(
                        "PATIENT",
                        "REPRESENTATIVE",
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

    JwtGrantedAuthoritiesConverter scopes =
        new JwtGrantedAuthoritiesConverter();

    JwtAuthenticationConverter converter =
        new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {

          Collection<GrantedAuthority> authorities =
              new ArrayList<>();

          Collection<GrantedAuthority>
              scopeAuthorities =
                  scopes.convert(jwt);

          if (scopeAuthorities != null) {
            authorities.addAll(
                scopeAuthorities);
          }

          Map<String,Object> realmAccess =
              jwt.getClaim(
                  "realm_access");

          if (
              realmAccess != null &&
              realmAccess.get("roles")
                  instanceof Collection<?> roles
          ) {

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

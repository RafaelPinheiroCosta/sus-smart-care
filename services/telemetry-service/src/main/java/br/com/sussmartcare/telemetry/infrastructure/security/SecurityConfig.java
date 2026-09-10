package br.com.sussmartcare.telemetry.infrastructure.security;

import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {

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
                HttpMethod.POST,
                "/api/v1/devices")
            .hasRole("ADMIN")

            .requestMatchers(
                HttpMethod.PATCH,
                "/api/v1/devices/*/status")
            .hasRole("ADMIN")

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/devices/*/placements",
                "/api/v1/device-placements/*/end")
            .hasAnyRole("OPERATOR", "ADMIN")

            .requestMatchers(
                HttpMethod.GET,
                "/api/v1/devices/**")
            .hasAnyRole(
                "OPERATOR",
                "TRIAGE_NURSE",
                "DOCTOR",
                "AMBULANCE_TEAM",
                "ADMIN")

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/telemetry/sessions")
            .hasAnyRole(
                "TRIAGE_NURSE",
                "AMBULANCE_TEAM",
                "ADMIN")

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/telemetry/sessions/*/end",
                "/api/v1/telemetry/sessions/*/assignments",
                "/api/v1/device-assignments/*/end")
            .hasAnyRole(
                "TRIAGE_NURSE",
                "AMBULANCE_TEAM",
                "ADMIN")

            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/telemetry/sessions/*/observations")
            .hasAnyRole(
                "DEVICE",
                "AMBULANCE_TEAM",
                "TRIAGE_NURSE",
                "ADMIN")

            .requestMatchers(
                HttpMethod.GET,
                "/api/v1/telemetry/**")
            .hasAnyRole(
                "TRIAGE_NURSE",
                "DOCTOR",
                "AMBULANCE_TEAM",
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

  private JwtAuthenticationConverter jwtAuthenticationConverter() {

    JwtGrantedAuthoritiesConverter scopes =
        new JwtGrantedAuthoritiesConverter();

    JwtAuthenticationConverter converter =
        new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(jwt -> {

      Collection<GrantedAuthority> authorities =
          new ArrayList<>();

      Collection<GrantedAuthority> scopeAuthorities =
          scopes.convert(jwt);

      if (scopeAuthorities != null) {
        authorities.addAll(scopeAuthorities);
      }

      Map<String,Object> realm =
          jwt.getClaim("realm_access");

      if (realm != null &&
          realm.get("roles") instanceof Collection<?> roles) {

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

package br.com.sussmartcare.gateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
@Configuration
class SecurityConfig {
  @Bean SecurityWebFilterChain filter(ServerHttpSecurity http){
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
      .authorizeExchange(a->a
        .pathMatchers("/actuator/**","/api/v1/queues/*/estimate","/api/v1/queues/*/public-view").permitAll()
        .anyExchange().authenticated())
      .oauth2ResourceServer(o->o.jwt(j->{})).build();
  }
}

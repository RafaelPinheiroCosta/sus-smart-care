package br.com.sussmartcare.telemetry.adapters.in.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sussmartcare.telemetry.application.*;
import br.com.sussmartcare.telemetry.infrastructure.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TelemetryController.class)
@Import(SecurityConfig.class)
class TelemetryDeviceModelSecurityTest {

  @Autowired MockMvc mockMvc;

  @MockBean TelemetryApplicationService app;
  @MockBean TelemetrySummaryService summaries;
  @MockBean TelemetrySessionCommandService sessionCommands;
  @MockBean TelemetrySampleProcessor processor;
  @MockBean TelemetryReadService reads;

  @Test
  void anonymousCannotListDevices() throws Exception {
    mockMvc.perform(get("/api/v1/devices"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void patientCannotListDevices() throws Exception {
    mockMvc.perform(
        get("/api/v1/devices")
            .with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_PATIENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanRegisterDevice() throws Exception {
    mockMvc.perform(
        post("/api/v1/devices")
            .with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "externalId":"SEC-01",
                  "deviceType":"MONITOR"
                }
                """))
        .andExpect(status().isCreated());
  }

  @Test
  void triageCanStartSession() throws Exception {
    mockMvc.perform(
        post("/api/v1/telemetry/sessions")
            .with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_TRIAGE_NURSE")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "patientId":"%s",
                  "sourceContext":"TRIAGE"
                }
                """.formatted(UUID.randomUUID())))
        .andExpect(status().isCreated());
  }

  @Test
  void deviceCanIngestObservation() throws Exception {
    mockMvc.perform(
        post(
            "/api/v1/telemetry/sessions/" +
            UUID.randomUUID() +
            "/observations")
            .with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_DEVICE")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "deviceId":"%s",
                  "type":"HEART_RATE",
                  "value":80
                }
                """.formatted(UUID.randomUUID())))
        .andExpect(status().isAccepted());
  }

  @Test
  void deviceCannotStartSession() throws Exception {
    mockMvc.perform(
        post("/api/v1/telemetry/sessions")
            .with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_DEVICE")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "patientId":"%s",
                  "sourceContext":"TRIAGE"
                }
                """.formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }
}

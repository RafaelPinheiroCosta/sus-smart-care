package br.com.sussmartcare.prehospital.adapters.in.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sussmartcare.prehospital.application.AmbulanceApplicationService;
import br.com.sussmartcare.prehospital.application.PreHospitalApplicationService;
import br.com.sussmartcare.prehospital.infrastructure.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {
        AmbulanceController.class,
        PreHospitalController.class
    })
@Import(SecurityConfig.class)
class PreHospitalApiSecurityTest {

  @Autowired
  MockMvc mockMvc;

  @MockBean
  AmbulanceApplicationService
      ambulanceApplicationService;

  @MockBean
  PreHospitalApplicationService
      preHospitalApplicationService;

  @Test
  void anonymousCannotReadFleet()
      throws Exception {

    mockMvc.perform(
            get(
                "/api/v1/pre-hospital/ambulances"))
        .andExpect(
            status().isUnauthorized());
  }

  @Test
  void patientCannotReadFleet()
      throws Exception {

    mockMvc.perform(
            get(
                "/api/v1/pre-hospital/ambulances")
                .with(
                    jwt().authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_PATIENT"))))
        .andExpect(
            status().isForbidden());
  }

  @Test
  void ambulanceTeamCanReadFleet()
      throws Exception {

    mockMvc.perform(
            get(
                "/api/v1/pre-hospital/ambulances")
                .with(
                    jwt().authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_AMBULANCE_TEAM"))))
        .andExpect(
            status().isOk());
  }

  @Test
  void ambulanceTeamCannotCreateFleetResource()
      throws Exception {

    mockMvc.perform(
            post(
                "/api/v1/pre-hospital/ambulances")
                .with(
                    jwt().authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_AMBULANCE_TEAM")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "id": "SAMU-SEC-01",
                      "displayName": "USA Security"
                    }
                    """))
        .andExpect(
            status().isForbidden());
  }

  @Test
  void adminCanCreateFleetResource()
      throws Exception {

    mockMvc.perform(
            post(
                "/api/v1/pre-hospital/ambulances")
                .with(
                    jwt().authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "id": "SAMU-SEC-02",
                      "displayName": "USA Security"
                    }
                    """))
        .andExpect(
            status().isCreated());
  }

  @Test
  void ambulanceTeamCanCreateEncounter()
      throws Exception {

    mockMvc.perform(
            post(
                "/api/v1/pre-hospital/encounters")
                .with(
                    jwt().authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_AMBULANCE_TEAM")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "patientId": "%s",
                      "ambulanceId": "SAMU-SEC-03",
                      "destinationFacilityId": "%s"
                    }
                    """.formatted(
                        UUID.randomUUID(),
                        UUID.randomUUID())))
        .andExpect(
            status().isCreated());
  }

  @Test
  void patientCannotReadEncounter()
      throws Exception {

    mockMvc.perform(
            get(
                "/api/v1/pre-hospital/encounters/" +
                UUID.randomUUID())
                .with(
                    jwt().authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_PATIENT"))))
        .andExpect(
            status().isForbidden());
  }

  @Test
  void doctorCanReadEncounter()
      throws Exception {

    mockMvc.perform(
            get(
                "/api/v1/pre-hospital/encounters/" +
                UUID.randomUUID())
                .with(
                    jwt().authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_DOCTOR"))))
        .andExpect(
            status().isOk());
  }
}

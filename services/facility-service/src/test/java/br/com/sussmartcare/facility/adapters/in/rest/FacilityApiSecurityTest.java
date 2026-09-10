package br.com.sussmartcare.facility.adapters.in.rest;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sussmartcare.facility.application.FacilityApplicationService;
import br.com.sussmartcare.facility.application.FacilityConflictException;
import br.com.sussmartcare.facility.application.FacilityNotFoundException;
import br.com.sussmartcare.facility.domain.Bed;
import br.com.sussmartcare.facility.domain.BedOccupation;
import br.com.sussmartcare.facility.domain.BedOperationalStatus;
import br.com.sussmartcare.facility.domain.BedType;
import br.com.sussmartcare.facility.domain.CareZone;
import br.com.sussmartcare.facility.domain.CareZoneType;
import br.com.sussmartcare.facility.domain.FacilityType;
import br.com.sussmartcare.facility.domain.HealthFacility;
import br.com.sussmartcare.facility.infrastructure.security.SecurityConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {
    FacilityController.class,
    ApiExceptionHandler.class,
    SecurityConfig.class
})
class FacilityApiSecurityTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  FacilityApplicationService service;

  @MockitoBean
  JwtDecoder jwtDecoder;

  @Test
  void unauthenticatedRequestReturns401()
      throws Exception {

    mockMvc.perform(
            get("/api/v1/facilities"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void patientCannotReadInternalFacilityTopology()
      throws Exception {

    mockMvc.perform(
            get("/api/v1/facilities")
                .with(jwt().authorities(
                    role("PATIENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void doctorCanReadFacilityTopology()
      throws Exception {

    when(service.listFacilities())
        .thenReturn(List.of());

    mockMvc.perform(
            get("/api/v1/facilities")
                .with(jwt().authorities(
                    role("DOCTOR"))))
        .andExpect(status().isOk());
  }

  @Test
  void operatorCannotCreateFacility()
      throws Exception {

    mockMvc.perform(
            post("/api/v1/facilities")
                .with(jwt().authorities(
                    role("OPERATOR")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "UPA-01",
                      "name": "UPA Central",
                      "type": "UPA"
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanCreateFacility()
      throws Exception {

    HealthFacility created =
        new HealthFacility(
            "UPA-01",
            "UPA Central",
            FacilityType.UPA);

    when(service.createFacility(
        "UPA-01",
        "UPA Central",
        FacilityType.UPA))
        .thenReturn(created);

    mockMvc.perform(
            post("/api/v1/facilities")
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "UPA-01",
                      "name": "UPA Central",
                      "type": "UPA"
                    }
                    """))
        .andExpect(status().isCreated());
  }

  @Test
  void adminCanDeactivateFacility()
      throws Exception {

    HealthFacility facility =
        new HealthFacility(
            "UPA-01",
            "UPA Central",
            FacilityType.UPA);

    facility.deactivate();

    when(service.setFacilityActive(
        facility.getId(),
        false))
        .thenReturn(facility);

    mockMvc.perform(
            patch(
                "/api/v1/facilities/{facilityId}/status",
                facility.getId())
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "active": false
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.active")
                .value(false));
  }

  @Test
  void adminCanDeactivateZone()
      throws Exception {

    CareZone zone =
        new CareZone(
            UUID.randomUUID(),
            "OBS-01",
            "Observation",
            CareZoneType.OBSERVATION);

    zone.deactivate();

    when(service.setZoneActive(
        zone.getId(),
        false))
        .thenReturn(zone);

    mockMvc.perform(
            patch(
                "/api/v1/zones/{zoneId}/status",
                zone.getId())
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "active": false
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.active")
                .value(false));
  }

  @Test
  void adminCanPutBedInMaintenance()
      throws Exception {

    Bed bed =
        new Bed(
            UUID.randomUUID(),
            "BED-01",
            BedType.BED);

    bed.putInMaintenance();

    when(service.setBedOperationalStatus(
        bed.getId(),
        BedOperationalStatus.MAINTENANCE))
        .thenReturn(bed);

    mockMvc.perform(
            patch(
                "/api/v1/beds/{bedId}/operational-status",
                bed.getId())
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "MAINTENANCE"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.operationalStatus")
                .value("MAINTENANCE"));
  }

  @Test
  void operatorCannotChangeFacilityState()
      throws Exception {

    mockMvc.perform(
            patch(
                "/api/v1/facilities/{facilityId}/status",
                UUID.randomUUID())
                .with(jwt().authorities(
                    role("OPERATOR")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "active": false
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void missingActivationValueReturns400()
      throws Exception {

    mockMvc.perform(
            patch(
                "/api/v1/facilities/{facilityId}/status",
                UUID.randomUUID())
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void operatorCanOccupyBed()
      throws Exception {

    UUID bedId = UUID.randomUUID();
    UUID visitId = UUID.randomUUID();

    BedOccupation occupation =
        new BedOccupation(
            bedId,
            visitId);

    when(service.occupyBed(
        bedId,
        visitId))
        .thenReturn(occupation);

    mockMvc.perform(
            post(
                "/api/v1/beds/{bedId}/occupations",
                bedId)
                .with(jwt().authorities(
                    role("OPERATOR")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "visitId": "%s"
                    }
                    """.formatted(visitId)))
        .andExpect(status().isCreated());
  }

  @Test
  void patientCannotOccupyBed()
      throws Exception {

    UUID bedId = UUID.randomUUID();

    mockMvc.perform(
            post(
                "/api/v1/beds/{bedId}/occupations",
                bedId)
                .with(jwt().authorities(
                    role("PATIENT")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "visitId": "%s"
                    }
                    """.formatted(
                        UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidRequestReturns400()
      throws Exception {

    mockMvc.perform(
            post("/api/v1/facilities")
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "",
                      "name": "",
                      "type": "UPA"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void malformedJsonReturns400()
      throws Exception {

    mockMvc.perform(
            post("/api/v1/facilities")
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("{ invalid-json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invalidBedStatusReturns400()
      throws Exception {

    mockMvc.perform(
            patch(
                "/api/v1/beds/{bedId}/operational-status",
                UUID.randomUUID())
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "INVALID"
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void missingFacilityReturns404()
      throws Exception {

    UUID facilityId = UUID.randomUUID();

    when(service.getFacility(facilityId))
        .thenThrow(
            new FacilityNotFoundException(
                "Health facility not found"));

    mockMvc.perform(
            get(
                "/api/v1/facilities/{facilityId}",
                facilityId)
                .with(jwt().authorities(
                    role("DOCTOR"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void duplicateFacilityReturns409()
      throws Exception {

    when(service.createFacility(
        "UPA-01",
        "UPA Central",
        FacilityType.UPA))
        .thenThrow(
            new FacilityConflictException(
                "Facility code already exists"));

    mockMvc.perform(
            post("/api/v1/facilities")
                .with(jwt().authorities(
                    role("ADMIN")))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "UPA-01",
                      "name": "UPA Central",
                      "type": "UPA"
                    }
                    """))
        .andExpect(status().isConflict());
  }

  private SimpleGrantedAuthority role(
      String role) {

    return new SimpleGrantedAuthority(
        "ROLE_" + role);
  }
}

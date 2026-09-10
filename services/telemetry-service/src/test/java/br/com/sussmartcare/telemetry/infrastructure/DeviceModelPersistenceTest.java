package br.com.sussmartcare.telemetry.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sussmartcare.telemetry.domain.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE)
class DeviceModelPersistenceTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("telemetry_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired DeviceRepository devices;
  @Autowired TelemetrySessionRepository sessions;
  @Autowired DevicePlacementRepository placements;
  @Autowired DeviceAssignmentRepository assignments;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.execute(
        "truncate table biometric_observations, device_assignments, " +
        "device_placements, telemetry_sessions, medical_devices, " +
        "outbox_events cascade");
  }

  @Test
  void onlyOneActivePlacementPerDevice() {

    MedicalDevice device =
        devices.saveAndFlush(
            new MedicalDevice("DEV-P-1","MONITOR",null,null));

    placements.saveAndFlush(
        new DevicePlacement(
            device.getId(),
            DevicePlacementType.FACILITY,
            UUID.randomUUID(),
            null,
            null));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> placements.saveAndFlush(
            new DevicePlacement(
                device.getId(),
                DevicePlacementType.FACILITY,
                UUID.randomUUID(),
                null,
                null)));
  }

  @Test
  void onlyOneActiveAssignmentPerDevice() {

    MedicalDevice device =
        devices.saveAndFlush(
            new MedicalDevice("DEV-A-1","MONITOR",null,null));

    TelemetrySession s1 =
        sessions.saveAndFlush(
            new TelemetrySession(
                UUID.randomUUID(),null,null,"TRIAGE"));

    TelemetrySession s2 =
        sessions.saveAndFlush(
            new TelemetrySession(
                UUID.randomUUID(),null,null,"TRIAGE"));

    assignments.saveAndFlush(
        new DeviceAssignment(
            device.getId(),
            s1.getId()));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> assignments.saveAndFlush(
            new DeviceAssignment(
                device.getId(),
                s2.getId())));
  }

  @Test
  void closedPlacementAllowsReuse() {

    MedicalDevice device =
        devices.saveAndFlush(
            new MedicalDevice("DEV-P-2","MONITOR",null,null));

    DevicePlacement first =
        placements.saveAndFlush(
            new DevicePlacement(
                device.getId(),
                DevicePlacementType.FACILITY,
                UUID.randomUUID(),
                null,
                null));

    first.close();
    placements.saveAndFlush(first);

    DevicePlacement second =
        placements.saveAndFlush(
            new DevicePlacement(
                device.getId(),
                DevicePlacementType.AMBULANCE,
                null,
                null,
                "SAMU-01"));

    assertTrue(second.isActive());
  }
}

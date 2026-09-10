package br.com.sussmartcare.prehospital.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.sussmartcare.prehospital.domain.Ambulance;
import br.com.sussmartcare.prehospital.domain.AmbulanceCoverage;
import br.com.sussmartcare.prehospital.domain.AmbulanceOperationalStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
    replace =
        AutoConfigureTestDatabase.Replace.NONE)
class AmbulancePersistenceTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(
          "postgres:17-alpine")
          .withDatabaseName(
              "prehospital_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configure(
      DynamicPropertyRegistry registry) {

    registry.add(
        "spring.datasource.url",
        POSTGRES::getJdbcUrl);

    registry.add(
        "spring.datasource.username",
        POSTGRES::getUsername);

    registry.add(
        "spring.datasource.password",
        POSTGRES::getPassword);
  }

  @Autowired
  AmbulanceRepository ambulanceRepository;

  @Autowired
  AmbulanceCoverageRepository
      coverageRepository;

  @BeforeEach
  void cleanDatabase() {

    coverageRepository.deleteAll();
    ambulanceRepository.deleteAll();
  }

  @Test
  void persistsAmbulance() {

    Ambulance saved =
        ambulanceRepository.saveAndFlush(
            new Ambulance(
                "SAMU-USA-10",
                "USA 10"));

    Ambulance loaded =
        ambulanceRepository
            .findById(saved.getId())
            .orElseThrow();

    assertEquals(
        "USA 10",
        loaded.getDisplayName());

    assertEquals(
        AmbulanceOperationalStatus.ACTIVE,
        loaded.getOperationalStatus());
  }

  @Test
  void onlyOneActiveCoveragePerAmbulance() {

    Ambulance ambulance =
        ambulanceRepository.saveAndFlush(
            new Ambulance(
                "SAMU-USB-11",
                "USB 11"));

    coverageRepository.saveAndFlush(
        new AmbulanceCoverage(
            ambulance.getId(),
            UUID.randomUUID()));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            coverageRepository.saveAndFlush(
                new AmbulanceCoverage(
                    ambulance.getId(),
                    UUID.randomUUID())));
  }

  @Test
  void closedCoverageAllowsNewCoverage() {

    Ambulance ambulance =
        ambulanceRepository.saveAndFlush(
            new Ambulance(
                "SAMU-USA-12",
                "USA 12"));

    AmbulanceCoverage first =
        coverageRepository.saveAndFlush(
            new AmbulanceCoverage(
                ambulance.getId(),
                UUID.randomUUID()));

    first.close();

    coverageRepository.saveAndFlush(first);

    AmbulanceCoverage second =
        coverageRepository.saveAndFlush(
            new AmbulanceCoverage(
                ambulance.getId(),
                UUID.randomUUID()));

    assertFalse(first.isActive());
    assertTrue(second.isActive());
  }
}

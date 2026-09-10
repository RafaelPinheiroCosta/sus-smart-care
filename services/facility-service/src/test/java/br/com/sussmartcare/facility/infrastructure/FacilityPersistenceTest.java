package br.com.sussmartcare.facility.infrastructure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.sussmartcare.facility.domain.Bed;
import br.com.sussmartcare.facility.domain.BedOccupation;
import br.com.sussmartcare.facility.domain.BedType;
import br.com.sussmartcare.facility.domain.CareZone;
import br.com.sussmartcare.facility.domain.CareZoneType;
import br.com.sussmartcare.facility.domain.FacilityType;
import br.com.sussmartcare.facility.domain.HealthFacility;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class FacilityPersistenceTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("facility_test")
          .withUsername("sus")
          .withPassword("sus");

  @DynamicPropertySource
  static void databaseProperties(
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

    registry.add(
        "spring.jpa.hibernate.ddl-auto",
        () -> "validate");
  }

  @Autowired
  JpaHealthFacilityRepository facilityRepository;

  @Autowired
  JpaCareZoneRepository zoneRepository;

  @Autowired
  JpaBedRepository bedRepository;

  @Autowired
  JpaBedOccupationRepository occupationRepository;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Test
  void flywayV1IsApplied() {

    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
              from flyway_schema_history
             where version = '1'
               and success = true
            """,
            Integer.class);

    assertEquals(1, count);
  }

  @Test
  void oneBedCannotHaveTwoActiveOccupations() {

    Bed bed = createBed();

    BedOccupation first =
        new BedOccupation(
            bed.getId(),
            UUID.randomUUID());

    occupationRepository.saveAndFlush(first);

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                """
                insert into bed_occupations
                    (id, bed_id, visit_id, started_at, ended_at)
                values
                    (?, ?, ?, now(), null)
                """,
                UUID.randomUUID(),
                bed.getId(),
                UUID.randomUUID()));
  }

  @Test
  void oneVisitCannotOccupyTwoBedsAtTheSameTime() {

    TestBeds testBeds = createTwoBeds();

    UUID visitId = UUID.randomUUID();

    BedOccupation first =
        new BedOccupation(
            testBeds.first().getId(),
            visitId);

    occupationRepository.saveAndFlush(first);

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                """
                insert into bed_occupations
                    (id, bed_id, visit_id, started_at, ended_at)
                values
                    (?, ?, ?, now(), null)
                """,
                UUID.randomUUID(),
                testBeds.second().getId(),
                visitId));
  }

  @Test
  void closedOccupationAllowsReuse() {

    Bed bed = createBed();
    UUID visitId = UUID.randomUUID();

    BedOccupation first =
        occupationRepository.saveAndFlush(
            new BedOccupation(
                bed.getId(),
                visitId));

    first.close();

    occupationRepository.saveAndFlush(first);

    assertDoesNotThrow(
        () ->
            occupationRepository.saveAndFlush(
                new BedOccupation(
                    bed.getId(),
                    visitId)));
  }

  private Bed createBed() {

    String suffix =
        UUID.randomUUID()
            .toString()
            .substring(0, 8);

    HealthFacility facility =
        facilityRepository.saveAndFlush(
            new HealthFacility(
                "FAC-" + suffix,
                "Facility " + suffix,
                FacilityType.UPA));

    CareZone zone =
        zoneRepository.saveAndFlush(
            new CareZone(
                facility.getId(),
                "ZONE-" + suffix,
                "Observation " + suffix,
                CareZoneType.OBSERVATION));

    return bedRepository.saveAndFlush(
        new Bed(
            zone.getId(),
            "BED-" + suffix,
            BedType.BED));
  }

  private TestBeds createTwoBeds() {

    String suffix =
        UUID.randomUUID()
            .toString()
            .substring(0, 8);

    HealthFacility facility =
        facilityRepository.saveAndFlush(
            new HealthFacility(
                "FAC-" + suffix,
                "Facility " + suffix,
                FacilityType.HOSPITAL));

    CareZone zone =
        zoneRepository.saveAndFlush(
            new CareZone(
                facility.getId(),
                "ZONE-" + suffix,
                "Emergency " + suffix,
                CareZoneType.EMERGENCY));

    Bed first =
        bedRepository.saveAndFlush(
            new Bed(
                zone.getId(),
                "BED-A-" + suffix,
                BedType.BED));

    Bed second =
        bedRepository.saveAndFlush(
            new Bed(
                zone.getId(),
                "BED-B-" + suffix,
                BedType.STRETCHER));

    return new TestBeds(first, second);
  }

  private record TestBeds(
      Bed first,
      Bed second) {
  }
}

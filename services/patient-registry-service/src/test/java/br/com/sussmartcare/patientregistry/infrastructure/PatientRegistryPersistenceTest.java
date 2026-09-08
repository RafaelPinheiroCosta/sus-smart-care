package br.com.sussmartcare.patientregistry.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.sussmartcare.patientregistry.domain.IdentifierType;
import br.com.sussmartcare.patientregistry.domain.IdentityStatus;
import br.com.sussmartcare.patientregistry.domain.Patient;
import br.com.sussmartcare.patientregistry.domain.PatientIdentifier;
import br.com.sussmartcare.patientregistry.domain.RelationshipType;
import br.com.sussmartcare.patientregistry.domain.RepresentativeRelationship;
import java.time.LocalDate;
import java.util.UUID;
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
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PatientRegistryPersistenceTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("patient_registry_test")
          .withUsername("sus")
          .withPassword("sus");

  @DynamicPropertySource
  static void database(
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

    registry.add(
        "spring.flyway.enabled",
        () -> "true");
  }

  @Autowired
  JpaPatientRepository patients;

  @Autowired
  JpaPatientIdentifierRepository identifiers;

  @Autowired
  JpaRepresentativeRelationshipRepository representatives;

  @Test
  void persistsCanonicalPatientAndEnforcesIdentifierUniqueness() {

    Patient first =
        patients.saveAndFlush(
            new Patient(
                UUID.randomUUID(),
                "Paciente Integracao",
                LocalDate.of(1990, 1, 2),
                IdentityStatus.CONFIRMED));

    identifiers.saveAndFlush(
        new PatientIdentifier(
            first.getId(),
            IdentifierType.CPF,
            "12345678900"));

    Patient second =
        patients.saveAndFlush(
            new Patient(
                UUID.randomUUID(),
                "Outro Paciente",
                LocalDate.of(1991, 2, 3),
                IdentityStatus.CONFIRMED));

    assertThat(
        identifiers.findByPatientId(
            first.getId()))
        .hasSize(1);

    assertThatThrownBy(
        () ->
            identifiers.saveAndFlush(
                new PatientIdentifier(
                    second.getId(),
                    IdentifierType.CPF,
                    "12345678900")))
        .isInstanceOf(
            DataIntegrityViolationException.class);
  }

  @Test
  void databaseAllowsOnlyOneActiveSelfPerPatient() {

    UUID patientId =
        UUID.randomUUID();

    representatives.saveAndFlush(
        new RepresentativeRelationship(
            patientId,
            UUID.randomUUID(),
            RelationshipType.SELF));

    assertThatThrownBy(
        () ->
            representatives.saveAndFlush(
                new RepresentativeRelationship(
                    patientId,
                    UUID.randomUUID(),
                    RelationshipType.SELF)))
        .isInstanceOf(
            DataIntegrityViolationException.class);
  }

  @Test
  void databaseAllowsOnlyOneActiveSelfPerUser() {

    UUID userId =
        UUID.randomUUID();

    representatives.saveAndFlush(
        new RepresentativeRelationship(
            UUID.randomUUID(),
            userId,
            RelationshipType.SELF));

    assertThatThrownBy(
        () ->
            representatives.saveAndFlush(
                new RepresentativeRelationship(
                    UUID.randomUUID(),
                    userId,
                    RelationshipType.SELF)))
        .isInstanceOf(
            DataIntegrityViolationException.class);
  }
}
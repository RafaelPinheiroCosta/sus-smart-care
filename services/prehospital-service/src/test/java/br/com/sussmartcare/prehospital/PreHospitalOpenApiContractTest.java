package br.com.sussmartcare.prehospital;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PreHospitalOpenApiContractTest {

  @Test
  void contractContainsStageTwoOperations()
      throws Exception {

    String yaml =
        Files.readString(
            locateContract());

    assertTrue(
        yaml.contains(
            "version: 0.5.0"));

    assertTrue(
        yaml.contains(
            "operationId: createAmbulance"));

    assertTrue(
        yaml.contains(
            "operationId: assignAmbulanceCoverage"));

    assertTrue(
        yaml.contains(
            "operationId: createPreHospitalEncounter"));

    assertTrue(
        yaml.contains(
            "operationId: updatePreHospitalEta"));

    assertTrue(
        yaml.contains(
            "operationId: updatePreHospitalRisk"));

    assertTrue(
        yaml.contains(
            "operationId: markPreHospitalEncounterArrived"));

    assertTrue(
        yaml.contains(
            "operationId: cancelPreHospitalEncounter"));

    assertTrue(
        yaml.contains(
            "EN_ROUTE"));

    assertTrue(
        yaml.contains(
            "ARRIVED"));

    assertTrue(
        yaml.contains(
            "CANCELLED"));
  }

  private Path locateContract() {

    Path cwd =
        Path.of("")
            .toAbsolutePath()
            .normalize();

    Path fromRoot =
        cwd.resolve(
            "contracts/openapi/prehospital.yaml");

    if (Files.exists(fromRoot)) {
      return fromRoot;
    }

    Path fromModule =
        cwd.resolve(
            "../../contracts/openapi/prehospital.yaml")
            .normalize();

    assertTrue(
        Files.exists(fromModule),
        "Prehospital OpenAPI contract not found");

    return fromModule;
  }
}

package br.com.sussmartcare.telemetry;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class TelemetryOpenApiContractTest {

  @Test
  void stageThreeContractIsPresent() throws Exception {

    Path cwd = Path.of("").toAbsolutePath().normalize();

    Path contract =
        cwd.resolve("contracts/openapi/telemetry.yaml");

    if (!Files.exists(contract)) {
      contract =
          cwd.resolve("../../contracts/openapi/telemetry.yaml")
              .normalize();
    }

    assertTrue(Files.exists(contract));

    String yaml = Files.readString(contract);

    assertTrue(yaml.contains("version: 0.5.0"));
    assertTrue(yaml.contains("operationId: placeDevice"));
    assertTrue(yaml.contains("operationId: assignDeviceToTelemetrySession"));
    assertTrue(yaml.contains("operationId: endDeviceAssignment"));
    assertTrue(yaml.contains("operationId: ingestObservation"));
    assertTrue(yaml.contains("CARE_ZONE"));
    assertTrue(yaml.contains("AMBULANCE"));
    assertTrue(yaml.contains("REVOKED"));
  }
}

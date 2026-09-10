package br.com.sussmartcare.telemetry;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TelemetryMqttContractTest {

  @Test
  void asyncApiContainsStageFourContract()
      throws Exception {

    Path cwd =
        Path.of("")
            .toAbsolutePath()
            .normalize();

    Path contract =
        cwd.resolve(
            "contracts/asyncapi/telemetry-mqtt.yaml");

    if (!Files.exists(contract)) {

      contract =
          cwd.resolve(
              "../../contracts/asyncapi/telemetry-mqtt.yaml")
              .normalize();
    }

    assertTrue(
        Files.exists(contract));

    String yaml =
        Files.readString(contract);

    assertTrue(
        yaml.contains(
            "sus/v1/devices/{deviceExternalId}/telemetry"));

    assertTrue(
        yaml.contains(
            "sus/v1/devices/{deviceExternalId}/ack"));

    assertTrue(
        yaml.contains(
            "messageId"));

    assertTrue(
        yaml.contains(
            "sequence"));

    assertTrue(
        yaml.contains(
            "ACCEPTED"));

    assertTrue(
        yaml.contains(
            "DUPLICATE"));

    assertTrue(
        yaml.contains(
            "REJECTED"));
  }
}

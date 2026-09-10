package br.com.sussmartcare.telemetry.adapters.in.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MqttTelemetryPayloadParser {

  private static final Set<String> FORBIDDEN_FIELDS =
      Set.of(
          "patientId",
          "patient_id",
          "visitId",
          "visit_id",
          "sessionId",
          "session_id",
          "cpf",
          "cns");

  private final ObjectMapper objectMapper;

  public MqttTelemetryPayloadParser(
      ObjectMapper objectMapper) {

    this.objectMapper = objectMapper;
  }

  public Payload parse(
      byte[] bytes) {

    try {

      JsonNode root =
          objectMapper.readTree(bytes);

      if (root == null ||
          !root.isObject()) {

        throw new IllegalArgumentException(
            "MQTT payload must be a JSON object");
      }

      for (String forbidden :
          FORBIDDEN_FIELDS) {

        if (root.has(forbidden)) {

          throw new IllegalArgumentException(
              "Patient or session identifiers are forbidden in MQTT payload");
        }
      }

      UUID messageId =
          UUID.fromString(
              requiredText(
                  root,
                  "messageId"));

      JsonNode sequenceNode =
          root.get("sequence");

      if (sequenceNode == null ||
          !sequenceNode.canConvertToLong()) {

        throw new IllegalArgumentException(
            "sequence is required");
      }

      long sequence =
          sequenceNode.longValue();

      if (sequence < 0) {

        throw new IllegalArgumentException(
            "sequence must be non-negative");
      }

      String type =
          requiredText(
              root,
              "type");

      JsonNode valueNode =
          root.get("value");

      if (valueNode == null ||
          !valueNode.isNumber()) {

        throw new IllegalArgumentException(
            "value must be numeric");
      }

      Double value =
          valueNode.doubleValue();

      String unit =
          optionalText(
              root,
              "unit");

      Instant measuredAt =
          parseInstant(
              optionalText(
                  root,
                  "measuredAt"));

      return new Payload(
          messageId,
          sequence,
          type,
          value,
          unit,
          measuredAt);
    }
    catch (IllegalArgumentException e) {
      throw e;
    }
    catch (Exception e) {

      throw new IllegalArgumentException(
          "Invalid MQTT telemetry payload",
          e);
    }
  }

  private String requiredText(
      JsonNode root,
      String field) {

    String value =
        optionalText(
            root,
            field);

    if (value == null ||
        value.isBlank()) {

      throw new IllegalArgumentException(
          field + " is required");
    }

    return value.trim();
  }

  private String optionalText(
      JsonNode root,
      String field) {

    JsonNode node =
        root.get(field);

    if (node == null ||
        node.isNull()) {

      return null;
    }

    if (!node.isTextual()) {

      throw new IllegalArgumentException(
          field + " must be text");
    }

    return node.textValue();
  }

  private Instant parseInstant(
      String value) {

    if (value == null ||
        value.isBlank()) {

      return Instant.now();
    }

    try {
      return Instant.parse(value);
    }
    catch (DateTimeParseException e) {

      throw new IllegalArgumentException(
          "measuredAt must use ISO-8601 UTC format",
          e);
    }
  }

  public record Payload(
      UUID messageId,
      long sequence,
      String type,
      Double value,
      String unit,
      Instant measuredAt) {
  }
}

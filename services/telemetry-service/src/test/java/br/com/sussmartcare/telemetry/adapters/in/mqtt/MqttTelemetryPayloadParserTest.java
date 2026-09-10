package br.com.sussmartcare.telemetry.adapters.in.mqtt;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MqttTelemetryPayloadParserTest {

  private final MqttTelemetryPayloadParser parser =
      new MqttTelemetryPayloadParser(
          new ObjectMapper());

  @Test
  void validPayloadIsParsed() {

    var payload =
        parser.parse(
            """
            {
              "messageId":"5b870242-e57a-48ec-a31f-e7451194bca3",
              "sequence":10,
              "type":"HEART_RATE",
              "value":82,
              "unit":"bpm",
              "measuredAt":"2026-09-10T03:00:00Z"
            }
            """.getBytes(StandardCharsets.UTF_8));

    assertEquals(
        10,
        payload.sequence());

    assertEquals(
        "HEART_RATE",
        payload.type());

    assertEquals(
        82.0,
        payload.value());
  }

  @Test
  void patientIdentifierIsRejected() {

    assertThrows(
        IllegalArgumentException.class,
        () ->
            parser.parse(
                """
                {
                  "messageId":"5b870242-e57a-48ec-a31f-e7451194bca3",
                  "sequence":11,
                  "type":"HEART_RATE",
                  "value":82,
                  "patientId":"2981cd4c-2052-4590-b3e2-d4c54bcd6dd3"
                }
                """.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void negativeSequenceIsRejected() {

    assertThrows(
        IllegalArgumentException.class,
        () ->
            parser.parse(
                """
                {
                  "messageId":"5b870242-e57a-48ec-a31f-e7451194bca3",
                  "sequence":-1,
                  "type":"HEART_RATE",
                  "value":82
                }
                """.getBytes(StandardCharsets.UTF_8)));
  }
}

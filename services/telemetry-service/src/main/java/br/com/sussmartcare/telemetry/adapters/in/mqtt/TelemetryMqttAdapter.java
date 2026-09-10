package br.com.sussmartcare.telemetry.adapters.in.mqtt;

import br.com.sussmartcare.telemetry.application.MqttIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "mqtt.enabled",
    havingValue = "true")
public class TelemetryMqttAdapter {

  private static final Logger log =
      LoggerFactory.getLogger(
          TelemetryMqttAdapter.class);

  private static final Pattern TOPIC =
      Pattern.compile(
          "^sus/v1/devices/([^/]+)/telemetry$");

  private final MqttIngestionService ingestion;
  private final MqttTelemetryPayloadParser parser;
  private final ObjectMapper objectMapper;

  private final String brokerUri;
  private final String username;
  private final String password;
  private final String clientId;

  private final ExecutorService connector =
      Executors.newSingleThreadExecutor();

  private volatile MqttAsyncClient client;
  private volatile boolean stopping;

  public TelemetryMqttAdapter(
      MqttIngestionService ingestion,
      MqttTelemetryPayloadParser parser,
      ObjectMapper objectMapper,
      @Value("${mqtt.broker-uri}") String brokerUri,
      @Value("${mqtt.username}") String username,
      @Value("${mqtt.password}") String password,
      @Value("${mqtt.client-id:telemetry-service}") String clientId) {

    this.ingestion = ingestion;
    this.parser = parser;
    this.objectMapper = objectMapper;
    this.brokerUri = brokerUri;
    this.username = username;
    this.password = password;
    this.clientId = clientId;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start() {

    connector.submit(
        this::connectLoop);
  }

  private void connectLoop() {

    while (!stopping) {

      try {

        if (client == null) {

          client =
              new MqttAsyncClient(
                  brokerUri,
                  clientId,
                  new MemoryPersistence());

          client.setCallback(
              callback());
        }

        if (!client.isConnected()) {

          MqttConnectOptions options =
              new MqttConnectOptions();

          options.setAutomaticReconnect(true);
          options.setCleanSession(true);
          options.setConnectionTimeout(10);
          options.setKeepAliveInterval(30);
          options.setUserName(username);
          options.setPassword(
              password.toCharArray());

          client.connect(options)
              .waitForCompletion(
                  15000);
        }

        if (client.isConnected()) {

          client.subscribe(
              "sus/v1/devices/+/telemetry",
              1)
              .waitForCompletion(
                  10000);

          log.info(
              "MQTT telemetry adapter connected to {}",
              brokerUri);

          return;
        }
      }
      catch (Exception e) {

        log.warn(
            "MQTT connection unavailable, retrying: {}",
            e.getMessage(),
            e);
      }

      try {
        Thread.sleep(5000);
      }
      catch (InterruptedException e) {

        Thread.currentThread()
            .interrupt();

        return;
      }
    }
  }

  private MqttCallbackExtended callback() {

    return new MqttCallbackExtended() {

      @Override
      public void connectComplete(
          boolean reconnect,
          String serverURI) {

        try {

          if (client != null &&
              client.isConnected()) {

            client.subscribe(
                "sus/v1/devices/+/telemetry",
                1);
          }
        }
        catch (Exception e) {

          log.error(
              "MQTT resubscribe failed",
              e);
        }
      }

      @Override
      public void connectionLost(
          Throwable cause) {

        log.warn(
            "MQTT connection lost: {}",
            cause == null
                ? "unknown"
                : cause.getMessage());
      }

      @Override
      public void messageArrived(
          String topic,
          MqttMessage message) {

        handle(
            topic,
            message);
      }

      @Override
      public void deliveryComplete(
          IMqttDeliveryToken token) {
      }
    };
  }

  private void handle(
      String topic,
      MqttMessage message) {

    Matcher matcher =
        TOPIC.matcher(topic);

    if (!matcher.matches()) {

      log.warn(
          "Rejected unexpected MQTT topic: {}",
          topic);

      return;
    }

    String externalId =
        matcher.group(1);

    UUID messageId = null;
    long sequence = -1;

    try {

      MqttTelemetryPayloadParser.Payload payload =
          parser.parse(
              message.getPayload());

      messageId =
          payload.messageId();

      sequence =
          payload.sequence();

      MqttIngestionService.Result result =
          ingestion.ingest(
              externalId,
              payload);

      publishAck(
          externalId,
          new Ack(
              result.messageId(),
              result.sequence(),
              result.status().name(),
              result.receivedAt(),
              null));
    }
    catch (Exception e) {

      log.warn(
          "Rejected MQTT telemetry from {}: {}",
          externalId,
          e.getMessage());

      publishAck(
          externalId,
          new Ack(
              messageId,
              sequence,
              "REJECTED",
              Instant.now(),
              safeReason(
                  e.getMessage())));
    }
  }

  private void publishAck(
      String externalId,
      Ack ack) {

    try {

      if (client == null ||
          !client.isConnected()) {

        return;
      }

      byte[] payload =
          objectMapper.writeValueAsBytes(
              ack);

      client.publish(
          "sus/v1/devices/" +
              externalId +
              "/ack",
          payload,
          1,
          false);
    }
    catch (Exception e) {

      log.error(
          "MQTT acknowledgement publication failed",
          e);
    }
  }

  private String safeReason(
      String reason) {

    if (reason == null ||
        reason.isBlank()) {

      return "Rejected telemetry message";
    }

    if (reason.length() <= 200) {
      return reason;
    }

    return reason.substring(
        0,
        200);
  }

  @PreDestroy
  public void stop() {

    stopping = true;

    connector.shutdownNow();

    try {

      if (client != null &&
          client.isConnected()) {

        client.disconnect()
            .waitForCompletion(
                5000);
      }

      if (client != null) {
        client.close();
      }
    }
    catch (Exception e) {

      log.debug(
          "MQTT shutdown failure",
          e);
    }
  }

  public record Ack(
      UUID messageId,
      long sequence,
      String status,
      Instant receivedAt,
      String reason) {
  }
}

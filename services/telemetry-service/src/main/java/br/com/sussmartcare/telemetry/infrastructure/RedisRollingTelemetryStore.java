package br.com.sussmartcare.telemetry.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRollingTelemetryStore {

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final int rollingSize;
  private final long ttlMinutes;

  public RedisRollingTelemetryStore(
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      @Value("${TELEMETRY_ROLLING_SIZE:120}")
      int rollingSize,
      @Value("${TELEMETRY_ROLLING_TTL_MINUTES:120}")
      long ttlMinutes) {

    this.redis = redis;
    this.objectMapper = objectMapper;
    this.rollingSize = rollingSize;
    this.ttlMinutes = ttlMinutes;
  }

  public void append(
      UUID sessionId,
      String type,
      UUID deviceId,
      double value,
      String unit,
      Instant measuredAt) {

    String key =
        key(
            sessionId,
            type);

    try {

      String json =
          objectMapper.writeValueAsString(
              new RollingSample(
                  deviceId,
                  value,
                  unit,
                  measuredAt));

      redis.opsForList()
          .leftPush(
              key,
              json);

      redis.opsForList()
          .trim(
              key,
              0,
              rollingSize - 1L);

      redis.expire(
          key,
          Duration.ofMinutes(
              ttlMinutes));
    }
    catch (Exception e) {

      throw new IllegalStateException(
          "Could not store rolling telemetry",
          e);
    }
  }

  public Long size(
      UUID sessionId,
      String type) {

    return redis.opsForList()
        .size(
            key(
                sessionId,
                type));
  }

  private String key(
      UUID sessionId,
      String type) {

    return "telemetry:rolling:" +
        sessionId +
        ":" +
        type;
  }

  public record RollingSample(
      UUID deviceId,
      double value,
      String unit,
      Instant measuredAt) {
  }
}

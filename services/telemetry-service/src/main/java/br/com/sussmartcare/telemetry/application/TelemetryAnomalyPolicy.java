package br.com.sussmartcare.telemetry.application;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TelemetryAnomalyPolicy {

  private static final Map<String, Range> RANGES =
      Map.of(
          "HEART_RATE",
          new Range(40.0, 130.0),

          "SPO2",
          new Range(90.0, null),

          "TEMPERATURE",
          new Range(35.0, 39.0),

          "SYSTOLIC_BP",
          new Range(80.0, 180.0),

          "DIASTOLIC_BP",
          new Range(40.0, 120.0),

          "RESPIRATORY_RATE",
          new Range(8.0, 30.0));

  public Optional<String> evaluate(
      String type,
      double value) {

    Range range =
        RANGES.get(type);

    if (range == null) {
      return Optional.empty();
    }

    if (range.minimum != null &&
        value < range.minimum) {

      return Optional.of(
          "Value " +
          value +
          " below configured minimum " +
          range.minimum);
    }

    if (range.maximum != null &&
        value > range.maximum) {

      return Optional.of(
          "Value " +
          value +
          " above configured maximum " +
          range.maximum);
    }

    return Optional.empty();
  }

  private record Range(
      Double minimum,
      Double maximum) {
  }
}

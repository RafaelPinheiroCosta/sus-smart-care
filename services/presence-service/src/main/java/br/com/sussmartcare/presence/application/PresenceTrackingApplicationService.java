package br.com.sussmartcare.presence.application;

import br.com.sussmartcare.presence.domain.*;
import br.com.sussmartcare.presence.infrastructure.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresenceTrackingApplicationService {

  private final PresenceTrackingSessionRepository sessions;
  private final PresenceGatewayRepository gateways;
  private final PresenceSignalRepository signals;
  private final VisitAccessPort visitAccess;
  private final PresenceApplicationService presence;
  private final int bottleneckThreshold;
  private final SecureRandom random =
      new SecureRandom();

  public PresenceTrackingApplicationService(
      PresenceTrackingSessionRepository sessions,
      PresenceGatewayRepository gateways,
      PresenceSignalRepository signals,
      VisitAccessPort visitAccess,
      PresenceApplicationService presence,
      @Value("${PRESENCE_BOTTLENECK_THRESHOLD:3}")
      int bottleneckThreshold) {

    this.sessions = sessions;
    this.gateways = gateways;
    this.signals = signals;
    this.visitAccess = visitAccess;
    this.presence = presence;
    this.bottleneckThreshold =
        Math.max(
            1,
            bottleneckThreshold);
  }

  @Transactional
  public PresenceGateway registerGateway(
      String externalId,
      PresenceSourceType sourceType,
      UUID facilityId,
      String zoneId) {

    String normalized =
        requireText(
            externalId,
            "Gateway external id is required");

    if (
        gateways
            .findByExternalId(normalized)
            .isPresent()
    ) {

      throw new PresenceConflictException(
          "Presence gateway already exists");
    }

    return gateways.save(
        new PresenceGateway(
            normalized,
            sourceType,
            facilityId,
            zoneId));
  }

  @Transactional(readOnly = true)
  public List<PresenceGateway> gateways() {

    return gateways
        .findAllByOrderByExternalIdAsc();
  }

  @Transactional
  public StartedSession start(
      UUID visitId,
      UUID patientId,
      UUID facilityId) {

    UUID visitPatient =
        visitAccess
            .assertCurrentActorCanAccess(
                visitId);

    if (!visitPatient.equals(patientId)) {

      throw new IllegalArgumentException(
          "patientId does not belong to visit");
    }

    if (
        sessions
            .findByVisitIdAndEndedAtIsNull(
                visitId)
            .isPresent()
    ) {

      throw new PresenceConflictException(
          "Visit already has an active tracking session");
    }

    String token =
        newToken();

    PresenceTrackingSession session =
        sessions.save(
            new PresenceTrackingSession(
                visitId,
                patientId,
                facilityId,
                hash(token)));

    return new StartedSession(
        session,
        token);
  }

  @Transactional(readOnly = true)
  public PresenceTrackingSession get(
      UUID sessionId) {

    PresenceTrackingSession session =
        requireSession(
            sessionId);

    visitAccess
        .assertCurrentActorCanAccess(
            session.getVisitId());

    return session;
  }

  @Transactional
  public PresenceTrackingSession end(
      UUID sessionId) {

    PresenceTrackingSession session =
        requireSession(
            sessionId);

    visitAccess
        .assertCurrentActorCanAccess(
            session.getVisitId());

    if (session.isInsideFacility()) {

      presence.record(
          session.getVisitId(),
          session.getPatientId(),
          "LEFT_FACILITY",
          null,
          "SYSTEM",
          Instant.now());
    }

    session.end();

    return sessions.save(
        session);
  }

  @Transactional
  public SignalResult signal(
      UUID signalId,
      String trackingToken,
      PresenceSourceType sourceType,
      PresenceState state,
      String zoneId,
      String gatewayExternalId,
      Instant occurredAt) {

    if (signalId == null) {
      throw new IllegalArgumentException(
          "signalId is required");
    }

    String token =
        requireText(
            trackingToken,
            "Tracking token is required");

    PresenceTrackingSession session =
        sessions
            .findByTrackingTokenHash(
                hash(token))
            .orElseThrow(
                () ->
                    new PresenceNotFoundException(
                        "Tracking session not found"));

    if (!session.isActive()) {

      throw new PresenceConflictException(
          "Tracking session is closed");
    }

    if (signals.existsById(signalId)) {

      return result(
          SignalStatus.DUPLICATE,
          signalId,
          session,
          false);
    }

    String normalizedGateway =
        normalizeGateway(
            gatewayExternalId);

    String normalizedZone =
        normalizeZone(
            state,
            zoneId);

    validateSource(
        session,
        sourceType,
        state,
        normalizedZone,
        normalizedGateway);

    Instant actualOccurredAt =
        occurredAt == null
            ? Instant.now()
            : occurredAt;

    PresenceTrackingSession.Transition transition;

    try {

      transition =
          session.apply(
              state,
              normalizedZone,
              sourceType,
              normalizedGateway,
              actualOccurredAt);
    }
    catch (IllegalStateException exception) {

      throw new PresenceConflictException(
          exception.getMessage());
    }

    signals.save(
        new PresenceSignal(
            signalId,
            session.getId(),
            sourceType,
            state,
            normalizedZone,
            normalizedGateway,
            actualOccurredAt));

    sessions.save(
        session);

    if (transition.changed()) {

      String eventType =
          eventType(
              transition);

      presence.record(
          session.getVisitId(),
          session.getPatientId(),
          eventType,
          transition.zoneId(),
          sourceType.name(),
          transition.occurredAt());
    }

    return result(
        SignalStatus.ACCEPTED,
        signalId,
        session,
        transition.changed());
  }

  @Transactional(readOnly = true)
  public List<ZoneLoad> zoneLoads() {

    Map<ZoneKey,List<PresenceTrackingSession>>
        grouped =
            sessions
                .findByEndedAtIsNullAndInsideFacilityTrue()
                .stream()
                .filter(
                    session ->
                        session.getCurrentZoneId() != null)
                .collect(
                    Collectors.groupingBy(
                        session ->
                            new ZoneKey(
                                session.getFacilityId(),
                                session.getCurrentZoneId())));

    return grouped
        .entrySet()
        .stream()
        .map(
            entry -> {

              int active =
                  entry.getValue()
                      .size();

              Instant oldest =
                  entry.getValue()
                      .stream()
                      .map(
                          PresenceTrackingSession::
                              getZoneEnteredAt)
                      .filter(Objects::nonNull)
                      .min(Instant::compareTo)
                      .orElse(null);

              return new ZoneLoad(
                  entry.getKey().facilityId(),
                  entry.getKey().zoneId(),
                  active,
                  oldest,
                  active >=
                      bottleneckThreshold);
            })
        .sorted(
            Comparator
                .comparing(
                    ZoneLoad::facilityId)
                .thenComparing(
                    ZoneLoad::zoneId))
        .toList();
  }

  private void validateSource(
      PresenceTrackingSession session,
      PresenceSourceType sourceType,
      PresenceState state,
      String zoneId,
      String gatewayExternalId) {

    if (sourceType == null ||
        state == null) {

      throw new IllegalArgumentException(
          "Source type and presence state are required");
    }

    if (sourceType.requiresGateway()) {

      if (gatewayExternalId == null) {

        throw new IllegalArgumentException(
            "Physical presence source requires gatewayExternalId");
      }

      PresenceGateway gateway =
          gateways
              .findByExternalId(
                  gatewayExternalId)
              .orElseThrow(
                  () ->
                      new PresenceNotFoundException(
                          "Presence gateway not found"));

      if (!gateway.isActive()) {

        throw new PresenceConflictException(
            "Presence gateway is inactive");
      }

      if (gateway.getSourceType() !=
          sourceType) {

        throw new IllegalArgumentException(
            "Gateway technology does not match sourceType");
      }

      if (!gateway
          .getFacilityId()
          .equals(
              session.getFacilityId())) {

        throw new IllegalArgumentException(
            "Gateway does not belong to tracking facility");
      }

      if (
          state == PresenceState.INSIDE &&
          !gateway
              .getZoneId()
              .equals(zoneId)
      ) {

        throw new IllegalArgumentException(
            "Signal zone does not match gateway zone");
      }

      return;
    }

    if (gatewayExternalId != null) {

      throw new IllegalArgumentException(
          "MANUAL and SYSTEM sources must not use a gateway");
    }
  }

  private String eventType(
      PresenceTrackingSession.Transition transition) {

    if (
        transition.previousState() ==
            PresenceState.OUTSIDE &&
        transition.state() ==
            PresenceState.INSIDE
    ) {

      return "ENTERED_FACILITY";
    }

    if (
        transition.previousState() ==
            PresenceState.INSIDE &&
        transition.state() ==
            PresenceState.OUTSIDE
    ) {

      return "LEFT_FACILITY";
    }

    return "ENTERED_ZONE";
  }

  private PresenceTrackingSession requireSession(
      UUID id) {

    return sessions
        .findById(id)
        .orElseThrow(
            () ->
                new PresenceNotFoundException(
                    "Tracking session not found"));
  }

  private String normalizeZone(
      PresenceState state,
      String zoneId) {

    if (state == PresenceState.OUTSIDE) {
      return null;
    }

    return requireText(
        zoneId,
        "Zone is required while inside");
  }

  private String normalizeGateway(
      String gatewayExternalId) {

    if (gatewayExternalId == null ||
        gatewayExternalId.trim().isEmpty()) {

      return null;
    }

    return gatewayExternalId.trim();
  }

  private String requireText(
      String value,
      String message) {

    if (value == null ||
        value.trim().isEmpty()) {

      throw new IllegalArgumentException(
          message);
    }

    return value.trim();
  }

  private String newToken() {

    byte[] bytes =
        new byte[32];

    random.nextBytes(
        bytes);

    return Base64
        .getUrlEncoder()
        .withoutPadding()
        .encodeToString(
            bytes);
  }

  private String hash(
      String value) {

    try {

      byte[] digest =
          MessageDigest
              .getInstance("SHA-256")
              .digest(
                  value.getBytes(
                      StandardCharsets.UTF_8));

      return java.util.HexFormat
          .of()
          .formatHex(
              digest);
    }
    catch (Exception exception) {

      throw new IllegalStateException(
          "Could not hash tracking token",
          exception);
    }
  }

  private SignalResult result(
      SignalStatus status,
      UUID signalId,
      PresenceTrackingSession session,
      boolean changed) {

    return new SignalResult(
        status,
        signalId,
        session.getId(),
        session.isInsideFacility(),
        session.getCurrentZoneId(),
        changed,
        session.getLastSignalAt());
  }

  public enum SignalStatus {
    ACCEPTED,
    DUPLICATE
  }

  public record StartedSession(
      PresenceTrackingSession session,
      String trackingToken) {
  }

  public record SignalResult(
      SignalStatus status,
      UUID signalId,
      UUID trackingSessionId,
      boolean insideFacility,
      String zoneId,
      boolean changed,
      Instant lastSignalAt) {
  }

  public record ZoneLoad(
      UUID facilityId,
      String zoneId,
      int activePatients,
      Instant oldestZoneEntry,
      boolean bottleneck) {
  }

  private record ZoneKey(
      UUID facilityId,
      String zoneId) {
  }
}

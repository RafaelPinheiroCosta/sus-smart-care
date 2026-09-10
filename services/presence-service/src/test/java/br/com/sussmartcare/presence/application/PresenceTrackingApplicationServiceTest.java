package br.com.sussmartcare.presence.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.sussmartcare.presence.domain.*;
import br.com.sussmartcare.presence.infrastructure.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PresenceTrackingApplicationServiceTest {

  PresenceTrackingSessionRepository sessions;
  PresenceGatewayRepository gateways;
  PresenceSignalRepository signals;
  VisitAccessPort access;
  PresenceApplicationService presence;
  PresenceTrackingApplicationService service;

  @BeforeEach
  void setUp() {

    sessions =
        mock(
            PresenceTrackingSessionRepository.class);

    gateways =
        mock(
            PresenceGatewayRepository.class);

    signals =
        mock(
            PresenceSignalRepository.class);

    access =
        mock(
            VisitAccessPort.class);

    presence =
        mock(
            PresenceApplicationService.class);

    service =
        new PresenceTrackingApplicationService(
            sessions,
            gateways,
            signals,
            access,
            presence,
            2);
  }

  @Test
  void startsTrackingOnlyForMatchingVisitPatient() {

    UUID visitId =
        UUID.randomUUID();

    UUID patientId =
        UUID.randomUUID();

    UUID facilityId =
        UUID.randomUUID();

    when(
        access.assertCurrentActorCanAccess(
            visitId))
        .thenReturn(
            patientId);

    when(
        sessions
            .findByVisitIdAndEndedAtIsNull(
                visitId))
        .thenReturn(
            Optional.empty());

    when(
        sessions.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    var started =
        service.start(
            visitId,
            patientId,
            facilityId);

    assertNotNull(
        started.trackingToken());

    assertTrue(
        started.trackingToken()
            .length() >= 40);

    assertEquals(
        visitId,
        started.session()
            .getVisitId());
  }

  @Test
  void physicalSignalUsesRegisteredGateway() {

    String token =
        "stage6-secret";

    UUID facilityId =
        UUID.randomUUID();

    PresenceTrackingSession session =
        new PresenceTrackingSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            facilityId,
            hash(token));

    PresenceGateway gateway =
        new PresenceGateway(
            "BLE-01",
            PresenceSourceType.BLE,
            facilityId,
            "TRIAGE-A");

    when(
        sessions.findByTrackingTokenHash(
            hash(token)))
        .thenReturn(
            Optional.of(session));

    when(
        signals.existsById(any()))
        .thenReturn(false);

    when(
        gateways.findByExternalId(
            "BLE-01"))
        .thenReturn(
            Optional.of(gateway));

    when(
        signals.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    when(
        sessions.save(any()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0));

    var result =
        service.signal(
            UUID.randomUUID(),
            token,
            PresenceSourceType.BLE,
            PresenceState.INSIDE,
            "TRIAGE-A",
            "BLE-01",
            Instant.now());

    assertEquals(
        PresenceTrackingApplicationService
            .SignalStatus.ACCEPTED,
        result.status());

    assertTrue(
        result.insideFacility());

    verify(presence)
        .record(
            eq(session.getVisitId()),
            eq(session.getPatientId()),
            eq("ENTERED_FACILITY"),
            eq("TRIAGE-A"),
            eq("BLE"),
            any());
  }

  @Test
  void repeatedSignalIdIsIdempotent() {

    String token =
        "stage6-secret-2";

    PresenceTrackingSession session =
        new PresenceTrackingSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            hash(token));

    UUID signalId =
        UUID.randomUUID();

    when(
        sessions.findByTrackingTokenHash(
            hash(token)))
        .thenReturn(
            Optional.of(session));

    when(
        signals.existsById(
            signalId))
        .thenReturn(true);

    var result =
        service.signal(
            signalId,
            token,
            PresenceSourceType.MANUAL,
            PresenceState.OUTSIDE,
            null,
            null,
            Instant.now());

    assertEquals(
        PresenceTrackingApplicationService
            .SignalStatus.DUPLICATE,
        result.status());

    verify(
        signals,
        never())
        .save(any());
  }

  @Test
  void detectsOperationalBottleneck() {

    UUID facilityId =
        UUID.randomUUID();

    PresenceTrackingSession first =
        sessionInside(
            facilityId,
            "WAITING-A");

    PresenceTrackingSession second =
        sessionInside(
            facilityId,
            "WAITING-A");

    when(
        sessions
            .findByEndedAtIsNullAndInsideFacilityTrue())
        .thenReturn(
            List.of(
                first,
                second));

    var result =
        service.zoneLoads();

    assertEquals(
        1,
        result.size());

    assertEquals(
        2,
        result.get(0)
            .activePatients());

    assertTrue(
        result.get(0)
            .bottleneck());
  }

  private PresenceTrackingSession sessionInside(
      UUID facilityId,
      String zone) {

    PresenceTrackingSession session =
        new PresenceTrackingSession(
            UUID.randomUUID(),
            UUID.randomUUID(),
            facilityId,
            "b".repeat(64));

    session.apply(
        PresenceState.INSIDE,
        zone,
        PresenceSourceType.SYSTEM,
        null,
        Instant.now());

    return session;
  }

  private String hash(
      String value) {

    try {

      byte[] bytes =
          MessageDigest
              .getInstance("SHA-256")
              .digest(
                  value.getBytes(
                      StandardCharsets.UTF_8));

      return HexFormat
          .of()
          .formatHex(
              bytes);
    }
    catch (Exception exception) {

      throw new RuntimeException(
          exception);
    }
  }
}

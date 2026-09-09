package br.com.sussmartcare.presence.application;

import br.com.sussmartcare.presence.domain.PresenceEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PresenceAccessApplicationService {

  private final PresenceApplicationService presence;
  private final VisitAccessPort visitAccess;

  public PresenceAccessApplicationService(
      PresenceApplicationService presence,
      VisitAccessPort visitAccess) {

    this.presence = presence;
    this.visitAccess = visitAccess;
  }

  public PresenceEvent record(
      UUID visitId,
      UUID patientId,
      String eventType,
      String zoneId,
      String source,
      Instant occurredAt) {

    UUID visitPatientId =
        visitAccess.assertCurrentActorCanAccess(visitId);

    if (!visitPatientId.equals(patientId)) {
      throw new IllegalArgumentException(
          "patientId nao pertence a visita informada");
    }

    return presence.record(
        visitId,
        patientId,
        eventType,
        zoneId,
        source,
        occurredAt);
  }

  public List<PresenceEvent> history(UUID visitId) {

    visitAccess.assertCurrentActorCanAccess(visitId);

    return presence.history(visitId);
  }
}
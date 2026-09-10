package br.com.sussmartcare.telemetry.application;

import br.com.sussmartcare.telemetry.domain.TelemetryMode;
import br.com.sussmartcare.telemetry.domain.TelemetrySession;
import br.com.sussmartcare.telemetry.infrastructure.TelemetrySessionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetrySessionCommandService {

  private final TelemetrySessionRepository sessions;

  public TelemetrySessionCommandService(
      TelemetrySessionRepository sessions) {

    this.sessions = sessions;
  }

  @Transactional
  public TelemetrySession start(
      UUID patientId,
      UUID visitId,
      UUID preHospitalEncounterId,
      String sourceContext,
      TelemetryMode mode) {

    return sessions.save(
        new TelemetrySession(
            patientId,
            visitId,
            preHospitalEncounterId,
            sourceContext,
            mode));
  }
}

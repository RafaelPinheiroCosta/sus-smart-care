package br.com.sussmartcare.telemetry.infrastructure;

import br.com.sussmartcare.telemetry.domain.MqttIngestionMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MqttIngestionMessageRepository
    extends JpaRepository<MqttIngestionMessage, UUID> {

  boolean existsByDeviceIdAndMessageId(
      UUID deviceId,
      UUID messageId);

  boolean existsByDeviceIdAndSequenceNumber(
      UUID deviceId,
      long sequenceNumber);
}

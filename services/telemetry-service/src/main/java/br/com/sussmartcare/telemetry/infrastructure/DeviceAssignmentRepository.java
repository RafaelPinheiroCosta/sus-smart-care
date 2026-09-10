package br.com.sussmartcare.telemetry.infrastructure;

import br.com.sussmartcare.telemetry.domain.DeviceAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceAssignmentRepository
    extends JpaRepository<DeviceAssignment, UUID> {

  Optional<DeviceAssignment>
      findByDeviceIdAndEndedAtIsNull(UUID deviceId);

  List<DeviceAssignment>
      findByTelemetrySessionIdAndEndedAtIsNull(UUID telemetrySessionId);
}

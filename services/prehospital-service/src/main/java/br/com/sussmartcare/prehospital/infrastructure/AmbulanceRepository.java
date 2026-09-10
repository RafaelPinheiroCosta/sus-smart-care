package br.com.sussmartcare.prehospital.infrastructure;

import br.com.sussmartcare.prehospital.domain.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceRepository
    extends JpaRepository<Ambulance, String> {
}

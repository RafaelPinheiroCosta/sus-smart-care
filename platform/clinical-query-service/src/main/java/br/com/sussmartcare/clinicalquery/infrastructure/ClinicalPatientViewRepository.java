package br.com.sussmartcare.clinicalquery.infrastructure;

import br.com.sussmartcare.clinicalquery.domain.ClinicalPatientView;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicalPatientViewRepository
    extends JpaRepository<ClinicalPatientView, UUID> {

  List<ClinicalPatientView> findByPatientId(
      UUID patientId);

  /*
   * Creates the projection shell safely even when events from different
   * Kafka topics reach a brand-new visit concurrently.
   */
  @Modifying
  @Query(
      value = """
          insert into clinical_patient_views (
            visit_id,
            updated_at
          )
          values (
            :visitId,
            now()
          )
          on conflict (visit_id) do nothing
          """,
      nativeQuery = true)
  void ensureExists(
      @Param("visitId")
      UUID visitId);

  /*
   * All event consumers mutate the same read-model row.
   * PESSIMISTIC_WRITE prevents lost updates between Kafka listener threads.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select v
      from ClinicalPatientView v
      where v.visitId = :visitId
      """)
  Optional<ClinicalPatientView> findByVisitIdForUpdate(
      @Param("visitId")
      UUID visitId);
}

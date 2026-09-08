package br.com.sussmartcare.prehospital.infrastructure.outbox; import org.springframework.data.jpa.repository.JpaRepository; import java.time.*; import java.util.*;
public interface OutboxRepository extends JpaRepository<OutboxEvent,UUID>{List<OutboxEvent> findTop100ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Instant now);}

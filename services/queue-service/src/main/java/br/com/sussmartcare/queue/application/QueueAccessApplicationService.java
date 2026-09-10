package br.com.sussmartcare.queue.application;

import br.com.sussmartcare.queue.domain.QueueEntry;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QueueAccessApplicationService {

  private final QueueApplicationService queueApplicationService;
  private final VisitAccessPort visitAccess;

  public QueueAccessApplicationService(
      QueueApplicationService queueApplicationService,
      VisitAccessPort visitAccess) {

    this.queueApplicationService = queueApplicationService;
    this.visitAccess = visitAccess;
  }

  public QueueEntry getByVisit(UUID visitId) {
    visitAccess.assertCurrentActorCanAccess(visitId);
    return queueApplicationService.getByVisit(visitId);
  }

  public QueueEntry leave(UUID entryId, long expectedReturnMinutes, long graceMinutes) {
    QueueEntry queueEntry = queueApplicationService.get(entryId);
    visitAccess.assertCurrentActorCanAccess(queueEntry.getVisitId());
    return queueApplicationService.leave(entryId, expectedReturnMinutes, graceMinutes);
  }

  public QueueEntry returned(UUID entryId) {
    QueueEntry queueEntry = queueApplicationService.get(entryId);
    visitAccess.assertCurrentActorCanAccess(queueEntry.getVisitId());
    return queueApplicationService.returned(entryId);
  }
}


package br.com.sussmartcare.queue.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.sussmartcare.queue.domain.QueueEntry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class QueueAccessApplicationServiceTest {

  @Test
  void getByVisitValidatesAccessBeforeReadingQueue() {
    QueueApplicationService queue = mock(QueueApplicationService.class);
    VisitAccessPort visitAccess = mock(VisitAccessPort.class);

    QueueAccessApplicationService service =
        new QueueAccessApplicationService(queue, visitAccess);

    UUID visitId = UUID.randomUUID();
    QueueEntry expected = mock(QueueEntry.class);

    when(queue.getByVisit(visitId)).thenReturn(expected);

    QueueEntry actual = service.getByVisit(visitId);

    assertSame(expected, actual);

    InOrder order = inOrder(visitAccess, queue);
    order.verify(visitAccess).assertCurrentActorCanAccess(visitId);
    order.verify(queue).getByVisit(visitId);
  }

  @Test
  void leaveDoesNotMutateQueueWhenAccessIsDenied() {
    QueueApplicationService queue = mock(QueueApplicationService.class);
    VisitAccessPort visitAccess = mock(VisitAccessPort.class);

    QueueAccessApplicationService service =
        new QueueAccessApplicationService(queue, visitAccess);

    UUID entryId = UUID.randomUUID();
    UUID visitId = UUID.randomUUID();

    QueueEntry entry = mock(QueueEntry.class);

    when(queue.get(entryId)).thenReturn(entry);
    when(entry.getVisitId()).thenReturn(visitId);

    doThrow(new VisitAccessDeniedException("denied"))
        .when(visitAccess)
        .assertCurrentActorCanAccess(visitId);

    assertThrows(
        VisitAccessDeniedException.class,
        () -> service.leave(entryId, 10, 5));

    verify(queue, never()).leave(entryId, 10, 5);
  }

  @Test
  void returnDoesNotMutateQueueWhenAccessIsDenied() {
    QueueApplicationService queue = mock(QueueApplicationService.class);
    VisitAccessPort visitAccess = mock(VisitAccessPort.class);

    QueueAccessApplicationService service =
        new QueueAccessApplicationService(queue, visitAccess);

    UUID entryId = UUID.randomUUID();
    UUID visitId = UUID.randomUUID();

    QueueEntry entry = mock(QueueEntry.class);

    when(queue.get(entryId)).thenReturn(entry);
    when(entry.getVisitId()).thenReturn(visitId);

    doThrow(new VisitAccessDeniedException("denied"))
        .when(visitAccess)
        .assertCurrentActorCanAccess(visitId);

    assertThrows(
        VisitAccessDeniedException.class,
        () -> service.returned(entryId));

    verify(queue, never()).returned(entryId);
  }
}
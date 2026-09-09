package br.com.sussmartcare.queue.application;

import java.util.UUID;

public interface VisitAccessPort {

  void assertCurrentActorCanAccess(UUID visitId);
}


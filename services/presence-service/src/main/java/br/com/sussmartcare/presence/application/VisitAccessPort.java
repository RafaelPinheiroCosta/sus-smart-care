package br.com.sussmartcare.presence.application;

import java.util.UUID;

public interface VisitAccessPort {
  UUID assertCurrentActorCanAccess(UUID visitId);
}
package br.com.sussmartcare.notification.application;

import java.util.UUID;

public interface PatientAccessPort {
  void assertCurrentActorCanAccess(UUID patientId);
}
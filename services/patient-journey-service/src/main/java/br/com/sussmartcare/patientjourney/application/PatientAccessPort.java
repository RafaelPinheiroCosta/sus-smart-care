package br.com.sussmartcare.patientjourney.application;

import java.util.UUID;

public interface PatientAccessPort {

  void assertCurrentActorCanAccess(UUID patientId);
}
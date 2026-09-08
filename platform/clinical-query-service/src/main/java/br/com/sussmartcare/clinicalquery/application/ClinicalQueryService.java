package br.com.sussmartcare.clinicalquery.application;

import br.com.sussmartcare.clinicalquery.domain.PatientProfileView;
import br.com.sussmartcare.clinicalquery.infrastructure.ClinicalLatestObservationViewRepository;
import br.com.sussmartcare.clinicalquery.infrastructure.ClinicalPatientViewCache;
import br.com.sussmartcare.clinicalquery.infrastructure.ClinicalPatientViewRepository;
import br.com.sussmartcare.clinicalquery.infrastructure.PatientProfileViewRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClinicalQueryService {
  private final ClinicalPatientViewRepository views;
  private final PatientProfileViewRepository profiles;
  private final ClinicalLatestObservationViewRepository observations;
  private final ClinicalPatientViewCache cache;

  public ClinicalQueryService(
      ClinicalPatientViewRepository views,
      PatientProfileViewRepository profiles,
      ClinicalLatestObservationViewRepository observations,
      ClinicalPatientViewCache cache) {
    this.views = views;
    this.profiles = profiles;
    this.observations = observations;
    this.cache = cache;
  }

  public ClinicalPatientViewSnapshot get(UUID visitId) {
    return cache.get(visitId).orElseGet(() -> loadAndCache(visitId));
  }

  ClinicalPatientViewSnapshot loadAndCache(UUID visitId) {
    var view = views.findById(visitId)
        .orElseThrow(() -> new IllegalArgumentException("View clínica ainda não projetada"));
    PatientProfileView profile = view.getPatientId() == null
        ? null
        : profiles.findById(view.getPatientId()).orElse(null);
    var snapshot = ClinicalPatientViewSnapshot.from(
        view,
        profile,
        observations.findByVisitIdOrderByTypeAsc(visitId));
    cache.put(snapshot);
    return snapshot;
  }
}

package br.com.sussmartcare.identityaccess.application;

import br.com.sussmartcare.identityaccess.domain.CommunicationProfile;
import br.com.sussmartcare.identityaccess.infrastructure.CommunicationProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunicationProfileApplicationService {

  private final CommunicationProfileRepository profiles;

  public CommunicationProfileApplicationService(
      CommunicationProfileRepository profiles) {
    this.profiles = profiles;
  }

  @Transactional
  public CommunicationProfile save(
      UUID userId,
      boolean hasSmartphone,
      String preferredChannel) {

    return profiles.save(
        new CommunicationProfile(
            userId,
            hasSmartphone,
            preferredChannel));
  }

  public CommunicationProfile get(UUID userId) {
    return profiles.findById(userId)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "Perfil não encontrado"));
  }
}

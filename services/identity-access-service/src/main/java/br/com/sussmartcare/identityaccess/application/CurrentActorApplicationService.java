package br.com.sussmartcare.identityaccess.application;

import br.com.sussmartcare.identityaccess.domain.ActorIdentity;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CurrentActorApplicationService {

  public ActorIdentity current(
      UUID userId,
      Set<String> roles) {

    return new ActorIdentity(
        userId,
        roles);
  }
}
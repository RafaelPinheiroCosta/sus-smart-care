package br.com.sussmartcare.identityaccess.domain;

import java.util.Set;
import java.util.UUID;

public record ActorIdentity(
    UUID userId,
    Set<String> roles) {

  public ActorIdentity {
    roles = Set.copyOf(roles);
  }
}
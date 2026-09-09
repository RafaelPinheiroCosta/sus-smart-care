package br.com.sussmartcare.notification.application;

import br.com.sussmartcare.notification.domain.Notification;
import br.com.sussmartcare.notification.domain.NotificationPreference;
import br.com.sussmartcare.notification.infrastructure.NotificationPreferenceRepository;
import br.com.sussmartcare.notification.infrastructure.NotificationRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationApplicationService {

  private final NotificationRepository repo;
  private final NotificationPreferenceRepository preferences;
  private final List<NotificationChannel> channels;

  public NotificationApplicationService(
      NotificationRepository repo,
      NotificationPreferenceRepository preferences,
      List<NotificationChannel> channels) {

    this.repo = repo;
    this.preferences = preferences;
    this.channels = channels;
  }

  @Transactional
  public NotificationPreference preference(
      UUID patient,
      boolean smartphone,
      String preferred,
      String fallback) {

    return preferences.save(
        new NotificationPreference(
            patient,
            smartphone,
            preferred,
            fallback));
  }

  @Transactional
  public List<Notification> sendAuto(
      UUID patient,
      UUID visit,
      String type,
      String message) {

    var preference =
        preferences
            .findById(patient)
            .orElse(
                new NotificationPreference(
                    patient,
                    false,
                    "DISPLAY",
                    "STAFF_ASSISTED"));

    var selected =
        new LinkedHashSet<String>();

    selected.add(
        preference.getPreferredChannel());

    if (preference.getFallbackChannel() != null
        && !preference.getFallbackChannel().isBlank()) {

      selected.add(
          preference.getFallbackChannel());
    }

    var result =
        new ArrayList<Notification>();

    for (String channel : selected) {
      result.add(
          send(
              patient,
              visit,
              type,
              channel,
              message));
    }

    return result;
  }

  @Transactional
  public Notification send(
      UUID patient,
      UUID visit,
      String type,
      String channel,
      String message) {

    var notification =
        repo.save(
            new Notification(
                patient,
                visit,
                type,
                channel,
                message));

    var adapter =
        channels
            .stream()
            .filter(candidate ->
                candidate.supports(channel))
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException(
                    "Canal nao suportado: " + channel));

    adapter.send(notification);

    notification.delivered();

    return repo.save(notification);
  }

  public List<Notification> history(UUID patient) {

    return repo
        .findByPatientIdOrderByCreatedAtDesc(
            patient);
  }
}
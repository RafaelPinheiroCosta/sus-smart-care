package br.com.sussmartcare.prehospital.infrastructure.outbox;
import java.time.*; import java.util.concurrent.TimeUnit; import org.slf4j.*; import org.springframework.kafka.core.KafkaTemplate; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import org.springframework.transaction.annotation.Transactional;
@Component public class OutboxPublisher {
  private static final Logger log=LoggerFactory.getLogger(OutboxPublisher.class); private final OutboxRepository repo; private final KafkaTemplate<String,String> kafka;
  public OutboxPublisher(OutboxRepository r,KafkaTemplate<String,String> k){repo=r;kafka=k;}
  @Scheduled(fixedDelayString="${outbox.publish-delay-ms:1000}") @Transactional public void publish(){
    for(var e:repo.findTop100ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Instant.now())){
      try{kafka.send(e.getTopic(),e.getEventKey(),e.getPayload()).get(5,TimeUnit.SECONDS);e.published();}
      catch(Exception ex){e.failed(ex);log.warn("outbox_publish_failed id={} topic={} attempts={} next={}",e.getId(),e.getTopic(),e.getAttempts(),e.getNextAttemptAt());}
      repo.save(e);
    }
  }
}

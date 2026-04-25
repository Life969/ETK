package ProjectForJob.example.Job.services;


import ProjectForJob.example.Job.DataTransferObject.kafkaDto.OrderStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, OrderStartedEvent> kafkaTemplate;
    private static final String TOPIC = "order.production.started";

    public void sendOrderStartedEvent(OrderStartedEvent event) {
        log.info("Sending OrderStartedEvent to topic {}: {}", TOPIC, event);
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Message sent successfully: {}", result.getRecordMetadata());
                    } else {
                        log.error("Failed to send message", ex);
                    }
                });
    }
}
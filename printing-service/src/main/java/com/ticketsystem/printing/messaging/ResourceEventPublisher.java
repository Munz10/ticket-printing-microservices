package com.ticketsystem.printing.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes ResourceLowEvent messages to the resource-events exchange.
 */
@Component
public class ResourceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ResourceEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ResourceEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTonerLow(int currentLevel) {
        log.info("Publishing toner low event (level={})", currentLevel);
        rabbitTemplate.convertAndSend(RabbitMQConfig.RESOURCE_EVENTS_EXCHANGE, RabbitMQConfig.TONER_LOW_ROUTING_KEY,
                new ResourceLowEvent(ResourceLowEvent.Resource.TONER, currentLevel));
    }

    public void publishPaperLow(int currentLevel) {
        log.info("Publishing paper low event (level={})", currentLevel);
        rabbitTemplate.convertAndSend(RabbitMQConfig.RESOURCE_EVENTS_EXCHANGE, RabbitMQConfig.PAPER_LOW_ROUTING_KEY,
                new ResourceLowEvent(ResourceLowEvent.Resource.PAPER, currentLevel));
    }
}

package com.ticketsystem.resupply.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RESOURCE_EVENTS_EXCHANGE = "resource-events";
    public static final String RESOURCE_EVENTS_QUEUE = "resupply.resource-events";
    public static final String RESOURCE_LOW_ROUTING_PATTERN = "resource.*.low";

    @Bean
    public TopicExchange resourceEventsExchange() {
        return new TopicExchange(RESOURCE_EVENTS_EXCHANGE);
    }

    @Bean
    public Queue resourceEventsQueue() {
        return new Queue(RESOURCE_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding resourceEventsBinding(Queue resourceEventsQueue, TopicExchange resourceEventsExchange) {
        return BindingBuilder.bind(resourceEventsQueue)
                .to(resourceEventsExchange)
                .with(RESOURCE_LOW_ROUTING_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

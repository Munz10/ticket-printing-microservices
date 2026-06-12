package com.ticketsystem.printing.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RESOURCE_EVENTS_EXCHANGE = "resource-events";
    public static final String TONER_LOW_ROUTING_KEY = "resource.toner.low";
    public static final String PAPER_LOW_ROUTING_KEY = "resource.paper.low";

    @Bean
    public TopicExchange resourceEventsExchange() {
        return new TopicExchange(RESOURCE_EVENTS_EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

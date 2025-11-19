package org.example.storyreading.paymentservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_QUEUE = "payment.queue";
    public static final String PAYMENT_ROUTING_KEY = "payment.success";

    // Story purchase configuration
    public static final String STORY_PURCHASE_EXCHANGE = "story.purchase.exchange";
    public static final String STORY_PURCHASE_QUEUE = "story.purchase.queue";
    public static final String STORY_PURCHASE_ROUTING_KEY = "story.purchase.success";

    // Payment notification configuration
    public static final String PAYMENT_NOTIFICATION_EXCHANGE = "payment.notification.exchange";
    public static final String PAYMENT_NOTIFICATION_QUEUE = "payment.notification.queue";
    public static final String PAYMENT_NOTIFICATION_ROUTING_KEY = "payment.notification";

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Queue storyPurchaseQueue() {
        return new Queue(STORY_PURCHASE_QUEUE, true);
    }

    @Bean
    public Queue paymentNotificationQueue() {
        return new Queue(PAYMENT_NOTIFICATION_QUEUE, true);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange storyPurchaseExchange() {
        return new TopicExchange(STORY_PURCHASE_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentNotificationExchange() {
        return new TopicExchange(PAYMENT_NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Binding storyPurchaseBinding() {
        return BindingBuilder.bind(storyPurchaseQueue()).to(storyPurchaseExchange()).with(STORY_PURCHASE_ROUTING_KEY);
    }

    @Bean
    public Binding paymentNotificationBinding(Queue paymentNotificationQueue, TopicExchange paymentNotificationExchange) {
        return BindingBuilder.bind(paymentNotificationQueue).to(paymentNotificationExchange).with(PAYMENT_NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}

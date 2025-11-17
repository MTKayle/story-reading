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

    // Story purchase configuration (gửi tới story-service)
    public static final String STORY_PURCHASE_EXCHANGE = "story.purchase.exchange";
    public static final String STORY_PURCHASE_QUEUE = "story.purchase.queue";
    public static final String STORY_PURCHASE_ROUTING_KEY = "story.purchase.success";

    // Deposit configuration (gửi tới notification-service)
    public static final String DEPOSIT_EXCHANGE = "deposit-exchange";
    public static final String DEPOSIT_QUEUE = "deposit-queue";
    public static final String DEPOSIT_ROUTING_KEY = "deposit.created";

    // Payment configuration (gửi tới notification-service)
    public static final String PAYMENT_EXCHANGE = "payment-exchange";
    public static final String PAYMENT_QUEUE = "payment-queue";
    public static final String PAYMENT_ROUTING_KEY = "payment.created";

    // Story purchase beans
    @Bean
    public Queue storyPurchaseQueue() {
        return new Queue(STORY_PURCHASE_QUEUE, true);
    }

    @Bean
    public TopicExchange storyPurchaseExchange() {
        return new TopicExchange(STORY_PURCHASE_EXCHANGE);
    }

    @Bean
    public Binding storyPurchaseBinding() {
        return BindingBuilder.bind(storyPurchaseQueue()).to(storyPurchaseExchange()).with(STORY_PURCHASE_ROUTING_KEY);
    }

    // Deposit beans
    @Bean
    public Queue depositQueue() {
        return new Queue(DEPOSIT_QUEUE, true);
    }

    @Bean
    public TopicExchange depositExchange() {
        return new TopicExchange(DEPOSIT_EXCHANGE);
    }

    @Bean
    public Binding depositBinding() {
        return BindingBuilder.bind(depositQueue()).to(depositExchange()).with(DEPOSIT_ROUTING_KEY);
    }

    // Payment beans
    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue()).to(paymentExchange()).with(PAYMENT_ROUTING_KEY);
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

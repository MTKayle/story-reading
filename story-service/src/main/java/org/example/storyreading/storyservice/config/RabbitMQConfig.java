package org.example.storyreading.storyservice.config;

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

    public static final String STORY_PURCHASE_EXCHANGE = "story.purchase.exchange";
    public static final String STORY_PURCHASE_QUEUE = "story.purchase.queue";
    public static final String STORY_PURCHASE_ROUTING_KEY = "story.purchase.success";

    // === NEW CHAPTER ===
    public static final String NEW_CHAPTER_EXCHANGE = "new-chapter-exchange";
    public static final String NEW_CHAPTER_QUEUE = "new-chapter-queue";
    public static final String NEW_CHAPTER_ROUTING_KEY = "new.chapter.created";

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
        return BindingBuilder.bind(storyPurchaseQueue())
                .to(storyPurchaseExchange())
                .with(STORY_PURCHASE_ROUTING_KEY);
    }

    @Bean
    public Queue newChapterQueue() {
        return new Queue(NEW_CHAPTER_QUEUE, true);
    }

    @Bean
    public TopicExchange newChapterExchange() {
        return new TopicExchange(NEW_CHAPTER_EXCHANGE);
    }

    @Bean
    public Binding newChapterBinding() {
        return BindingBuilder.bind(newChapterQueue())
                .to(newChapterExchange())
                .with(NEW_CHAPTER_ROUTING_KEY);
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

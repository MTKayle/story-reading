package org.example.storyreading.notificationservice.config;

import org.example.storyreading.notificationservice.dto.comment.CommentDeletedEvent;
import org.example.storyreading.notificationservice.dto.comment.CommentEvent;
import org.example.storyreading.notificationservice.dto.deposit.DepositEvent;
import org.example.storyreading.notificationservice.dto.payment.PaymentEvent;
import org.example.storyreading.notificationservice.dto.reaction.ReactionDeletedEvent;
import org.example.storyreading.notificationservice.dto.reaction.ReactionEvent;
import org.example.storyreading.notificationservice.dto.rating.RatingDeletedEvent;
import org.example.storyreading.notificationservice.dto.rating.RatingEvent;
import org.example.storyreading.notificationservice.dto.story.NewChapterEvent;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // === COMMENT ===
    public static final String COMMENT_EXCHANGE = "comment-exchange";
    public static final String COMMENT_QUEUE = "comment-queue";
    public static final String COMMENT_ROUTING_KEY = "comment.created";
    public static final String COMMENT_DELETE_QUEUE = "comment-delete-queue";
    public static final String COMMENT_DELETE_ROUTING_KEY = "comment.deleted";

    // === REACTION ===
    public static final String REACTION_EXCHANGE = "reaction-exchange";
    public static final String REACTION_QUEUE = "reaction-queue";
    public static final String REACTION_ROUTING_KEY = "reaction.created";
    public static final String REACTION_DELETE_QUEUE = "reaction-delete-queue";
    public static final String REACTION_DELETE_ROUTING_KEY = "reaction.deleted";

    // === RATING ===
    public static final String RATING_EXCHANGE = "rating-exchange";
    public static final String RATING_QUEUE = "rating-queue";
    public static final String RATING_ROUTING_KEY = "rating.created";
    public static final String RATING_DELETE_QUEUE = "rating-delete-queue";
    public static final String RATING_DELETE_ROUTING_KEY = "rating.deleted";

    // === DEPOSIT ===
    public static final String DEPOSIT_EXCHANGE = "deposit-exchange";
    public static final String DEPOSIT_QUEUE = "deposit-queue";
    public static final String DEPOSIT_ROUTING_KEY = "deposit.created";

    // === PAYMENT ===
    public static final String PAYMENT_EXCHANGE = "payment-exchange";
    public static final String PAYMENT_QUEUE = "payment-queue";
    public static final String PAYMENT_ROUTING_KEY = "payment.created";

    // === NEW CHAPTER ===
    public static final String NEW_CHAPTER_EXCHANGE = "new-chapter-exchange";
    public static final String NEW_CHAPTER_QUEUE = "new-chapter-queue";
    public static final String NEW_CHAPTER_ROUTING_KEY = "new.chapter.created";


    // === COMMENT ===
    @Bean
    public Queue commentQueue() {
        return new Queue(COMMENT_QUEUE, true);
    }

    @Bean
    public TopicExchange commentExchange() {
        return new TopicExchange(COMMENT_EXCHANGE);
    }

    @Bean
    public Binding commentBinding(Queue commentQueue, TopicExchange commentExchange) {
        return BindingBuilder.bind(commentQueue).to(commentExchange).with(COMMENT_ROUTING_KEY);
    }

    @Bean
    public Queue commentDeleteQueue() {
        return new Queue(COMMENT_DELETE_QUEUE, true);
    }

    @Bean
    public Binding commentDeleteBinding(Queue commentDeleteQueue, TopicExchange commentExchange) {
        return BindingBuilder.bind(commentDeleteQueue).to(commentExchange).with(COMMENT_DELETE_ROUTING_KEY);
    }

    // === REACTION ===
    @Bean
    public Queue reactionQueue() {
        return new Queue(REACTION_QUEUE, true);
    }

    @Bean
    public TopicExchange reactionExchange() {
        return new TopicExchange(REACTION_EXCHANGE);
    }

    @Bean
    public Binding reactionBinding(Queue reactionQueue, TopicExchange reactionExchange) {
        return BindingBuilder.bind(reactionQueue).to(reactionExchange).with(REACTION_ROUTING_KEY);
    }

    @Bean
    public Queue reactionDeleteQueue() {
        return new Queue(REACTION_DELETE_QUEUE, true);
    }

    @Bean
    public Binding reactionDeleteBinding(Queue reactionDeleteQueue, TopicExchange reactionExchange) {
        return BindingBuilder.bind(reactionDeleteQueue).to(reactionExchange).with(REACTION_DELETE_ROUTING_KEY);
    }

    // === RATING ===
    @Bean
    public Queue ratingQueue() {
        return new Queue(RATING_QUEUE, true);
    }

    @Bean
    public TopicExchange ratingExchange() {
        return new TopicExchange(RATING_EXCHANGE);
    }

    @Bean
    public Binding ratingBinding(Queue ratingQueue, TopicExchange ratingExchange) {
        return BindingBuilder.bind(ratingQueue).to(ratingExchange).with(RATING_ROUTING_KEY);
    }

    @Bean
    public Queue ratingDeleteQueue() {
        return new Queue(RATING_DELETE_QUEUE, true);
    }

    @Bean
    public Binding ratingDeleteBinding(Queue ratingDeleteQueue, TopicExchange ratingExchange) {
        return BindingBuilder.bind(ratingDeleteQueue).to(ratingExchange).with(RATING_DELETE_ROUTING_KEY);
    }

    // === DEPOSIT ===
    @Bean
    public Queue depositQueue() {
        return new Queue(DEPOSIT_QUEUE, true);
    }

    @Bean
    public TopicExchange depositExchange() {
        return new TopicExchange(DEPOSIT_EXCHANGE);
    }

    @Bean
    public Binding depositBinding(Queue depositQueue, TopicExchange depositExchange) {
        return BindingBuilder.bind(depositQueue).to(depositExchange).with(DEPOSIT_ROUTING_KEY);
    }


    // === PAYMENT ===
    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(PAYMENT_ROUTING_KEY);
    }


    // === NEW CHAPTER ===
    @Bean
    public Queue newChapterQueue() {
        return new Queue(NEW_CHAPTER_QUEUE, true);
    }

    @Bean
    public TopicExchange newChapterExchange() {
        return new TopicExchange(NEW_CHAPTER_EXCHANGE);
    }

    @Bean
    public Binding newChapterBinding(Queue newChapterQueue, TopicExchange newChapterExchange) {
        return BindingBuilder.bind(newChapterQueue).to(newChapterExchange).with(NEW_CHAPTER_ROUTING_KEY);
    }


    // === COMMON - CHỈ 1 BEAN ===
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        converter.setAlwaysConvertToInferredType(true);

        // ✅ Map type từ Comment Service → Notification Service
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();

        // Comment events
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.CommentNotificationEvent",
                CommentEvent.class
        );
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.comment.CommentDeletedEvent",
                CommentDeletedEvent.class
        );

        // Reaction events (nếu có)
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.ReactionNotificationEvent",
                ReactionEvent.class
        );
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.reaction.ReactionDeletedEvent",
                ReactionDeletedEvent.class
        );

        // Rating events (nếu có)
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.RatingNotificationEvent",
                RatingEvent.class
        );
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.rating.RatingDeletedEvent",
                RatingDeletedEvent.class
        );

        // DepositEvent events (nếu có)
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.DepositNotificationEvent",
                DepositEvent.class
        );
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.deposit.DepositEvent",
                DepositEvent.class
        );

        // PaymentEvent events (nếu có)
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.PaymentNotificationEvent",
                PaymentEvent.class
        );
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.payment.PaymentEvent",
                PaymentEvent.class
        );

        // New Chapter events (nếu có)
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.NewChapterNotificationEvent",
                NewChapterEvent.class
        );
        idClassMapping.put(
                "org.example.storyreading.commentservice.event.story.NewChapterEvent",
                NewChapterEvent.class
        );

        typeMapper.setIdClassMapping(idClassMapping);
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);  // Không retry nếu fail
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
package org.example.storyreading.commentservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

    // === COMMENT BINDINGS ===
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

    // === REACTION BINDINGS ===
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

    // === RATING BINDINGS ===
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

    // === COMMON BEANS ===
    @Bean
    public MessageConverter messageConverter() {
        // ✅ Thêm ObjectMapper với JavaTimeModule để xử lý LocalDateTime
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        converter.setCreateMessageIds(true);

        // ✅ Không gửi __TypeId__ header, để Notification Service tự infer
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
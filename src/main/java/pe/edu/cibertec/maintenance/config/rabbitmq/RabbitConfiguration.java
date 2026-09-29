package pe.edu.cibertec.maintenance.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
@Import(RabbitRoutesRegistrar.class)
public class RabbitConfiguration {

    private final RabbitProperties rabbitProperties;

    public RabbitConfiguration(RabbitProperties rabbitProperties) {
        this.rabbitProperties = rabbitProperties;
    }

    /**
     * Declara dinámicamente en el broker de RabbitMQ todos los Exchanges,
     * Queues y Bindings definidos en el Map de properties.
     */
    @Bean
    public Declarables rabbitDeclarables() {
        List<Declarable> declarablesList = new ArrayList<>();

        rabbitProperties.getRoutes().forEach((key, route) -> {
            if (route != null && route.getExchange() != null && route.getQueue() != null) {
                TopicExchange exchange = new TopicExchange(route.getExchange(), true, false);
                Queue queue = new Queue(route.getQueue(), true);
                Binding binding = BindingBuilder
                        .bind(queue)
                        .to(exchange)
                        .with(route.getRoutingKey() != null ? route.getRoutingKey() : "");

                declarablesList.add(exchange);
                declarablesList.add(queue);
                declarablesList.add(binding);
            }
        });

        return new Declarables(declarablesList);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitMqTemplate rabbitTemplate = new RabbitMqTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}

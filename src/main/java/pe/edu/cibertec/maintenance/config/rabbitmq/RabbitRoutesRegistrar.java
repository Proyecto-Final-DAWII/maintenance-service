package pe.edu.cibertec.maintenance.config.rabbitmq;

import org.jspecify.annotations.NonNull;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * Registrador dinámico que expone cada ruta, exchange, queue y binding
 * configurados en app.rabbitmq.routes como beans individuales en el contexto de Spring.
 *
 * Permite inyectarlos individualmente mediante @Qualifier:
 * - Queue: @Qualifier("ordersQueue") o @Qualifier("orders-queue")
 * - TopicExchange: @Qualifier("ordersExchange") o @Qualifier("orders-exchange")
 * - Binding: @Qualifier("ordersBinding") o @Qualifier("orders-binding")
 * - Route: @Qualifier("ordersRoute") o @Qualifier("orders")
 */
public class RabbitRoutesRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        RabbitProperties properties = Binder.get(environment)
                .bind("app.rabbitmq", RabbitProperties.class)
                .orElseGet(RabbitProperties::new);

        Map<String, RabbitProperties.Route> routes = properties.getRoutes();
        if (routes == null || routes.isEmpty()) {
            return;
        }

        routes.forEach((name, route) -> {
            if (route == null) {
                return;
            }

            String camelCaseName = toCamelCase(name);

            String routeBeanName = camelCaseName + "Route";
            AbstractBeanDefinition routeDef = BeanDefinitionBuilder
                    .genericBeanDefinition(RabbitProperties.Route.class, () -> route)
                    .getBeanDefinition();
            registry.registerBeanDefinition(routeBeanName, routeDef);
            registerAliasIfDifferent(registry, routeBeanName, name);
            registerAliasIfDifferent(registry, routeBeanName, name + "Route");
            registerAliasIfDifferent(registry, routeBeanName, name + "-route");

            if (route.getExchange() != null && !route.getExchange().isBlank()) {
                String exchangeBeanName = camelCaseName + "Exchange";
                AbstractBeanDefinition exchangeDef = BeanDefinitionBuilder
                        .genericBeanDefinition(TopicExchange.class, () -> new TopicExchange(route.getExchange(), true, false))
                        .getBeanDefinition();
                registry.registerBeanDefinition(exchangeBeanName, exchangeDef);
                registerAliasIfDifferent(registry, exchangeBeanName, name + "Exchange");
                registerAliasIfDifferent(registry, exchangeBeanName, name + "-exchange");
            }

            if (route.getQueue() != null && !route.getQueue().isBlank()) {
                String queueBeanName = camelCaseName + "Queue";
                AbstractBeanDefinition queueDef = BeanDefinitionBuilder
                        .genericBeanDefinition(Queue.class, () -> new Queue(route.getQueue(), true))
                        .getBeanDefinition();
                registry.registerBeanDefinition(queueBeanName, queueDef);
                registerAliasIfDifferent(registry, queueBeanName, name + "Queue");
                registerAliasIfDifferent(registry, queueBeanName, name + "-queue");
            }

            if (route.getExchange() != null && !route.getExchange().isBlank()
                    && route.getQueue() != null && !route.getQueue().isBlank()
                    && route.getRoutingKey() != null) {
                String bindingBeanName = camelCaseName + "Binding";
                AbstractBeanDefinition bindingDef = BeanDefinitionBuilder
                        .genericBeanDefinition(Binding.class, () -> {
                            Queue queue = new Queue(route.getQueue(), true);
                            TopicExchange exchange = new TopicExchange(route.getExchange(), true, false);
                            return BindingBuilder.bind(queue).to(exchange).with(route.getRoutingKey());
                        })
                        .getBeanDefinition();
                registry.registerBeanDefinition(bindingBeanName, bindingDef);
                registerAliasIfDifferent(registry, bindingBeanName, name + "Binding");
                registerAliasIfDifferent(registry, bindingBeanName, name + "-binding");
            }
        });
    }

    private void registerAliasIfDifferent(BeanDefinitionRegistry registry, String beanName, String alias) {
        if (!beanName.equalsIgnoreCase(alias) && !registry.isAlias(alias) && !registry.containsBeanDefinition(alias)) {
            registry.registerAlias(beanName, alias);
        }
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '-' || c == '_' || c == '.') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

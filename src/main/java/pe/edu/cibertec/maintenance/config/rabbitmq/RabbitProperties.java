package pe.edu.cibertec.maintenance.config.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.rabbitmq")
@Getter
@Setter
public class RabbitProperties {

    private Map<String, Route> routes = new HashMap<>();

    public Route getRoute(String name) {
        Route route = routes.get(name);
        if (route == null) {
            throw new IllegalArgumentException(
                    "RabbitMQ route not found for key: '" + name + "'. Available routes: " + routes.keySet()
            );
        }
        return route;
    }

    public String getExchange(String name) {
        return getRoute(name).getExchange();
    }

    public String getQueue(String name) {
        return getRoute(name).getQueue();
    }

    public String getRoutingKey(String name) {
        return getRoute(name).getRoutingKey();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Route {
        private String exchange;
        private String queue;
        private String routingKey;
    }
}

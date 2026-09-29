package pe.edu.cibertec.maintenance.config.openfeign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración compartida por todos los clientes OpenFeign de la aplicación.
 *
 * <p>Al estar dentro del paquete base de Spring, el {@link RequestInterceptor}
 * se registra globalmente y se aplica a los clientes declarados con
 * {@code @FeignClient}.</p>
 */
@Configuration(proxyBeanMethods = false)
public class OpenFeignConfiguration {

    /**
     * Propaga la credencial de la petición HTTP actual hacia la llamada Feign.
     */
    @Bean
    public RequestInterceptor authTokenRequestInterceptor() {
        return new AuthTokenRequestInterceptor();
    }
}

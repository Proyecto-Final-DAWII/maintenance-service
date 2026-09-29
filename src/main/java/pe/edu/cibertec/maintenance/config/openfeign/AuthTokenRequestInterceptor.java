package pe.edu.cibertec.maintenance.config.openfeign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Copia el header {@code Authorization} de la petición HTTP entrante a las
 * peticiones salientes realizadas con OpenFeign.
 *
 * <p>El valor se propaga completo para conservar el esquema de autenticación,
 * por ejemplo {@code Bearer eyJ...}. Si no existe una petición HTTP activa, no
 * hay un token o el cliente ya definió su propio header {@code Authorization},
 * el interceptor no modifica la petición.</p>
 *
 * <p>La clase no almacena el token en ningún campo, por lo que el bean puede ser
 * singleton sin mezclar credenciales entre solicitudes concurrentes.</p>
 */
public final class AuthTokenRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        if (hasAuthorizationHeader(template)) {
            return;
        }

        HttpServletRequest currentRequest = getCurrentHttpRequest();
        if (currentRequest == null) {
            return;
        }

        String authorization = currentRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            template.header(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private boolean hasAuthorizationHeader(RequestTemplate template) {
        return template.headers().keySet().stream()
                .anyMatch(HttpHeaders.AUTHORIZATION::equalsIgnoreCase);
    }

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}

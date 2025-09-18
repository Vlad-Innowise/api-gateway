package by.innowise.internship.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import static by.innowise.internship.gateway.config.InternalServiceProperties.AUTH_SERVICE_PROPERTY;
import static by.innowise.internship.gateway.config.InternalServiceProperties.ORDER_SERVICE_PROPERTY;
import static by.innowise.internship.gateway.config.InternalServiceProperties.USER_SERVICE_PROPERTY;

@Configuration
public class RoutesConfig {

    @Bean
    public RouteLocator authorizedRoutes(RouteLocatorBuilder builder, InternalServiceProperties serviceProperty) {
        return builder.routes()

                      .route("user-service-users", r ->
                              r.path("/api/v1/users", "/api/v1/users/**")
                               .and()
                               .method(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD,
                                       HttpMethod.OPTIONS)
                               .uri(serviceProperty.getServices().get(USER_SERVICE_PROPERTY)))

                      .route("user-service-admin", r ->
                              r.path("/api/v1/admin", "/api/v1/admin/**")
                               .uri(serviceProperty.getServices().get(USER_SERVICE_PROPERTY)))

                      .route("auth-service", r ->
                              r.path("/token/refresh", "/authenticate")
                               .uri(serviceProperty.getServices().get(AUTH_SERVICE_PROPERTY)))

                      .route("order-service-items", r ->
                              r.path("/api/v1/items", "/api/v1/items/**")
                               .uri(serviceProperty.getServices().get(ORDER_SERVICE_PROPERTY)))

                      .route("order-service-orders", r ->
                              r.path("/api/v1/orders", "/api/v1/orders/**")
                               .uri(serviceProperty.getServices().get(ORDER_SERVICE_PROPERTY)))

                      .build();
    }

}

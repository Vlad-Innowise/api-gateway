package by.innowise.internship.gateway.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "application.gateway")
@Validated
public class InternalServiceProperties {

    public static final String USER_SERVICE_PROPERTY = "user-service";
    public static final String AUTH_SERVICE_PROPERTY = "auth-service";
    public static final String ORDER_SERVICE_PROPERTY = "order-service";
    public static final String PAYMENT_SERVICE_PROPERTY = "payment-service";

    @NotEmpty(message = "Services urls can't be null or blank!")
    private Map<@NotNull String, @NotNull String> services;

}

package by.innowise.internship.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "application.cors")
@Data
public class AllowedOriginPatternsProperty {

    private List<String> allowedOrigins;

}

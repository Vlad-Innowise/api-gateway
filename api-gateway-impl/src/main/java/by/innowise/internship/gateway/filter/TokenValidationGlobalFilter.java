package by.innowise.internship.gateway.filter;

import by.innowise.common.library.exception.dto.SimpleExceptionDto;
import by.innowise.internship.gateway.config.InternalServiceProperties;
import by.innowise.internship.gateway.exception.ApiApplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static by.innowise.internship.gateway.config.InternalServiceProperties.AUTH_SERVICE_PROPERTY;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenValidationGlobalFilter implements GlobalFilter, Ordered {

    private static final String JWT_AUTH_HEADER_PREFIX = "Bearer ";
    private static final List<String> WHITELIST_PATHS = List.of("/api/v1/auth/login", "/api/v1/auth/register");
    private final WebClient webClient;
    private final InternalServiceProperties serviceProperty;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Invoked token validation global filter for path: {}", exchange.getRequest().getPath().value());
        String authzHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String uri = serviceProperty.getServices().get(AUTH_SERVICE_PROPERTY) + "/token/validate";

        if (pathIsWhitelisted(exchange)) {
            return chain.filter(exchange);
        }

        if (headerIsNotValid(authzHeader)) {
            return getFailedAuthzResponse(exchange, new RuntimeException(
                    "Authorization header is missing or invalid: [%s]".formatted(authzHeader)));
        }

        return validationPostRequest(exchange, chain, uri, authzHeader)
                .onErrorResume(ApiApplicationException.class, err -> {
                    logTokenValidationError(err);
                    return writeErrorResponse(exchange, err);
                })
                .onErrorResume(Exception.class, err -> {
                    log.error("Internal server error", err);
                    return getFailedAuthzResponse(exchange, err);
                });

    }

    @Override
    public int getOrder() {
        return -100;
    }

    public boolean pathIsWhitelisted(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.info("Incoming path to check against whitelisted paths: {}", path);
        return WHITELIST_PATHS.stream()
                              .anyMatch(urlPattern -> matcher.match(urlPattern, path));
    }

    private boolean headerIsNotValid(String header) {
        return !StringUtils.hasText(header) || !header.startsWith(JWT_AUTH_HEADER_PREFIX);
    }

    private Mono<Void> validationPostRequest(ServerWebExchange exchange, GatewayFilterChain chain, String uri,
                                             String authzHeader) {
        return webClient.post()
                        .uri(uri)
                        .bodyValue(Map.of("token", getTokenFromHeader(authzHeader)))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse ->
                                clientResponse.bodyToMono(SimpleExceptionDto.class)
                                              .flatMap(dto -> Mono.error(
                                                      new ApiApplicationException(dto.message(),
                                                                                  HttpStatus.valueOf(dto.code()),
                                                                                  dto)
                                              ))
                        )
                        .toBodilessEntity()
                        .flatMap(respEntity->{
                            log.info("Token validation was successful");
                            return chain.filter(exchange);
                        });
    }

    private Mono<Void> getFailedAuthzResponse(ServerWebExchange exchange, Throwable err) {
        logTokenValidationError(err);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private void logTokenValidationError(Throwable err) {
        log.error("Token validation failed: {}", err.getMessage(), err);
    }

    private String getTokenFromHeader(String authzHeader) {
        Objects.requireNonNull(authzHeader);
        return authzHeader.substring(JWT_AUTH_HEADER_PREFIX.length());
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, ApiApplicationException exception) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(exception.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = getErrorDtoAsByteArray(exception);
        } catch (Exception e) {
            return response.setComplete();
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private byte[] getErrorDtoAsByteArray(ApiApplicationException exception) throws JsonProcessingException {
        byte[] bytes;
        if (exception.getErrorDto() == null) {
            bytes = "{}".getBytes(StandardCharsets.UTF_8);
        } else {
            bytes = objectMapper.writeValueAsBytes(exception.getErrorDto());
        }
        return bytes;
    }
}

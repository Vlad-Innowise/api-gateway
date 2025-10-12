package by.innowise.internship.gateway.controller;

import by.innowise.internship.gateway.config.InternalServiceProperties;
import by.innowise.internship.gateway.exception.ApiApplicationException;
import by.innowise.internship.gateway.model.dto.ApiExceptionDto;
import by.innowise.internship.gateway.model.dto.TokenResponseDto;
import by.innowise.internship.gateway.model.dto.UserRegistrationDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static by.innowise.internship.gateway.config.InternalServiceProperties.AUTH_SERVICE_PROPERTY;
import static by.innowise.internship.gateway.config.InternalServiceProperties.USER_SERVICE_PROPERTY;

@RestController
@Slf4j
@RequiredArgsConstructor
public class UserRegistrationConroller {

    public static final String JWT_PREFIX = "Bearer ";
    private final WebClient webClient;
    private final InternalServiceProperties serviceProperty;
    private final ObjectMapper objectMapper;

    @PostMapping("/api/v1/users/register")
    public Mono<ResponseEntity<Object>> register(@RequestBody UserRegistrationDto registrationDto) {

        String authServiceUri = serviceProperty.getServices().get(AUTH_SERVICE_PROPERTY) + "/auth/register";

        return createAuthUser(registrationDto, authServiceUri)
                .flatMap(
                        tokenResponse -> createUserProfile(registrationDto, tokenResponse)
                )
                .onErrorResume(ApiApplicationException.class, err ->
                        Mono.just(ResponseEntity.status(err.getHttpStatus())
                                                .body(err.getErrorDto())
                        )
                )
                .onErrorResume(Exception.class, err ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                .body(err.getMessage())
                        )
                );
    }

    private Mono<TokenResponseDto> createAuthUser(UserRegistrationDto registrationDto, String authUri) {
        return webClient.post()
                        .uri(authUri)
                        .bodyValue(UserRegistrationDto.AuthUserDto.from(
                                registrationDto))
                        .retrieve()
                        .bodyToMono(TokenResponseDto.class);
    }

    private Mono<ResponseEntity<Object>> createUserProfile(UserRegistrationDto registrationDto,
                                                           TokenResponseDto tokenResponse) {
        String userServiceUri = serviceProperty.getServices().get(USER_SERVICE_PROPERTY) + "/users";
        return webClient.post()
                        .uri(userServiceUri)
                        .header(HttpHeaders.AUTHORIZATION, toJwtHeader(tokenResponse))
                        .bodyValue(UserRegistrationDto.UserServiceDto.from(registrationDto))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, clientResponse ->
                                clientResponse.bodyToMono(String.class)
                                              .flatMap(rawBody ->
                                                               rollbackAuthUser(toJwtHeader(tokenResponse))
                                                                       .then(generateMonoError(rawBody))
                                              )
                        )
                        .toEntity(String.class)
                        .map(resp -> ResponseEntity.ok()
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .body(resp.getBody())
                        );
    }

    private Mono<Void> rollbackAuthUser(String jwtAccessToken) {
        String authServiceUri = serviceProperty.getServices().get(AUTH_SERVICE_PROPERTY) + "/auth/remove";

        return webClient.delete()
                        .uri(authServiceUri)
                        .header(HttpHeaders.AUTHORIZATION, jwtAccessToken)
                        .retrieve()
                        .toBodilessEntity()
                        .then()
                        .doOnSuccess(empty -> log.info("Rollback successful. Auth user was deleted."))
                        .doOnError(err -> log.error("Failed to rollback auth user", err));
    }

    private Mono<Throwable> generateMonoError(String rawBody) {
        ApiExceptionDto apiExceptionDto = parseClientErrorDto(rawBody);
        return Mono.error(new ApiApplicationException("Failed to save user to userService",
                                                      HttpStatus.valueOf(apiExceptionDto.getCode()),
                                                      apiExceptionDto));
    }

    private ApiExceptionDto parseClientErrorDto(String rawJson) {

        try {
            return parseToApiExceptionDto(rawJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse a raw json {} to ApiExceptionDto", rawJson);
        }

        log.warn("Unknown exception dto type: {}. Parse to default", rawJson);
        return getDefaultApiException();
    }

    private ApiExceptionDto parseToApiExceptionDto(String rawJson) throws JsonProcessingException {
        ApiExceptionDto apiExceptionDto = objectMapper.readValue(rawJson, ApiExceptionDto.class);
        if (apiExceptionDto.getErrors() == null) {
            apiExceptionDto.setErrors(Collections.emptyList());
        }
        return apiExceptionDto;
    }


    private ApiExceptionDto getDefaultApiException() {
        return ApiExceptionDto.builder()
                              .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                              .message("Internal server error")
                              .errors(Collections.emptyList())
                              .build();
    }

    private String toJwtHeader(TokenResponseDto tokenResponse) {
        return JWT_PREFIX + tokenResponse.accessToken();
    }
}

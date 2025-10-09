package by.innowise.internship.gateway.model.dto;

public record TokenResponseDto(
        String accessToken,
        String refreshToken
) {
}

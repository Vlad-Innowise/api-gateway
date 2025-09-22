package by.innowise.internship.gateway.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDto {

    private String name;

    private String surname;

    @JsonProperty("birth_date")
    private String birthDate;

    private String email;

    private String password;

    private String role;

    @Getter
    @EqualsAndHashCode
    @ToString
    @AllArgsConstructor
    public static class AuthUserDto {

        private String email;

        private String password;

        private String role;

        public static AuthUserDto from(UserRegistrationDto dto) {
            return new AuthUserDto(dto.email, dto.password, dto.role);
        }
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    @AllArgsConstructor
    public static class UserServiceDto {

        private String name;

        private String surname;

        @JsonProperty("birth_date")
        private String birthDate;

        private String email;

        public static UserServiceDto from(UserRegistrationDto dto) {
            return new UserServiceDto(dto.name, dto.surname, dto.birthDate, dto.email);
        }
    }

}

package by.innowise.internship.gateway.exception;

import by.innowise.common.library.exception.ApplicationException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

public class ApiApplicationException extends ApplicationException {

    @Getter
    private Object errorDto;

    public ApiApplicationException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }

    public ApiApplicationException(String message, HttpStatus httpStatus, Object errorDto) {
        this(message, httpStatus);
        this.errorDto = errorDto;
    }
}

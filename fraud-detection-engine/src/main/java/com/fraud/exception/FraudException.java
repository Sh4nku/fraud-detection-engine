package com.fraud.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FraudException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public FraudException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}

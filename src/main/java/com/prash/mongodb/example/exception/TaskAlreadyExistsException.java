package com.prash.mongodb.example.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TaskAlreadyExistsException extends RuntimeException{

    public TaskAlreadyExistsException(String message) {
        super(message);
    }
}

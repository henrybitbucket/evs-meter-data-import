package com.pa.evs.exception;


import org.springframework.security.access.AccessDeniedException;

import com.pa.evs.constant.ValueConstant;
import com.pa.evs.dto.ApiErrorDto;
import com.pa.evs.exception.customException.DuplicateUserException;
import com.pa.evs.exception.customException.InvalidNodeTypeException;
import com.pa.evs.exception.customException.NotFoundWorkflowException;
import com.pa.evs.exception.customException.SlackException;
import com.pa.evs.exception.customException.UserNotFoundException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAuthenticationException (AccessDeniedException ex) {
        return  new ResponseEntity<>(ApiErrorDto.builder()
                .reason(ex.getMessage())
                .status(HttpStatus.UNAUTHORIZED)
                .success(ValueConstant.FALSE)
                .build(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DuplicateUserException.class)
    protected ResponseEntity<Object> handleDuplicateUserException (DuplicateUserException ex) {
        return  new ResponseEntity<>(ApiErrorDto.builder()
                .reason(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST)
                .success(ValueConstant.FALSE)
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    protected ResponseEntity<Object> handleUserNotFoundException (UserNotFoundException ex) {
        return  new ResponseEntity<>(
                ApiErrorDto.builder()
                .reason(ex.getMessage())
                .status(HttpStatus.UNAUTHORIZED)
                .success(ValueConstant.FALSE)
                .build(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NotFoundWorkflowException.class)
    protected ResponseEntity<Object> handleNotFoundWorkflowException (NotFoundWorkflowException ex) {
        return  new ResponseEntity<>(
                ApiErrorDto.builder()
                        .reason(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST)
                        .success(ValueConstant.FALSE)
                        .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidNodeTypeException.class)
    protected ResponseEntity<Object> handleInvalidNodeTypeException (InvalidNodeTypeException ex) {
        return  new ResponseEntity<>(
                ApiErrorDto.builder()
                        .reason(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST)
                        .success(ValueConstant.FALSE)
                        .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SlackException.class)
    protected ResponseEntity<Object> handleSlackException (SlackException ex) {
        return new ResponseEntity<>(
                ApiErrorDto.builder()
                        .reason(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST)
                        .success(ValueConstant.FALSE)
                        .build(), HttpStatus.BAD_REQUEST);
    }
}

package com.campushub.common.exception;

import com.campushub.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException exception) {
        return ApiResponse.fail(404, "资源不存在");
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleBadRequest(Exception exception) {
        String msg;
        if (exception instanceof MethodArgumentNotValidException ex) {
            msg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        } else if (exception instanceof BindException ex) {
            msg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        } else {
            msg = ApiCode.BAD_REQUEST.getMessage();
        }
        return ApiResponse.fail(ApiCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleMessageNotReadable() {
        return ApiResponse.fail(ApiCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("未捕获的异常: {}", exception.getMessage(), exception);
        return ApiResponse.fail(ApiCode.INTERNAL_ERROR.getCode(), ApiCode.INTERNAL_ERROR.getMessage());
    }
}

package com.myledger.api.exception;

import com.myledger.api.model.dto.response.ApiResponse;
import com.nfwork.dbfound.exception.CollisionException;
import com.nfwork.dbfound.exception.DBFoundPackageException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

/**
 * Spring MVC 接口的统一异常出口。
 * dbfound 的 {@code *.query/*.execute} 请求由 starter 内部处理，这里主要覆盖 {@code /api/**} 控制器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode status = exception.getStatusCode();
        String message = getResponseStatusMessage(exception);
        logByStatus(status, exception, request, message);
        return fail(status, message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception, HttpServletRequest request) {
        String message = "请求参数错误";
        log.info("{}: {}, request url: {}", exception.getClass().getName(), exception.getMessage(), request.getRequestURI());
        return fail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String message = "请求方法不支持";
        log.info("{}: {}, request url: {}", exception.getClass().getName(), exception.getMessage(), request.getRequestURI());
        return fail(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    @ExceptionHandler(DBFoundPackageException.class)
    public ResponseEntity<ApiResponse<Void>> handleDbfoundPackage(
            DBFoundPackageException exception,
            HttpServletRequest request
    ) {
        Exception unwrapped = unwrapDbfoundPackage(exception);
        return handleExceptionLikeDbfound(unwrapped, request);
    }

    @ExceptionHandler(CollisionException.class)
    public ResponseEntity<ApiResponse<Void>> handleCollision(CollisionException exception, HttpServletRequest request) {
        return handleExceptionLikeDbfound(exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return handleExceptionLikeDbfound(exception, request);
    }

    private ResponseEntity<ApiResponse<Void>> handleExceptionLikeDbfound(Exception exception, HttpServletRequest request) {
        if (exception instanceof CollisionException) {
            String message = getMessageOrDefault(exception, "请求冲突");
            log.info("{}: {}", exception.getClass().getName(), message);
            return fail(HttpStatus.FORBIDDEN, message);
        }

        String message = getServerErrorMessage(exception);
        log.error(
                "an exception: {} caused, when request url: {}",
                exception.getClass().getName(),
                request.getRequestURI(),
                exception
        );
        return fail(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    private static Exception unwrapDbfoundPackage(DBFoundPackageException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof Exception e) {
            return e;
        }
        return exception;
    }

    private static String getResponseStatusMessage(ResponseStatusException exception) {
        if (StringUtils.hasText(exception.getReason())) {
            return exception.getReason();
        }
        String detail = exception.getBody().getDetail();
        return StringUtils.hasText(detail) ? detail : "请求失败";
    }

    private static String getServerErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (exception.getCause() instanceof SQLException) {
            message = exception.getCause().getMessage();
        }
        return exception.getClass().getName() + ": " + getMessageOrDefault(message, "服务器内部错误");
    }

    private static String getMessageOrDefault(Exception exception, String defaultMessage) {
        return getMessageOrDefault(exception.getMessage(), defaultMessage);
    }

    private static String getMessageOrDefault(String message, String defaultMessage) {
        return StringUtils.hasText(message) ? message : defaultMessage;
    }

    private static void logByStatus(
            HttpStatusCode status,
            Exception exception,
            HttpServletRequest request,
            String message
    ) {
        if (status.is5xxServerError()) {
            log.error(
                    "an exception: {} caused, when request url: {}",
                    exception.getClass().getName(),
                    request.getRequestURI(),
                    exception
            );
            return;
        }
        log.info("{}: {}, request url: {}", exception.getClass().getName(), message, request.getRequestURI());
    }

    private static ResponseEntity<ApiResponse<Void>> fail(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(message));
    }
}

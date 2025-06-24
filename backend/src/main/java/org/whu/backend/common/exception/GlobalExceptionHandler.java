package org.whu.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.whu.backend.common.Result;

import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理自定义的业务异常 BizException
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 业务异常通常返回HTTP 200，具体错误通过Result的code和message体现
    public Result<?> handleBizException(BizException e) {
        // 对于业务异常，通常不需要打印完整的堆栈，除非错误码指示了严重问题或用于调试
        log.warn("业务异常发生: [Code: {}, Message: {}]", e.getCode(), e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    // 专门处理 MissingServletRequestParameterException (HTTP 400 Bad Request) ---
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String parameterName = e.getParameterName();
        String parameterType = e.getParameterType();
        String requestPath = request.getRequestURI();

        String message = String.format("请求处理失败：必需的请求参数 '%s' (类型: %s) 未提供。", parameterName, parameterType);

        log.warn("必需的请求参数缺失 (400) for path: {} from client: {}. Parameter name: '{}', Type: '{}'. Details: {}",
                requestPath, request.getRemoteAddr(), parameterName, parameterType, e.getMessage());

        return Result.failure(HttpStatus.BAD_REQUEST.value(), message);
    }

    // 专门处理 HttpMessageNotReadableException (HTTP 400 Bad Request) ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        // 尝试获取更具体的根本原因，特别是JSON解析相关的错误
        Throwable cause = e.getCause();
        log.warn("请求体JSON解析失败 (400) for path: {} from client: {}. Jackson Error: {}",
                requestPath, request.getRemoteAddr(), cause.getMessage()); // 日志记录更具体的Jackson错误
        // 给前端一个相对友好的提示
        String userMessage = "请检查请求数据的格式或内容是否正确。";
        return Result.failure(HttpStatus.BAD_REQUEST.value(), userMessage);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<?>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String actualMethod = e.getMethod();
        Set<String> supportedMethods = e.getSupportedHttpMethods() != null ? e.getSupportedHttpMethods().stream().map(Objects::toString).collect(Collectors.toSet()) : null;
        String message = String.format("请求方法 '%s' 不被支持。路径 '%s' 支持的方法有: %s", actualMethod, requestUrl, supportedMethods);
        log.warn("不支持的HTTP方法 (405): {} {} - Supported: {}", actualMethod, requestUrl, supportedMethods);
        return new ResponseEntity<>(Result.failure(HttpStatus.METHOD_NOT_ALLOWED.value(), message), HttpStatus.METHOD_NOT_ALLOWED);
    }

    // 专门处理 NoResourceFoundException (404 Not Found) ---
    @ExceptionHandler(NoResourceFoundException.class)
    // @ResponseStatus(HttpStatus.NOT_FOUND) // 如果用ResponseEntity，这个可以省略
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        // 对于404错误，通常记录为WARN级别，并包含请求的URL和方法，一般不需要完整堆栈
        log.warn("请求的资源未找到 (404): {} {} (Referer: {})",
                request.getMethod(),
                requestUrl,
                request.getHeader("Referer")); // 记录访问来源，有助于分析
        return Result.failure(HttpStatus.NOT_FOUND.value(), "您访问的页面或资源不存在");
    }

    // 专门处理 HttpMediaTypeNotSupportedException (415 Unsupported Media Type) ---
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<?> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        MediaType contentType = e.getContentType();

        String message = String.format("请求的内容类型 '%s' 不被支持",
                contentType != null ? contentType.toString() : "未知/未提供");

        log.warn("不支持的内容类型 (415): {} {} - Received: {}",
                request.getMethod(),
                requestUrl,
                contentType != null ? contentType.toString() : "N/A"); // 可以记录异常本身，但不一定需要完整堆栈给前端

        return Result.failure(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), message);
    }

    // 处理 @Valid 注解参数校验失败的异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 参数校验失败，返回HTTP 400
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 从异常中获取第一个校验错误信息作为提示
        String firstErrorMessage = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        log.warn("请求参数校验失败: {}", firstErrorMessage, e); // 记录更详细的异常信息用于调试
        return Result.failure(HttpStatus.BAD_REQUEST.value(), "参数校验失败");
    }

    // 处理其他常见的运行时异常 (可以根据需要细化)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage(), e);
        return Result.failure(HttpStatus.BAD_REQUEST.value(), "请求参数不合法: " + e.getMessage());
    }

    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleDateTimeParseException(DateTimeParseException e) {
        log.warn("日期时间格式解析错误: {}", e.getMessage(), e);
        return Result.failure(HttpStatus.BAD_REQUEST.value(), "日期时间格式无效，期望格式：yyyy-MM-dd HH:mm:ss");
    }

    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleNumberFormatException(NumberFormatException e) {
        log.warn("数字格式转换错误: {}", e.getMessage(), e);
        return Result.failure(HttpStatus.BAD_REQUEST.value(), "提供的数字格式无效");
    }

    // --- JWT 相关异常处理 ---
//    @ExceptionHandler(ExpiredJwtException.class)
//    @ResponseStatus(HttpStatus.UNAUTHORIZED) // Token过期，返回HTTP 401
//    public Result<?> handleExpiredJwtException(ExpiredJwtException e) {
//        logger.warn("JWT已过期: {}", e.getMessage()); // 可以不打印完整堆栈，因为这是预期内的客户端错误
//        return Result.failure(HttpStatus.UNAUTHORIZED.value(), "登录已过期，请重新登录");
//    }
//
//    @ExceptionHandler(JwtException.class) // 捕获其他JWT相关异常，如签名错误、格式错误等
//    @ResponseStatus(HttpStatus.UNAUTHORIZED) // 无效Token，返回HTTP 401
//    public Result<?> handleJwtException(JwtException e) {
//        logger.warn("无效的JWT: {}", e.getMessage());
//        return Result.failure(HttpStatus.UNAUTHORIZED.value(), "无效的身份认证信息");
//    }

    // --- 通用运行时异常处理器 (作为最后的防线) ---
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 未知运行时异常，返回HTTP 500
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("未捕获的运行时异常，系统内部错误: ", e); // 记录完整堆栈，方便排查
        return Result.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器繁忙，请稍后再试");
    }

    // 如果需要，可以添加处理 Exception.class 的方法，但通常 RuntimeException 已经能覆盖大部分情况
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("未处理的顶层异常: ", e);
        return Result.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器繁忙，请稍后再试");
    }

}



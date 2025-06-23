package org.whu.backend.common; // 或者你项目的通用配置包

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "org.whu.fleetingtime") // 指定扫描的包，确保只处理你项目下的Result返回
public class ResultHttpStatusAdvice implements ResponseBodyAdvice<Result<?>> {

    private static final Logger logger = LoggerFactory.getLogger(ResultHttpStatusAdvice.class);

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 判断是否是对Result<?>类型的返回值进行处理
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Result<?> beforeBodyWrite(Result<?> body, MethodParameter returnType, MediaType selectedContentType,
                                     Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                     ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null && body.getCode() != null) {
            HttpStatus status = HttpStatus.resolve(body.getCode()); // 尝试将Result的code解析为HttpStatus
            if (status != null) {
                // 如果解析成功，并且当前的HTTP状态码还是默认的200 (或者你想强制覆盖)
                // 我们需要获取底层的 HttpServletResponse 来设置状态码，因为 ServerHttpResponse 的 setStatusCode 在某些情况下可能不立即生效或被覆盖
                if (response instanceof ServletServerHttpResponse) {
                    HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
                    if (servletResponse.getStatus() == HttpStatus.OK.value() || body.getCode() != HttpStatus.OK.value()) {
                        // 只有当原始状态是200，或者Result的code不是200时，才尝试设置新的状态码
                        // 这样可以避免覆盖由@ResponseStatus或ResponseEntity已经设置的更具体的错误状态
//                        logger.debug("ResultHttpStatusAdvice: Setting HTTP status to {} based on Result.code {}", status.value(), body.getCode());
                        response.setStatusCode(status); // 设置HTTP状态码
                    }
                }
            } else {
                logger.warn("ResultHttpStatusAdvice: Result.code {} is not a standard HTTP status code. HTTP status will likely remain as default (e.g., 200 OK).", body.getCode());
            }
        }
        return body; // 返回原始的body，由HttpMessageConverter写入
    }
}
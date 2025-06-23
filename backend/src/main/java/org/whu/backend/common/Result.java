package org.whu.backend.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Result<T> {

    private final Integer code; // 状态码，设为final，通过构造函数初始化
    private final String message; // 提示信息，设为final
    private final T data; // 实际数据，设为final

    // 构造函数设为private，强制通过静态工厂方法创建，保证一致性
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // --- 成功返回的静态工厂方法 ---
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null); // 统一默认成功消息
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data); // 统一默认成功消息
    }

    public static <T> Result<T> success(String message, T data) {
        if (message == null || message.trim().isEmpty()) {
            message = "操作成功";
        }
        return new Result<>(200, message, data);
    }

    // --- 失败返回的静态工厂方法 ---
    public static <T> Result<T> failure(String message) {
        return new Result<>(400, message, null); // 默认使用400作为客户端错误
    }

    public static <T> Result<T> failure(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    // 这个方法允许在失败时也返回一些数据，某些场景下可能有用，但通常失败时不带data
     public static <T> Result<T> failure(Integer code, String message, T data) {
         return new Result<>(code, message, data);
     }
}
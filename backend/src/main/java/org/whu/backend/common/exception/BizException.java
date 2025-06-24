package org.whu.backend.common.exception;

public class BizException extends RuntimeException {

    private int code;

    public BizException(String message) {
        super(message);
        this.code = 400; // 默认业务异常状态码
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }

    public int getCode() {
        return code;
    }
}


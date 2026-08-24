package com.zsh.zshpicturebackend.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException{

    /**
     * 状态码
     */
    private final int code;

    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code=errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message){
        super(message);
        this.code= errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause){
        super(message,cause);
        this.code= errorCode.getCode();
    }
}

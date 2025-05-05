package com.trxy.exception;

public interface BaseException {
    // 获取异常码
    String getErrorCode();
    // 获取异常信息
    String getErrorMessage();
}

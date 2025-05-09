package com.trxy.enums;

import com.trxy.exception.BaseException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseException {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("OSS-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("OSS-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    FILE_SIZE_IS_NULL("OSS-20001","文件大小不能为空"),
    FILE_UPLOAD_FAIL("OSS-20002","文件上传失败")
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}


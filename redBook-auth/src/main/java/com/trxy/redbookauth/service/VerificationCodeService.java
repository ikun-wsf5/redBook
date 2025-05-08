package com.trxy.redbookauth.service;

import com.trxy.redbookauth.entity.vo.SendVerificationCodeReqVO;
import com.trxy.response.Response;

public interface VerificationCodeService {
    /**
     * 发送短信验证码
     *
     * @param sendVerificationCodeReqVO
     * @return
     */
    void send(SendVerificationCodeReqVO sendVerificationCodeReqVO);
}

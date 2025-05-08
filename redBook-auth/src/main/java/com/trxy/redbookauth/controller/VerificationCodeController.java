package com.trxy.redbookauth.controller;

import com.trxy.aspect.ApiOperationLog;
import com.trxy.redbookauth.entity.vo.SendVerificationCodeReqVO;
import com.trxy.redbookauth.service.VerificationCodeService;
import com.trxy.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class VerificationCodeController {

    @Resource
    private VerificationCodeService verificationCodeService;

    @PostMapping("/verification/code/send")
    @ApiOperationLog(description = "发送短信验证码")
    public Response<?> send(@Validated @RequestBody SendVerificationCodeReqVO sendVerificationCodeReqVO) {
        verificationCodeService.send(sendVerificationCodeReqVO);
        return Response.success();
    }
}

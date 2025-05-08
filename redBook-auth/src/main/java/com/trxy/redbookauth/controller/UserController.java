package com.trxy.redbookauth.controller;

import com.trxy.aspect.ApiOperationLog;
import com.trxy.redbookauth.entity.vo.UpdatePasswordReqVO;
import com.trxy.redbookauth.entity.vo.UserLoginReqVO;
import com.trxy.redbookauth.service.UserService;
import com.trxy.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;


    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录/注册")
    public Response<String> loginAndRegister(@Validated @RequestBody UserLoginReqVO userLoginReqVO) {
        return Response.success(userService.loginAndRegister(userLoginReqVO));
    }
    @PostMapping("/logout")
    @ApiOperationLog(description = "账号登出")
    public Response<?> logout() {
        userService.logout();
        return Response.success();
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "修改密码")
    public Response<?> updatePassword(@Validated @RequestBody UpdatePasswordReqVO updatePasswordReqVO) {
        userService.updatePassword(updatePasswordReqVO);
        return Response.success();

    }
}
package com.trxy.redbookauth.service;

import com.trxy.redbookauth.entity.vo.UpdatePasswordReqVO;
import com.trxy.redbookauth.entity.vo.UserLoginReqVO;
import com.trxy.response.Response;

public interface UserService {
    String loginAndRegister(UserLoginReqVO userLoginReqVO);

    void logout();

    void updatePassword(UpdatePasswordReqVO updatePasswordReqVO);
}

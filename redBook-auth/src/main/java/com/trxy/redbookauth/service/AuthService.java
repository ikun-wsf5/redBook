package com.trxy.redbookauth.service;

import com.trxy.redbookauth.entity.vo.UpdatePasswordReqVO;
import com.trxy.redbookauth.entity.vo.UserLoginReqVO;

public interface AuthService {
    String loginAndRegister(UserLoginReqVO userLoginReqVO);

    void logout();

    void updatePassword(UpdatePasswordReqVO updatePasswordReqVO);
}

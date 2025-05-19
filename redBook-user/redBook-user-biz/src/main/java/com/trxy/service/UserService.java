package com.trxy.service;

import com.trxy.entity.dto.*;
import com.trxy.entity.vo.UpdateUserInfoReqVO;
import com.trxy.response.Response;

import java.util.List;

public interface UserService {
    void updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

    Response register(RegisterUserReqDTO registerUserReqDTO);

    FindUserByPhoneRspDTO findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    void updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    FindUserByIdRspDTO findById(FindUserByIdReqDTO findUserByIdReqDTO);

    List<FindUserByIdRspDTO> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO);
}

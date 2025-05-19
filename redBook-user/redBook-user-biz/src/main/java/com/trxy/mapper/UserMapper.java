package com.trxy.mapper;


import com.trxy.entity.dto.UserDO;
import feign.Param;

import java.util.List;


public interface UserMapper {
    int insert(UserDO userDO);
    int update(UserDO userDO);
    int delete(Long id);
    UserDO selectById(Long id);

    /**
     * 根据手机号查询是否注册过
     * @param phone
     * @return
     */
    UserDO selectByPhone(String phone);

    /**
     * 批量查询用户信息
     *
     * @param ids
     * @return
     */
    List<UserDO> selectByIds(@Param("ids") List<Long> ids);
}
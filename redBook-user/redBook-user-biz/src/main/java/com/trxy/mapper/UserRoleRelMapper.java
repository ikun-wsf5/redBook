package com.trxy.mapper;

import com.trxy.entity.dto.UserRoleRelDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleRelMapper {
    int insert(UserRoleRelDO userRoleRelDO);
    int update(UserRoleRelDO userRoleRelDO);
    int delete(Long id);
    UserRoleRelDO selectById(Long id);
}
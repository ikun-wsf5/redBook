package com.trxy.mapper;


import com.trxy.entity.dto.RolePermissionRelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RolePermissionRelMapper {
    int insert(RolePermissionRelDO rolePermissionRelDO);
    int update(RolePermissionRelDO rolePermissionRelDO);
    int delete(Long id);
    RolePermissionRelDO selectById(Long id);

    List<RolePermissionRelDO> selectByRoleIds(List<Long> roleIds);
}
package com.trxy.mapper;


import com.trxy.entity.dto.RoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RoleMapper {
    int insert(RoleDO roleDO);
    int update(RoleDO roleDO);
    int delete(Long id);
    RoleDO selectById(Long id);

    /**
     * 查询所有被启用的角色
     * @return
     */
    List<RoleDO> selectEnabledList();



}
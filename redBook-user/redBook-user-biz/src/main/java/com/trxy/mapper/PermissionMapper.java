package com.trxy.mapper;


import com.trxy.entity.dto.PermissionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PermissionMapper {
    int insert(PermissionDO permissionDO);
    int update(PermissionDO permissionDO);
    int delete(Long id);
    PermissionDO selectById(Long id);

    List<PermissionDO> selectAppEnabledList();
}
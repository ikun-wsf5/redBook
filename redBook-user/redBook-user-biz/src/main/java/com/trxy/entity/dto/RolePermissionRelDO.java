package com.trxy.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RolePermissionRelDO {
    private Long id;
    private Long roleId;
    private Long permissionId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isDeleted;
}
package com.trxy.entity.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoleDO {
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer status;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isDeleted;
}
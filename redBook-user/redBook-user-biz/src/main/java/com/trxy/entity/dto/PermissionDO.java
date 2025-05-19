package com.trxy.entity.dto;

import lombok.Data;

import java.util.Date;

@Data
public class PermissionDO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer type;
    private String menuUrl;
    private String menuIcon;
    private Integer sort;
    private String permissionKey;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private Boolean isDeleted;
}
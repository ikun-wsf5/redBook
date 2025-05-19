package com.trxy.entity.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户与关注数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Entry {
    private Long id;
    private Integer count;
}

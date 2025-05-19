package com.trxy.runner;


import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.trxy.constants.RedisKeyConstants;
import com.trxy.entity.dto.PermissionDO;
import com.trxy.entity.dto.RoleDO;
import com.trxy.entity.dto.RolePermissionRelDO;
import com.trxy.mapper.PermissionMapper;
import com.trxy.mapper.RoleMapper;
import com.trxy.mapper.RolePermissionRelMapper;
import com.trxy.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Component
@Slf4j
public class PushRolePermissions2RedisRunner implements ApplicationRunner {


    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private RolePermissionRelMapper rolePermissionRelMapper;
    @Resource
    private PermissionMapper permissionMapper;

    /**
     * 将角色对应的权限放入Redis 中
     * key roleId  value 权限集合
     * @param args
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("==> 服务启动，开始同步角色权限数据到 Redis 中...");

        try {
            // 是否能够同步数据: 原子操作，只有在键 PUSH_PERMISSION_FLAG 不存在时，才会设置该键的值为 "1"，并设置过期时间为 1 天
            boolean canPushed = redisTemplate.opsForValue().setIfAbsent(RedisKeyConstants.PUSH_PERMISSION_FLAG, "1", 1, TimeUnit.DAYS);
            if (!canPushed) {
                log.info("==> 服务启动，角色权限数据已经同步到 Redis 中，无需再次同步...");
                return;
            }
            // 查询出所有角色
            List<RoleDO> roleDOS = roleMapper.selectEnabledList();

            if (CollUtil.isNotEmpty(roleDOS)) {
                // 拿到所有角色的 ID
                List<Long> roleIds = roleDOS.stream().map(RoleDO::getId).toList();

                // 根据角色 ID, 批量查询出所有角色对应的权限
                List<RolePermissionRelDO> rolePermissionDOS = rolePermissionRelMapper.selectByRoleIds(roleIds);
                // 按角色 ID 分组, 每个角色 ID 对应多个权限 ID
                Map<Long, List<Long>> roleIdPermissionIdsMap = rolePermissionDOS.stream().collect(
                        Collectors.groupingBy(RolePermissionRelDO::getRoleId,
                                Collectors.mapping(RolePermissionRelDO::getPermissionId, Collectors.toList()))
                );

                // 查询 APP 端所有被启用的权限
                List<PermissionDO> permissionDOS = permissionMapper.selectAppEnabledList();
                // 权限 ID - 权限 DO
                Map<Long, PermissionDO> permissionIdDOMap = permissionDOS.stream().collect(
                        Collectors.toMap(PermissionDO::getId, permissionDO -> permissionDO)
                );

                // 组织 角色ID-权限 关系
                Map<String, List<String>> roleKeyPermissionMap = Maps.newHashMap();

                // 循环所有角色
                roleDOS.forEach(roleDO -> {
                    // 当前角色 ID
                    Long roleId = roleDO.getId();
                    //当前角色的roleKey
                    String roleKey = roleDO.getRoleKey();
                    // 当前角色 ID 对应的权限 ID 集合
                    List<Long> permissionIds = roleIdPermissionIdsMap.get(roleId);
                    if (CollUtil.isNotEmpty(permissionIds)) {
                        List<String> permissionKeys = Lists.newArrayList();
                        permissionIds.forEach(permissionId -> {
                            // 根据权限 ID 获取具体的权限 DO 对象
                            PermissionDO permissionDO = permissionIdDOMap.get(permissionId);
                            permissionKeys.add(permissionDO.getPermissionKey());
                        });
                        roleKeyPermissionMap.put(roleKey, permissionKeys);
                    }
                });

                // 同步至 Redis 中，方便后续网关查询鉴权使用
                roleKeyPermissionMap.forEach((roleKey, permissions) -> {
                    String key = RedisKeyConstants.buildRolePermissionsKey(roleKey);
                    redisTemplate.opsForValue().set(key, JsonUtils.toJsonString(permissions));
                });
            }
            log.info("==> 服务启动，成功同步角色权限数据到 Redis 中...");
        } catch (Exception e) {
            log.error("==> 同步角色权限数据到 Redis 中失败: ", e);
        }

        log.info("==> 服务启动，成功同步角色权限数据到 Redis 中...");
    }
}

package com.trxy.redbookauth.constant;

public class RedisKeyConstants {

    /**
     * 验证码 KEY 前缀
     */
    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";
    /**
     * 小书全局 ID 生成器 KEY
     */
    public static final String REDBOOK_ID_GENERATOR_KEY = "red_book_id_generator";
    /**
     * 用户角色数据 KEY 前缀
     */
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";
    /**
     * 角色对应的权限集合 KEY 前缀
     */
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";

    /**
     * 角色权限同步锁
     */
    public static final String PUSH_PERMISSION_FLAG = "role:permissions:lock";

    /**
     * 构建角色对应的权限集合 KEY
     * @param roleKey
     * @return
     */
    public static String buildRolePermissionsKey(String roleKey) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleKey;
    }

    /**
     * 构建验证码 KEY
     * @param phone
     * @return
     */
    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

    /**
     * 构建用户对应的角色 KEY
     * @param roleId
     * @return
     */
    public static String buildUserRoleKey(Long roleId) {
        return USER_ROLES_KEY_PREFIX + roleId;
    }
}
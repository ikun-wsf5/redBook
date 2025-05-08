package com.trxy.redbookauth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.trxy.enums.DeletedEnum;
import com.trxy.enums.StatusEnum;
import com.trxy.exception.BaseException;
import com.trxy.exception.BusinessException;
import com.trxy.holder.LoginUserContextHolder;
import com.trxy.redbookauth.constant.RedisKeyConstants;
import com.trxy.redbookauth.constant.RoleConstants;
import com.trxy.redbookauth.entity.dto.RoleDO;
import com.trxy.redbookauth.entity.dto.UserDO;
import com.trxy.redbookauth.entity.dto.UserRoleRelDO;
import com.trxy.redbookauth.entity.vo.UpdatePasswordReqVO;
import com.trxy.redbookauth.entity.vo.UserLoginReqVO;
import com.trxy.redbookauth.enums.LoginTypeEnum;
import com.trxy.redbookauth.enums.ResponseCodeEnum;
import com.trxy.redbookauth.mapper.RoleMapper;
import com.trxy.redbookauth.mapper.UserMapper;
import com.trxy.redbookauth.mapper.UserRoleRelMapper;
import com.trxy.redbookauth.service.UserService;
import com.trxy.response.Response;
import com.trxy.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleRelMapper userRoleRelMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 登录注册
     * @param userLoginReqVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String loginAndRegister(UserLoginReqVO userLoginReqVO) {
        Integer type = userLoginReqVO.getType();
        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);//获取登录方式
        String phone = userLoginReqVO.getPhone();
        Long userId = null;

        switch (loginTypeEnum) {
            case VERIFICATION_CODE:
                // 验证码登录
                // 校验入参验证码是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(userLoginReqVO.getCode()), "验证码不能为空");
                String redisCodeKey = RedisKeyConstants.buildVerificationCodeKey(userLoginReqVO.getPhone());
                String code = (String) redisTemplate.opsForValue().get(redisCodeKey);
                if (!code.equals(userLoginReqVO.getCode())){
                    throw new BusinessException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }

                UserDO userDO = userMapper.selectByPhone(phone);
                log.info("用户信息：{}",userDO);
                if (userDO == null){
                    //系统自动注册
                    userId = registerUser(phone);
                }else {
                    userId = userDO.getId();
                }
                break;
            case PASSWORD:
                // 密码登录
                String password = userLoginReqVO.getPassword();
                // 根据手机号查询
                UserDO userDO1 = userMapper.selectByPhone(phone);

                // 判断该手机号是否注册
                if (Objects.isNull(userDO1)) {
                    throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                // 拿到密文密码
                String encodePassword = userDO1.getPassword();

                // 匹配密码是否一致
                boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

                // 如果不正确，则抛出业务异常，提示用户名或者密码不正确
                if (!isPasswordCorrect) {
                    throw new BusinessException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }
                userId = userDO1.getId();
                break;
            default:
                throw new BusinessException(null,"非法请求");
        }
        StpUtil.login(userId);
        String token = StpUtil.getTokenInfo().getTokenValue();
        log.info("==> 用户登录成功，token: {}", token);

        return token;
    }

    /**
     * 用户退出登录
     * @param
     */
    @Override
    public void logout() {
        Long userId = LoginUserContextHolder.getUserId();
        log.info("==> 用户退出登录，userId: {}", userId);
        StpUtil.logout(userId);
    }

    @Override
    public void updatePassword(UpdatePasswordReqVO updatePasswordReqVO) {
        // 新密码
        String newPassword = updatePasswordReqVO.getNewPassword();
        // 密码加密
        String encodePassword = passwordEncoder.encode(newPassword);

        // 获取当前请求对应的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(encodePassword)
                .updateTime(LocalDateTime.now())
                .build();
        // 更新密码
        userMapper.update(userDO);
    }


    /**
     * 系统自动注册用户
     * @param phone
     * @return
     */

    public Long registerUser(String phone) {

        return transactionTemplate.execute(status->{
            try {
                // 获取全局自增的小书 ID
                Long redBookId = redisTemplate.opsForValue().increment(RedisKeyConstants.REDBOOK_ID_GENERATOR_KEY);

                UserDO userDO = UserDO.builder()
                        .phone(phone)
                        .redBookId(String.valueOf(redBookId)) // 自动生成小红书号 ID
                        .nickname("小红薯" + redBookId) // 自动生成昵称, 如：小红薯10000
                        .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                        .build();

                // 添加入库
                userMapper.insert(userDO);

                // 获取刚刚添加入库的用户 ID
                Long userId = userDO.getId();

                // 给该用户分配一个默认角色
                UserRoleRelDO userRoleDO = UserRoleRelDO.builder()
                        .userId(userId)
                        .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .isDeleted(DeletedEnum.NO.getValue())
                        .build();
                userRoleRelMapper.insert(userRoleDO);

                RoleDO roleDO = roleMapper.selectById(RoleConstants.COMMON_USER_ROLE_ID);

                // 将该用户的角色 ID 存入 Redis 中，指定初始容量为 1，这样可以减少在扩容时的性能开销
                //key-> redBook.user.role.1
                //value->t_role.role_key
                List<String> roles = new ArrayList<>(1);
                roles.add(roleDO.getRoleKey());

                String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
                redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

                return userId;
            }catch (Exception e){
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("==> 系统注册用户异常: ", e);
                return null;
            }
        });
    }
}

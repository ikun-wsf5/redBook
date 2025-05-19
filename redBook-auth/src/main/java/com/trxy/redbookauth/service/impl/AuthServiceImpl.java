package com.trxy.redbookauth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.google.common.base.Preconditions;
import com.trxy.entity.dto.FindUserByPhoneRspDTO;
import com.trxy.exception.BusinessException;
import com.trxy.holder.LoginUserContextHolder;
import com.trxy.redbookauth.constant.RedisKeyConstants;
import com.trxy.redbookauth.entity.vo.UpdatePasswordReqVO;
import com.trxy.redbookauth.entity.vo.UserLoginReqVO;
import com.trxy.redbookauth.enums.LoginTypeEnum;
import com.trxy.redbookauth.enums.ResponseCodeEnum;
import com.trxy.redbookauth.rpc.UserRpcService;
import com.trxy.redbookauth.service.AuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;


@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private UserRpcService userRpcService;

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
                //RPC系统自动判断登录注册
                userRpcService.registerUser(phone);

                break;
            case PASSWORD:
                // 密码登录
                String password = userLoginReqVO.getPassword();
                // RPC: 调用用户服务，通过手机号查询用户
                FindUserByPhoneRspDTO findUserByPhoneRspDTO = userRpcService.findUserByPhone(phone);

                // 判断该手机号是否注册
                if (Objects.isNull(findUserByPhoneRspDTO)) {
                    throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
                }

                // 拿到密文密码
                String encodePassword = findUserByPhoneRspDTO.getPassword();

                // 匹配密码是否一致
                boolean isPasswordCorrect = passwordEncoder.matches(password, encodePassword);

                // 如果不正确，则抛出业务异常，提示用户名或者密码不正确
                if (!isPasswordCorrect) {
                    throw new BusinessException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }
                userId = findUserByPhoneRspDTO.getId();
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

        // RPC: 调用用户服务：更新密码
        userRpcService.updatePassword(encodePassword);
    }
}

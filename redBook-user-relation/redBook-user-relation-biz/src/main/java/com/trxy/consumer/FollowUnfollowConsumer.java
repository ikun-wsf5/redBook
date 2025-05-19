package com.trxy.consumer;

import com.alibaba.nacos.shaded.com.google.common.util.concurrent.RateLimiter;
import com.trxy.constants.MQConstants;
import com.trxy.constants.RedisKeyConstants;
import com.trxy.entity.dto.CountFollowUnfollowMqDTO;
import com.trxy.entity.dto.FansDO;
import com.trxy.entity.dto.FollowUserMqDTO;
import com.trxy.entity.dto.FollowingDO;
import com.trxy.enums.FollowUnfollowTypeEnum;
import com.trxy.mapper.FansMapper;
import com.trxy.mapper.FollowingMapper;
import com.trxy.util.DateUtils;
import com.trxy.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

@Component
@Slf4j
public class FollowUnfollowConsumer {

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private FollowingMapper followingMapper;
    @Resource
    private FansMapper fansMapper;
    @Resource
    private RateLimiter rateLimiter;
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;

    // 使用 @RabbitListener 注解绑定队列、交换机和路由键
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(                        // 定义队列
                            name = "follow.queue",             // 队列名称（与流程图中的 MQ 对应）
                            durable = "true"                   // 持久化
                    ),
                    exchange = @Exchange(                 // 定义 Direct 交换机
                            name = "follow.exchange",          // 交换机名称
                            type = ExchangeTypes.DIRECT,      // 交换机类型为 Direct
                            durable = "true"
                    ),
                    key = "follow.operation"             // 路由键（流程图中的 MQ 发送标识）
            )
    )
    /**
     * 收到mq消息，关注取关服务，同步数据库和缓存
     */
    public void handleFollowMessage(@Payload FollowUserMqDTO followUserMqDTO) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();
        // 判空
        if (Objects.isNull(followUserMqDTO)) return;
        log.info("消费者接收到消息：{}", followUserMqDTO);
        String operationType = followUserMqDTO.getOperationType();
        if (operationType.equals(MQConstants.TAG_FOLLOW)){
            // 关注操作
            handleFollowTagMessage(followUserMqDTO);
        }else if (operationType.equals(MQConstants.TAG_UNFOLLOW)) {
            // 取关操作
            handleUnfollowTagMessage(followUserMqDTO);
        }
    }

    /**
     * 取关消息
     * @param unfollowUserMqDTO
     */
    private void handleUnfollowTagMessage(FollowUserMqDTO unfollowUserMqDTO) {
        Long userId = unfollowUserMqDTO.getUserId();
        Long unfollowUserId = unfollowUserMqDTO.getFollowUserId();
        LocalDateTime createTime = unfollowUserMqDTO.getCreateTime();

        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 取关成功需要删除数据库两条记录
                // 关注表：一条记录
                int count = followingMapper.delete(
                        new FollowingDO().builder()
                                .userId(userId)
                                .followingUserId(unfollowUserId)
                                .build()
                );

                // 粉丝表：一条记录
                if (count > 0) {
                    fansMapper.delete(
                        new FansDO().builder()
                                .userId(unfollowUserId)
                                .fansUserId(userId)
                                .build()
                    );
                }
                return true;
            } catch (Exception ex) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("", ex);
            }
            return false;
        }));

        // 若数据库删除成功，更新 Redis，将自己从被取注用户的 ZSet 粉丝列表删除
        if (isSuccess) {
            // 被取关用户的粉丝列表 Redis Key
            String fansRedisKey = RedisKeyConstants.buildUserFansKey(unfollowUserId);
            // 删除指定粉丝
            redisTemplate.opsForZSet().remove(fansRedisKey, userId);

            // 发送 MQ 通知计数服务：统计关注数
            // 构建消息体 DTO
            CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = CountFollowUnfollowMqDTO.builder()
                    .userId(userId)
                    .targetUserId(unfollowUserId)
                    .type(FollowUnfollowTypeEnum.UNFOLLOW.getCode()) // 取关
                    .build();

            // 发送 MQ
            sendMQ(countFollowUnfollowMqDTO);

        }
    }

    /**
     * 关注操作
     * @param followUserMqDTO
     */
    private void handleFollowTagMessage(FollowUserMqDTO followUserMqDTO) {
        Long userId = followUserMqDTO.getUserId();
        Long followUserId = followUserMqDTO.getFollowUserId();
        LocalDateTime createTime = followUserMqDTO.getCreateTime();

        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 关注成功需往数据库添加两条记录
                // 关注表：一条记录
                int count = followingMapper.insert(FollowingDO.builder()
                        .userId(userId)
                        .followingUserId(followUserId)
                        .createTime(createTime)
                        .build());

                // 粉丝表：一条记录
                if (count > 0) {
                    fansMapper.insert(FansDO.builder()
                            .userId(followUserId)
                            .fansUserId(userId)
                            .createTime(createTime)
                            .build());
                }
                return true;
            } catch (Exception ex) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("", ex);
            }
            return false;
        }));

        log.info("## 数据库添加记录结果：{}", isSuccess);
        //  更新 Redis 中被关注用户的 ZSet 粉丝列表
        // 若数据库操作成功，更新 Redis 中被关注用户的 ZSet 粉丝列表
        if (isSuccess) {
            // Lua 脚本
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
            script.setResultType(Long.class);

            // 时间戳
            long timestamp = DateUtils.localDateTime2Timestamp(createTime);

            // 构建被关注用户的粉丝列表 Redis Key
            String fansRedisKey = RedisKeyConstants.buildUserFansKey(followUserId);
            // 执行脚本
            redisTemplate.execute(script, Collections.singletonList(fansRedisKey), userId, timestamp);

            // 发送 MQ 通知计数服务：统计关注数
            // 构建消息体 DTO
            CountFollowUnfollowMqDTO countFollowUnfollowMqDTO = CountFollowUnfollowMqDTO.builder()
                    .userId(userId)
                    .targetUserId(followUserId)
                    .type(FollowUnfollowTypeEnum.FOLLOW.getCode()) // 关注
                    .build();

            // 发送 MQ
            sendMQ(countFollowUnfollowMqDTO);
        }

    }

    /**
     * 发送mq消息，触发计数业务
     */
    private void sendMQ(CountFollowUnfollowMqDTO countFollowUnfollowMqDTO){
        //将countFollowUnfollowMqDTO对象封装进，message
        Message message = MessageBuilder
                .withBody(JsonUtils.toJsonString(countFollowUnfollowMqDTO).getBytes()) // 消息体为字节数组
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT) // 设置持久化
                .setContentType("application/json") // 声明内容类型为 JSON
                .build();

       //发送MQ消息
       rabbitTemplate.convertAndSend(
                "count.exchange",          // 交换机名称（与消费者绑定的一致）
                "",         // 路由键（与绑定键一致）
                message
       );

    }
}

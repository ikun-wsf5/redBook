package com.trxy.comsumer;

import com.trxy.constans.RedisKeyConstants;
import com.trxy.entity.dto.CountFollowUnfollowMqDTO;
import com.trxy.enums.FollowUnfollowTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 消费者,统计粉丝数
 * @author 29773
 */
@Component
@Slf4j
public class CountFollowingConsumer {

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(                        // 队列名称
                            name = "countFollows.queue",             // 队列名称（与流程图中的 MQ 对应）
                            durable = "true"                   // 持久化
                    ),
                    exchange = @Exchange(                 // 定义 Direct 交换机
                            name = "count.exchange",          // 交换机名称
                            type = ExchangeTypes.FANOUT,      // 交换机类型为 FANOUT
                            durable = "true"
                    )
            )
    )
    public void onMessage(@Payload CountFollowUnfollowMqDTO countFollowUnfollowMqDTO) {
        // 处理消息
        log.info("## 消费到了 MQ 【计数: 关注数】, {}...", countFollowUnfollowMqDTO);
        // 操作类型：关注 or 取关
        Integer type = countFollowUnfollowMqDTO.getType();
        // 原用户ID
        Long userId = countFollowUnfollowMqDTO.getUserId();

        // 更新 Redis
        String redisKey = RedisKeyConstants.buildCountUserKey(userId);
        // 判断 Hash 是否存在
        boolean isExisted = redisTemplate.hasKey(redisKey);

        // 若存在
        if (isExisted) {
            // 关注数：关注 +1， 取关 -1
            long count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
            // 对 Hash 中的 followingTotal 字段进行加减操作
            redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FOLLOWING_TOTAL, count);
        }

        // 发送 MQ, 发送mq
        rabbitTemplate.convertAndSend(
                "countSql.exchange",
                "countFollowingSql.queue",
                countFollowUnfollowMqDTO
        );
    }
}

package com.trxy.comsumer;

import com.google.common.util.concurrent.RateLimiter;
import com.trxy.entity.dto.CountFollowUnfollowMqDTO;
import com.trxy.enums.FollowUnfollowTypeEnum;
import com.trxy.mapper.UserCountDOMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class CountFollowing2DBConsumer {

    @Resource
    private UserCountDOMapper userCountDOMapper;

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(                        // 队列名称
                            name = "countFollowingSql.queue",             // 队列名称（与流程图中的 MQ 对应）
                            durable = "true"                   // 持久化
                    ),
                    exchange = @Exchange(                 // 定义 Direct 交换机
                            name = "countSql.exchange",          // 交换机名称
                            type = ExchangeTypes.DIRECT,      // 交换机类型为 FANOUT
                            durable = "true"
                    ),
                    key = "countFollowingSql.queue"            // 路由键（需与发送端路由键一致）

            )
    )
    public void onMessage(@Payload CountFollowUnfollowMqDTO countFollowUnfollowMqDTO) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数: 关注数入库】, {}...", countFollowUnfollowMqDTO);

        if (countFollowUnfollowMqDTO == null)return;

        // 操作类型：关注 or 取关
        Integer type = countFollowUnfollowMqDTO.getType();
        // 原用户ID
        Long userId = countFollowUnfollowMqDTO.getUserId();

        // 关注数：关注 +1， 取关 -1
        int count = Objects.equals(type, FollowUnfollowTypeEnum.FOLLOW.getCode()) ? 1 : -1;
        // 判断数据库中，若原用户的记录不存在，则插入；若记录已存在，则直接更新
        userCountDOMapper.insertOrUpdateFollowingTotalByUserId(count, userId);
    }
}

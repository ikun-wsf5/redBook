package com.trxy.comsumer;

import com.google.common.util.concurrent.RateLimiter;
import com.trxy.entity.bean.Entry;
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

import java.util.List;

@Slf4j
@Component
public class CountFans2DBConsumer {

    // 每秒创建 5000 个令牌
    private RateLimiter rateLimiter = RateLimiter.create(5000);
    @Resource
    private UserCountDOMapper userCountDOMapper;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(                        // 队列名称
                            name = "countFansSql.queue",             // 队列名称（与流程图中的 MQ 对应）
                            durable = "true"                   // 持久化
                    ),
                    exchange = @Exchange(                 // 定义 Direct 交换机
                            name = "countSql.exchange",          // 交换机名称
                            type = ExchangeTypes.DIRECT,      // 交换机类型为 FANOUT
                            durable = "true"
                    ),
                    key = "countFansSql.queue"            // 路由键（需与发送端路由键一致）

            )
    )
    public void onMessage(@Payload List<Entry> entries) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();

        log.info("## 消费到了 MQ 【计数: 粉丝数入库】, {}...", entries);

        if (entries != null && !entries.isEmpty()) {
            // 判断数据库中，若目标用户的记录不存在，则插入；若记录已存在，则直接更新
            entries.forEach((entry) -> userCountDOMapper.insertOrUpdateFansTotalByUserId(entry.getCount(),entry.getId()));
        }

    }

}

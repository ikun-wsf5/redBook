package com.trxy.comsumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import com.trxy.constans.RedisKeyConstants;
import com.trxy.entity.bean.Entry;
import com.trxy.entity.dto.CountFollowUnfollowMqDTO;
import com.trxy.enums.FollowUnfollowTypeEnum;
import com.trxy.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CountFansConsumer {
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Resource
    private RabbitTemplate rabbitTemplate;


    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1)) // 多久聚合一次
            .setConsumerEx(this::consumeMessage)
            .build();


    private void consumeMessage(List<String> messages) {
        log.info("==> 聚合消息, size: {}", messages.size());
        log.info("==> 聚合消息, {}", JsonUtils.toJsonString(messages));
        //批量反序列化为Java对象
        List<CountFollowUnfollowMqDTO> countFollowUnfollowMqDTOS = messages.stream()
                .map(message -> JsonUtils.parseObject(message, CountFollowUnfollowMqDTO.class))
                .toList();

        //因为是累计消息，所以要按按目标用户进行分组
        Map<Long, List<CountFollowUnfollowMqDTO>> groupMap = countFollowUnfollowMqDTOS.stream()
                .collect(Collectors.groupingBy(CountFollowUnfollowMqDTO::getTargetUserId));

        // 按组汇总数据，统计出最终的计数
        // key 为目标用户ID, value 为最终操作的计数
        Map<Long, Integer> countMap = Maps.newHashMap();

        for (Map.Entry<Long, List<CountFollowUnfollowMqDTO>> entry : groupMap.entrySet()) {
            List<CountFollowUnfollowMqDTO> list = entry.getValue();
            // 最终的计数值，默认为 0
            int finalCount = 0;
            for (CountFollowUnfollowMqDTO countFollowUnfollowMqDTO : list) {
                // 获取操作类型
                Integer type = countFollowUnfollowMqDTO.getType();

                // 根据操作类型，获取对应枚举
                FollowUnfollowTypeEnum followUnfollowTypeEnum = FollowUnfollowTypeEnum.valueOf(type);

                // 若枚举为空，跳到下一次循环
                if (Objects.isNull(followUnfollowTypeEnum)) continue;

                switch (followUnfollowTypeEnum) {
                    case FOLLOW -> finalCount += 1; // 如果为关注操作，粉丝数 +1
                    case UNFOLLOW -> finalCount -= 1; // 如果为取关操作，粉丝数 -1
                }
            }
            // 将分组后统计出的最终计数，存入 countMap 中
            countMap.put(entry.getKey(), finalCount);
        }

        log.info("## 聚合后的计数数据: {}", JsonUtils.toJsonString(countMap));
        // 更新 Redis
        countMap.forEach((k, v) -> {
            // Redis Key
            String redisKey = RedisKeyConstants.buildCountUserKey(k);
            // 判断 Redis 中 Hash 是否存在
            boolean isExisted = redisTemplate.hasKey(redisKey);

            // 若存在才会更新
            // (因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断一下，存在才会去更新，而初始化工作放在查询计数来做)
            if (isExisted) {
                // 对目标用户 Hash 中的粉丝数字段进行计数操作
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FANS_TOTAL, v);
            }
        });
        // 发送端：转换为 List<Entry>对象列表 并发送到 MQ
        List<Entry> entries = countMap.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        Message message = MessageBuilder
                .withBody(JsonUtils.toJsonString(entries).getBytes())
                .setContentType("application/json") // 声明内容类型
                .build();

            //发送mq消息，写入数据库
            rabbitTemplate.convertAndSend(
                    "count.exchange",
                    "countFansSql.queue",
                    message
            );

    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(                        // 队列名称
                            name = "countFans.queue",             // 队列名称（与流程图中的 MQ 对应）
                            durable = "true"                   // 持久化
                    ),
                    exchange = @Exchange(                 // 定义 Direct 交换机
                            name = "count.exchange",          // 交换机名称
                            type = ExchangeTypes.FANOUT,      // 交换机类型为 FANOUT
                            durable = "true"
                    )
            )
    )
    public void onMessage(@Payload CountFollowUnfollowMqDTO message) {

        // 处理消息
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(message.toString());
    }
}

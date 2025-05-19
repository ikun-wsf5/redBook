package com.trxy.consumer;

import jakarta.annotation.Resource;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;

public class DelayDeleteNoteRedisCacheConsumer {
    @Resource
    private RedisTemplate<String, String> redisTemplate;


    /**
     * 延迟删除缓存
     * @param key
     */
    // 绑定延迟队列和交换机
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(                          // 定义队列
                            name = "delayed.queue",               // 队列名称
                            durable = "true"                     // 持久化
                    ),
                    exchange = @Exchange(                    // 定义交换机
                            name = "delayed.exchange",           // 交换机名称
                            type = ExchangeTypes.DIRECT,         // 类型（需与插件类型匹配）
                            durable = "true",                    // 持久化
                            arguments = @Argument(
                                    name = "x-delayed-type",         // RabbitMQ 延迟插件的参数
                                    value = "direct"                 // 底层路由方式
                            )
                    ),
                    key = "delayed.deleteNoteRedis"               // 路由键
            )
    )
   public void DeleteNoteRedisCache(String key) {
       // 第二次删除缓存
       redisTemplate.delete(key);
       System.out.println("延迟双删完成！！！");
   }

}

package com.trxy.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.shaded.com.google.common.base.Preconditions;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.trxy.constant.RedisKeyConstants;
import com.trxy.entity.dto.FindUserByIdRspDTO;
import com.trxy.entity.dto.NoteDO;
import com.trxy.entity.dto.NoteLikeDO;
import com.trxy.entity.vo.*;
import com.trxy.enums.*;
import com.trxy.exception.BusinessException;
import com.trxy.holder.LoginUserContextHolder;
import com.trxy.mapper.NoteLikeMapper;
import com.trxy.mapper.NoteMapper;
import com.trxy.mapper.TopicMapper;
import com.trxy.rpc.DistributedIdGeneratorRpcService;
import com.trxy.rpc.KeyValueRpcService;
import com.trxy.rpc.UserRpcService;
import com.trxy.service.NoteService;
import com.trxy.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private TopicMapper topicMapper;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private NoteLikeMapper noteLikeMapper;

    @Override
    public void publishNote(PublishNoteReqVO publishNoteReqVO) {
        // 笔记类型
        Integer type = publishNoteReqVO.getType();

        // 获取对应类型的枚举
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

        // 若非图文、视频，抛出业务业务异常
        if (Objects.isNull(noteTypeEnum)) {
            throw new BusinessException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        // 笔记内容是否为空，默认值为 true，即空
        Boolean isContentEmpty = true;
        String videoUri = null;
        switch (noteTypeEnum) {
            case IMAGE_TEXT: // 图文笔记
                List<String> imgUriList = publishNoteReqVO.getImgUris();
                // 校验图片是否为空
                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                // 校验图片数量
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");
                // 将图片链接拼接，以逗号分隔
                imgUris = StringUtils.join(imgUriList, ",");

                break;
            case VIDEO: // 视频笔记
                videoUri = publishNoteReqVO.getVideoUri();
                // 校验视频链接是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
                break;
            default:
                break;
        }

        // RPC: 调用分布式 ID 生成服务，生成笔记 ID
        String snowflakeIdId = distributedIdGeneratorRpcService.getSnowflakeId();
        // 笔记内容 UUID
        String contentUuid = null;

        // 笔记内容
        String content = publishNoteReqVO.getContent();

        // 若用户填写了笔记内容
        if (StringUtils.isNotBlank(content)) {
            // 内容是否为空，置为 false，即不为空
            isContentEmpty = false;
            // 生成笔记内容 UUID
            contentUuid = UUID.randomUUID().toString();
            // RPC: 调用 KV 键值服务，存储短文本
            boolean isSavedSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);

            // 若存储失败，抛出业务异常，提示用户发布笔记失败
            if (!isSavedSuccess) {
                throw new BusinessException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        // 话题
        Long topicId = publishNoteReqVO.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            // 获取话题名称
            topicName = topicMapper.selectNameById(topicId);
        }

        // 发布者用户 ID
        Long creatorId = LoginUserContextHolder.getUserId();

        // 构建笔记 DO 对象
        NoteDO noteDO = NoteDO.builder()
                .id(Long.valueOf(snowflakeIdId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .imgUris(imgUris)
                .title(publishNoteReqVO.getTitle())
                .topicId(publishNoteReqVO.getTopicId())
                .topicName(topicName)
                .type(type)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .isTop(Boolean.FALSE)
                .videoUri(videoUri)
                .contentUuid(contentUuid)
                .build();

        try {
            // 笔记入库存储
            noteMapper.insert(noteDO);
        } catch (Exception e) {
            log.error("==> 笔记存储失败", e);

            // RPC: 笔记保存失败，则删除笔记内容
            if (StringUtils.isNotBlank(contentUuid)) {
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }

    }

    @Override
    public FindNoteDetailRspVO findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO) {
        // 查询的笔记 ID
        Long noteId = findNoteDetailReqVO.getId();

        // 当前登录用户
        Long userId = LoginUserContextHolder.getUserId();
        // 从 Redis 缓存中获取
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

        // 若缓存中有该笔记的数据，则直接返回
        if (StringUtils.isNotBlank(noteDetailJson)) {
            FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);
            // 可见性校验
            if (Objects.nonNull(findNoteDetailRspVO)) {
                Integer visible = findNoteDetailRspVO.getVisible();
                checkNoteVisible(visible, userId, findNoteDetailRspVO.getCreatorId());
            }

            return findNoteDetailRspVO;
        }

        // 查询笔记
        NoteDO noteDO = noteMapper.selectById(noteId);

        // 若该笔记不存在，则抛出业务异常
        if (Objects.isNull(noteDO)) {
            threadPoolTaskExecutor.execute(() -> {
                // 防止缓存穿透，将空数据存入 Redis 缓存 (过期时间不宜设置过长)
                // 保底1分钟 + 随机秒数
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(noteDetailRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new BusinessException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 可见性校验
        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, userId, noteDO.getCreatorId());

        // RPC: 调用用户服务
        Long creatorId = noteDO.getCreatorId();
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(creatorId);

        // RPC: 调用 K-V 存储服务获取内容
        String content = null;
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            content = keyValueRpcService.findNoteContent(noteDO.getContentUuid());
        }

        // 笔记类型
        Integer noteType = noteDO.getType();
        // 图文笔记图片链接(字符串)
        String imgUrisStr = noteDO.getImgUris();
        // 图文笔记图片链接(集合)
        List<String> imgUris = null;
        // 如果查询的是图文笔记，需要将图片链接的逗号分隔开，转换成集合
        if (Objects.equals(noteType, NoteTypeEnum.IMAGE_TEXT.getCode())
                && StringUtils.isNotBlank(imgUrisStr)) {
            imgUris = List.of(imgUrisStr.split(","));
        }

        // 构建返参 VO 实体类
        FindNoteDetailRspVO findNoteDetailRspVO = FindNoteDetailRspVO.builder()
                .id(noteDO.getId())
                .type(noteDO.getType())
                .title(noteDO.getTitle())
                .content(content)
                .imgUris(imgUris)
                .topicId(noteDO.getTopicId())
                .topicName(noteDO.getTopicName())
                .creatorId(noteDO.getCreatorId())
                .creatorName(findUserByIdRspDTO.getNickName())
                .avatar(findUserByIdRspDTO.getAvatar())
                .videoUri(noteDO.getVideoUri())
                .updateTime(noteDO.getUpdateTime())
                .visible(noteDO.getVisible())
                .build();

        // 异步线程中将笔记详情存入 Redis
        threadPoolTaskExecutor.submit(() -> {
            String noteDetailJson1 = JsonUtils.toJsonString(findNoteDetailRspVO);
            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            redisTemplate.opsForValue().set(noteDetailRedisKey, noteDetailJson1, expireSeconds, TimeUnit.SECONDS);
        });


        return findNoteDetailRspVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNote(UpdateNoteReqVO updateNoteReqVO) {

    // 笔记 ID
    Long noteId = updateNoteReqVO.getId();
    // 笔记类型
    Integer type = updateNoteReqVO.getType();

    // 获取对应类型的枚举
    NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);

    // 若非图文、视频，抛出业务业务异常
    if (Objects.isNull(noteTypeEnum)) {
        throw new BusinessException(ResponseCodeEnum.NOTE_TYPE_ERROR);
    }

    String imgUris = null;
    String videoUri = null;
    switch (noteTypeEnum) {
        case IMAGE_TEXT: // 图文笔记
            List<String> imgUriList = updateNoteReqVO.getImgUris();
            // 校验图片是否为空
            Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
            // 校验图片数量
            Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");

            imgUris = StringUtils.join(imgUriList, ",");
            break;
        case VIDEO: // 视频笔记
            videoUri = updateNoteReqVO.getVideoUri();
            // 校验视频链接是否为空
            Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
            break;
        default:
            break;
    }

        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();
        NoteDO selectNoteDO = noteMapper.selectById(noteId);

        // 笔记不存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BusinessException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许更新笔记
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BusinessException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

    // 话题
    Long topicId = updateNoteReqVO.getTopicId();
    String topicName = null;
    if (Objects.nonNull(topicId)) {
        topicName = topicMapper.selectNameById(topicId);

        // 判断一下提交的话题, 是否是真实存在的
        if (StringUtils.isBlank(topicName)) throw new BusinessException(ResponseCodeEnum.TOPIC_NOT_FOUND);
    }

    // 删除 Redis 缓存
    String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
    redisTemplate.delete(noteDetailRedisKey);
    // 更新笔记元数据表 t_note
    String content = updateNoteReqVO.getContent();
    NoteDO noteDO = NoteDO.builder()
            .id(noteId)
            .isContentEmpty(StringUtils.isBlank(content))
            .imgUris(imgUris)
            .title(updateNoteReqVO.getTitle())
            .topicId(updateNoteReqVO.getTopicId())
            .topicName(topicName)
            .type(type)
            .updateTime(LocalDateTime.now())
            .videoUri(videoUri)
            .build();

    noteMapper.update(noteDO);

    //延迟 删除 Redis 缓存,发送延迟消息
    rabbitTemplate.convertAndSend(
            "delayed.exchange",     // 交换机名称
            "delayed.deleteNoteRedis",   // 路由键
            noteDetailRedisKey,                    // 消息内容
            message -> {            // 消息后置处理器
                message.getMessageProperties().setHeader("x-delay", 1000); // 设置延迟时间
                return message;
            }
    );
    //redisTemplate.delete(noteDetailRedisKey);

    // 笔记内容更新
    // 查询此篇笔记内容对应的 UUID
    NoteDO noteDO1 = noteMapper.selectById(noteId);
    String contentUuid = noteDO1.getContentUuid();

    // 笔记内容是否更新成功
    boolean isUpdateContentSuccess = false;
    if (StringUtils.isBlank(content)) {
        // 若笔记内容为空，则删除 K-V 存储
        isUpdateContentSuccess = keyValueRpcService.deleteNoteContent(contentUuid);
    } else {
        // 若将无内容的笔记，更新为了有内容的笔记，需要重新生成 UUID
        contentUuid = StringUtils.isBlank(contentUuid) ? UUID.randomUUID().toString() : contentUuid;
        // 调用 K-V 更新短文本
        isUpdateContentSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);
    }

    // 如果更新失败，抛出业务异常，回滚事务
    if (!isUpdateContentSuccess) {
        throw new BusinessException(ResponseCodeEnum.NOTE_UPDATE_FAIL);
    }
    }

    @Override
    public void deleteNote(DeleteNoteReqVO deleteNoteReqVO) {
        // 笔记 ID
        Long noteId = deleteNoteReqVO.getId();


        NoteDO selectNoteDO = noteMapper.selectById(noteId);

        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BusinessException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许删除笔记
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BusinessException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }
        // 逻辑删除
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .status(NoteStatusEnum.DELETED.getCode())
                .updateTime(LocalDateTime.now())
                .build();

        int count = noteMapper.update(noteDO);

        // 若影响的行数为 0，则表示该笔记不存在
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 删除缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rabbitTemplate.convertAndSend(
                "delayed.exchange",     // 交换机名称
                "delayed.deleteNoteRedis",   // 路由键
                noteDetailRedisKey,                    // 消息内容
                message -> {            // 消息后置处理器
                    message.getMessageProperties().setHeader("x-delay", 1000); // 设置延迟时间
                    return message;
                }
        );
    }

    @Override
    public void visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        // 笔记 ID
        Long noteId = updateNoteVisibleOnlyMeReqVO.getId();

        NoteDO selectNoteDO = noteMapper.selectById(noteId);

        // 判断笔记是否存在
        if (Objects.isNull(selectNoteDO)) {
            throw new BusinessException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        // 判断权限：非笔记发布者不允许修改笔记权限
        Long currUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(currUserId, selectNoteDO.getCreatorId())) {
            throw new BusinessException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }
        // 构建更新 DO 实体类
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .visible(NoteVisibleEnum.PRIVATE.getCode()) // 可见性设置为仅对自己可见
                .updateTime(LocalDateTime.now())
                .build();

        // 执行更新 SQL
        int count = noteMapper.updateByStatus(noteDO);

        // 若影响的行数为 0，则表示该笔记无法修改为仅自己可见
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.NOTE_CANT_VISIBLE_ONLY_ME);
        }

        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rabbitTemplate.convertAndSend(
                "delayed.exchange",     // 交换机名称
                "delayed.deleteNoteRedis",   // 路由键
                noteDetailRedisKey,                    // 消息内容
                message -> {            // 消息后置处理器
                    message.getMessageProperties().setHeader("x-delay", 1000); // 设置延迟时间
                    return message;
                }
        );
    }

    @Override
    public void topNote(TopNoteReqVO topNoteReqVO) {
        // 笔记 ID
        Long noteId = topNoteReqVO.getId();
        // 是否置顶
        Boolean isTop = topNoteReqVO.getIsTop();

        // 当前登录用户 ID
        Long currUserId = LoginUserContextHolder.getUserId();

        // 构建置顶/取消置顶 DO 实体类
        NoteDO noteDO = NoteDO.builder()
                .id(noteId)
                .isTop(isTop)
                .updateTime(LocalDateTime.now())
                .creatorId(currUserId) // 只有笔记所有者，才能置顶/取消置顶笔记
                .build();

        int count = noteMapper.updateByOwnerId(noteDO);

        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.NOTE_CANT_OPERATE);
        }

        // 删除 Redis 缓存
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);
        redisTemplate.delete(noteDetailRedisKey);

        // 同步发送广播模式 MQ，将所有实例中的本地缓存都删除掉
        rabbitTemplate.convertAndSend(
                "delayed.exchange",     // 交换机名称
                "delayed.deleteNoteRedis",   // 路由键
                noteDetailRedisKey,                    // 消息内容
                message -> {            // 消息后置处理器
                    message.getMessageProperties().setHeader("x-delay", 1000); // 设置延迟时间
                    return message;
                }
        );
    }

    @Override
    public void likeNote(LikeNoteReqVO likeNoteReqVO) {
        // 笔记ID
        Long noteId = likeNoteReqVO.getId();

        // 1. 校验被点赞的笔记是否存在
        checkNoteIsExist(noteId);

        // 2. 判断目标笔记，是否已经点赞过
        // 当前登录用户ID
        Long userId = LoginUserContextHolder.getUserId();

        // 布隆过滤器 Key
        String bloomUserNoteLikeListKey = RedisKeyConstants.buildBloomUserNoteLikeListKey(userId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_note_like_check.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 执行 Lua 脚本，拿到返回结果
        Long result = redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId);

        NoteLikeLuaResultEnum noteLikeLuaResultEnum = NoteLikeLuaResultEnum.valueOf(result);

        switch (noteLikeLuaResultEnum) {
            // Redis 中布隆过滤器不存在
            case BLOOM_NOT_EXIST -> {
                // TODO: 从数据库中校验笔记是否被点赞，并异步初始化布隆过滤器，设置过期时间
                int count = noteLikeMapper.selectCountByUserIdAndNoteId(userId, noteId);

                // 保底1天+随机秒数
                long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                // 目标笔记已经被点赞
                if (count > 0) {
                    // 异步初始化布隆过滤器
                    asynBatchAddNoteLike2BloomAndExpire(userId, expireSeconds, bloomUserNoteLikeListKey);
                    throw new BusinessException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
                }

                // 若数据库中也没有点赞记录，说明该用户还未点赞过任何笔记
                // Lua 脚本路径
                script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_add_note_like_and_expire.lua")));
                // 返回值类型
                script.setResultType(Long.class);
                redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), noteId, expireSeconds);
            }
            // 目标笔记已经被点赞
            case NOTE_LIKED -> throw new BusinessException(ResponseCodeEnum.NOTE_ALREADY_LIKED);
        }
        // 3. 更新用户 ZSET 点赞列表

        // 4. 发送 MQ, 将点赞数据落库
    }

    /**
     * 异步初始化布隆过滤器，并设置过期时间
     * @param userId
     * @param expireSeconds
     * @param bloomUserNoteLikeListKey
     */
    private void asynBatchAddNoteLike2BloomAndExpire(Long userId, long expireSeconds, String bloomUserNoteLikeListKey) {
        threadPoolTaskExecutor.submit(() -> {
            try {
                // 异步全量同步一下，并设置过期时间
                List<NoteLikeDO> noteLikeDOS = noteLikeMapper.selectByUserId(userId);

                if (CollUtil.isNotEmpty(noteLikeDOS)) {
                    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                    // Lua 脚本路径
                    script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/bloom_batch_add_note_like_and_expire.lua")));
                    // 返回值类型
                    script.setResultType(Long.class);

                    // 构建 Lua 参数
                    List<Object> luaArgs = Lists.newArrayList();
                    noteLikeDOS.forEach(noteLikeDO -> luaArgs.add(noteLikeDO.getNoteId())); // 将每个点赞的笔记 ID 传入
                    luaArgs.add(expireSeconds);  // 最后一个参数是过期时间（秒）
                    redisTemplate.execute(script, Collections.singletonList(bloomUserNoteLikeListKey), luaArgs.toArray());
                }
            } catch (Exception e) {
                log.error("## 异步初始化布隆过滤器异常: ", e);
            }
        });
    }

    /**
     * 检验笔记是否存在
     * @param noteId
     */
    private void checkNoteIsExist(Long noteId) {
        // 再从 Redis 中校验
        String noteDetailRedisKey = RedisKeyConstants.buildNoteDetailKey(noteId);

        String noteDetailJson = redisTemplate.opsForValue().get(noteDetailRedisKey);

        // 解析 Json 字符串为 VO 对象
        FindNoteDetailRspVO findNoteDetailRspVO = JsonUtils.parseObject(noteDetailJson, FindNoteDetailRspVO.class);

        // 都不存在，再查询数据库校验是否存在
        if (Objects.isNull(findNoteDetailRspVO)) {
            int count = noteMapper.selectCountByNoteId(noteId);

            // 若数据库中也不存在，提示用户
            if (count == 0) {
                throw new BusinessException(ResponseCodeEnum.NOTE_NOT_FOUND);
            }

            // 若数据库中存在，异步同步一下缓存
            threadPoolTaskExecutor.submit(() -> {
                FindNoteDetailReqVO findNoteDetailReqVO = FindNoteDetailReqVO.builder().id(noteId).build();
                findNoteDetail(findNoteDetailReqVO);
            });
        }
    }

    /**
     * 校验笔记的可见性
     * @param visible 是否可见
     * @param currUserId 当前用户 ID
     * @param creatorId 笔记创建者
     */
    private void checkNoteVisible(Integer visible, Long currUserId, Long creatorId) {
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
                && !Objects.equals(currUserId, creatorId)) { // 仅自己可见, 并且访问用户为笔记创建者
            throw new BusinessException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

}

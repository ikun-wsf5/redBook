-- 获得参数
local key = KEYS[1] -- 操作的 Redis Key
local followUserId = ARGV[1] -- 关注的用户ID
local timestamp = ARGV[2] -- 时间戳

-- 使用 EXISTS 命令检查 当前用户缓存 是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 校验关注人数是否上限（是否达到 1000）
local size = redis.call('ZCARD', key)
if size >= 1000 then
    return -2
end

-- 校验目标用户是否已经关注（获取关注用户的得分），如果有得分则代表已经关注
if redis.call('ZSCORE', key, followUserId) then
    return -3
end

-- ZADD 添加关注关系（存入缓存）
redis.call('ZADD', key, timestamp, followUserId)
return 0
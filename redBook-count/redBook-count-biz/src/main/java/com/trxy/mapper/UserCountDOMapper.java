package com.trxy.mapper;

import com.trxy.entity.dto.UserCountDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

@Mapper
public interface UserCountDOMapper {
    UserCountDO selectById(Long id);
    int updateFansTotal(Long userId);
    int updateFollowingTotal(Long userId);
    int updateNoteTotal(Long userId);
    int updateLikeTotal(Long userId);
    int updateCollectTotal(Long userId);

    /**
     * 添加或更新粉丝总数
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateFansTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);

    /**
     * 添加或更新关注总数
     * @param count
     * @param userId
     * @return
     */
    int insertOrUpdateFollowingTotalByUserId(@Param("count") Integer count, @Param("userId") Long userId);
}
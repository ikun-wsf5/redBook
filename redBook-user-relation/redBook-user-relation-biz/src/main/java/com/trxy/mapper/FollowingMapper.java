package com.trxy.mapper;


import com.trxy.entity.dto.FollowingDO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowingMapper {
    int insert(FollowingDO followingDO);
    int delete(FollowingDO followingDO);
    int update(FollowingDO followingDO);
    FollowingDO selectById(Long id);
    List<FollowingDO> selectByUserId(Long userId);
    /**
     * 查询记录总数
     *
     * @param userId
     * @return
     */
    long selectCountByUserId(Long userId);
    /**
     * 分页查询
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    List<FollowingDO> selectPageListByUserId(@Param("userId") Long userId,
                                             @Param("offset") long offset,
                                             @Param("limit") long limit);

    /**
            * 查询关注用户列表
     * @param userId
     * @return
             */
    List<FollowingDO> selectAllByUserId(Long userId);
}
package com.trxy.mapper;

import com.trxy.entity.dto.FansDO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FansMapper {

    int insert(FansDO fansDO);

    int delete(FansDO fansDO);

    int update(FansDO fansDO);

    FansDO selectById(Long id);
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
    List<FansDO> selectPageListByUserId(@Param("userId") Long userId,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /**
     * 查询最新关注的 5000 位粉丝
     * @param userId
     * @return
     */
    List<FansDO> select5000FansByUserId(Long userId);
}
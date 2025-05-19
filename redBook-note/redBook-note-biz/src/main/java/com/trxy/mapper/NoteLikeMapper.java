package com.trxy.mapper;

import com.trxy.entity.dto.NoteLikeDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface NoteLikeMapper {
    NoteLikeDO selectById(Long id);
    List<NoteLikeDO> selectByNoteId(Long noteId);
    int insert(NoteLikeDO noteLike);
    int updateStatus(Long noteId, Long userId, Byte status);
    int deleteById(Long id);

    int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteLikeDO> selectByUserId(Long userId);
}
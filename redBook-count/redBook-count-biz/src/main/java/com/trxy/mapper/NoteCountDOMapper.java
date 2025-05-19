package com.trxy.mapper;

import com.trxy.entity.dto.NoteCountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteCountDOMapper {
    NoteCountDO selectById(Long id);
    int updateLikeTotal(Long noteId);
    int updateCollectTotal(Long noteId);
    int updateCommentTotal(Long noteId);
}
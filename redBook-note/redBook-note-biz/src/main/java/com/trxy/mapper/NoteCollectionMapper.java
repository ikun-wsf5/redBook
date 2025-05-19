package com.trxy.mapper;

import com.trxy.entity.dto.NoteCollectionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NoteCollectionMapper {
    NoteCollectionDO selectById(Long id);
    List<NoteCollectionDO> selectByNoteId(Long noteId);
    int insert(NoteCollectionDO noteCollection);
    int updateStatus(Long noteId, Long userId, Byte status);
    int deleteById(Long id);
}
package com.trxy.service.impl;

import com.trxy.Repository.NoteContentRepository;
import com.trxy.entity.dto.*;
import com.trxy.enums.ResponseCodeEnum;
import com.trxy.exception.BusinessException;
import com.trxy.service.NoteContentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    private NoteContentRepository noteContentRepository;


    @Override
    public void addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        // 笔记 ID
        String uuid = addNoteContentReqDTO.getUuid();
        // 笔记内容
        String content = addNoteContentReqDTO.getContent();

        // 构建数据库 DO 实体类
        NoteContentDO nodeContent = NoteContentDO.builder()
                .id(UUID.fromString(uuid)) // TODO: 暂时用 UUID, 目的是为了下一章讲解压测，不用动态传笔记 ID。后续改为笔记服务传过来的笔记 ID
                .content(content)
                .build();

        // 插入数据
        noteContentRepository.save(nodeContent);

    }

    @Override
    public FindNoteContentRspDTO findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO) {

            // 笔记 ID
            String uuid = findNoteContentReqDTO.getUuid();
            // 根据笔记 ID 查询笔记内容
            Optional<NoteContentDO> optional = noteContentRepository.findById(UUID.fromString(uuid));

            // 若笔记内容不存在
            if (!optional.isPresent()) {
                throw new BusinessException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
            }

            NoteContentDO noteContentDO = optional.get();
            // 构建返参 DTO
            FindNoteContentRspDTO findNoteContentRspDTO = FindNoteContentRspDTO.builder()
                    .uuid(noteContentDO.getId())
                    .content(noteContentDO.getContent())
                    .build();

            return findNoteContentRspDTO;
        }

    @Override
    public void deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        // 笔记 ID
        String uuid = deleteNoteContentReqDTO.getUuid();
        // 删除笔记内容
        noteContentRepository.deleteById(UUID.fromString(uuid));
    }

}
package com.trxy.controller;

import com.trxy.aspect.ApiOperationLog;
import com.trxy.entity.vo.*;
import com.trxy.response.Response;
import com.trxy.service.NoteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/note")
@Slf4j
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping(value = "/publish")
    @ApiOperationLog(description = "笔记发布")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteReqVO publishNoteReqVO) {
        noteService.publishNote(publishNoteReqVO);
        return Response.success();
    }
    @PostMapping(value = "/detail")
    @ApiOperationLog(description = "查询笔记详情")
    public Response<FindNoteDetailRspVO> findNoteDetail(@Validated @RequestBody FindNoteDetailReqVO findNoteDetailReqVO) {
        return Response.success(noteService.findNoteDetail(findNoteDetailReqVO));
    }
    @PostMapping(value = "/update")
    @ApiOperationLog(description = "笔记修改/更新笔记")
    public Response<?> updateNote(@Validated @RequestBody UpdateNoteReqVO updateNoteReqVO) {
        noteService.updateNote(updateNoteReqVO);
        return Response.success();
    }
    @PostMapping(value = "/delete")
    @ApiOperationLog(description = "删除笔记")
    public Response<?> deleteNote(@Validated @RequestBody DeleteNoteReqVO deleteNoteReqVO) {
        noteService.deleteNote(deleteNoteReqVO);
        return Response.success();
    }

    @PostMapping(value = "/visible/onlyme")
    @ApiOperationLog(description = "笔记仅对自己可见")
    public Response<?> visibleOnlyMe(@Validated @RequestBody UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO) {
        noteService.visibleOnlyMe(updateNoteVisibleOnlyMeReqVO);
        return Response.success();
    }

    @PostMapping(value = "/top")
    @ApiOperationLog(description = "置顶/取消置顶笔记")
    public Response<?> topNote(@Validated @RequestBody TopNoteReqVO topNoteReqVO) {
        noteService.topNote(topNoteReqVO);
        return Response.success();
    }

}
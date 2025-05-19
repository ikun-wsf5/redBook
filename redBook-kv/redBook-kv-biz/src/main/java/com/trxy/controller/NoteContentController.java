package com.trxy.controller;

import com.trxy.entity.dto.AddNoteContentReqDTO;
import com.trxy.entity.dto.DeleteNoteContentReqDTO;
import com.trxy.entity.dto.FindNoteContentReqDTO;
import com.trxy.entity.dto.FindNoteContentRspDTO;
import com.trxy.response.Response;
import com.trxy.service.NoteContentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv")
@Slf4j
public class NoteContentController {

    @Resource
    private NoteContentService noteContentService;

    @PostMapping(value = "/note/content/add")
    public Response<?> addNoteContent(@Validated @RequestBody AddNoteContentReqDTO addNoteContentReqDTO) {
        noteContentService.addNoteContent(addNoteContentReqDTO);
        return Response.success();
    }

    @PostMapping(value = "/note/content/find")
    public Response<FindNoteContentRspDTO> findNoteContent(@Validated @RequestBody FindNoteContentReqDTO findNoteContentReqDTO) {
        noteContentService.findNoteContent(findNoteContentReqDTO);
        return Response.success();
    }

    @PostMapping(value = "/note/content/delete")
    public Response<?> deleteNoteContent(@Validated @RequestBody DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        noteContentService.deleteNoteContent(deleteNoteContentReqDTO);
        return Response.success();
    }

}

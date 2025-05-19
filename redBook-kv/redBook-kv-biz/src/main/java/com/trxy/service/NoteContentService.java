package com.trxy.service;

import com.trxy.entity.dto.AddNoteContentReqDTO;
import com.trxy.entity.dto.DeleteNoteContentReqDTO;
import com.trxy.entity.dto.FindNoteContentReqDTO;
import com.trxy.entity.dto.FindNoteContentRspDTO;

public interface NoteContentService {
    void addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

    FindNoteContentRspDTO findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);

    void deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO);
}

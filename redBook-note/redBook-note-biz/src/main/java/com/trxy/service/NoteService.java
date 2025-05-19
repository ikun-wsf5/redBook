package com.trxy.service;

import com.trxy.entity.vo.*;

public interface NoteService {
    void publishNote(PublishNoteReqVO publishNoteReqVO);

    FindNoteDetailRspVO findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

    void updateNote(UpdateNoteReqVO updateNoteReqVO);

    void deleteNote(DeleteNoteReqVO deleteNoteReqVO);

    void visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO);

    void topNote(TopNoteReqVO topNoteReqVO);

    /**
     * 点赞笔记
     * @param likeNoteReqVO
     * @return
     */
    void likeNote(LikeNoteReqVO likeNoteReqVO);
}

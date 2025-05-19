package com.trxy.service;

import com.trxy.entity.vo.*;
import com.trxy.response.PageResponse;

public interface RelationService {

    /**
     * 关注用户
     * @param followUserReqVO
     * @return
     */
    void follow(FollowUserReqVO followUserReqVO);

    void unfollow(UnfollowUserReqVO unfollowUserReqVO);

    PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO);

    /**
     * 查询关注列表
     * @param findFansListReqVO
     * @return
     */
    PageResponse<FindFansUserRspVO> findFansList(FindFansListReqVO findFansListReqVO);
}
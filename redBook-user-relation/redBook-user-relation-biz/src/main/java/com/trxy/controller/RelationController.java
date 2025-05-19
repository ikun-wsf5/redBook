package com.trxy.controller;


import com.trxy.aspect.ApiOperationLog;
import com.trxy.entity.vo.FindFollowingListReqVO;
import com.trxy.entity.vo.FindFollowingUserRspVO;
import com.trxy.entity.vo.FollowUserReqVO;
import com.trxy.entity.vo.UnfollowUserReqVO;
import com.trxy.response.PageResponse;
import com.trxy.response.Response;
import com.trxy.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
@Slf4j
public class RelationController {

    @Resource
    private RelationService relationService;

    @PostMapping("/follow")
    @ApiOperationLog(description = "关注用户")
    public Response<?> follow(@Validated @RequestBody FollowUserReqVO followUserReqVO) {
        relationService.follow(followUserReqVO);
        return Response.success();
    }
    @PostMapping("/unfollow")
    @ApiOperationLog(description = "取关用户")
    public Response<?> unfollow(@Validated @RequestBody UnfollowUserReqVO unfollowUserReqVO) {
        relationService.unfollow(unfollowUserReqVO);
        return Response.success();
    }
    @PostMapping("/following/list")
    @ApiOperationLog(description = "查询用户关注列表")
    public PageResponse<FindFollowingUserRspVO> findFollowingList(@Validated @RequestBody FindFollowingListReqVO findFollowingListReqVO) {
        return relationService.findFollowingList(findFollowingListReqVO);
    }

}
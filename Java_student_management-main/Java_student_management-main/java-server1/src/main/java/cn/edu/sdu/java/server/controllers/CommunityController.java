package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.CommunityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.edu.sdu.java.server.payload.response.OptionItemList;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/community")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping("/getPostList")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getPostList(@Valid @RequestBody DataRequest dataRequest) {
        return communityService.getPostList(dataRequest);
    }

    @PostMapping("/getCategoryOptionList")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public OptionItemList getCategoryOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return communityService.getCategoryOptionList(dataRequest);
    }

    @PostMapping("/getPostDetail")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getPostDetail(@Valid @RequestBody DataRequest dataRequest) {
        return communityService.getPostDetail(dataRequest);
    }

    @PostMapping("/postSave")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse postSave(@Valid @RequestBody DataRequest dataRequest) {
        return communityService.postSave(dataRequest);
    }

    @PostMapping("/postDelete")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse postDelete(@Valid @RequestBody DataRequest dataRequest) {
        return communityService.postDelete(dataRequest);
    }

    @PostMapping("/commentSave")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse commentSave(@Valid @RequestBody DataRequest dataRequest) {
        return communityService.commentSave(dataRequest);
    }

    @PostMapping("/commentDelete")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse commentDelete(@Valid @RequestBody DataRequest dataRequest) {
        return communityService.commentDelete(dataRequest);
    }
}

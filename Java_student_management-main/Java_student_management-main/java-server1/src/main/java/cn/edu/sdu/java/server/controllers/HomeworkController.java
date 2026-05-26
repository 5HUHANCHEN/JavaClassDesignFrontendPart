package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.HomeworkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/homework")
public class HomeworkController {
    private final HomeworkService homeworkService;

    public HomeworkController(HomeworkService homeworkService) {
        this.homeworkService = homeworkService;
    }

    @PostMapping("/getHomeworkList")
    public DataResponse getHomeworkList(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getHomeworkList(dataRequest);
    }

    @PostMapping("/homeworkSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse homeworkSave(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.homeworkSave(dataRequest);
    }

    @PostMapping("/homeworkDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse homeworkDelete(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.homeworkDelete(dataRequest);
    }

    @PostMapping("/submitHomework")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse submitHomework(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.submitHomework(dataRequest);
    }

    @PostMapping("/getSubmissionList")
    public DataResponse getSubmissionList(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getSubmissionList(dataRequest);
    }

    @PostMapping("/gradeSubmission")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse gradeSubmission(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.gradeSubmission(dataRequest);
    }

    @PostMapping(path = "/uploadHomeworkImage")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse uploadHomeworkImage(@RequestBody byte[] bytes,
                                            @RequestParam(name = "homeworkId") Integer homeworkId,
                                            @RequestParam(name = "fileName", required = false) String fileName) {
        return homeworkService.uploadHomeworkImage(bytes, homeworkId, fileName);
    }

    @PostMapping(path = "/uploadSubmissionImage")
    @PreAuthorize("hasRole('STUDENT')")
    public DataResponse uploadSubmissionImage(@RequestBody byte[] bytes,
                                              @RequestParam(name = "submissionId") Integer submissionId,
                                              @RequestParam(name = "fileName", required = false) String fileName) {
        return homeworkService.uploadSubmissionImage(bytes, submissionId, fileName);
    }

    @PostMapping("/getHomeworkImage")
    public ResponseEntity<StreamingResponseBody> getHomeworkImage(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getHomeworkImage(dataRequest);
    }

    @PostMapping("/getSubmissionImage")
    public ResponseEntity<StreamingResponseBody> getSubmissionImage(@Valid @RequestBody DataRequest dataRequest) {
        return homeworkService.getSubmissionImage(dataRequest);
    }
}

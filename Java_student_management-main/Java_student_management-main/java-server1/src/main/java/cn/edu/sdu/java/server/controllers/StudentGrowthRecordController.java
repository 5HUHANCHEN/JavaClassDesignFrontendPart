package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.services.StudentGrowthRecordService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/studentGrowth")
public class StudentGrowthRecordController {
    private final StudentGrowthRecordService studentGrowthRecordService;

    public StudentGrowthRecordController(StudentGrowthRecordService studentGrowthRecordService) {
        this.studentGrowthRecordService = studentGrowthRecordService;
    }

    @PostMapping("/getStudentItemOptionList")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public OptionItemList getStudentItemOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return studentGrowthRecordService.getStudentItemOptionList(dataRequest);
    }

    @PostMapping("/getGrowthRecordList")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse getGrowthRecordList(@Valid @RequestBody DataRequest dataRequest) {
        return studentGrowthRecordService.getGrowthRecordList(dataRequest);
    }

    @PostMapping("/getTypeOptionList")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public OptionItemList getTypeOptionList(@Valid @RequestBody DataRequest dataRequest) {
        return studentGrowthRecordService.getTypeOptionList(dataRequest);
    }

    @PostMapping("/saveGrowthRecord")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse saveGrowthRecord(@Valid @RequestBody DataRequest dataRequest) {
        return studentGrowthRecordService.saveGrowthRecord(dataRequest);
    }

    @PostMapping("/deleteGrowthRecord")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse deleteGrowthRecord(@Valid @RequestBody DataRequest dataRequest) {
        return studentGrowthRecordService.deleteGrowthRecord(dataRequest);
    }

    @PostMapping("/saveType")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse saveType(@Valid @RequestBody DataRequest dataRequest) {
        return studentGrowthRecordService.saveType(dataRequest);
    }

    @PostMapping("/deleteType")
    @PreAuthorize("hasRole('ADMIN')")
    public DataResponse deleteType(@Valid @RequestBody DataRequest dataRequest) {
        return studentGrowthRecordService.deleteType(dataRequest);
    }
}

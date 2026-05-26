package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.CourseMaterialService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/courseMaterial")
public class CourseMaterialController {
    @Autowired
    private CourseMaterialService courseMaterialService;

    @PostMapping("/getMaterialList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getMaterialList(@Valid @RequestBody DataRequest dataRequest) {
        return courseMaterialService.getMaterialList(dataRequest);
    }

    @PostMapping("/materialSave")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse materialSave(@Valid @RequestBody DataRequest dataRequest) {
        return courseMaterialService.materialSave(dataRequest);
    }

    @PostMapping("/materialDelete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse materialDelete(@Valid @RequestBody DataRequest dataRequest) {
        return courseMaterialService.materialDelete(dataRequest);
    }

    @PostMapping(path = "/uploadMaterialFile")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public DataResponse uploadMaterialFile(@RequestBody byte[] bytes,
                                           @RequestParam(name = "materialId") Integer materialId,
                                           @RequestParam(name = "fileName", required = false) String fileName) {
        return courseMaterialService.uploadMaterialFile(bytes, materialId, fileName);
    }

    @PostMapping("/downloadMaterialFile")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public ResponseEntity<StreamingResponseBody> downloadMaterialFile(@Valid @RequestBody DataRequest dataRequest) {
        return courseMaterialService.downloadMaterialFile(dataRequest);
    }
}

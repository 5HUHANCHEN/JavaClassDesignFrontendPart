package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.DivinationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/divination")
public class DivinationController {
    private final DivinationService divinationService;

    public DivinationController(DivinationService divinationService) {
        this.divinationService = divinationService;
    }

    @PostMapping("/plumBlossom")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse plumBlossom(@Valid @RequestBody DataRequest dataRequest) {
        return divinationService.plumBlossom(dataRequest);
    }

    @PostMapping("/tarot")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse tarot(@Valid @RequestBody DataRequest dataRequest) {
        return divinationService.tarot(dataRequest);
    }

    @PostMapping("/getHistoryList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getHistoryList(@Valid @RequestBody DataRequest dataRequest) {
        return divinationService.getHistoryList(dataRequest);
    }

    @PostMapping("/getHistoryDetail")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse getHistoryDetail(@Valid @RequestBody DataRequest dataRequest) {
        return divinationService.getHistoryDetail(dataRequest);
    }

    @PostMapping("/deleteHistory")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse deleteHistory(@Valid @RequestBody DataRequest dataRequest) {
        return divinationService.deleteHistory(dataRequest);
    }
}

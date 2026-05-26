package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.EntertainmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/entertainment")
public class EntertainmentController {
    private final EntertainmentService entertainmentService;

    public EntertainmentController(EntertainmentService entertainmentService) {
        this.entertainmentService = entertainmentService;
    }

    @PostMapping("/weather")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse weather(@Valid @RequestBody DataRequest dataRequest) {
        return entertainmentService.weather(dataRequest);
    }

    @PostMapping("/hitokoto")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse hitokoto(@Valid @RequestBody DataRequest dataRequest) {
        return entertainmentService.hitokoto(dataRequest);
    }

    @PostMapping("/movieRecommend")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse movieRecommend(@Valid @RequestBody DataRequest dataRequest) {
        return entertainmentService.movieRecommend(dataRequest);
    }

    @PostMapping("/musicRecommend")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse musicRecommend(@Valid @RequestBody DataRequest dataRequest) {
        return entertainmentService.musicRecommend(dataRequest);
    }

    @PostMapping("/copywriting")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse copywriting(@Valid @RequestBody DataRequest dataRequest) {
        return entertainmentService.copywriting(dataRequest);
    }

    @PostMapping("/horoscope")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse horoscope(@Valid @RequestBody DataRequest dataRequest) {
        return entertainmentService.horoscope(dataRequest);
    }

    @PostMapping("/hotList")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public DataResponse hotList(@Valid @RequestBody DataRequest dataRequest) {
        return entertainmentService.hotList(dataRequest);
    }
}

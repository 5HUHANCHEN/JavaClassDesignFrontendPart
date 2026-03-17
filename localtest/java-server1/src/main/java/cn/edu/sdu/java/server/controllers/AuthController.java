package cn.edu.sdu.java.server.controllers;

import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.request.LoginRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.services.AuthService;
import cn.edu.sdu.java.server.util.CommonMethod;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 *  AuthController 实现 登录和注册Web服务
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     *  用户登录
     * @param loginRequest   username 登录名  password 密码
     * @return   JwtResponse 用户信息， 该信息再后续的web请求时作为请求头的一部分，用于框架的请求服务权限验证
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticateUser(loginRequest);
    }
    @PostMapping("/getValidateCode")
    public DataResponse getValidateCode(@Valid @RequestBody DataRequest dataRequest) {
        return authService.getValidateCode(dataRequest);
    }

    @PostMapping("/testValidateInfo")
    public DataResponse testValidateInfo(@Valid @RequestBody DataRequest dataRequest) {
        return authService.testValidateInfo(dataRequest);
    }
    @PostMapping("/registerUser")
    public DataResponse registerUser(@Valid @RequestBody DataRequest dataRequest) {
        String role = dataRequest.getString("role");

        if(!"STUDENT".equals(role)){
            return CommonMethod.getReturnMessageError("教师和管理员不能直接注册，请提交申请！");
        }
        return authService.registerUser(dataRequest);
    }
    @PostMapping("/applyRegister")
    public DataResponse applyRegister(@Valid @RequestBody DataRequest dataRequest){
        return authService.applyRegister(dataRequest);
    }
    @PostMapping("/getRegisterApplyList")
    public DataResponse getRegisterApplyList(@Valid @RequestBody DataRequest dataRequest) {
        return authService.getRegisterApplyList(dataRequest);
    }

    @PostMapping("/approveRegisterApply")
    public DataResponse approveRegisterApply(@Valid @RequestBody DataRequest dataRequest) {
        return authService.approveRegisterApply(dataRequest);
    }

    @PostMapping("/rejectRegisterApply")
    public DataResponse rejectRegisterApply(@Valid @RequestBody DataRequest dataRequest) {
        return authService.rejectRegisterApply(dataRequest);
    }

}

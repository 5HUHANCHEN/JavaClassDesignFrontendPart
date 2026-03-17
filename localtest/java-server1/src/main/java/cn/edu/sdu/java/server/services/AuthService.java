package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.request.LoginRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.JwtResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import cn.edu.sdu.java.server.util.LoginControlUtil;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final StudentRepository studentRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final RegisterApplyRepository registerApplyRepository;

    private final TeacherRepository teacherRepository;

    public AuthService(PersonRepository personRepository,
                       UserRepository userRepository,
                       UserTypeRepository userTypeRepository,
                       StudentRepository studentRepository,
                       TeacherRepository teacherRepository,
                       RegisterApplyRepository registerApplyRepository,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       PasswordEncoder encoder,
                       ResourceLoader resourceLoader) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.registerApplyRepository = registerApplyRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.encoder = encoder;
    }

    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Optional<User> op = userRepository.findByUserName(loginRequest.getUsername());
        if (op.isPresent()) {
            User user = op.get();
            user.setLastLoginTime(DateTimeTool.parseDateTime(new Date()));
            Integer count = user.getLoginCount();
            if (count == null) {
                count = 1;
            } else {
                count += 1;
            }
            user.setLoginCount(count);
            userRepository.save(user);
        }

        String jwt = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getPerName(),
                roles.getFirst()));
    }

    public DataResponse getValidateCode(DataRequest dataRequest) {
        return CommonMethod.getReturnData(LoginControlUtil.getInstance().getValidateCodeDataMap());
    }

    public DataResponse testValidateInfo(DataRequest dataRequest) {
        Integer validateCodeId = dataRequest.getInteger("validateCodeId");
        String validateCode = dataRequest.getString("validateCode");
        LoginControlUtil li = LoginControlUtil.getInstance();
        if (validateCodeId == null || validateCode == null || validateCode.isEmpty()) {
            return CommonMethod.getReturnMessageError("验证码为空！");
        }
        String value = li.getValidateCode(validateCodeId);
        if (!validateCode.equals(value)) {
            return CommonMethod.getReturnMessageError("验证码错位！");
        }
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse registerUser(DataRequest dataRequest) {
        String username = dataRequest.getString("username");
        String password = dataRequest.getString("password");
        String perName = dataRequest.getString("perName");
        String email = dataRequest.getString("email");
        String role = dataRequest.getString("role");
        UserType ut = null;

        Optional<User> uOp = userRepository.findByUserName(username);
        if (uOp.isPresent()) {
            return CommonMethod.getReturnMessageError("用户已经存在，不能注册！");
        }

        Person p = new Person();
        p.setNum(username);
        p.setName(perName);
        p.setEmail(email);

        if ("ADMIN".equals(role)) {
            p.setType("0");
            ut = userTypeRepository.findByName(EUserType.ROLE_ADMIN.name());
        } else if ("STUDENT".equals(role)) {
            p.setType("1");
            ut = userTypeRepository.findByName(EUserType.ROLE_STUDENT.name());
        } else if ("TEACHER".equals(role)) {
            p.setType("2");
            ut = userTypeRepository.findByName(EUserType.ROLE_TEACHER.name());
        }

        p = personRepository.saveAndFlush(p);

        User u = new User();
        u.setPersonId(p.getPersonId());
        u.setUserType(ut);
        u.setUserName(username);
        u.setPassword(encoder.encode(password));
        u.setCreateTime(DateTimeTool.parseDateTime(new Date()));
        u.setCreatorId(p.getPersonId());
        u.setLoginCount(0);


        userRepository.saveAndFlush(u);

        if ("STUDENT".equals(role)) {
            Student s = new Student();
            s.setPersonId(p.getPersonId());
            studentRepository.saveAndFlush(s);
        }

        return CommonMethod.getReturnMessageOK("注册成功！");
    }

    public DataResponse applyRegister(DataRequest dataRequest) {
        String username = dataRequest.getString("username");
        String password = dataRequest.getString("password");
        String name = dataRequest.getString("name");
        String role = dataRequest.getString("role");
        String email = dataRequest.getString("email");
        String dept = dataRequest.getString("dept");
        String major = dataRequest.getString("major");
        String className = dataRequest.getString("className");
        String phone = dataRequest.getString("phone");
        String reason = dataRequest.getString("reason");

        if ("STUDENT".equals(role)) {
            return CommonMethod.getReturnMessageError("学生请直接注册！");
        }

        Optional<User> uOp = userRepository.findByUserName(username);
        if (uOp.isPresent()) {
            return CommonMethod.getReturnMessageError("账号已存在！");
        }

        RegisterApply apply = new RegisterApply();
        apply.setUsername(username);
        apply.setPassword(encoder.encode(password));
        apply.setName(name);
        apply.setRole(role);
        apply.setEmail(email);
        apply.setDept(dept);
        apply.setMajor(major);
        apply.setClassName(className);
        apply.setPhone(phone);
        apply.setReason(reason);
        apply.setStatus(0);
        apply.setApplyTime(DateTimeTool.parseDateTime(new Date()));
        apply.setRemark("");

        registerApplyRepository.saveAndFlush(apply);

        return CommonMethod.getReturnMessageOK("申请提交成功，等待管理员审核！");


    }

    public DataResponse getRegisterApplyList(DataRequest dataRequest) {
        Integer status = dataRequest.getInteger("status");
        if (status == null) {
            status = 0;
        }
        List<RegisterApply> list = registerApplyRepository.findByStatusOrderByApplyIdDesc(status);
        return CommonMethod.getReturnData(list);
    }

    public DataResponse rejectRegisterApply(DataRequest dataRequest) {
        Integer applyId = dataRequest.getInteger("applyId");
        String remark = dataRequest.getString("remark");

        if (applyId == null) {
            return CommonMethod.getReturnMessageError("申请ID不能为空！");
        }

        Optional<RegisterApply> op = registerApplyRepository.findById(applyId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("申请记录不存在！");
        }

        RegisterApply apply = op.get();
        if (apply.getStatus() != null && apply.getStatus() != 0) {
            return CommonMethod.getReturnMessageError("该申请已审核，不能重复操作！");
        }

        apply.setStatus(2);
        apply.setApproveTime(DateTimeTool.parseDateTime(new Date()));
        apply.setRemark(remark == null ? "" : remark);

        registerApplyRepository.saveAndFlush(apply);
        return CommonMethod.getReturnMessageOK("已拒绝该申请！");
    }

    public DataResponse approveRegisterApply(DataRequest dataRequest) {
        Integer applyId = dataRequest.getInteger("applyId");
        String remark = dataRequest.getString("remark");

        if (applyId == null) {
            return CommonMethod.getReturnMessageError("申请ID不能为空！");
        }

        Optional<RegisterApply> op = registerApplyRepository.findById(applyId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("申请记录不存在！");
        }

        RegisterApply apply = op.get();
        if (apply.getStatus() != null && apply.getStatus() != 0) {
            return CommonMethod.getReturnMessageError("该申请已审核，不能重复操作！");
        }

        Optional<User> uOp = userRepository.findByUserName(apply.getUsername());
        if (uOp.isPresent()) {
            return CommonMethod.getReturnMessageError("该账号已存在，无法重复开户！");
        }

        UserType ut = null;
        Person p = new Person();
        p.setNum(apply.getUsername());
        p.setName(apply.getName());
        p.setEmail(apply.getEmail());
        p.setPhone(apply.getPhone());
        p.setDept(apply.getDept());

        if ("ADMIN".equals(apply.getRole())) {
            p.setType("0");
            ut = userTypeRepository.findByName(EUserType.ROLE_ADMIN.name());
        } else if ("TEACHER".equals(apply.getRole())) {
            p.setType("2");
            ut = userTypeRepository.findByName(EUserType.ROLE_TEACHER.name());
        } else {
            return CommonMethod.getReturnMessageError("申请角色错误！");
        }

        p = personRepository.saveAndFlush(p);

        User u = new User();
        u.setPersonId(p.getPersonId());
        u.setUserType(ut);
        u.setUserName(apply.getUsername());
        u.setPassword(apply.getPassword());
        u.setCreateTime(DateTimeTool.parseDateTime(new Date()));
        u.setCreatorId(p.getPersonId());
        u.setLoginCount(0);
        userRepository.saveAndFlush(u);

        if ("TEACHER".equals(apply.getRole())) {
            Teacher t = new Teacher();
            t.setPerson(p);
            t.setTitle("");
            t.setDegree("");
            teacherRepository.saveAndFlush(t);
        }

        apply.setStatus(1);
        apply.setApproveTime(DateTimeTool.parseDateTime(new Date()));
        apply.setRemark(remark == null ? "审核通过" : remark);

        registerApplyRepository.saveAndFlush(apply);
        return CommonMethod.getReturnMessageOK("审核通过，账号创建成功！");
    }
}
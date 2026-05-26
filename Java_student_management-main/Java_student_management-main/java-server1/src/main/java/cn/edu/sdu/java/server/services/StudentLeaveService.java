package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.StudentLeave;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.OptionItem;
import cn.edu.sdu.java.server.payload.response.OptionItemList;
import cn.edu.sdu.java.server.repositorys.StudentLeaveRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.ComDataUtil;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentLeaveService {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentLeaveRepository studentLeaveRepository;

    public StudentLeaveService(
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            StudentLeaveRepository studentLeaveRepository
    ) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentLeaveRepository = studentLeaveRepository;
    }

    public OptionItemList getTeacherItemOptionList(DataRequest dataRequest) {
        List<Teacher> teacherList = teacherRepository.findAll();
        List<OptionItem> itemList = new ArrayList<>();
        for (Teacher teacher : teacherList) {
            itemList.add(new OptionItem(
                    teacher.getPersonId(),
                    teacher.getPersonId() + "",
                    teacher.getPerson().getNum() + "-" + teacher.getPerson().getName()
            ));
        }
        return new OptionItemList(0, itemList);
    }

    public DataResponse getStudentLeaveList(DataRequest dataRequest) {
        String roleName = CommonMethod.getRoleName();
        String userName = CommonMethod.getUsername();
        Integer state = dataRequest.getInteger("state");
        if (state == null) {
            state = -1;
        }
        String search = dataRequest.getString("search");

        List<StudentLeave> leaveList = switch (roleName) {
            case "ROLE_STUDENT" -> studentLeaveRepository.getStudentLeaveList(state, search, userName, "");
            case "ROLE_TEACHER" -> studentLeaveRepository.getStudentLeaveList(state, search, "", userName);
            case "ROLE_ADMIN" -> studentLeaveRepository.getStudentLeaveList(state, search, "", "");
            default -> new ArrayList<>();
        };

        List<Map<String, Object>> dataList = new ArrayList<>();
        ComDataUtil di = ComDataUtil.getInstance();
        for (StudentLeave leave : leaveList) {
            Map<String, Object> map = new HashMap<>();
            Student student = leave.getStudent();
            Teacher teacher = leave.getTeacher();
            map.put("studentLeaveId", leave.getStudentLeaveId());
            map.put("studentNum", student.getPerson().getNum());
            map.put("studentName", student.getPerson().getName());
            map.put("studentId", student.getPersonId());
            map.put("teacherName", teacher == null ? "" : teacher.getPerson().getNum() + "-" + teacher.getPerson().getName());
            map.put("state", leave.getState());
            map.put("stateName", di.getDictionaryLabelByValue("SHZTM", leave.getState() + ""));
            map.put("reason", leave.getReason());
            map.put("leaveDate", leave.getLeaveDate());
            map.put("adminComment", leave.getAdminComment());
            map.put("teacherId", teacher == null ? null : teacher.getPersonId());
            map.put("teacherComment", leave.getTeacherComment());
            dataList.add(map);
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse studentLeaveSave(DataRequest dataRequest) {
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        Integer teacherId = dataRequest.getInteger("teacherId");
        String leaveDate = dataRequest.getString("leaveDate");
        String reason = dataRequest.getString("reason");

        StudentLeave leave = null;
        if (studentLeaveId != null && studentLeaveId > 0) {
            leave = studentLeaveRepository.findById(studentLeaveId).orElse(null);
        }

        if (leave == null) {
            leave = new StudentLeave();
            leave.setState(0);
            leave.setApplyTime(new Date());
            leave.setTeacherComment("");
            leave.setAdminComment("");
            leave.setStudent(studentRepository.findByPersonNum(CommonMethod.getUsername()).orElse(null));
        } else {
            if (leave.getStudent() == null || leave.getStudent().getPerson() == null
                    || !Objects.equals(leave.getStudent().getPerson().getNum(), CommonMethod.getUsername())) {
                return CommonMethod.getReturnMessageError("只能修改自己的请假记录！");
            }
            if (leave.getState() != null && leave.getState() != 0) {
                return CommonMethod.getReturnMessageError("仅待审核的请假可以修改！");
            }
        }

        if (leave.getStudent() == null) {
            return CommonMethod.getReturnMessageError("学生不存在！");
        }

        if (teacherId != null && teacherId > 0) {
            Optional<Teacher> teacherOp = teacherRepository.findById(teacherId);
            teacherOp.ifPresent(leave::setTeacher);
        }
        if (leave.getTeacher() == null) {
            return CommonMethod.getReturnMessageError("请选择审批教师！");
        }

        leave.setLeaveDate(leaveDate);
        leave.setReason(reason);
        leave.setState(0);
        leave.setTeacherComment("");
        leave.setTeacherTime(null);
        leave.setAdminComment("");
        leave.setAdminTime(null);
        studentLeaveRepository.save(leave);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse studentLeaveCheck(DataRequest dataRequest) {
        Integer state = dataRequest.getInteger("state");
        Integer studentLeaveId = dataRequest.getInteger("studentLeaveId");
        String teacherComment = dataRequest.getString("teacherComment");

        StudentLeave leave = null;
        if (studentLeaveId != null && studentLeaveId > 0) {
            leave = studentLeaveRepository.findById(studentLeaveId).orElse(null);
        }
        if (leave == null) {
            return CommonMethod.getReturnMessageError("请假记录不存在！");
        }
        if (leave.getTeacher() == null || leave.getTeacher().getPerson() == null
                || !Objects.equals(leave.getTeacher().getPerson().getNum(), CommonMethod.getUsername())) {
            return CommonMethod.getReturnMessageError("只能审核分配给自己的请假记录！");
        }
        if (state == null || (state != 1 && state != 2)) {
            return CommonMethod.getReturnMessageError("审核状态错误！");
        }

        leave.setTeacherComment(teacherComment);
        leave.setTeacherTime(new Date());
        leave.setState(state);
        leave.setAdminComment("");
        leave.setAdminTime(null);
        studentLeaveRepository.save(leave);
        return CommonMethod.getReturnMessageOK();
    }
}


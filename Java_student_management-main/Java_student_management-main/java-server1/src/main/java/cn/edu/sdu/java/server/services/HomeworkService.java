package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.HomeworkAssignment;
import cn.edu.sdu.java.server.models.HomeworkSubmission;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.HomeworkAssignmentRepository;
import cn.edu.sdu.java.server.repositorys.HomeworkSubmissionRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class HomeworkService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public HomeworkService(HomeworkAssignmentRepository homeworkAssignmentRepository,
                           HomeworkSubmissionRepository homeworkSubmissionRepository,
                           StudentRepository studentRepository,
                           TeacherRepository teacherRepository) {
        this.homeworkAssignmentRepository = homeworkAssignmentRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    public DataResponse getHomeworkList(DataRequest dataRequest) {
        String keyword = dataRequest.getString("keyword");
        String role = CommonMethod.getRoleName();
        Integer personId = CommonMethod.getPersonId();
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findByKeyword(keyword == null ? "" : keyword);
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (HomeworkAssignment assignment : assignments) {
            HomeworkSubmission submission = null;
            if ("ROLE_STUDENT".equals(role) && personId != null) {
                submission = homeworkSubmissionRepository
                        .findByAssignmentHomeworkIdAndStudentPersonId(assignment.getHomeworkId(), personId)
                        .orElse(null);
            }
            dataList.add(getHomeworkMap(assignment, submission));
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse homeworkSave(DataRequest dataRequest) {
        String role = CommonMethod.getRoleName();
        if (!"ROLE_TEACHER".equals(role) && !"ROLE_ADMIN".equals(role)) {
            return CommonMethod.getReturnMessageError("只有教师或管理员可以发布作业。");
        }
        Integer homeworkId = dataRequest.getInteger("homeworkId");
        HomeworkAssignment assignment = homeworkId == null
                ? new HomeworkAssignment()
                : homeworkAssignmentRepository.findById(homeworkId).orElse(new HomeworkAssignment());
        assignment.setTitle(dataRequest.getString("title"));
        assignment.setDescription(dataRequest.getString("description"));
        assignment.setDueDate(dataRequest.getString("dueDate"));
        Double totalScore = dataRequest.getDouble("totalScore");
        if (totalScore == null) {
            totalScore = 100d;
        }
        if (Double.isNaN(totalScore) || Double.isInfinite(totalScore) || totalScore <= 0 || totalScore > 1000) {
            return CommonMethod.getReturnMessageError("\u4f5c\u4e1a\u6ee1\u5206\u5fc5\u987b\u5927\u4e8e 0\uff0c\u4e14\u4e0d\u80fd\u8d85\u8fc7 1000\u3002");
        }
        assignment.setTotalScore(totalScore);
        if (assignment.getCreateTime() == null || assignment.getCreateTime().isBlank()) {
            assignment.setCreateTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        }
        Integer personId = CommonMethod.getPersonId();
        if ("ROLE_TEACHER".equals(role) && personId != null) {
            teacherRepository.findById(personId).ifPresent(assignment::setTeacher);
        }
        homeworkAssignmentRepository.save(assignment);
        return CommonMethod.getReturnData(getHomeworkMap(assignment, null), "作业保存成功。");
    }

    public DataResponse homeworkDelete(DataRequest dataRequest) {
        String role = CommonMethod.getRoleName();
        if (!"ROLE_TEACHER".equals(role) && !"ROLE_ADMIN".equals(role)) {
            return CommonMethod.getReturnMessageError("只有教师或管理员可以删除作业。");
        }
        Integer homeworkId = dataRequest.getInteger("homeworkId");
        if (homeworkId != null) {
            homeworkSubmissionRepository.findSubmissionList(homeworkId, "").forEach(homeworkSubmissionRepository::delete);
            homeworkAssignmentRepository.findById(homeworkId).ifPresent(homeworkAssignmentRepository::delete);
        }
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse uploadHomeworkImage(byte[] data, Integer homeworkId, String fileName) {
        if (homeworkId == null || data == null || data.length == 0) {
            return CommonMethod.getReturnMessageError("请先保存作业，再上传题图。");
        }
        Optional<HomeworkAssignment> op = homeworkAssignmentRepository.findById(homeworkId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("作业不存在。");
        }
        HomeworkAssignment assignment = op.get();
        assignment.setImageData(data);
        assignment.setImageName(fileName);
        homeworkAssignmentRepository.save(assignment);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse submitHomework(DataRequest dataRequest) {
        if (!"ROLE_STUDENT".equals(CommonMethod.getRoleName())) {
            return CommonMethod.getReturnMessageError("只有学生可以提交作业。");
        }
        Integer homeworkId = dataRequest.getInteger("homeworkId");
        Integer personId = CommonMethod.getPersonId();
        if (homeworkId == null || personId == null) {
            return CommonMethod.getReturnMessageError("作业或学生信息为空。");
        }
        HomeworkAssignment assignment = homeworkAssignmentRepository.findById(homeworkId).orElse(null);
        Student student = studentRepository.findById(personId).orElse(null);
        if (assignment == null || student == null) {
            return CommonMethod.getReturnMessageError("作业或学生不存在。");
        }
        HomeworkSubmission submission = homeworkSubmissionRepository
                .findByAssignmentHomeworkIdAndStudentPersonId(homeworkId, personId)
                .orElse(new HomeworkSubmission());
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent(dataRequest.getString("content"));
        submission.setSubmitTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        submission.setState("SUBMITTED");
        submission.setGrade(null);
        submission.setTeacherComment("");
        homeworkSubmissionRepository.save(submission);
        return CommonMethod.getReturnData(getSubmissionMap(submission), "作业提交成功。");
    }

    public DataResponse uploadSubmissionImage(byte[] data, Integer submissionId, String fileName) {
        if (submissionId == null || data == null || data.length == 0) {
            return CommonMethod.getReturnMessageError("请先提交作业，再上传图片。");
        }
        Optional<HomeworkSubmission> op = homeworkSubmissionRepository.findById(submissionId);
        if (op.isEmpty()) {
            return CommonMethod.getReturnMessageError("提交记录不存在。");
        }
        HomeworkSubmission submission = op.get();
        submission.setImageData(data);
        submission.setImageName(fileName);
        homeworkSubmissionRepository.save(submission);
        return CommonMethod.getReturnMessageOK();
    }

    public DataResponse getSubmissionList(DataRequest dataRequest) {
        String role = CommonMethod.getRoleName();
        Integer homeworkId = dataRequest.getInteger("homeworkId");
        String keyword = dataRequest.getString("keyword");
        List<HomeworkSubmission> submissions;
        if ("ROLE_STUDENT".equals(role)) {
            Integer personId = CommonMethod.getPersonId();
            submissions = personId == null ? new ArrayList<>() : homeworkSubmissionRepository.findByStudentPersonId(personId);
        } else {
            submissions = homeworkSubmissionRepository.findSubmissionList(homeworkId == null ? 0 : homeworkId, keyword == null ? "" : keyword);
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (HomeworkSubmission submission : submissions) {
            dataList.add(getSubmissionMap(submission));
        }
        return CommonMethod.getReturnData(dataList);
    }

    public DataResponse gradeSubmission(DataRequest dataRequest) {
        String role = CommonMethod.getRoleName();
        if (!"ROLE_TEACHER".equals(role) && !"ROLE_ADMIN".equals(role)) {
            return CommonMethod.getReturnMessageError("只有教师或管理员可以评分。");
        }
        Integer submissionId = dataRequest.getInteger("submissionId");
        Double grade = dataRequest.getDouble("grade");
        if (submissionId == null || grade == null) {
            return CommonMethod.getReturnMessageError("请选择提交记录并填写分数。");
        }
        HomeworkSubmission submission = homeworkSubmissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            return CommonMethod.getReturnMessageError("提交记录不存在。");
        }
        double totalScore = submission.getAssignment().getTotalScore() == null ? 100d : submission.getAssignment().getTotalScore();
        if (Double.isNaN(grade) || Double.isInfinite(grade) || grade < 0 || grade > totalScore) {
            return CommonMethod.getReturnMessageError("\u8bc4\u5206\u5fc5\u987b\u5728 0 \u5230\u6ee1\u5206 " + trimNumber(totalScore) + " \u4e4b\u95f4\u3002");
        }
        submission.setGrade(grade);
        submission.setTeacherComment(dataRequest.getString("teacherComment"));
        submission.setState("GRADED");
        homeworkSubmissionRepository.save(submission);
        return CommonMethod.getReturnMessageOK();
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    public ResponseEntity<StreamingResponseBody> getHomeworkImage(DataRequest dataRequest) {
        Integer homeworkId = dataRequest.getInteger("homeworkId");
        byte[] data = homeworkId == null ? null : homeworkAssignmentRepository.findById(homeworkId).map(HomeworkAssignment::getImageData).orElse(null);
        return getImageResponse(data);
    }

    public ResponseEntity<StreamingResponseBody> getSubmissionImage(DataRequest dataRequest) {
        Integer submissionId = dataRequest.getInteger("submissionId");
        byte[] data = submissionId == null ? null : homeworkSubmissionRepository.findById(submissionId).map(HomeworkSubmission::getImageData).orElse(null);
        return getImageResponse(data);
    }

    private ResponseEntity<StreamingResponseBody> getImageResponse(byte[] data) {
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }
        StreamingResponseBody stream = outputStream -> outputStream.write(data);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(stream);
    }

    private Map<String, Object> getHomeworkMap(HomeworkAssignment assignment, HomeworkSubmission submission) {
        Map<String, Object> map = new HashMap<>();
        map.put("homeworkId", assignment.getHomeworkId());
        map.put("title", assignment.getTitle());
        map.put("description", assignment.getDescription());
        map.put("dueDate", assignment.getDueDate());
        map.put("totalScore", assignment.getTotalScore());
        map.put("createTime", assignment.getCreateTime());
        map.put("hasImage", assignment.getImageData() != null && assignment.getImageData().length > 0);
        if (assignment.getTeacher() != null && assignment.getTeacher().getPerson() != null) {
            map.put("teacherName", assignment.getTeacher().getPerson().getName());
        } else {
            map.put("teacherName", "管理员");
        }
        if (submission != null) {
            map.put("submissionId", submission.getSubmissionId());
            map.put("content", submission.getContent());
            map.put("submitState", getSubmissionStateName(submission));
            map.put("grade", submission.getGrade());
            map.put("teacherComment", submission.getTeacherComment());
            map.put("hasSubmissionImage", submission.getImageData() != null && submission.getImageData().length > 0);
        }
        return map;
    }

    private Map<String, Object> getSubmissionMap(HomeworkSubmission submission) {
        Map<String, Object> map = new HashMap<>();
        HomeworkAssignment assignment = submission.getAssignment();
        Person person = submission.getStudent().getPerson();
        map.put("submissionId", submission.getSubmissionId());
        map.put("homeworkId", assignment.getHomeworkId());
        map.put("title", assignment.getTitle());
        map.put("studentNum", person.getNum());
        map.put("studentName", person.getName());
        map.put("content", submission.getContent());
        map.put("submitTime", submission.getSubmitTime());
        map.put("grade", submission.getGrade());
        map.put("totalScore", assignment.getTotalScore());
        map.put("teacherComment", submission.getTeacherComment());
        map.put("state", submission.getState());
        map.put("stateName", getSubmissionStateName(submission));
        map.put("hasImage", submission.getImageData() != null && submission.getImageData().length > 0);
        return map;
    }

    private String getSubmissionStateName(HomeworkSubmission submission) {
        if (submission == null) {
            return "\u672a\u63d0\u4ea4";
        }
        String state = submission.getState();
        String normalizedState = state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
        if ("GRADED".equals(normalizedState)) {
            return "\u5df2\u8bc4\u5206";
        }
        if ("SUBMITTED".equals(normalizedState)) {
            return "\u5df2\u63d0\u4ea4";
        }
        if (submission.getGrade() != null) {
            return "\u5df2\u8bc4\u5206";
        }
        if (submission.getSubmitTime() != null && !submission.getSubmitTime().isBlank()) {
            return "\u5df2\u63d0\u4ea4";
        }
        if (submission.getContent() != null && !submission.getContent().isBlank()) {
            return "\u5df2\u63d0\u4ea4";
        }
        return "\u672a\u63d0\u4ea4";
    }

    private String getStateName(String state) {
        if ("GRADED".equals(state)) {
            return "已评分";
        }
        if ("SUBMITTED".equals(state)) {
            return "已提交";
        }
        return "未提交";
    }
}

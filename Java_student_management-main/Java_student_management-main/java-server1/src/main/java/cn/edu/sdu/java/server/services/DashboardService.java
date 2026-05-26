package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.CommonMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final ScoreRepository scoreRepository;
    private final RegisterApplyRepository registerApplyRepository;
    private final StudentLeaveRepository studentLeaveRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CourseMaterialRepository courseMaterialRepository;

    public DashboardService(StudentRepository studentRepository,
                            TeacherRepository teacherRepository,
                            CourseRepository courseRepository,
                            ScoreRepository scoreRepository,
                            RegisterApplyRepository registerApplyRepository,
                            StudentLeaveRepository studentLeaveRepository,
                            HomeworkAssignmentRepository homeworkAssignmentRepository,
                            HomeworkSubmissionRepository homeworkSubmissionRepository,
                            CommunityPostRepository communityPostRepository,
                            CourseMaterialRepository courseMaterialRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.courseRepository = courseRepository;
        this.scoreRepository = scoreRepository;
        this.registerApplyRepository = registerApplyRepository;
        this.studentLeaveRepository = studentLeaveRepository;
        this.homeworkAssignmentRepository = homeworkAssignmentRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
        this.communityPostRepository = communityPostRepository;
        this.courseMaterialRepository = courseMaterialRepository;
    }

    @Transactional(readOnly = true)
    public DataResponse getDashboardSummary(DataRequest dataRequest) {
        String role = CommonMethod.getRoleName();
        Map<String, Object> summary = switch (role == null ? "" : role) {
            case "ROLE_ADMIN" -> buildAdminSummary();
            case "ROLE_TEACHER" -> buildTeacherSummary();
            case "ROLE_STUDENT" -> buildStudentSummary();
            default -> buildEmptySummary(role);
        };
        return CommonMethod.getReturnData(summary);
    }

    private Map<String, Object> buildAdminSummary() {
        List<StudentLeave> leaveList = studentLeaveRepository.findAll();
        List<HomeworkSubmission> submissionList = homeworkSubmissionRepository.findAll();
        List<Score> scoreList = scoreRepository.findAll();
        long pendingApplyCount = registerApplyRepository.findByStatusOrderByApplyIdDesc(0).size();
        long pendingLeaveCount = countLeaveState(leaveList, 0);
        long ungradedCount = countUngradedSubmissions(submissionList);
        Map<String, Object> summary = baseSummary("管理员");
        summary.put("kpiList", List.of(
                kpi("学生总数", studentRepository.count(), "人", "blue"),
                kpi("教师总数", teacherRepository.count(), "人", "green"),
                kpi("课程数量", courseRepository.count(), "门", "purple"),
                kpi("成绩记录", scoreRepository.count(), "条", "orange"),
                kpi("待审账号", pendingApplyCount, "条", "red"),
                kpi("请假待审", pendingLeaveCount, "条", "amber"),
                kpi("作业数量", homeworkAssignmentRepository.count(), "个", "indigo"),
                kpi("社区帖子", communityPostRepository.count(), "篇", "teal")
        ));
        summary.put("todoList", List.of(
                todo("待审核账号申请", pendingApplyCount, "register-apply-list", "账号申请审核", "account"),
                todo("待批改作业提交", ungradedCount, "homework-panel", "作业中心", "homework")
        ));
        summary.put("recentList", recentPosts());
        summary.put("chartData", Map.of(
                "scoreAvgByCourse", scoreAverageByCourse(scoreList, 8),
                "homeworkStatus", homeworkStatusChart(submissionList),
                "leaveStatus", leaveStatusChart(leaveList)
        ));
        return summary;
    }

    private Map<String, Object> buildTeacherSummary() {
        Integer personId = CommonMethod.getPersonId();
        List<StudentLeave> teacherLeaves = studentLeaveRepository.findAll().stream()
                .filter(leave -> leave.getTeacher() != null && Objects.equals(leave.getTeacher().getPersonId(), personId))
                .toList();
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findAll().stream()
                .filter(assignment -> isTeacherAssignment(assignment, personId))
                .toList();
        Set<Integer> assignmentIds = assignments.stream().map(HomeworkAssignment::getHomeworkId).collect(Collectors.toSet());
        List<HomeworkSubmission> submissions = homeworkSubmissionRepository.findAll().stream()
                .filter(submission -> submission.getAssignment() != null && assignmentIds.contains(submission.getAssignment().getHomeworkId()))
                .toList();
        List<Score> scoreList = scoreRepository.findAll();
        long pendingLeaveCount = countLeaveState(teacherLeaves, 0);
        long ungradedCount = countUngradedSubmissions(submissions);
        Map<String, Object> summary = baseSummary("教师");
        summary.put("kpiList", List.of(
                kpi("待审批请假", pendingLeaveCount, "条", "amber"),
                kpi("已发布作业", assignments.size(), "个", "blue"),
                kpi("待批改提交", ungradedCount, "份", "red"),
                kpi("平均成绩", formatAverage(scoreList), "分", "green"),
                kpi("课程资料", courseMaterialRepository.count(), "份", "purple"),
                kpi("社区帖子", communityPostRepository.count(), "篇", "teal")
        ));
        summary.put("todoList", List.of(
                todo("待审批学生请假", pendingLeaveCount, "student-leave-panel", "学生请假", "leave"),
                todo("待批改作业提交", ungradedCount, "homework-panel", "作业中心", "homework"),
                todo("维护课程资料", courseMaterialRepository.count(), "course-material-panel", "课程资料", "material")
        ));
        summary.put("recentList", recentPosts());
        summary.put("chartData", Map.of(
                "scoreAvgByCourse", scoreAverageByCourse(scoreList, 8),
                "homeworkStatus", homeworkStatusChart(submissions),
                "leaveStatus", leaveStatusChart(teacherLeaves)
        ));
        return summary;
    }

    private Map<String, Object> buildStudentSummary() {
        Integer personId = CommonMethod.getPersonId();
        List<Score> scores = personId == null ? List.of() : scoreRepository.findByStudentPersonId(personId);
        List<StudentLeave> leaves = studentLeaveRepository.findAll().stream()
                .filter(leave -> leave.getStudent() != null && Objects.equals(leave.getStudent().getPersonId(), personId))
                .toList();
        List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findAll();
        List<HomeworkSubmission> submissions = personId == null ? List.of() : homeworkSubmissionRepository.findByStudentPersonId(personId);
        Map<Integer, HomeworkSubmission> submissionMap = submissions.stream()
                .filter(submission -> submission.getAssignment() != null)
                .collect(Collectors.toMap(submission -> submission.getAssignment().getHomeworkId(), Function.identity(), (a, b) -> a));
        long pendingHomeworkCount = assignments.stream()
                .filter(assignment -> isPendingForStudent(submissionMap.get(assignment.getHomeworkId())))
                .count();
        long gradedHomeworkCount = submissions.stream().filter(this::isGradedSubmission).count();
        long courseCount = scores.stream()
                .filter(score -> score.getCourse() != null && score.getCourse().getCourseId() != null)
                .map(score -> score.getCourse().getCourseId())
                .distinct()
                .count();
        Map<String, Object> summary = baseSummary("学生");
        summary.put("kpiList", List.of(
                kpi("我的课程", courseCount, "门", "blue"),
                kpi("平均成绩", formatAverage(scores), "分", "green"),
                kpi("待提交作业", pendingHomeworkCount, "项", "red"),
                kpi("已评分作业", gradedHomeworkCount, "项", "purple"),
                kpi("请假申请", leaves.size(), "条", "amber"),
                kpi("课程资料", courseMaterialRepository.count(), "份", "teal")
        ));
        summary.put("todoList", List.of(
                todo("待提交作业", pendingHomeworkCount, "homework-panel", "作业中心", "homework"),
                todo("查看我的成绩", scores.size(), "score-table-panel", "成绩管理", "score"),
                todo("查看课程资料", courseMaterialRepository.count(), "course-material-panel", "课程资料", "material")
        ));
        summary.put("recentList", recentPosts());
        summary.put("chartData", Map.of(
                "scoreAvgByCourse", studentScoreChart(scores),
                "homeworkStatus", homeworkStatusChart(submissions, pendingHomeworkCount),
                "leaveStatus", leaveStatusChart(leaves)
        ));
        return summary;
    }

    private Map<String, Object> buildEmptySummary(String role) {
        Map<String, Object> summary = baseSummary(role == null ? "" : role);
        summary.put("kpiList", List.of());
        summary.put("todoList", List.of());
        summary.put("recentList", List.of());
        summary.put("chartData", Map.of());
        return summary;
    }

    private Map<String, Object> baseSummary(String roleLabel) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", roleLabel);
        map.put("kpiList", List.of());
        map.put("todoList", List.of());
        map.put("recentList", List.of());
        map.put("chartData", Map.of());
        return map;
    }

    private Map<String, Object> kpi(String title, Object value, String unit, String tone) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("value", value);
        map.put("unit", unit);
        map.put("tone", tone);
        return map;
    }

    private Map<String, Object> todo(String title, Object count, String targetPage, String targetTitle, String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("count", count);
        map.put("targetPage", targetPage);
        map.put("targetTitle", targetTitle);
        map.put("type", type);
        return map;
    }

    private Map<String, Object> recent(String title, String subtitle, String time, String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("subtitle", subtitle);
        map.put("time", time);
        map.put("type", type);
        return map;
    }

    private List<Map<String, Object>> recentPosts() {
        return communityPostRepository.findAllByOrderByUpdatedTimeDescCreatedTimeDesc().stream()
                .limit(6)
                .map(post -> recent(
                        safe(post.getTitle()),
                        (post.getCategory() == null || post.getCategory().isBlank() ? "社区动态" : post.getCategory())
                                + (post.getAuthor() == null ? "" : " · " + safe(post.getAuthor().getName())),
                        formatDate(post.getUpdatedTime() == null ? post.getCreatedTime() : post.getUpdatedTime()),
                        "post"
                ))
                .toList();
    }

    private List<Map<String, Object>> scoreAverageByCourse(List<Score> scores, int limit) {
        Map<String, List<Score>> grouped = scores.stream()
                .filter(score -> score.getCourse() != null && score.getMark() != null)
                .collect(Collectors.groupingBy(score -> safe(score.getCourse().getName()), LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> chartPoint(entry.getKey(), average(entry.getValue())))
                .sorted((a, b) -> Double.compare(toDouble(b.get("value")), toDouble(a.get("value"))))
                .limit(limit)
                .toList();
    }

    private List<Map<String, Object>> studentScoreChart(List<Score> scores) {
        return scores.stream()
                .filter(score -> score.getCourse() != null && score.getMark() != null)
                .map(score -> chartPoint(safe(score.getCourse().getName()), score.getMark()))
                .toList();
    }

    private List<Map<String, Object>> homeworkStatusChart(List<HomeworkSubmission> submissions) {
        long graded = submissions.stream().filter(this::isGradedSubmission).count();
        long submitted = submissions.stream().filter(submission -> !isGradedSubmission(submission) && isSubmittedSubmission(submission)).count();
        long draft = submissions.size() - graded - submitted;
        return List.of(
                chartPoint("已评分", graded),
                chartPoint("已提交", submitted),
                chartPoint("未评分", Math.max(draft, 0))
        );
    }

    private List<Map<String, Object>> homeworkStatusChart(List<HomeworkSubmission> submissions, long pendingCount) {
        long graded = submissions.stream().filter(this::isGradedSubmission).count();
        long submitted = submissions.stream().filter(submission -> !isGradedSubmission(submission) && isSubmittedSubmission(submission)).count();
        return List.of(
                chartPoint("待提交", pendingCount),
                chartPoint("已提交", submitted),
                chartPoint("已评分", graded)
        );
    }

    private List<Map<String, Object>> leaveStatusChart(List<StudentLeave> leaveList) {
        return List.of(
                chartPoint("待审核", countLeaveState(leaveList, 0)),
                chartPoint("已通过", countLeaveState(leaveList, 1)),
                chartPoint("已驳回", countLeaveState(leaveList, 2))
        );
    }

    private Map<String, Object> chartPoint(String name, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name == null || name.isBlank() ? "未命名" : name);
        map.put("value", value);
        return map;
    }

    private boolean isTeacherAssignment(HomeworkAssignment assignment, Integer personId) {
        if (assignment.getTeacher() == null || personId == null) {
            return false;
        }
        return Objects.equals(assignment.getTeacher().getPersonId(), personId);
    }

    private boolean isPendingForStudent(HomeworkSubmission submission) {
        return submission == null || (!isSubmittedSubmission(submission) && !isGradedSubmission(submission));
    }

    private boolean isSubmittedSubmission(HomeworkSubmission submission) {
        if (submission == null) {
            return false;
        }
        String state = submission.getState() == null ? "" : submission.getState().trim().toUpperCase(Locale.ROOT);
        return "SUBMITTED".equals(state)
                || "GRADED".equals(state)
                || submission.getSubmitTime() != null
                || (submission.getContent() != null && !submission.getContent().isBlank());
    }

    private boolean isGradedSubmission(HomeworkSubmission submission) {
        if (submission == null) {
            return false;
        }
        String state = submission.getState() == null ? "" : submission.getState().trim().toUpperCase(Locale.ROOT);
        return "GRADED".equals(state) || submission.getGrade() != null;
    }

    private long countUngradedSubmissions(List<HomeworkSubmission> submissions) {
        return submissions.stream()
                .filter(submission -> isSubmittedSubmission(submission) && !isGradedSubmission(submission))
                .count();
    }

    private long countLeaveState(List<StudentLeave> leaveList, int state) {
        return leaveList.stream().filter(leave -> Objects.equals(leave.getState(), state)).count();
    }

    private String formatAverage(List<Score> scores) {
        List<Score> validScores = scores.stream().filter(score -> score.getMark() != null).toList();
        if (validScores.isEmpty()) {
            return "0";
        }
        return String.format(Locale.ROOT, "%.1f", average(validScores));
    }

    private double average(List<Score> scores) {
        return scores.stream()
                .filter(score -> score.getMark() != null)
                .mapToInt(Score::getMark)
                .average()
                .orElse(0d);
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0d;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
}

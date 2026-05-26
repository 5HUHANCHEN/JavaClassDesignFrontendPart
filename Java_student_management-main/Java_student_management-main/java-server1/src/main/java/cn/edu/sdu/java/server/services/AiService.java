package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.CommunityComment;
import cn.edu.sdu.java.server.models.CommunityPost;
import cn.edu.sdu.java.server.models.Course;
import cn.edu.sdu.java.server.models.CourseMaterial;
import cn.edu.sdu.java.server.models.CourseSchedule;
import cn.edu.sdu.java.server.models.HomeworkAssignment;
import cn.edu.sdu.java.server.models.HomeworkSubmission;
import cn.edu.sdu.java.server.models.Person;
import cn.edu.sdu.java.server.models.RegisterApply;
import cn.edu.sdu.java.server.models.Score;
import cn.edu.sdu.java.server.models.Student;
import cn.edu.sdu.java.server.models.StudentLeave;
import cn.edu.sdu.java.server.models.StudentStatistics;
import cn.edu.sdu.java.server.models.Teacher;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.repositorys.CommunityCommentRepository;
import cn.edu.sdu.java.server.repositorys.CommunityPostRepository;
import cn.edu.sdu.java.server.repositorys.CourseRepository;
import cn.edu.sdu.java.server.repositorys.CourseMaterialRepository;
import cn.edu.sdu.java.server.repositorys.CourseScheduleRepository;
import cn.edu.sdu.java.server.repositorys.HomeworkAssignmentRepository;
import cn.edu.sdu.java.server.repositorys.HomeworkSubmissionRepository;
import cn.edu.sdu.java.server.repositorys.RegisterApplyRepository;
import cn.edu.sdu.java.server.repositorys.ScoreRepository;
import cn.edu.sdu.java.server.repositorys.StudentLeaveRepository;
import cn.edu.sdu.java.server.repositorys.StudentRepository;
import cn.edu.sdu.java.server.repositorys.StudentStatisticsRepository;
import cn.edu.sdu.java.server.repositorys.TeacherRepository;
import cn.edu.sdu.java.server.util.CommonMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AiService {
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentLeaveRepository studentLeaveRepository;
    private final ScoreRepository scoreRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final RegisterApplyRepository registerApplyRepository;
    private final StudentStatisticsRepository studentStatisticsRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:" + DEFAULT_BASE_URL + "}")
    private String baseUrl;

    @Value("${ai.model:" + DEFAULT_MODEL + "}")
    private String model;

    public AiService(
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            StudentLeaveRepository studentLeaveRepository,
            ScoreRepository scoreRepository,
            CommunityPostRepository communityPostRepository,
            CommunityCommentRepository communityCommentRepository,
            HomeworkAssignmentRepository homeworkAssignmentRepository,
            HomeworkSubmissionRepository homeworkSubmissionRepository,
            CourseScheduleRepository courseScheduleRepository,
            CourseMaterialRepository courseMaterialRepository,
            RegisterApplyRepository registerApplyRepository,
            StudentStatisticsRepository studentStatisticsRepository,
            ObjectMapper objectMapper
    ) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.studentLeaveRepository = studentLeaveRepository;
        this.scoreRepository = scoreRepository;
        this.communityPostRepository = communityPostRepository;
        this.communityCommentRepository = communityCommentRepository;
        this.homeworkAssignmentRepository = homeworkAssignmentRepository;
        this.homeworkSubmissionRepository = homeworkSubmissionRepository;
        this.courseScheduleRepository = courseScheduleRepository;
        this.courseMaterialRepository = courseMaterialRepository;
        this.registerApplyRepository = registerApplyRepository;
        this.studentStatisticsRepository = studentStatisticsRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Transactional(readOnly = true)
    public DataResponse chat(DataRequest dataRequest) {
        String question = normalize(dataRequest.getString("question"));
        if (question.isEmpty()) {
            return CommonMethod.getReturnMessageError("请输入要咨询的问题。");
        }

        String context = buildContext(question);
        if (apiKey == null || apiKey.isBlank()) {
            return CommonMethod.getReturnData(buildLocalAnswer(question, context),
                    "AI API Key 未配置，当前返回系统本地查询结果。");
        }

        try {
            String answer = requestAiAnswer(question, context);
            return CommonMethod.getReturnData(answer, "智能助手回答完成。");
        } catch (Exception exception) {
            return CommonMethod.getReturnData(buildLocalAnswer(question, context),
                    "AI 调用失败，已返回系统本地上下文结果：" + exception.getMessage());
        }
    }

    private String requestAiAnswer(String question, String context) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.2);
        requestBody.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "你是教学管理系统中的智能助手。请严格根据提供的系统数据回答问题，不要编造不存在的信息。"
                                + "如果数据不足，请明确说明缺少哪些数据。回答要简洁、有条理，优先给出结论，再列出关键依据。"
                                + "涉及电话、地址、邮箱、成绩、作业提交、请假审批等敏感或个人数据时，只能使用上下文中已经提供的数据。"
                ),
                Map.of(
                        "role", "user",
                        "content", "用户问题：\n" + question + "\n\n系统数据：\n" + context
                )
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resolveChatCompletionsUrl()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        String content = rootNode.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("AI 返回内容为空。");
        }
        return content;
    }

    private String buildContext(String question) {
        String lowerQuestion = normalize(question).toLowerCase(Locale.ROOT);
        List<String> sectionList = new ArrayList<>();

        if (containsAny(lowerQuestion, "首页", "仪表盘", "待办", "首页数据", "系统现在怎么样", "最近有什么待办", "dashboard")) {
            sectionList.add(buildDashboardContext(question));
        }
        if (containsAny(lowerQuestion, "课程资料", "资料", "下载", "上传", "文件", "material")) {
            sectionList.add(buildCourseMaterialContext(question));
        }
        if (containsAny(lowerQuestion, "怎么用", "如何操作", "帮助", "说明", "使用说明", "提交作业", "批改作业", "上传资料", "审核账号")) {
            sectionList.add(buildSystemGuideContext());
        }

        if (containsAny(lowerQuestion, "系统", "介绍", "概况", "总览", "总体", "全部", "功能", "模块", "权限", "角色", "架构", "流程", "怎么用", "使用说明")) {
            sectionList.add(buildOverviewContext());
            sectionList.add(buildSystemGuideContext());
        }
        if (containsAny(lowerQuestion, "学生", "student", "专业", "班级", "学号", "电话", "手机号", "联系方式", "住址", "地址", "邮箱", "email")) {
            sectionList.add(buildStudentContext(question));
        }
        if (containsAny(lowerQuestion, "老师", "教师", "teacher", "职称", "学历", "电话", "手机号", "联系方式", "住址", "地址", "邮箱", "email")) {
            sectionList.add(buildTeacherContext(question));
        }
        if (containsAny(lowerQuestion, "成绩", "分数", "score", "排名", "最高", "最好", "平均分", "绩点", "gpa", "单科")) {
            sectionList.add(buildScoreContext(question));
        }
        if (containsAny(lowerQuestion, "作业", "homework", "提交", "批改", "评分", "满分", "未提交", "已提交", "已评分")) {
            sectionList.add(buildHomeworkContext(question));
        }
        if (containsAny(lowerQuestion, "请假", "假条", "审批", "leave", "待审核", "已通过", "已驳回")) {
            sectionList.add(buildLeaveContext(question));
        }
        if (containsAny(lowerQuestion, "课程", "course", "课表", "上课", "学分", "前置课")) {
            sectionList.add(buildCourseContext(question));
            sectionList.add(buildScheduleContext(question));
        }
        if (containsAny(lowerQuestion, "社区", "帖子", "评论", "贴吧", "community", "讨论")) {
            sectionList.add(buildCommunityContext(question));
        }
        if (containsAny(lowerQuestion, "账号申请", "注册申请", "申请列表", "同意", "驳回", "审核", "register")) {
            sectionList.add(buildRegisterApplyContext());
        }
        if (containsAny(lowerQuestion, "统计", "分析", "画像", "综合", "student statistics")) {
            sectionList.add(buildStatisticsContext(question));
        }

        if (sectionList.isEmpty()) {
            sectionList.add(buildOverviewContext());
            sectionList.add(buildSystemGuideContext());
            sectionList.add(buildStudentContext(""));
            sectionList.add(buildTeacherContext(""));
            sectionList.add(buildCourseContext(""));
            sectionList.add(buildCourseMaterialContext(""));
            sectionList.add(buildScoreContext(""));
            sectionList.add(buildHomeworkContext(""));
            sectionList.add(buildLeaveContext(""));
            sectionList.add(buildCommunityContext(""));
        }
        return String.join("\n\n", sectionList);
    }

    private String buildOverviewContext() {
        long homeworkCount = homeworkAssignmentRepository.count();
        long submissionCount = homeworkSubmissionRepository.count();
        long commentCount = communityCommentRepository.count();
        long scheduleCount = courseScheduleRepository.count();
        long materialCount = courseMaterialRepository.count();
        long applyCount = registerApplyRepository.count();
        long statisticsCount = studentStatisticsRepository.count();
        return "【系统概况】\n"
                + "- 当前登录角色：" + roleDisplayName() + "\n"
                + "- 学生总数：" + studentRepository.count() + "\n"
                + "- 教师总数：" + teacherRepository.count() + "\n"
                + "- 课程总数：" + courseRepository.count() + "\n"
                + "- 课程资料总数：" + materialCount + "\n"
                + "- 课表记录总数：" + scheduleCount + "\n"
                + "- 成绩记录总数：" + scoreRepository.count() + "\n"
                + "- 作业总数：" + homeworkCount + "\n"
                + "- 作业提交总数：" + submissionCount + "\n"
                + "- 请假记录总数：" + studentLeaveRepository.count() + "\n"
                + "- 校园社区帖子总数：" + communityPostRepository.count() + "\n"
                + "- 校园社区评论总数：" + commentCount + "\n"
                + "- 账号申请总数：" + applyCount + "\n"
                + "- 学生统计记录总数：" + statisticsCount + "\n";
    }

    private String buildSystemGuideContext() {
        return "【系统功能说明】\n"
                + "- 系统定位：面向管理员、教师、学生的教学管理桌面系统，覆盖首页仪表盘、人员、课程、课程资料、成绩、作业、请假、社区、统计和智能问答。\n"
                + "- 首页仪表盘：三种身份都可进入，但只显示当前角色相关的 KPI、待办、最近动态和统计图。管理员看全局审核与管理数据，教师看教学待办，学生看自己的课程、成绩、作业和请假状态。\n"
                + "- 管理员：维护学生、教师、课程、菜单、字典、账号申请、成绩、课程资料等基础数据；账号申请在“账号申请审核”中同意或驳回。\n"
                + "- 教师：维护个人简介，查看教学相关学生信息，处理请假，发布作业，查看学生提交并评分，上传或维护课程资料，查看统计分析。\n"
                + "- 学生：维护个人信息，查看自己的课程与成绩，提交作业，发起请假，下载课程资料，参与校园社区，使用智能助手查询本人相关信息。\n"
                + "- 学生提交作业：进入“作业中心”，选择作业，填写提交内容并可上传图片，提交后教师可以在提交列表中批改。\n"
                + "- 教师批改作业：进入“作业中心”，选择作业提交记录，填写分数和评语；分数必须在 0 到作业满分之间。\n"
                + "- 教师上传课程资料：进入“课程资料”，新增资料记录，选择课程并填写标题说明，保存后上传 PDF、PPT、Word、图片或文本等文件。\n"
                + "- 学生下载课程资料：进入“课程资料”，按课程或关键字查询，选择资料后点击下载；学生不能新增、修改或删除资料。\n"
                + "- 前端：JavaFX + FXML + CSS，支持默认、深色、紧凑主题。\n"
                + "- 后端：Spring Boot Controller、Service、Repository、Model 分层，数据库使用 MySQL。\n"
                + "- 典型流程：登录后按角色加载菜单，用户进入页面操作，前端提交请求，后端校验权限和业务规则，数据库保存后返回结果。\n";
    }

    private String buildDashboardContext(String question) {
        StringBuilder builder = new StringBuilder("【首页仪表盘与待办】\n");
        builder.append("- 当前登录角色：").append(roleDisplayName()).append("\n");
        if (isAdminRole()) {
            List<StudentLeave> leaveList = studentLeaveRepository.findAll();
            List<HomeworkSubmission> submissionList = homeworkSubmissionRepository.findAll();
            builder.append("- 管理员首页：学生 ").append(studentRepository.count())
                    .append(" 人，教师 ").append(teacherRepository.count())
                    .append(" 人，课程 ").append(courseRepository.count())
                    .append(" 门，成绩记录 ").append(scoreRepository.count())
                    .append(" 条，课程资料 ").append(courseMaterialRepository.count())
                    .append(" 份。\n")
                    .append("- 管理员待办：待审核账号申请 ")
                    .append(registerApplyRepository.findByStatusOrderByApplyIdDesc(0).size())
                    .append(" 条，待审核请假 ").append(countLeaveState(leaveList, 0))
                    .append(" 条，待批改作业提交 ").append(countUngradedSubmissions(submissionList))
                    .append(" 条。\n");
        } else if (isTeacherRole()) {
            Integer personId = CommonMethod.getPersonId();
            List<StudentLeave> teacherLeaves = studentLeaveRepository.findAll().stream()
                    .filter(leave -> leave.getTeacher() != null && Objects.equals(leave.getTeacher().getPersonId(), personId))
                    .toList();
            List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findAll().stream()
                    .filter(assignment -> isTeacherAssignment(assignment, personId))
                    .toList();
            List<Integer> assignmentIds = assignments.stream().map(HomeworkAssignment::getHomeworkId).toList();
            List<HomeworkSubmission> submissions = homeworkSubmissionRepository.findAll().stream()
                    .filter(submission -> submission.getAssignment() != null && assignmentIds.contains(submission.getAssignment().getHomeworkId()))
                    .toList();
            builder.append("- 教师首页：已发布作业 ").append(assignments.size())
                    .append(" 个，待审批请假 ").append(countLeaveState(teacherLeaves, 0))
                    .append(" 条，待批改提交 ").append(countUngradedSubmissions(submissions))
                    .append(" 份，课程资料 ").append(courseMaterialRepository.count())
                    .append(" 份。\n")
                    .append("- 教师待办建议：优先处理待审批请假和待批改提交，需要补充教学资源时进入“课程资料”。\n");
        } else if (isStudentRole()) {
            Integer personId = CommonMethod.getPersonId();
            List<Score> scores = personId == null ? List.of() : scoreRepository.findByStudentPersonId(personId);
            List<StudentLeave> leaves = studentLeaveRepository.findAll().stream()
                    .filter(leave -> leave.getStudent() != null && Objects.equals(leave.getStudent().getPersonId(), personId))
                    .toList();
            List<HomeworkAssignment> assignments = homeworkAssignmentRepository.findAll();
            List<HomeworkSubmission> submissions = personId == null ? List.of() : homeworkSubmissionRepository.findByStudentPersonId(personId);
            Map<Integer, HomeworkSubmission> submissionMap = new LinkedHashMap<>();
            for (HomeworkSubmission submission : submissions) {
                if (submission.getAssignment() != null) {
                    submissionMap.put(submission.getAssignment().getHomeworkId(), submission);
                }
            }
            long pendingHomeworkCount = assignments.stream()
                    .filter(assignment -> isPendingForStudent(submissionMap.get(assignment.getHomeworkId())))
                    .count();
            long gradedHomeworkCount = submissions.stream().filter(this::isGradedSubmission).count();
            long courseCount = scores.stream()
                    .filter(score -> score.getCourse() != null && score.getCourse().getCourseId() != null)
                    .map(score -> score.getCourse().getCourseId())
                    .distinct()
                    .count();
            builder.append("- 学生首页：我的课程 ").append(courseCount)
                    .append(" 门，平均成绩 ").append(formatAverageScore(scores))
                    .append(" 分，待提交作业 ").append(pendingHomeworkCount)
                    .append(" 项，已评分作业 ").append(gradedHomeworkCount)
                    .append(" 项，请假申请 ").append(leaves.size())
                    .append(" 条，可下载课程资料 ").append(courseMaterialRepository.count())
                    .append(" 份。\n")
                    .append("- 学生权限说明：学生首页不会返回其他学生的成绩、请假或作业提交详情。\n");
        } else {
            builder.append("- 当前角色无法识别，暂时只提供系统概览类信息。\n");
        }
        return builder.toString();
    }

    private String buildStudentContext(String question) {
        String normalizedQuestion = normalize(question);
        boolean countIntent = isCountIntent(normalizedQuestion);
        boolean contactIntent = isContactIntent(normalizedQuestion);
        String searchKeyword = countIntent ? "" : extractSearchKeyword(normalizedQuestion,
                "学生", "student", "信息", "相关", "情况", "查询", "查一下", "查", "数量", "多少", "总数", "人数",
                "名单", "列表", "电话", "手机号", "联系方式", "住址", "地址", "邮箱", "email", "是谁", "什么", "的");
        if (containsAny(normalizedQuestion, "成绩", "分数", "score", "平均分", "绩点", "gpa")
                && containsAny(searchKeyword, "成绩", "分数", "score", "平均分", "绩点", "gpa")) {
            searchKeyword = extractSearchKeyword(searchKeyword, "成绩", "分数", "score", "平均分", "绩点", "gpa");
        }
        String finalSearchKeyword = searchKeyword;
        List<Student> studentList = scopedStudents(studentRepository.findAll()).stream()
                .filter(student -> matchStudent(student, finalSearchKeyword))
                .toList();

        StringBuilder builder = new StringBuilder("【学生信息】\n");
        if (isStudentRole()) {
            builder.append("- 当前是学生账号，只提供当前学生本人的学生信息；不会返回其他学生的个人资料。\n");
        }
        if (countIntent || searchKeyword.isEmpty()) {
            builder.append("- 可查询学生数：").append(studentList.size()).append("\n");
        }
        if (studentList.isEmpty()) {
            builder.append("- 未检索到匹配学生。\n");
            return builder.toString();
        }

        for (Student student : studentList.stream().limit(8).toList()) {
            Person person = student.getPerson();
            builder.append("- ")
                    .append(person == null ? "" : safe(person.getNum())).append(" ")
                    .append(person == null ? "" : safe(person.getName()))
                    .append("，学院：").append(person == null ? "" : safe(person.getDept()))
                    .append("，专业：").append(safe(student.getMajor()))
                    .append("，班级：").append(safe(student.getClassName()));
            if (contactIntent) {
                if (canViewContact(person)) {
                    builder.append("，电话：").append(person == null ? "" : safe(person.getPhone()))
                            .append("，地址：").append(person == null ? "" : safe(person.getAddress()))
                            .append("，邮箱：").append(person == null ? "" : safe(person.getEmail()));
                } else {
                    builder.append("，联系方式：当前角色无权查看");
                }
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String buildTeacherContext(String question) {
        String normalizedQuestion = normalize(question);
        boolean countIntent = isCountIntent(normalizedQuestion);
        boolean contactIntent = isContactIntent(normalizedQuestion);
        String searchKeyword = countIntent ? "" : extractSearchKeyword(normalizedQuestion,
                "老师", "教师", "teacher", "信息", "相关", "情况", "查询", "查一下", "查", "数量", "多少", "总数", "人数",
                "名单", "列表", "电话", "手机号", "联系方式", "住址", "地址", "邮箱", "email", "是谁", "什么", "的");
        List<Teacher> teacherList = teacherRepository.findAll().stream()
                .filter(teacher -> matchTeacher(teacher, searchKeyword))
                .toList();

        StringBuilder builder = new StringBuilder("【教师信息】\n");
        if (countIntent || searchKeyword.isEmpty()) {
            builder.append("- 教师总数：").append(teacherList.size()).append("\n");
        }
        if (teacherList.isEmpty()) {
            builder.append("- 未检索到匹配教师。\n");
            return builder.toString();
        }

        for (Teacher teacher : teacherList.stream().limit(8).toList()) {
            Person person = teacher.getPerson();
            builder.append("- ")
                    .append(person == null ? "" : safe(person.getNum())).append(" ")
                    .append(person == null ? "" : safe(person.getName()))
                    .append("，学院：").append(person == null ? "" : safe(person.getDept()))
                    .append("，职称：").append(safe(teacher.getTitle()))
                    .append("，学历：").append(safe(teacher.getDegree()));
            if (contactIntent) {
                if (canViewContact(person)) {
                    builder.append("，电话：").append(person == null ? "" : safe(person.getPhone()))
                            .append("，地址：").append(person == null ? "" : safe(person.getAddress()))
                            .append("，邮箱：").append(person == null ? "" : safe(person.getEmail()));
                } else {
                    builder.append("，联系方式：当前角色无权查看");
                }
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String buildCourseContext(String question) {
        String normalizedQuestion = normalize(question);
        boolean countIntent = isCountIntent(normalizedQuestion);
        String searchKeyword = countIntent ? "" : extractSearchKeyword(normalizedQuestion,
                "课程", "课表", "course", "信息", "相关", "情况", "查询", "查一下", "查", "多少", "数量", "总数", "学分", "前置课", "的");
        List<Course> courseList = courseRepository.findCourseListByNumName(searchKeyword);
        if (courseList.isEmpty() && searchKeyword.isBlank()) {
            courseList = courseRepository.findAll();
        }

        StringBuilder builder = new StringBuilder("【课程信息】\n");
        if (countIntent || searchKeyword.isEmpty()) {
            builder.append("- 课程总数：").append(courseList.size()).append("\n");
        }
        if (courseList.isEmpty()) {
            builder.append("- 未检索到匹配课程。\n");
            return builder.toString();
        }
        courseList.stream().limit(10).forEach(course -> builder.append("- ")
                .append(safe(course.getNum())).append(" ")
                .append(safe(course.getName()))
                .append("，学分：").append(course.getCredit() == null ? "" : course.getCredit())
                .append("，前置课程：").append(course.getPreCourse() == null ? "无" : safe(course.getPreCourse().getName()))
                .append("\n"));
        return builder.toString();
    }

    private String buildCourseMaterialContext(String question) {
        String normalizedQuestion = normalize(question);
        boolean countIntent = isCountIntent(normalizedQuestion) || containsAny(normalizedQuestion, "多吗", "多少", "有多少");
        String searchKeyword = countIntent ? "" : extractSearchKeyword(normalizedQuestion,
                "课程资料", "资料", "文件", "下载", "上传", "教师", "老师", "学生", "有哪些", "有什么", "有啥",
                "怎么", "如何", "查询", "查一下", "查", "多吗", "多少", "有多少", "的");
        List<CourseMaterial> materialList = courseMaterialRepository.findMaterialList(0, searchKeyword);
        if (materialList.isEmpty() && !searchKeyword.isBlank()) {
            materialList = courseMaterialRepository.findMaterialList(0, "");
        }

        StringBuilder builder = new StringBuilder("【课程资料】\n");
        if (isStudentRole()) {
            builder.append("- 当前是学生账号：可以查看和下载课程资料，但不能新增、修改、上传或删除资料。\n");
        } else if (isTeacherRole() || isAdminRole()) {
            builder.append("- 当前角色可以维护课程资料：新增资料记录、保存说明、上传文件、删除资料。\n");
        }
        long fileReadyCount = materialList.stream()
                .filter(material -> material.getFileData() != null && material.getFileData().length > 0)
                .count();
        builder.append("- 匹配资料数：").append(materialList.size())
                .append("，已上传文件：").append(fileReadyCount)
                .append("，未上传文件：").append(materialList.size() - fileReadyCount)
                .append("\n");
        if (materialList.isEmpty()) {
            return builder.append("- 未检索到课程资料。\n").toString();
        }
        materialList.stream().limit(12).forEach(material -> builder.append("- ")
                .append(material.getCourse() == null ? "" : safe(material.getCourse().getName()))
                .append("：").append(safe(material.getTitle()))
                .append("，文件：").append(hasText(material.getFileName()) ? safe(material.getFileName()) : "未上传")
                .append("，类型：").append(safe(material.getFileType()))
                .append("，大小：").append(formatFileSize(material.getFileSize()))
                .append("，上传人：").append(material.getUploader() == null ? "" : safe(material.getUploader().getName()))
                .append("，上传时间：").append(safe(material.getUploadTime()))
                .append("\n"));
        if (containsAny(normalizedQuestion, "下载")) {
            builder.append("- 下载方法：进入“课程资料”，按课程或关键字查询，选中资料后点击“下载”。\n");
        }
        if (containsAny(normalizedQuestion, "上传")) {
            builder.append("- 上传方法：教师或管理员进入“课程资料”，新增或选择资料记录，保存课程、标题和说明后点击“上传文件”。\n");
        }
        return builder.toString();
    }

    private String buildScheduleContext(String question) {
        String normalizedQuestion = normalize(question);
        String searchKeyword = extractSearchKeyword(normalizedQuestion,
                "课程", "课表", "course", "上课", "安排", "信息", "相关", "情况", "查询", "查一下", "查", "的");
        List<CourseSchedule> scheduleList = courseScheduleRepository.findAllByOrderByDayOfWeekAscStartTimeAscStartWeekAsc().stream()
                .filter(schedule -> searchKeyword.isBlank() || containsIgnoreCase(schedule.getName(), searchKeyword))
                .toList();

        StringBuilder builder = new StringBuilder("【课表信息】\n");
        builder.append("- 匹配课表记录数：").append(scheduleList.size()).append("\n");
        if (scheduleList.isEmpty()) {
            return builder.append("- 未检索到匹配课表记录。\n").toString();
        }
        scheduleList.stream().limit(10).forEach(schedule -> builder.append("- ")
                .append(formatWeekday(schedule.getDayOfWeek()))
                .append(" ").append(safe(schedule.getStartTime()))
                .append("，").append(safe(schedule.getName()))
                .append("，周次：").append(schedule.getStartWeek() == null ? "" : schedule.getStartWeek())
                .append("-").append(schedule.getStopWeek() == null ? "" : schedule.getStopWeek())
                .append("\n"));
        return builder.toString();
    }

    private String buildScoreContext(String question) {
        String normalizedQuestion = normalize(question);
        String searchKeyword = extractSearchKeyword(normalizedQuestion,
                "学生", "同学", "大家", "所有", "全部", "整体", "总体", "目前", "当前", "成绩", "分数", "score",
                "排名", "更高", "最高", "最好", "比较", "谁", "查询", "查一下", "查", "平均分", "单科", "绩点",
                "gpa", "情况", "怎么样", "怎样", "如何", "咋样", "的");
        List<Score> scoreList = scoreRepository.findAll().stream()
                .filter(this::canViewScore)
                .filter(score -> matchScore(score, searchKeyword))
                .toList();

        StringBuilder builder = new StringBuilder("【成绩信息】\n");
        if (isStudentRole()) {
            builder.append("- 当前是学生账号，只提供当前学生本人的成绩信息。\n");
        }
        if (scoreList.isEmpty()) {
            builder.append("- 未检索到匹配成绩记录。\n");
            return builder.toString();
        }

        Map<Integer, StudentScoreSummary> summaryMap = new LinkedHashMap<>();
        for (Score score : scoreList) {
            if (score.getStudent() == null || score.getStudent().getPerson() == null || score.getMark() == null) {
                continue;
            }
            Integer personId = score.getStudent().getPersonId();
            StudentScoreSummary summary = summaryMap.computeIfAbsent(personId, key -> new StudentScoreSummary(
                    getStudentName(score),
                    getStudentNum(score),
                    safe(score.getStudent().getClassName()),
                    safe(score.getStudent().getMajor())
            ));
            summary.addScore(score.getMark(), score.getCourse() == null ? "" : safe(score.getCourse().getName()));
        }

        List<StudentScoreSummary> summaryList = summaryMap.values().stream()
                .sorted(Comparator.comparingDouble(StudentScoreSummary::getAverageScore).reversed())
                .toList();

        double averageScore = scoreList.stream()
                .filter(score -> score.getMark() != null)
                .mapToInt(Score::getMark)
                .average()
                .orElse(0);
        builder.append("- 成绩记录数：").append(scoreList.size()).append("\n");
        builder.append("- 参与统计学生数：").append(summaryList.size()).append("\n");
        builder.append("- 全部成绩平均分：").append(formatDouble(averageScore)).append("\n");
        Score topScore = scoreList.stream()
                .filter(score -> score.getMark() != null)
                .max(Comparator.comparingInt(Score::getMark))
                .orElse(null);
        if (topScore != null) {
            builder.append("- 当前最高单科成绩：")
                    .append(getStudentName(topScore)).append(" ")
                    .append(getStudentNum(topScore))
                    .append("，课程：").append(topScore.getCourse() == null ? "" : safe(topScore.getCourse().getName()))
                    .append("，分数：").append(topScore.getMark())
                    .append("\n");
        }

        summaryList.stream().limit(8).forEach(summary -> builder.append("- ")
                .append(summary.studentNum).append(" ")
                .append(summary.studentName)
                .append("，班级：").append(summary.className)
                .append("，专业：").append(summary.major)
                .append("，课程数：").append(summary.courseCount)
                .append("，平均分：").append(formatDouble(summary.getAverageScore()))
                .append("，最高分：").append(summary.maxScore)
                .append("，最高分课程：").append(summary.topCourseName)
                .append("\n"));
        return builder.toString();
    }

    private String buildHomeworkContext(String question) {
        String normalizedQuestion = normalize(question);
        String searchKeyword = extractSearchKeyword(normalizedQuestion,
                "作业", "homework", "提交", "批改", "评分", "满分", "未提交", "已提交", "已评分", "查询", "查一下", "查", "的");
        List<HomeworkAssignment> assignmentList = homeworkAssignmentRepository.findByKeyword(searchKeyword);
        List<HomeworkSubmission> submissionList = homeworkSubmissionRepository.findAll().stream()
                .filter(this::canViewSubmission)
                .filter(submission -> matchSubmission(submission, searchKeyword))
                .toList();

        StringBuilder builder = new StringBuilder("【作业信息】\n");
        if (isStudentRole()) {
            builder.append("- 当前是学生账号，只提供当前学生本人的作业提交与评分信息。\n");
        }
        builder.append("- 匹配作业数：").append(assignmentList.size()).append("\n");
        builder.append("- 可查询提交记录数：").append(submissionList.size()).append("\n");
        long gradedCount = submissionList.stream().filter(submission -> submission.getGrade() != null).count();
        long submittedCount = submissionList.stream().filter(submission -> submission.getSubmitTime() != null || hasText(submission.getContent())).count();
        builder.append("- 已提交记录：").append(submittedCount).append("，已评分记录：").append(gradedCount).append("\n");

        assignmentList.stream().limit(6).forEach(assignment -> builder.append("- 作业：")
                .append(safe(assignment.getTitle()))
                .append("，截止：").append(safe(assignment.getDueDate()))
                .append("，满分：").append(assignment.getTotalScore() == null ? "" : formatDouble(assignment.getTotalScore()))
                .append("，发布教师：").append(teacherName(assignment.getTeacher()))
                .append("，要求：").append(shortText(assignment.getDescription(), 80))
                .append("\n"));
        submissionList.stream().limit(8).forEach(submission -> builder.append("- 提交：")
                .append(submission.getStudent() == null || submission.getStudent().getPerson() == null ? "" : safe(submission.getStudent().getPerson().getName()))
                .append("，作业：").append(submission.getAssignment() == null ? "" : safe(submission.getAssignment().getTitle()))
                .append("，状态：").append(formatSubmissionState(submission))
                .append("，分数：").append(submission.getGrade() == null ? "未评分" : formatDouble(submission.getGrade()))
                .append("，教师评语：").append(shortText(submission.getTeacherComment(), 50))
                .append("\n"));
        return builder.toString();
    }

    private String buildLeaveContext(String question) {
        String normalizedQuestion = normalize(question);
        boolean countIntent = isCountIntent(normalizedQuestion);
        String searchKeyword = countIntent ? "" : extractSearchKeyword(normalizedQuestion,
                "请假", "假条", "审批", "leave", "记录", "信息", "相关", "情况", "查询", "查一下", "查", "数量", "多少", "总数", "的");
        List<StudentLeave> leaveList = studentLeaveRepository.getStudentLeaveList(-1, searchKeyword, "", "").stream()
                .filter(this::canViewLeave)
                .toList();

        StringBuilder builder = new StringBuilder("【请假记录】\n");
        if (isStudentRole()) {
            builder.append("- 当前是学生账号，只提供当前学生本人的请假记录。\n");
        }
        long pendingCount = leaveList.stream().filter(leave -> leave.getState() == null || leave.getState() == 0).count();
        long approvedCount = leaveList.stream().filter(leave -> leave.getState() != null && leave.getState() == 1).count();
        long rejectedCount = leaveList.stream().filter(leave -> leave.getState() != null && leave.getState() == 2).count();
        builder.append("- 请假记录总数：").append(leaveList.size()).append("\n");
        builder.append("- 待审核：").append(pendingCount)
                .append("，已通过：").append(approvedCount)
                .append("，已驳回：").append(rejectedCount)
                .append("\n");
        if (leaveList.isEmpty()) {
            return builder.append("- 未检索到匹配请假记录。\n").toString();
        }
        leaveList.stream().limit(10).forEach(leave -> builder.append("- ")
                .append(leave.getStudent() == null || leave.getStudent().getPerson() == null ? "" : safe(leave.getStudent().getPerson().getName()))
                .append("，日期：").append(safe(leave.getLeaveDate()))
                .append("，原因：").append(safe(leave.getReason()))
                .append("，审批教师：").append(teacherName(leave.getTeacher()))
                .append("，状态：").append(formatLeaveState(leave.getState()))
                .append("，教师意见：").append(safe(leave.getTeacherComment()))
                .append("\n"));
        return builder.toString();
    }

    private String buildCommunityContext(String question) {
        String normalizedQuestion = normalize(question);
        boolean countIntent = isCountIntent(normalizedQuestion);
        String searchKeyword = countIntent ? "" : extractSearchKeyword(normalizedQuestion,
                "社区", "帖子", "评论", "贴吧", "community", "讨论", "信息", "相关", "情况", "查询", "查一下", "查", "数量", "多少", "总数", "的");
        List<CommunityPost> filteredPostList = communityPostRepository.findAllByOrderByUpdatedTimeDescCreatedTimeDesc().stream()
                .filter(post -> searchKeyword.isEmpty()
                        || containsIgnoreCase(post.getTitle(), searchKeyword)
                        || containsIgnoreCase(post.getContent(), searchKeyword)
                        || containsIgnoreCase(post.getCategory(), searchKeyword))
                .toList();

        StringBuilder builder = new StringBuilder("【校园社区】\n");
        if (countIntent || searchKeyword.isEmpty()) {
            builder.append("- 匹配帖子数：").append(filteredPostList.size()).append("\n");
        }
        if (filteredPostList.isEmpty()) {
            return builder.append("- 未检索到匹配帖子。\n").toString();
        }
        filteredPostList.stream().limit(8).forEach(post -> {
            int commentCount = communityCommentRepository.findByPostCommunityPostIdOrderByCreatedTimeAsc(post.getCommunityPostId()).size();
            builder.append("- [")
                    .append(safe(post.getCategory()))
                    .append("] ")
                    .append(safe(post.getTitle()))
                    .append("，评论数：").append(commentCount)
                    .append("，内容：").append(shortText(post.getContent(), 80))
                    .append("\n");
        });
        return builder.toString();
    }

    private String buildRegisterApplyContext() {
        StringBuilder builder = new StringBuilder("【账号申请审核】\n");
        if (!isAdminRole()) {
            return builder.append("- 当前角色不是管理员，不能查询账号申请详情。\n").toString();
        }
        List<RegisterApply> applyList = registerApplyRepository.findAll();
        long pendingCount = applyList.stream().filter(apply -> Objects.equals(apply.getStatus(), 0)).count();
        long approvedCount = applyList.stream().filter(apply -> Objects.equals(apply.getStatus(), 1)).count();
        long rejectedCount = applyList.stream().filter(apply -> Objects.equals(apply.getStatus(), 2)).count();
        builder.append("- 申请总数：").append(applyList.size()).append("\n")
                .append("- 待审核：").append(pendingCount)
                .append("，已通过：").append(approvedCount)
                .append("，已驳回：").append(rejectedCount)
                .append("\n");
        applyList.stream()
                .sorted(Comparator.comparing(RegisterApply::getApplyId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .forEach(apply -> builder.append("- ")
                        .append(safe(apply.getUsername()))
                        .append("，姓名：").append(safe(apply.getName()))
                        .append("，角色：").append(formatApplyRole(apply.getRole()))
                        .append("，状态：").append(formatApplyStatus(apply.getStatus()))
                        .append("，申请时间：").append(safe(apply.getApplyTime()))
                        .append("，原因：").append(shortText(apply.getReason(), 60))
                        .append("\n"));
        return builder.toString();
    }

    private String buildStatisticsContext(String question) {
        String normalizedQuestion = normalize(question);
        String searchKeyword = extractSearchKeyword(normalizedQuestion,
                "统计", "分析", "画像", "综合", "student statistics", "查询", "查一下", "查", "的");
        List<StudentStatistics> statisticsList = studentStatisticsRepository.findAll().stream()
                .filter(this::canViewStatistics)
                .filter(statistics -> matchStatistics(statistics, searchKeyword))
                .sorted(Comparator.comparing(StudentStatistics::getGpa, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        StringBuilder builder = new StringBuilder("【学生统计分析】\n");
        if (isStudentRole()) {
            builder.append("- 当前是学生账号，只提供当前学生本人的统计记录。\n");
        }
        builder.append("- 匹配统计记录数：").append(statisticsList.size()).append("\n");
        if (statisticsList.isEmpty()) {
            return builder.append("- 未检索到统计记录。\n").toString();
        }
        statisticsList.stream().limit(8).forEach(statistics -> builder.append("- ")
                .append(statistics.getStudent() == null || statistics.getStudent().getPerson() == null ? "" : safe(statistics.getStudent().getPerson().getName()))
                .append("，年份：").append(safe(statistics.getYear()))
                .append("，课程数：").append(statistics.getCourseCount() == null ? "" : statistics.getCourseCount())
                .append("，平均分：").append(statistics.getAvgScore() == null ? "" : formatDouble(statistics.getAvgScore()))
                .append("，GPA：").append(statistics.getGpa() == null ? "" : formatDouble(statistics.getGpa()))
                .append("，作业数：").append(statistics.getHomeworkCount() == null ? "" : statistics.getHomeworkCount())
                .append("，作业均分：").append(statistics.getHomeworkAvgScore() == null ? "" : formatDouble(statistics.getHomeworkAvgScore()))
                .append("，请假数：").append(statistics.getLeaveCount() == null ? "" : statistics.getLeaveCount())
                .append("\n"));
        return builder.toString();
    }

    private List<Student> scopedStudents(List<Student> sourceList) {
        if (!isStudentRole()) {
            return sourceList;
        }
        Integer currentPersonId = CommonMethod.getPersonId();
        return sourceList.stream()
                .filter(student -> Objects.equals(student.getPersonId(), currentPersonId))
                .toList();
    }

    private boolean canViewScore(Score score) {
        if (!isStudentRole()) {
            return true;
        }
        return score != null && score.getStudent() != null && Objects.equals(score.getStudent().getPersonId(), CommonMethod.getPersonId());
    }

    private boolean canViewSubmission(HomeworkSubmission submission) {
        if (!isStudentRole()) {
            return true;
        }
        return submission != null && submission.getStudent() != null && Objects.equals(submission.getStudent().getPersonId(), CommonMethod.getPersonId());
    }

    private boolean canViewLeave(StudentLeave leave) {
        if (!isStudentRole()) {
            return true;
        }
        return leave != null && leave.getStudent() != null && Objects.equals(leave.getStudent().getPersonId(), CommonMethod.getPersonId());
    }

    private boolean canViewStatistics(StudentStatistics statistics) {
        if (!isStudentRole()) {
            return true;
        }
        return statistics != null && statistics.getStudent() != null && Objects.equals(statistics.getStudent().getPersonId(), CommonMethod.getPersonId());
    }

    private boolean canViewContact(Person person) {
        if (isAdminRole() || isTeacherRole()) {
            return true;
        }
        return person != null && Objects.equals(person.getPersonId(), CommonMethod.getPersonId());
    }

    private boolean matchStudent(Student student, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        Person person = student.getPerson();
        return containsIgnoreCase(person == null ? "" : person.getName(), keyword)
                || containsIgnoreCase(person == null ? "" : person.getNum(), keyword)
                || containsIgnoreCase(person == null ? "" : person.getDept(), keyword)
                || containsIgnoreCase(student.getMajor(), keyword)
                || containsIgnoreCase(student.getClassName(), keyword);
    }

    private boolean matchTeacher(Teacher teacher, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        Person person = teacher.getPerson();
        return containsIgnoreCase(person == null ? "" : person.getName(), keyword)
                || containsIgnoreCase(person == null ? "" : person.getNum(), keyword)
                || containsIgnoreCase(person == null ? "" : person.getDept(), keyword)
                || containsIgnoreCase(teacher.getTitle(), keyword)
                || containsIgnoreCase(teacher.getDegree(), keyword);
    }

    private boolean matchScore(Score score, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(getStudentName(score), keyword)
                || containsIgnoreCase(getStudentNum(score), keyword)
                || containsIgnoreCase(score.getStudent() == null ? "" : score.getStudent().getClassName(), keyword)
                || containsIgnoreCase(score.getStudent() == null ? "" : score.getStudent().getMajor(), keyword)
                || containsIgnoreCase(score.getCourse() == null ? "" : score.getCourse().getName(), keyword)
                || containsIgnoreCase(score.getCourse() == null ? "" : score.getCourse().getNum(), keyword);
    }

    private boolean matchSubmission(HomeworkSubmission submission, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return containsIgnoreCase(submission.getAssignment() == null ? "" : submission.getAssignment().getTitle(), keyword)
                || containsIgnoreCase(submission.getAssignment() == null ? "" : submission.getAssignment().getDescription(), keyword)
                || containsIgnoreCase(submission.getStudent() == null || submission.getStudent().getPerson() == null ? "" : submission.getStudent().getPerson().getName(), keyword)
                || containsIgnoreCase(submission.getStudent() == null || submission.getStudent().getPerson() == null ? "" : submission.getStudent().getPerson().getNum(), keyword)
                || containsIgnoreCase(submission.getContent(), keyword)
                || containsIgnoreCase(submission.getTeacherComment(), keyword);
    }

    private boolean matchStatistics(StudentStatistics statistics, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        Student student = statistics.getStudent();
        Person person = student == null ? null : student.getPerson();
        return containsIgnoreCase(person == null ? "" : person.getName(), keyword)
                || containsIgnoreCase(person == null ? "" : person.getNum(), keyword)
                || containsIgnoreCase(student == null ? "" : student.getMajor(), keyword)
                || containsIgnoreCase(student == null ? "" : student.getClassName(), keyword)
                || containsIgnoreCase(statistics.getYear(), keyword);
    }

    private boolean isTeacherAssignment(HomeworkAssignment assignment, Integer personId) {
        return assignment != null
                && assignment.getTeacher() != null
                && Objects.equals(assignment.getTeacher().getPersonId(), personId);
    }

    private boolean isPendingForStudent(HomeworkSubmission submission) {
        return submission == null || (!isSubmittedSubmission(submission) && !isGradedSubmission(submission));
    }

    private boolean isSubmittedSubmission(HomeworkSubmission submission) {
        if (submission == null) {
            return false;
        }
        String state = safe(submission.getState()).trim().toUpperCase(Locale.ROOT);
        return "SUBMITTED".equals(state)
                || "GRADED".equals(state)
                || submission.getSubmitTime() != null
                || hasText(submission.getContent());
    }

    private boolean isGradedSubmission(HomeworkSubmission submission) {
        if (submission == null) {
            return false;
        }
        String state = safe(submission.getState()).trim().toUpperCase(Locale.ROOT);
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

    private String formatAverageScore(List<Score> scores) {
        double average = scores.stream()
                .filter(score -> score.getMark() != null)
                .mapToInt(Score::getMark)
                .average()
                .orElse(0d);
        return formatDouble(average);
    }

    private String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "";
        }
        if (size < 1024) {
            return size + " B";
        }
        double kb = size / 1024d;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        return String.format(Locale.ROOT, "%.1f MB", kb / 1024d);
    }

    private String buildLocalAnswer(String question, String context) {
        return "当前问题：" + question + "\n\n"
                + "由于后端尚未配置 AI API Key，系统先返回本地检索到的上下文数据：\n\n"
                + context + "\n"
                + "后续在 application.yml 中配置好 API Key 后，这些数据会一起提交给 AI 生成自然语言回答。";
    }

    private String getStudentName(Score score) {
        return score.getStudent() == null || score.getStudent().getPerson() == null ? "" : safe(score.getStudent().getPerson().getName());
    }

    private String getStudentNum(Score score) {
        return score.getStudent() == null || score.getStudent().getPerson() == null ? "" : safe(score.getStudent().getPerson().getNum());
    }

    private String teacherName(Teacher teacher) {
        return teacher == null || teacher.getPerson() == null ? "" : safe(teacher.getPerson().getName());
    }

    private boolean isAdminRole() {
        return "ROLE_ADMIN".equals(CommonMethod.getRoleName());
    }

    private boolean isTeacherRole() {
        return "ROLE_TEACHER".equals(CommonMethod.getRoleName());
    }

    private boolean isStudentRole() {
        return "ROLE_STUDENT".equals(CommonMethod.getRoleName());
    }

    private String roleDisplayName() {
        String roleName = CommonMethod.getRoleName();
        return switch (safe(roleName)) {
            case "ROLE_ADMIN" -> "管理员";
            case "ROLE_TEACHER" -> "教师";
            case "ROLE_STUDENT" -> "学生";
            default -> "未知角色";
        };
    }

    private boolean containsAny(String text, String... keywordArray) {
        for (String keyword : keywordArray) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return safe(source).toLowerCase(Locale.ROOT).contains(safe(keyword).toLowerCase(Locale.ROOT));
    }

    private String formatLeaveState(Integer state) {
        if (state == null) {
            return "未知";
        }
        return switch (state) {
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "待审核";
        };
    }

    private String formatSubmissionState(HomeworkSubmission submission) {
        if (submission == null) {
            return "未提交";
        }
        String state = safe(submission.getState()).trim().toUpperCase(Locale.ROOT);
        if ("GRADED".equals(state) || submission.getGrade() != null) {
            return "已评分";
        }
        if ("SUBMITTED".equals(state) || submission.getSubmitTime() != null || hasText(submission.getContent())) {
            return "已提交";
        }
        return "未提交";
    }

    private String formatApplyStatus(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 1 -> "已通过";
            case 2 -> "已驳回";
            default -> "待审核";
        };
    }

    private String formatApplyRole(String role) {
        return switch (safe(role).toUpperCase(Locale.ROOT)) {
            case "ADMIN", "ROLE_ADMIN" -> "管理员";
            case "TEACHER", "ROLE_TEACHER" -> "教师";
            case "STUDENT", "ROLE_STUDENT" -> "学生";
            default -> safe(role);
        };
    }

    private String formatWeekday(Integer dayOfWeek) {
        if (dayOfWeek == null) {
            return "未设置星期";
        }
        return switch (dayOfWeek) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "星期" + dayOfWeek;
        };
    }

    private String shortText(String text, int maxLength) {
        String value = safe(text).replaceAll("\\s+", " ").trim();
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String formatDouble(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isCountIntent(String question) {
        String normalizedQuestion = normalize(question);
        return containsAny(normalizedQuestion, "数量", "多少", "总数", "人数", "几条", "统计", "概况", "汇总", "几个");
    }

    private boolean isContactIntent(String question) {
        String normalizedQuestion = normalize(question).toLowerCase(Locale.ROOT);
        return containsAny(normalizedQuestion, "电话", "手机号", "联系方式", "住址", "地址", "邮箱", "email");
    }

    private String extractSearchKeyword(String question, String... removableWords) {
        String result = normalize(question);
        for (String removableWord : removableWords) {
            result = result.replace(removableWord, " ");
        }
        result = result.replace("？", " ")
                .replace("?", " ")
                .replace("，", " ")
                .replace(",", " ")
                .replace("。", " ")
                .replace(".", " ")
                .replace("一个", " ")
                .replace("帮我", " ")
                .replace("请问", " ")
                .replace("看看", " ")
                .replace("一下子", " ")
                .replace("一下", " ")
                .replace("怎么样", " ")
                .replace("怎样", " ")
                .replace("如何", " ")
                .replace("咋样", " ")
                .replace("情况", " ")
                .replace("目前", " ")
                .replace("当前", " ")
                .replace("所有", " ")
                .replace("全部", " ")
                .replace("整体", " ")
                .replace("总体", " ")
                .replace("大家", " ")
                .replace("们", " ")
                .replace("呢", " ")
                .replace("吗", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return result;
    }

    private String resolveChatCompletionsUrl() {
        String normalizedBaseUrl = normalize(baseUrl);
        if (normalizedBaseUrl.isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        if (normalizedBaseUrl.endsWith("/chat/completions")) {
            return normalizedBaseUrl;
        }
        if (normalizedBaseUrl.endsWith("/v1")) {
            return normalizedBaseUrl + "/chat/completions";
        }
        return normalizedBaseUrl + "/chat/completions";
    }

    private static class StudentScoreSummary {
        private final String studentName;
        private final String studentNum;
        private final String className;
        private final String major;
        private int courseCount;
        private int totalScore;
        private int maxScore;
        private String topCourseName = "";

        private StudentScoreSummary(String studentName, String studentNum, String className, String major) {
            this.studentName = studentName;
            this.studentNum = studentNum;
            this.className = className;
            this.major = major;
        }

        private void addScore(int score, String courseName) {
            courseCount++;
            totalScore += score;
            if (courseCount == 1 || score > maxScore) {
                maxScore = score;
                topCourseName = courseName;
            }
        }

        private double getAverageScore() {
            if (courseCount == 0) {
                return 0;
            }
            return (double) totalScore / courseCount;
        }
    }
}

import java.sql.*;

public class DbFixRunner {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://202.194.14.120:3306/java_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "java_server";
        String password = "JavaServer2025@";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false);

            upsertMenu(conn, 32, "student-leave-panel", "学生请假", 3, "2,3");
            upsertMenu(conn, 43, "student-statistics-panel", "学生统计", 4, "1,3");

            ensureDictionary(conn, 10, "SHZTM", "审核状态", null);
            ensureDictionary(conn, 11, "0", "待审核", 10);
            ensureDictionary(conn, 12, "1", "已通过", 10);
            ensureDictionary(conn, 13, "2", "已驳回", 10);

            ensureTeacher(conn, 5, "022200", "测试教师", "软件学院", "1", "1985-09-01", "13800002200", "022200@sdu.edu.cn", "山东大学软件学院", "副教授", "硕士");

            ensureStudentLeave(conn, 1, 2, 5, "2026-04-15", "课程项目演示请假", 0, "", "");
            ensureStudentStatistics(conn, 1, 2, 5, 88.0, 16, 3.8, 1, 1, "2026");
            ensureStudentStatistics(conn, 2, 3, 4, 82.0, 12, 3.4, 0, 2, "2026");

            conn.commit();
            System.out.println("database fix completed");
        }
    }

    private static void upsertMenu(Connection conn, int id, String name, String title, Integer pid, String userTypeIds) throws SQLException {
        String sql = "INSERT INTO menu(id,name,title,pid,user_type_ids) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name), title=VALUES(title), pid=VALUES(pid), user_type_ids=VALUES(user_type_ids)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, title);
            if (pid == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, pid);
            }
            ps.setString(5, userTypeIds);
            ps.executeUpdate();
        }
    }

    private static void ensureDictionary(Connection conn, int id, String value, String label, Integer pid) throws SQLException {
        String sql = "INSERT INTO dictionary(id,value,label,pid) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE value=VALUES(value), label=VALUES(label), pid=VALUES(pid)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, value);
            ps.setString(3, label);
            if (pid == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, pid);
            }
            ps.executeUpdate();
        }
    }

    private static void ensureTeacher(Connection conn, int personId, String num, String name, String dept, String gender,
                                      String birthday, String phone, String email, String address, String title, String degree) throws SQLException {
        String personSql = "INSERT INTO person(person_id,type,num,name,dept,gender,birthday,phone,address,email,introduce) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE type=VALUES(type), num=VALUES(num), name=VALUES(name), dept=VALUES(dept), gender=VALUES(gender), birthday=VALUES(birthday), phone=VALUES(phone), address=VALUES(address), email=VALUES(email), introduce=VALUES(introduce)";
        try (PreparedStatement ps = conn.prepareStatement(personSql)) {
            ps.setInt(1, personId);
            ps.setString(2, "2");
            ps.setString(3, num);
            ps.setString(4, name);
            ps.setString(5, dept);
            ps.setString(6, gender);
            ps.setString(7, birthday);
            ps.setString(8, phone);
            ps.setString(9, address);
            ps.setString(10, email);
            ps.setString(11, "用于 JavaFX 教师端功能测试的示例教师账号。");
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO teacher(person_id,degree,title) VALUES(?,?,?) ON DUPLICATE KEY UPDATE degree=VALUES(degree), title=VALUES(title)")) {
            ps.setInt(1, personId);
            ps.setString(2, degree);
            ps.setString(3, title);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO user(person_id,user_type_id,user_name,password,create_time,creator_id,login_count) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE user_type_id=VALUES(user_type_id), user_name=VALUES(user_name), password=VALUES(password), creator_id=VALUES(creator_id), login_count=VALUES(login_count)")) {
            ps.setInt(1, personId);
            ps.setInt(2, 3);
            ps.setString(3, num);
            ps.setString(4, "$2a$10$FV5lm..jdQWmV7hFguxKDeTrGyiWg1u6HYD2QiQc0tRROrNtSQVOy");
            ps.setString(5, "2026-04-09 20:00:00");
            ps.setInt(6, 1);
            ps.setInt(7, 0);
            ps.executeUpdate();
        }
    }

    private static void ensureStudentLeave(Connection conn, int studentLeaveId, int studentId, int teacherId, String leaveDate,
                                           String reason, int state, String teacherComment, String adminComment) throws SQLException {
        String sql = "INSERT INTO student_leave(student_leave_id,student_id,teacher_id,leave_date,reason,state,apply_time,teacher_comment,admin_comment) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE student_id=VALUES(student_id), teacher_id=VALUES(teacher_id), leave_date=VALUES(leave_date), reason=VALUES(reason), state=VALUES(state), teacher_comment=VALUES(teacher_comment), admin_comment=VALUES(admin_comment)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentLeaveId);
            ps.setInt(2, studentId);
            ps.setInt(3, teacherId);
            ps.setString(4, leaveDate);
            ps.setString(5, reason);
            ps.setInt(6, state);
            ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            ps.setString(8, teacherComment);
            ps.setString(9, adminComment);
            ps.executeUpdate();
        }
    }

    private static void ensureStudentStatistics(Connection conn, int statisticsId, int personId, int courseCount, double avgScore,
                                                int creditTotal, double gpa, int leaveCount, int ranking, String year) throws SQLException {
        String sql = "INSERT INTO student_statistics(statistics_id,person_id,course_count,avg_score,credit_total,gpa,leave_count,no,year,active_count) VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE person_id=VALUES(person_id), course_count=VALUES(course_count), avg_score=VALUES(avgScore), credit_total=VALUES(creditTotal), gpa=VALUES(gpa), leave_count=VALUES(leaveCount), no=VALUES(no), year=VALUES(year), active_count=VALUES(active_count)";
        sql = sql.replace("avgScore", "avg_score").replace("creditTotal", "credit_total").replace("leaveCount", "leave_count");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, statisticsId);
            ps.setInt(2, personId);
            ps.setInt(3, courseCount);
            ps.setDouble(4, avgScore);
            ps.setInt(5, creditTotal);
            ps.setDouble(6, gpa);
            ps.setInt(7, leaveCount);
            ps.setInt(8, ranking);
            ps.setString(9, year);
            ps.setInt(10, courseCount + leaveCount);
            ps.executeUpdate();
        }
    }
}
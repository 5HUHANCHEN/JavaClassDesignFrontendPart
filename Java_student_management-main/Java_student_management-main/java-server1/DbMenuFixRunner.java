import java.sql.*;

public class DbMenuFixRunner {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://202.194.14.120:3306/java_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai";
        try (Connection conn = DriverManager.getConnection(url, "java_server", "JavaServer2025@")) {
            conn.setAutoCommit(false);
            upsertMenu(conn, 6, null, "请假管理", null, "2,3");
            upsertMenu(conn, 7, null, "统计分析", null, "1,3");
            upsertMenu(conn, 32, "student-leave-panel", "学生请假", 6, "2,3");
            upsertMenu(conn, 43, "student-statistics-panel", "学生统计", 7, "1,3");
            conn.commit();
            System.out.println("menu fix completed");
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
}
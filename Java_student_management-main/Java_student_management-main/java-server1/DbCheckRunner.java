import java.sql.*;

public class DbCheckRunner {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://202.194.14.120:3306/java_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai";
        try (Connection conn = DriverManager.getConnection(url, "java_server", "JavaServer2025@")) {
            printCount(conn, "select count(*) from menu where name in ('student-leave-panel','student-statistics-panel')", "menu_count");
            printCount(conn, "select count(*) from dictionary where value='SHZTM' or pid=10", "dictionary_count");
            printCount(conn, "select count(*) from teacher", "teacher_count");
            printCount(conn, "select count(*) from student_leave", "student_leave_count");
            printCount(conn, "select count(*) from student_statistics", "student_statistics_count");
        }
    }

    private static void printCount(Connection conn, String sql, String label) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println(label + "=" + rs.getInt(1));
            }
        }
    }
}
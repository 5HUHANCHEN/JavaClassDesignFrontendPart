import java.sql.*;
public class MenuDump {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:mysql://202.194.14.120:3306/java_2_20?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai";
    try (Connection conn = DriverManager.getConnection(url, "java_2_20", "JavaP220@");
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery("select id,pid,name,title,user_type_ids from menu order by coalesce(pid,0), id")) {
      while (rs.next()) {
        System.out.println(rs.getInt(1)+"\t"+rs.getString(2)+"\t"+rs.getString(3)+"\t"+rs.getString(4)+"\t"+rs.getString(5));
      }
    }
  }
}

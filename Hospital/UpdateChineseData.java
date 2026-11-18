import java.sql.*;

public class UpdateChineseData {
    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String dbName = System.getenv().getOrDefault("DB_NAME", "hospital_db");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to database");
            
            // 更新医院名称
            updateHospital(conn, "H001", "邛崃市医疗中心医院", "四川省邛崃市文君街");
            updateHospital(conn, "H002", "西安交通大学第一附属医院", "西安市雁塔区雁塔西路");
            updateHospital(conn, "H003", "西京医院", "西安市新城区长乐西路");
            updateHospital(conn, "H004", "陕西省人民医院", "西安市碑林区友谊西路");
            updateHospital(conn, "H005", "四川大学华西医院", "成都市武侯区国学巷");
            updateHospital(conn, "H006", "西安医学院第二附属医院", "西安市新城区尚勤路");
            updateHospital(conn, "H007", "西安市中心医院", "西安市新城区西五路");
            updateHospital(conn, "H008", "西安市第一医院", "西安市碑林区南大街");
            updateHospital(conn, "H009", "西安市第三医院", "西安市未央区凤城三路东段");
            updateHospital(conn, "H010", "西北妇女儿童医院", "西安市雁塔区雁翔路");
            
            // 更新科室名称
            updateDepartment(conn, "DEPT001", "内科", "呼吸、消化、心血管疾病", "周一至周日 08:00-17:00");
            updateDepartment(conn, "DEPT002", "外科", "普通外科、骨科、神经外科", "周一至周日 08:00-17:00");
            updateDepartment(conn, "DEPT003", "儿科", "儿童疾病、预防接种", "周一至周日 08:00-18:00");
            updateDepartment(conn, "DEPT004", "妇产科", "妇科、产前检查、分娩", "周一至周日 08:00-17:00");
            updateDepartment(conn, "DEPT005", "急诊科", "24小时急诊服务", "24小时");
            updateDepartment(conn, "DEPT006", "眼科", "眼科疾病、视力检查", "周一至周六 08:00-17:00");
            updateDepartment(conn, "DEPT007", "口腔科", "口腔护理、种植牙、正畸", "周一至周六 08:00-17:00");
            updateDepartment(conn, "DEPT008", "皮肤科", "皮肤病、性病、皮肤科", "周一至周六 08:00-17:00");
            
            // 更新医生信息
            updateDoctor(conn, "DOC001", "张建国", "主任医师", "心血管疾病、高血压");
            updateDoctor(conn, "DOC002", "李明华", "副主任医师", "消化系统疾病、胃病");
            updateDoctor(conn, "DOC003", "王芳", "主治医师", "呼吸系统疾病、肺炎");
            updateDoctor(conn, "DOC004", "赵伟", "主任医师", "普通外科、胃肠外科");
            updateDoctor(conn, "DOC005", "陈静", "副主任医师", "骨科、关节置换");
            updateDoctor(conn, "DOC006", "刘洋", "主治医师", "创伤、急诊外科");
            updateDoctor(conn, "DOC007", "周小红", "主任医师", "小儿呼吸、新生儿科");
            updateDoctor(conn, "DOC008", "吴丽娟", "副主任医师", "儿童保健、预防接种");
            updateDoctor(conn, "DOC009", "孙梅", "主任医师", "妇科肿瘤、宫腔镜");
            updateDoctor(conn, "DOC010", "郑颖", "副主任医师", "产前检查、高危妊娠");
            updateDoctor(conn, "DOC011", "钱进", "主任医师", "急诊医学、重症监护");
            updateDoctor(conn, "DOC012", "马小东", "副主任医师", "急性中毒、创伤");
            updateDoctor(conn, "DOC013", "徐光明", "主任医师", "白内障、青光眼");
            updateDoctor(conn, "DOC014", "郭婷婷", "副主任医师", "种植牙、正畸");
            updateDoctor(conn, "DOC015", "韩雪", "主治医师", "过敏性皮肤病、湿疹");
            
            // 更新管理员信息
            updateAdmin(conn, "S001", "超级管理员");
            updateAdmin(conn, "H001_ADMIN", "邛崃医院管理员");
            updateAdmin(conn, "H002_ADMIN", "交大医院管理员");
            
            // 更新用户信息
            updateUser(conn, "admin001", "超级管理员");
            
            System.out.println("All data updated successfully!");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private static void updateHospital(Connection conn, String id, String name, String address) throws SQLException {
        String sql = "UPDATE hospital SET hospital_name = ?, address = ? WHERE hospital_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, address);
            pstmt.setString(3, id);
            pstmt.executeUpdate();
            System.out.println("Updated hospital: " + id + " -> " + name);
        }
    }
    
    private static void updateDepartment(Connection conn, String id, String name, String desc, String serviceTime) throws SQLException {
        String sql = "UPDATE department SET name = ?, description = ?, service_time = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, desc);
            pstmt.setString(3, serviceTime);
            pstmt.setString(4, id);
            pstmt.executeUpdate();
            System.out.println("Updated department: " + id + " -> " + name);
        }
    }
    
    private static void updateDoctor(Connection conn, String id, String name, String title, String expertise) throws SQLException {
        String sql = "UPDATE doctor SET name = ?, title = ?, expertise = ? WHERE doctor_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, title);
            pstmt.setString(3, expertise);
            pstmt.setString(4, id);
            pstmt.executeUpdate();
            System.out.println("Updated doctor: " + id + " -> " + name);
        }
    }
    
    private static void updateAdmin(Connection conn, String id, String realName) throws SQLException {
        String sql = "UPDATE admin SET real_name = ? WHERE admin_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, realName);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
            System.out.println("Updated admin: " + id + " -> " + realName);
        }
    }
    
    private static void updateUser(Connection conn, String id, String realName) throws SQLException {
        String sql = "UPDATE user SET real_name = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, realName);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
            System.out.println("Updated user: " + id + " -> " + realName);
        }
    }
}

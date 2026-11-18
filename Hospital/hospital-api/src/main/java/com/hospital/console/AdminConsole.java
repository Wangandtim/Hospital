package com.hospital.console;

import com.hospital.entity.Admin;
import com.hospital.entity.Hospital;
import com.hospital.entity.Department;
import com.hospital.service.AdminService;
import com.hospital.service.HospitalService;
import com.hospital.service.DepartmentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminConsole {

    private AdminService adminService;
    private HospitalService hospitalService;
    private DepartmentService departmentService;
    private Scanner scanner;
    private Admin currentAdmin;

    public AdminConsole(AdminService adminService, HospitalService hospitalService, 
                        DepartmentService departmentService, Scanner scanner) {
        this.adminService = adminService;
        this.hospitalService = hospitalService;
        this.departmentService = departmentService;
        this.scanner = scanner;
    }

    public void login() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("管理员登录");

        try {
            String username = ConsoleUtils.readString(scanner, "请输入用户名");
            String password = ConsoleUtils.readString(scanner, "请输入密码");

            currentAdmin = adminService.login(username, password);
            ConsoleUtils.printSuccess("登录成功！欢迎，" + currentAdmin.getRealName() + 
                    " [" + getRoleDisplayName(currentAdmin.getRole()) + "]");
            ConsoleUtils.waitEnter();
            showMenu();
        } catch (Exception e) {
            ConsoleUtils.printError("登录失败：" + e.getMessage());
            ConsoleUtils.waitEnter();
        }
    }

    private String getRoleDisplayName(String role) {
        switch (role) {
            case "SUPER_ADMIN": return "超级管理员";
            case "HOSPITAL_ADMIN": return "医院管理员";
            case "EMERGENCY_ADMIN": return "应急管理员";
            default: return role;
        }
    }

    private void showMenu() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("管理员系统");
            
            String role = currentAdmin.getRole();
            
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  当前用户：" + currentAdmin.getRealName() + " [" + getRoleDisplayName(role) + "]");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            
            if ("SUPER_ADMIN".equals(role)) {
                System.out.println("║  [1] 医院管理                                            ║");
                System.out.println("║  [2] 管理员管理                                          ║");
                System.out.println("║  [3] 密钥管理                                            ║");
                System.out.println("║  [0] 返回主菜单                                          ║");
            } else if ("HOSPITAL_ADMIN".equals(role)) {
                System.out.println("║  [1] 医院信息管理                                        ║");
                System.out.println("║  [2] 科室管理                                            ║");
                System.out.println("║  [3] 医院管理员管理                                      ║");
                System.out.println("║  [4] 密钥管理                                            ║");
                System.out.println("║  [0] 返回主菜单                                          ║");
            } else if ("EMERGENCY_ADMIN".equals(role)) {
                System.out.println("║  [1] 医院信息查看                                        ║");
                System.out.println("║  [2] 紧急排班调整                                        ║");
                System.out.println("║  [0] 返回主菜单                                          ║");
            }
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            
            if ("SUPER_ADMIN".equals(role)) {
                switch (choice) {
                    case 1: hospitalManagement(); break;
                    case 2: adminManagement(); break;
                    case 3: keyManagement(); break;
                    case 0: currentAdmin = null; return;
                    default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
                }
            } else if ("HOSPITAL_ADMIN".equals(role)) {
                switch (choice) {
                    case 1: hospitalInfoManagement(); break;
                    case 2: departmentManagement(); break;
                    case 3: hospitalAdminManagement(); break;
                    case 4: hospitalKeyManagement(); break;
                    case 0: currentAdmin = null; return;
                    default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
                }
            } else if ("EMERGENCY_ADMIN".equals(role)) {
                switch (choice) {
                    case 1: viewHospitalInfo(); break;
                    case 2: emergencyScheduleAdjustment(); break;
                    case 0: currentAdmin = null; return;
                    default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
                }
            }
        }
    }

    private void hospitalManagement() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("医院管理 [超级管理员]");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 新增医院                                            ║");
            System.out.println("║  [2] 查看医院列表                                        ║");
            System.out.println("║  [3] 修改医院信息                                        ║");
            System.out.println("║  [4] 删除医院                                            ║");
            System.out.println("║  [0] 返回上一级                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1: addHospital(); break;
                case 2: listHospitals(); break;
                case 3: updateHospital(); break;
                case 4: deleteHospital(); break;
                case 0: return;
                default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
            }
        }
    }

    private void adminManagement() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("管理员管理 [超级管理员]");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 创建管理员                                          ║");
            System.out.println("║  [2] 查看管理员列表                                      ║");
            System.out.println("║  [3] 修改管理员信息                                      ║");
            System.out.println("║  [4] 删除管理员                                          ║");
            System.out.println("║  [0] 返回上一级                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1: createAdmin(); break;
                case 2: listAdmins(); break;
                case 3: updateAdmin(); break;
                case 4: deleteAdmin(); break;
                case 0: return;
                default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
            }
        }
    }

    private void keyManagement() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("密钥管理 [超级管理员]");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 为管理员生成密钥                                    ║");
            System.out.println("║  [2] 续期管理员密钥                                      ║");
            System.out.println("║  [3] 作废管理员密钥                                      ║");
            System.out.println("║  [4] 查看密钥状态                                        ║");
            System.out.println("║  [0] 返回上一级                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1: generateApiKey(); break;
                case 2: renewApiKey(); break;
                case 3: invalidateApiKey(); break;
                case 4: viewApiKeyStatus(); break;
                case 0: return;
                default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
            }
        }
    }

    private void hospitalInfoManagement() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("医院信息管理");
        
        try {
            Hospital hospital = hospitalService.findById(currentAdmin.getHospitalId());
            
            System.out.println("┌──────────────────────────────────────────────────────────┐");
            System.out.println("│ 医院名称：" + hospital.getHospitalName());
            System.out.println("│ 医院地址：" + hospital.getAddress());
            System.out.println("│ 联系电话：" + hospital.getPhone());
            System.out.println("│ 医院等级：" + hospital.getLevel());
            System.out.println("│ 医院描述：" + hospital.getDescription());
            System.out.println("│ 状态：" + (hospital.getStatus() == 1 ? "正常" : "停用"));
            System.out.println("└──────────────────────────────────────────────────────────┘");

            if (ConsoleUtils.confirm(scanner, "是否修改医院信息？")) {
                Hospital update = new Hospital();
                update.setHospitalName(ConsoleUtils.readString(scanner, "请输入新名称（回车跳过）", hospital.getHospitalName()));
                update.setAddress(ConsoleUtils.readString(scanner, "请输入新地址（回车跳过）", hospital.getAddress()));
                update.setPhone(ConsoleUtils.readString(scanner, "请输入新电话（回车跳过）", hospital.getPhone()));
                update.setLevel(ConsoleUtils.readString(scanner, "请输入新等级（回车跳过）", hospital.getLevel()));
                update.setDescription(ConsoleUtils.readString(scanner, "请输入新描述（回车跳过）", hospital.getDescription()));

                hospitalService.update(currentAdmin.getHospitalId(), update);
                ConsoleUtils.printSuccess("医院信息修改成功！");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void departmentManagement() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("科室管理");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 新增科室                                            ║");
            System.out.println("║  [2] 查看科室列表                                        ║");
            System.out.println("║  [3] 修改科室信息                                        ║");
            System.out.println("║  [4] 删除科室                                            ║");
            System.out.println("║  [0] 返回上一级                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1: addDepartment(); break;
                case 2: listDepartments(); break;
                case 3: updateDepartment(); break;
                case 4: deleteDepartment(); break;
                case 0: return;
                default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
            }
        }
    }

    private void hospitalAdminManagement() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("医院管理员管理");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 创建医院管理员/应急管理员                            ║");
            System.out.println("║  [2] 查看医院管理员列表                                  ║");
            System.out.println("║  [3] 修改管理员信息                                      ║");
            System.out.println("║  [4] 删除管理员                                          ║");
            System.out.println("║  [0] 返回上一级                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1: createHospitalAdmin(); break;
                case 2: listHospitalAdmins(); break;
                case 3: updateHospitalAdmin(); break;
                case 4: deleteHospitalAdmin(); break;
                case 0: return;
                default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
            }
        }
    }

    private void hospitalKeyManagement() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("密钥管理");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 查看我的密钥                                        ║");
            System.out.println("║  [2] 生成新密钥                                          ║");
            System.out.println("║  [3] 续期密钥                                            ║");
            System.out.println("║  [0] 返回上一级                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1: viewMyKey(); break;
                case 2: generateMyKey(); break;
                case 3: renewMyKey(); break;
                case 0: return;
                default: ConsoleUtils.printError("无效选项"); ConsoleUtils.waitEnter();
            }
        }
    }

    private void viewHospitalInfo() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("医院信息查看 [应急管理员]");
        
        try {
            Hospital hospital = hospitalService.findById(currentAdmin.getHospitalId());
            
            System.out.println("┌──────────────────────────────────────────────────────────┐");
            System.out.println("│ 医院名称：" + hospital.getHospitalName());
            System.out.println("│ 医院地址：" + hospital.getAddress());
            System.out.println("│ 联系电话：" + hospital.getPhone());
            System.out.println("│ 医院等级：" + hospital.getLevel());
            System.out.println("└──────────────────────────────────────────────────────────┘");
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void emergencyScheduleAdjustment() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("紧急排班调整 [应急管理员]");
        ConsoleUtils.printWarning("此功能用于紧急情况下的排班调整");
        ConsoleUtils.printInfo("当前仅支持查看排班信息，调整功能开发中...");
        ConsoleUtils.waitEnter();
    }

    private void addHospital() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("新增医院");

        try {
            Hospital hospital = new Hospital();
            hospital.setHospitalName(ConsoleUtils.readString(scanner, "请输入医院名称"));
            hospital.setAddress(ConsoleUtils.readString(scanner, "请输入医院地址"));
            hospital.setPhone(ConsoleUtils.readString(scanner, "请输入联系电话"));
            hospital.setLevel(ConsoleUtils.readString(scanner, "请输入医院等级（如：三级甲等）"));
            hospital.setDescription(ConsoleUtils.readString(scanner, "请输入医院描述"));
            hospital.setProvince(ConsoleUtils.readString(scanner, "请输入省份"));
            hospital.setCity(ConsoleUtils.readString(scanner, "请输入城市"));
            hospital.setDistrict(ConsoleUtils.readString(scanner, "请输入区县"));
            hospital.setStatus(1);

            hospitalService.create(hospital);
            ConsoleUtils.printSuccess("医院新增成功！医院ID：" + hospital.getHospitalId());
        } catch (Exception e) {
            ConsoleUtils.printError("新增失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void listHospitals() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("医院列表");

        String province = ConsoleUtils.readString(scanner, "请输入省份筛选（回车跳过）");
        String city = ConsoleUtils.readString(scanner, "请输入城市筛选（回车跳过）");

        List<Hospital> hospitals = hospitalService.findAll();

        List<Hospital> filteredHospitals = new ArrayList<>();
        for (Hospital h : hospitals) {
            boolean match = true;
            if (!province.isEmpty() && !h.getAddress().contains(province)) match = false;
            if (!city.isEmpty() && !h.getAddress().contains(city)) match = false;
            if (match) {
                filteredHospitals.add(h);
            }
        }

        PaginationUtils.paginate(filteredHospitals, scanner, 10, new PaginationUtils.ItemRenderer<Hospital>() {
            @Override
            public void renderHeader() {
                ConsoleUtils.printTitle("医院列表");
                System.out.println("┌──────┬─────────────────────────────┬─────────────────────┬──────────┬────────────┐");
                System.out.println("│ 序号 │ 医院名称                    │ 地址                │ 等级     │ 状态       │");
                System.out.println("├──────┼─────────────────────────────┼─────────────────────┼──────────┼────────────┤");
            }

            @Override
            public void renderItem(Hospital h, int index) {
                System.out.printf("│ %-4d│ %-25s│ %-19s│ %-8s│ %-10s│\n",
                        index, h.getHospitalName(), truncate(h.getAddress(), 19),
                        h.getLevel(), h.getStatus() == 1 ? "正常" : "停用");
            }

            @Override
            public void renderFooter() {
                System.out.println("└──────┴─────────────────────────────┴─────────────────────┴──────────┴────────────┘");
            }
        });
    }

    private void updateHospital() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("修改医院信息");

        listHospitalsSimple();

        try {
            String hospitalId = ConsoleUtils.readString(scanner, "请输入要修改的医院ID");
            Hospital hospital = hospitalService.findById(hospitalId);

            System.out.println("\n当前信息：");
            System.out.println("名称：" + hospital.getHospitalName());
            System.out.println("地址：" + hospital.getAddress());
            System.out.println("电话：" + hospital.getPhone());
            System.out.println("等级：" + hospital.getLevel());
            System.out.println("描述：" + hospital.getDescription());
            System.out.println("省份：" + hospital.getProvince());
            System.out.println("城市：" + hospital.getCity());
            System.out.println("区县：" + hospital.getDistrict());

            Hospital update = new Hospital();
            update.setHospitalName(ConsoleUtils.readString(scanner, "请输入新名称（回车跳过）", hospital.getHospitalName()));
            update.setAddress(ConsoleUtils.readString(scanner, "请输入新地址（回车跳过）", hospital.getAddress()));
            update.setPhone(ConsoleUtils.readString(scanner, "请输入新电话（回车跳过）", hospital.getPhone()));
            update.setLevel(ConsoleUtils.readString(scanner, "请输入新等级（回车跳过）", hospital.getLevel()));
            update.setDescription(ConsoleUtils.readString(scanner, "请输入新描述（回车跳过）", hospital.getDescription()));
            update.setProvince(ConsoleUtils.readString(scanner, "请输入新省份（回车跳过）", hospital.getProvince()));
            update.setCity(ConsoleUtils.readString(scanner, "请输入新城市（回车跳过）", hospital.getCity()));
            update.setDistrict(ConsoleUtils.readString(scanner, "请输入新区县（回车跳过）", hospital.getDistrict()));
            update.setStatus(ConsoleUtils.readInt(scanner, "请输入新状态(1-正常, 0-停用)", hospital.getStatus()));

            hospitalService.update(hospitalId, update);
            ConsoleUtils.printSuccess("医院信息修改成功！");
        } catch (Exception e) {
            ConsoleUtils.printError("修改失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void deleteHospital() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("删除医院");

        listHospitalsSimple();

        try {
            String hospitalId = ConsoleUtils.readString(scanner, "请输入要删除的医院ID");

            if (ConsoleUtils.confirm(scanner, "确定要删除该医院吗？此操作不可恢复！")) {
                hospitalService.delete(hospitalId);
                ConsoleUtils.printSuccess("医院删除成功！");
            } else {
                ConsoleUtils.printSuccess("取消删除");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("删除失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void listHospitalsSimple() {
        List<Hospital> hospitals = hospitalService.findAll();
        PaginationUtils.paginate(hospitals, scanner, 10, new PaginationUtils.ItemRenderer<Hospital>() {
            @Override
            public void renderHeader() {
                System.out.println("当前医院列表：");
                System.out.println("┌────────────┬─────────────────────────────┬────────────┐");
                System.out.println("│ 医院ID     │ 医院名称                    │ 状态       │");
                System.out.println("├────────────┼─────────────────────────────┼────────────┤");
            }

            @Override
            public void renderItem(Hospital h, int index) {
                System.out.printf("│ %-10s│ %-25s│ %-10s│\n",
                        h.getHospitalId(), h.getHospitalName(), h.getStatus() == 1 ? "正常" : "停用");
            }

            @Override
            public void renderFooter() {
                System.out.println("└────────────┴─────────────────────────────┴────────────┘");
            }
        });
    }

    private void createAdmin() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("创建管理员");

        try {
            Admin admin = new Admin();
            admin.setUsername(ConsoleUtils.readString(scanner, "请输入用户名"));
            admin.setPassword(ConsoleUtils.readString(scanner, "请输入密码"));
            admin.setRealName(ConsoleUtils.readString(scanner, "请输入真实姓名"));
            admin.setPhone(ConsoleUtils.readString(scanner, "请输入联系电话（回车跳过）"));
            
            System.out.println("\n请选择管理员角色：");
            System.out.println("1. 超级管理员 (SUPER_ADMIN)");
            System.out.println("2. 医院管理员 (HOSPITAL_ADMIN)");
            System.out.println("3. 应急管理员 (EMERGENCY_ADMIN)");
            int roleChoice = ConsoleUtils.readInt(scanner, "请选择角色");
            
            String role;
            switch (roleChoice) {
                case 1: role = "SUPER_ADMIN"; break;
                case 2: role = "HOSPITAL_ADMIN"; break;
                case 3: role = "EMERGENCY_ADMIN"; break;
                default: role = "HOSPITAL_ADMIN";
            }
            admin.setRole(role);

            if (!"SUPER_ADMIN".equals(role)) {
                listHospitalsSimple();
                admin.setHospitalId(ConsoleUtils.readString(scanner, "请输入绑定的医院ID"));
            }

            adminService.create(admin);
            ConsoleUtils.printSuccess("管理员创建成功！管理员ID：" + admin.getAdminId());
        } catch (Exception e) {
            ConsoleUtils.printError("创建失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void listAdmins() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("管理员列表");

        List<Admin> admins = adminService.findAll();

        System.out.println("┌──────────┬────────────┬───────────────┬──────────────────┬────────────┐");
        System.out.println("│ 管理员ID │ 用户名     │ 真实姓名      │ 角色             │ 状态       │");
        System.out.println("├──────────┼────────────┼───────────────┼──────────────────┼────────────┤");

        for (Admin a : admins) {
            System.out.printf("│ %-8s│ %-10s│ %-13s│ %-16s│ %-10s│\n",
                    a.getAdminId(), a.getUsername(), a.getRealName(), 
                    getRoleDisplayName(a.getRole()), a.getStatus() == 1 ? "正常" : "禁用");
        }
        System.out.println("└──────────┴────────────┴───────────────┴──────────────────┴────────────┘");
        ConsoleUtils.waitEnter();
    }

    private void updateAdmin() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("修改管理员信息");

        listAdmins();

        try {
            String adminId = ConsoleUtils.readString(scanner, "请输入要修改的管理员ID");
            Admin admin = adminService.findById(adminId);

            System.out.println("\n当前信息：");
            System.out.println("用户名：" + admin.getUsername());
            System.out.println("真实姓名：" + admin.getRealName());
            System.out.println("联系电话：" + admin.getPhone());
            System.out.println("角色：" + getRoleDisplayName(admin.getRole()));
            System.out.println("状态：" + (admin.getStatus() == 1 ? "正常" : "禁用"));

            Admin update = new Admin();
            update.setRealName(ConsoleUtils.readString(scanner, "请输入新姓名（回车跳过）", admin.getRealName()));
            update.setPhone(ConsoleUtils.readString(scanner, "请输入新电话（回车跳过）", admin.getPhone()));
            String newPassword = ConsoleUtils.readString(scanner, "请输入新密码（回车跳过）");
            if (!newPassword.isEmpty()) {
                update.setPassword(newPassword);
            }
            update.setStatus(ConsoleUtils.readInt(scanner, "请输入新状态(1-正常, 0-禁用)", admin.getStatus()));

            adminService.update(adminId, update);
            ConsoleUtils.printSuccess("管理员信息修改成功！");
        } catch (Exception e) {
            ConsoleUtils.printError("修改失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void deleteAdmin() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("删除管理员");

        listAdmins();

        try {
            String adminId = ConsoleUtils.readString(scanner, "请输入要删除的管理员ID");
            Admin admin = adminService.findById(adminId);

            if ("SUPER_ADMIN".equals(admin.getRole()) && adminService.findByRole("SUPER_ADMIN").size() <= 1) {
                ConsoleUtils.printError("至少需要保留一个超级管理员！");
                ConsoleUtils.waitEnter();
                return;
            }

            if (ConsoleUtils.confirm(scanner, "确定要删除该管理员吗？")) {
                adminService.delete(adminId);
                ConsoleUtils.printSuccess("管理员删除成功！");
            } else {
                ConsoleUtils.printSuccess("取消删除");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("删除失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void generateApiKey() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("生成API密钥");

        listAdmins();

        try {
            String adminId = ConsoleUtils.readString(scanner, "请输入要生成密钥的管理员ID");
            
            if (ConsoleUtils.confirm(scanner, "确定要为该管理员生成新密钥吗？原密钥将失效")) {
                String apiKey = adminService.generateApiKey(adminId);
                ConsoleUtils.printSuccess("密钥生成成功！");
                System.out.println("生成的API密钥：" + apiKey);
                ConsoleUtils.printWarning("请妥善保管此密钥，一旦丢失将无法找回！");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void renewApiKey() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("续期API密钥");

        listAdmins();

        try {
            String adminId = ConsoleUtils.readString(scanner, "请输入要续期密钥的管理员ID");
            
            adminService.renewApiKey(adminId);
            ConsoleUtils.printSuccess("密钥续期成功！有效期延长30天");
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void invalidateApiKey() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("作废API密钥");

        listAdmins();

        try {
            String adminId = ConsoleUtils.readString(scanner, "请输入要作废密钥的管理员ID");
            
            if (ConsoleUtils.confirm(scanner, "确定要作废该管理员的密钥吗？")) {
                adminService.invalidateApiKey(adminId);
                ConsoleUtils.printSuccess("密钥已作废！");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void viewApiKeyStatus() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("密钥状态");

        List<Admin> admins = adminService.findAll();

        System.out.println("┌──────────┬────────────┬───────────────┬─────────────────────────────┬────────────────────┐");
        System.out.println("│ 管理员ID │ 用户名     │ 真实姓名      │ API密钥                     │ 过期时间          │");
        System.out.println("├──────────┼────────────┼───────────────┼─────────────────────────────┼────────────────────┤");

        for (Admin a : admins) {
            String apiKey = a.getApiKey() != null ? a.getApiKey() : "无";
            String expire = a.getApiKeyExpire() != null ? a.getApiKeyExpire().toString() : "无";
            System.out.printf("│ %-8s│ %-10s│ %-13s│ %-31s│ %-18s│\n",
                    a.getAdminId(), a.getUsername(), a.getRealName(), apiKey, expire);
        }
        System.out.println("└──────────┴────────────┴───────────────┴─────────────────────────────┴────────────────────┘");
        ConsoleUtils.waitEnter();
    }

    private void addDepartment() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("新增科室");

        try {
            Department dept = new Department();
            dept.setHospitalId(currentAdmin.getHospitalId());
            dept.setDeptName(ConsoleUtils.readString(scanner, "请输入科室名称"));
            dept.setDescription(ConsoleUtils.readString(scanner, "请输入科室描述（回车跳过）"));
            dept.setDiseases(ConsoleUtils.readString(scanner, "请输入诊治疾病（回车跳过）"));
            dept.setServiceTime(ConsoleUtils.readString(scanner, "请输入服务时间（回车跳过）"));
            dept.setIsFlagship(ConsoleUtils.readInt(scanner, "是否为重点科室(1-是, 0-否)", 0));

            departmentService.create(dept);
            ConsoleUtils.printSuccess("科室新增成功！科室ID：" + dept.getDeptId());
        } catch (Exception e) {
            ConsoleUtils.printError("新增失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void listDepartments() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("科室列表");

        List<Department> depts = departmentService.findByHospitalId(currentAdmin.getHospitalId());

        System.out.println("┌──────────┬─────────────────────┬────────────┬─────────────────────┐");
        System.out.println("│ 科室ID   │ 科室名称            │ 是否重点   │ 服务时间           │");
        System.out.println("├──────────┼─────────────────────┼────────────┼─────────────────────┤");

        for (Department d : depts) {
            System.out.printf("│ %-8s│ %-19s│ %-10s│ %-19s│\n",
                    d.getDeptId(), d.getDeptName(), 
                    d.getIsFlagship() == 1 ? "是" : "否", 
                    d.getServiceTime() != null ? d.getServiceTime() : "-");
        }
        System.out.println("└──────────┴─────────────────────┴────────────┴─────────────────────┘");
        ConsoleUtils.waitEnter();
    }

    private void updateDepartment() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("修改科室信息");

        listDepartments();

        try {
            String deptId = ConsoleUtils.readString(scanner, "请输入要修改的科室ID");
            Department dept = departmentService.findById(deptId);

            System.out.println("\n当前信息：");
            System.out.println("科室名称：" + dept.getDeptName());
            System.out.println("描述：" + dept.getDescription());
            System.out.println("诊治疾病：" + dept.getDiseases());
            System.out.println("服务时间：" + dept.getServiceTime());
            System.out.println("是否重点科室：" + (dept.getIsFlagship() == 1 ? "是" : "否"));

            Department update = new Department();
            update.setDeptName(ConsoleUtils.readString(scanner, "请输入新名称（回车跳过）", dept.getDeptName()));
            update.setDescription(ConsoleUtils.readString(scanner, "请输入新描述（回车跳过）", dept.getDescription()));
            update.setDiseases(ConsoleUtils.readString(scanner, "请输入新诊治疾病（回车跳过）", dept.getDiseases()));
            update.setServiceTime(ConsoleUtils.readString(scanner, "请输入新服务时间（回车跳过）", dept.getServiceTime()));
            update.setIsFlagship(ConsoleUtils.readInt(scanner, "是否为重点科室(1-是, 0-否)", dept.getIsFlagship()));

            departmentService.update(deptId, update);
            ConsoleUtils.printSuccess("科室信息修改成功！");
        } catch (Exception e) {
            ConsoleUtils.printError("修改失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void deleteDepartment() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("删除科室");

        listDepartments();

        try {
            String deptId = ConsoleUtils.readString(scanner, "请输入要删除的科室ID");

            if (ConsoleUtils.confirm(scanner, "确定要删除该科室吗？")) {
                departmentService.delete(deptId);
                ConsoleUtils.printSuccess("科室删除成功！");
            } else {
                ConsoleUtils.printSuccess("取消删除");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("删除失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void createHospitalAdmin() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("创建医院管理员");

        try {
            Admin admin = new Admin();
            admin.setUsername(ConsoleUtils.readString(scanner, "请输入用户名"));
            admin.setPassword(ConsoleUtils.readString(scanner, "请输入密码"));
            admin.setRealName(ConsoleUtils.readString(scanner, "请输入真实姓名"));
            admin.setPhone(ConsoleUtils.readString(scanner, "请输入联系电话（回车跳过）"));
            admin.setHospitalId(currentAdmin.getHospitalId());
            
            System.out.println("\n请选择管理员角色：");
            System.out.println("1. 医院管理员 (HOSPITAL_ADMIN)");
            System.out.println("2. 应急管理员 (EMERGENCY_ADMIN)");
            int roleChoice = ConsoleUtils.readInt(scanner, "请选择角色");
            
            String role = roleChoice == 2 ? "EMERGENCY_ADMIN" : "HOSPITAL_ADMIN";
            admin.setRole(role);

            adminService.create(admin);
            ConsoleUtils.printSuccess("管理员创建成功！管理员ID：" + admin.getAdminId());
        } catch (Exception e) {
            ConsoleUtils.printError("创建失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void listHospitalAdmins() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("医院管理员列表");

        List<Admin> admins = adminService.findByHospitalId(currentAdmin.getHospitalId());

        System.out.println("┌──────────┬────────────┬───────────────┬──────────────────┬────────────┐");
        System.out.println("│ 管理员ID │ 用户名     │ 真实姓名      │ 角色             │ 状态       │");
        System.out.println("├──────────┼────────────┼───────────────┼──────────────────┼────────────┤");

        for (Admin a : admins) {
            System.out.printf("│ %-8s│ %-10s│ %-13s│ %-16s│ %-10s│\n",
                    a.getAdminId(), a.getUsername(), a.getRealName(), 
                    getRoleDisplayName(a.getRole()), a.getStatus() == 1 ? "正常" : "禁用");
        }
        System.out.println("└──────────┴────────────┴───────────────┴──────────────────┴────────────┘");
        ConsoleUtils.waitEnter();
    }

    private void updateHospitalAdmin() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("修改医院管理员信息");

        listHospitalAdmins();

        try {
            String adminId = ConsoleUtils.readString(scanner, "请输入要修改的管理员ID");
            Admin admin = adminService.findById(adminId);

            if (!currentAdmin.getHospitalId().equals(admin.getHospitalId())) {
                ConsoleUtils.printError("只能修改本医院的管理员！");
                ConsoleUtils.waitEnter();
                return;
            }

            System.out.println("\n当前信息：");
            System.out.println("用户名：" + admin.getUsername());
            System.out.println("真实姓名：" + admin.getRealName());
            System.out.println("联系电话：" + admin.getPhone());
            System.out.println("角色：" + getRoleDisplayName(admin.getRole()));
            System.out.println("状态：" + (admin.getStatus() == 1 ? "正常" : "禁用"));

            Admin update = new Admin();
            update.setRealName(ConsoleUtils.readString(scanner, "请输入新姓名（回车跳过）", admin.getRealName()));
            update.setPhone(ConsoleUtils.readString(scanner, "请输入新电话（回车跳过）", admin.getPhone()));
            String newPassword = ConsoleUtils.readString(scanner, "请输入新密码（回车跳过）");
            if (!newPassword.isEmpty()) {
                update.setPassword(newPassword);
            }
            update.setStatus(ConsoleUtils.readInt(scanner, "请输入新状态(1-正常, 0-禁用)", admin.getStatus()));

            adminService.update(adminId, update);
            ConsoleUtils.printSuccess("管理员信息修改成功！");
        } catch (Exception e) {
            ConsoleUtils.printError("修改失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void deleteHospitalAdmin() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("删除医院管理员");

        listHospitalAdmins();

        try {
            String adminId = ConsoleUtils.readString(scanner, "请输入要删除的管理员ID");
            Admin admin = adminService.findById(adminId);

            if (!currentAdmin.getHospitalId().equals(admin.getHospitalId())) {
                ConsoleUtils.printError("只能删除本医院的管理员！");
                ConsoleUtils.waitEnter();
                return;
            }

            if (adminId.equals(currentAdmin.getAdminId())) {
                ConsoleUtils.printError("不能删除自己！");
                ConsoleUtils.waitEnter();
                return;
            }

            if (ConsoleUtils.confirm(scanner, "确定要删除该管理员吗？")) {
                adminService.delete(adminId);
                ConsoleUtils.printSuccess("管理员删除成功！");
            } else {
                ConsoleUtils.printSuccess("取消删除");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("删除失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void viewMyKey() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("我的密钥信息");

        try {
            Admin admin = adminService.findById(currentAdmin.getAdminId());
            
            System.out.println("┌──────────────────────────────────────────────────────────┐");
            System.out.println("│ 用户名：" + admin.getUsername());
            System.out.println("│ 真实姓名：" + admin.getRealName());
            if (admin.getApiKey() != null) {
                System.out.println("│ API密钥：" + admin.getApiKey());
                System.out.println("│ 过期时间：" + admin.getApiKeyExpire());
            } else {
                System.out.println("│ 暂无API密钥，请先生成");
            }
            System.out.println("└──────────────────────────────────────────────────────────┘");
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void generateMyKey() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("生成新密钥");

        try {
            if (ConsoleUtils.confirm(scanner, "确定要生成新密钥吗？原密钥将失效")) {
                String apiKey = adminService.generateApiKey(currentAdmin.getAdminId());
                ConsoleUtils.printSuccess("密钥生成成功！");
                System.out.println("生成的API密钥：" + apiKey);
                ConsoleUtils.printWarning("请妥善保管此密钥，一旦丢失将无法找回！");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void renewMyKey() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("续期密钥");

        try {
            adminService.renewApiKey(currentAdmin.getAdminId());
            ConsoleUtils.printSuccess("密钥续期成功！有效期延长30天");
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private String truncate(String str, int length) {
        if (str == null) return "";
        return str.length() > length ? str.substring(0, length - 1) + "…" : str;
    }
}
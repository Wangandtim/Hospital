package com.hospital;

import com.hospital.entity.*;
import com.hospital.service.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@SpringBootApplication
public class EnhancedConsoleApplication {

    private static ConfigurableApplicationContext context;
    
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BOLD = "\u001B[1m";
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy年MM月dd日 (E)");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {}

        Scanner scanner = new Scanner(System.in, "UTF-8");
        printBanner();

        int choice = showStartupMenu(scanner);
        if (choice == 2) {
            context = SpringApplication.run(EnhancedConsoleApplication.class, args);
            startConsoleMode(scanner);
            System.exit(0);
        } else {
            SpringApplication.run(EnhancedConsoleApplication.class, args);
        }
    }

    private static void printBanner() {
        clearScreen();
        System.out.println();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                          ║");
        System.out.println("║           ██╗  ██╗███████╗██╗   ██╗██████╗  ██████╗ ███████╗             ║");
        System.out.println("║           ██║ ██╔╝██╔════╝╚██╗ ██╔╝██╔══██╗██╔═══██╗██╔════╝             ║");
        System.out.println("║           █████╔╝ █████╗   ╚████╔╝ ██████╔╝██║   ██║███████╗             ║");
        System.out.println("║           ██╔═██╗ ██╔══╝    ╚██╔╝  ██╔═══╝ ██║   ██║╚════██║             ║");
        System.out.println("║           ██║  ██╗███████╗   ██║   ██║     ╚██████╔╝███████║             ║");
        System.out.println("║           ╚═╝  ╚═╝╚══════╝   ╚═╝   ╚═╝      ╚═════╝ ╚══════╝             ║");
        System.out.println("║                                                                          ║");
        System.out.println("║                      健康联防医院预约系统 v1.0.1                             ║");
        System.out.println("║                      Health Alliance Hospital                            ║");
        System.out.println("║                                                                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println();
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void waitEnter() {
        System.out.println("\n" + ANSI_YELLOW + "按回车键继续..." + ANSI_RESET);
        new Scanner(System.in).nextLine();
    }

    private static int showStartupMenu(Scanner scanner) {
        System.out.println(ANSI_YELLOW + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                              启动模式选择                                  ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║  " + ANSI_GREEN + "[1] Web服务模式" + ANSI_RESET + "   (启动API供小程序调用)  ║");
        System.out.println("║  " + ANSI_GREEN + "[2] 控制台模式" + ANSI_RESET + "   (命令行交互界面)        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        return readInt(scanner, ANSI_BLUE + "请选择运行模式");
    }

    private static void startConsoleMode(Scanner scanner) {
        AdminService adminService = context.getBean(AdminService.class);
        HospitalService hospitalService = context.getBean(HospitalService.class);
        DoctorService doctorService = context.getBean(DoctorService.class);
        DepartmentService departmentService = context.getBean(DepartmentService.class);
        ScheduleService scheduleService = context.getBean(ScheduleService.class);
        AppointmentService appointmentService = context.getBean(AppointmentService.class);
        PatientService patientService = context.getBean(PatientService.class);

        while (true) {
            printMainMenu();
            int choice = readInt(scanner, ANSI_BLUE + "请选择");
            switch (choice) {
                case 1 -> adminConsole(adminService, hospitalService, doctorService, departmentService, scheduleService, patientService, scanner);
                case 2 -> doctorConsole(doctorService, scheduleService, appointmentService, patientService, scanner);
                case 3 -> patientConsole(patientService, hospitalService, departmentService, doctorService, scheduleService, appointmentService, scanner);
                case 4 -> registerPatient(patientService, scanner);
                case 5 -> registerDoctor(doctorService, hospitalService, departmentService, scanner);
                case 6 -> showSystemStats(hospitalService, doctorService, departmentService, scheduleService, appointmentService, patientService);
                case 0 -> {
                    System.out.println("\n" + ANSI_GREEN + ANSI_BOLD + "感谢使用医院预约系统，再见！" + ANSI_RESET);
                    return;
                }
                default -> System.out.println("\n" + ANSI_RED + "✗ 无效选项" + ANSI_RESET);
            }
        }
    }

    private static void printMainMenu() {
        clearScreen();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    医院预约系统 - 角色选择                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  " + ANSI_GREEN + "[1] 管理员系统" + ANSI_RESET + "        ║");
        System.out.println("║  " + ANSI_GREEN + "[2] 医生系统" + ANSI_RESET + "          ║");
        System.out.println("║  " + ANSI_GREEN + "[3] 患者系统" + ANSI_RESET + "          ║");
        System.out.println("║  " + ANSI_GREEN + "[4] 患者注册" + ANSI_RESET + "          ║");
        System.out.println("║  " + ANSI_GREEN + "[5] 医生注册" + ANSI_RESET + "          ║");
        System.out.println("║  " + ANSI_GREEN + "[6] 系统统计" + ANSI_RESET + "          ║");
        System.out.println("║  " + ANSI_RED + "[0] 退出系统" + ANSI_RESET + "            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_YELLOW + "当前时间: " + LocalDateTime.now().format(DATETIME_FORMAT) + ANSI_RESET);
    }

    private static void adminConsole(AdminService adminService, HospitalService hospitalService,
                                     DoctorService doctorService, DepartmentService departmentService,
                                     ScheduleService scheduleService, PatientService patientService, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_BLUE + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   管理员登录                               ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        System.out.print(ANSI_CYAN + "用户名: " + ANSI_RESET);
        String username = scanner.nextLine();
        System.out.print(ANSI_CYAN + "密码: " + ANSI_RESET);
        String password = scanner.nextLine();

        try {
            Admin admin = adminService.login(username, password);
            System.out.println("\n" + ANSI_GREEN + "✓ 登录成功！欢迎, " + (admin.getRealName() != null ? admin.getRealName() : username) + ANSI_RESET);
            waitEnter();

            boolean isSuperAdmin = "SUPER_ADMIN".equals(admin.getRole());
            String hospitalId = admin.getHospitalId();

            while (true) {
                if (isSuperAdmin) {
                    printSuperAdminMenu();
                } else {
                    printHospitalAdminMenu();
                }
                int choice = readInt(scanner, ANSI_BLUE + "请选择操作");
                
                if (isSuperAdmin) {
                    switch (choice) {
                        case 1 -> manageHospitals(hospitalService, scanner);
                        case 2 -> manageAdmins(adminService, scanner);
                        case 0 -> {
                            System.out.println("\n" + ANSI_YELLOW + "退出管理员系统" + ANSI_RESET);
                            return;
                        }
                        default -> System.out.println("\n" + ANSI_RED + "✗ 无效选项" + ANSI_RESET);
                    }
                } else {
                    switch (choice) {
                        case 1 -> listDoctorsByHospital(doctorService, hospitalId);
                        case 2 -> listDepartmentsByHospital(departmentService, hospitalId);
                        case 3 -> manageSchedules(scheduleService, doctorService, hospitalId, scanner);
                        case 0 -> {
                            System.out.println("\n" + ANSI_YELLOW + "退出管理员系统" + ANSI_RESET);
                            return;
                        }
                        default -> System.out.println("\n" + ANSI_RED + "✗ 无效选项" + ANSI_RESET);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 登录失败: " + e.getMessage() + ANSI_RESET);
            waitEnter();
        }
    }

    private static void printSuperAdminMenu() {
        clearScreen();
        System.out.println(ANSI_BLUE + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                     总管理员控制台                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  " + ANSI_GREEN + "[1] 医院管理" + ANSI_RESET + "           ║");
        System.out.println("║  " + ANSI_GREEN + "[2] 管理员管理" + ANSI_RESET + "         ║");
        System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
    }

    private static void printHospitalAdminMenu() {
        clearScreen();
        System.out.println(ANSI_BLUE + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   医院管理员控制台                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  " + ANSI_GREEN + "[1] 查看本院医生" + ANSI_RESET + "      ║");
        System.out.println("║  " + ANSI_GREEN + "[2] 查看本科室" + ANSI_RESET + "       ║");
        System.out.println("║  " + ANSI_GREEN + "[3] 排班管理" + ANSI_RESET + "         ║");
        System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
    }

    private static void doctorConsole(DoctorService doctorService, ScheduleService scheduleService,
                                      AppointmentService appointmentService, PatientService patientService, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   医生登录                                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_YELLOW + "提示: 如果还没有账号，请先返回主菜单选择 [5] 医生注册" + ANSI_RESET);

        System.out.print(ANSI_CYAN + "账号: " + ANSI_RESET);
        String username = scanner.nextLine();
        System.out.print(ANSI_CYAN + "密码: " + ANSI_RESET);
        String password = scanner.nextLine();

        try {
            Doctor doctor = doctorService.login(username, password);
            String doctorId = doctor.getDoctorId();
            System.out.println("\n" + ANSI_GREEN + "✓ 登录成功！欢迎, " + doctor.getName() + ANSI_RESET);
            waitEnter();

            while (true) {
                printDoctorMenu();
                int choice = readInt(scanner, ANSI_BLUE + "请选择操作");
                switch (choice) {
                    case 1 -> viewDoctorSchedule(scheduleService, doctorId);
                    case 2 -> manageCustomSchedule(scheduleService, doctorService, doctorId, scanner);
                    case 3 -> viewDoctorAppointments(appointmentService, patientService, doctorId);
                    case 4 -> viewTodaySchedule(scheduleService, doctorId);
                    case 0 -> {
                        System.out.println("\n" + ANSI_YELLOW + "退出医生系统" + ANSI_RESET);
                        return;
                    }
                    default -> System.out.println("\n" + ANSI_RED + "✗ 无效选项" + ANSI_RESET);
                }
            }
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 登录失败: " + e.getMessage() + ANSI_RESET);
            waitEnter();
        }
    }

    private static void printDoctorMenu() {
        clearScreen();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                       医生控制台                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  " + ANSI_GREEN + "[1] 查看我的排班" + ANSI_RESET + "       ║");
        System.out.println("║  " + ANSI_GREEN + "[2] 自定义排班管理" + ANSI_RESET + "     ║");
        System.out.println("║  " + ANSI_GREEN + "[3] 查看预约患者" + ANSI_RESET + "       ║");
        System.out.println("║  " + ANSI_GREEN + "[4] 今日排班概览" + ANSI_RESET + "       ║");
        System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
    }

    private static void patientConsole(PatientService patientService, HospitalService hospitalService,
                                       DepartmentService departmentService, DoctorService doctorService,
                                       ScheduleService scheduleService, AppointmentService appointmentService,
                                       Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   患者登录                                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_YELLOW + "提示: 如果还没有账号，请先返回主菜单选择 [4] 患者注册" + ANSI_RESET);

        System.out.print(ANSI_CYAN + "电话号码: " + ANSI_RESET);
        String phone = scanner.nextLine();
        System.out.print(ANSI_CYAN + "密码: " + ANSI_RESET);
        String password = scanner.nextLine();

        try {
            Patient patient = patientService.login(phone, password);
            String patientId = patient.getPatientId();
            System.out.println("\n" + ANSI_GREEN + "✓ 登录成功！欢迎, " + patient.getName() + ANSI_RESET);
            waitEnter();

            while (true) {
                printPatientMenu();
                int choice = readInt(scanner, ANSI_BLUE + "请选择操作");
                switch (choice) {
                    case 1 -> searchHospitalAndBook(hospitalService, departmentService, doctorService, scheduleService, appointmentService, patientId, scanner);
                    case 2 -> viewPatientAppointments(appointmentService, doctorService, patientId);
                    case 3 -> cancelAppointment(appointmentService, doctorService, patientId, scanner);
                    case 0 -> {
                        System.out.println("\n" + ANSI_YELLOW + "退出患者系统" + ANSI_RESET);
                        return;
                    }
                    default -> System.out.println("\n" + ANSI_RED + "✗ 无效选项" + ANSI_RESET);
                }
            }
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 登录失败: " + e.getMessage() + ANSI_RESET);
            waitEnter();
        }
    }

    private static void printPatientMenu() {
        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                     患者控制台                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  " + ANSI_BLUE + "[1] 预约挂号" + ANSI_RESET + "          ║");
        System.out.println("║  " + ANSI_BLUE + "[2] 查看我的预约" + ANSI_RESET + "       ║");
        System.out.println("║  " + ANSI_BLUE + "[3] 取消预约" + ANSI_RESET + "          ║");
        System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
    }

    private static void registerPatient(PatientService patientService, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    患者注册                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        Patient patient = new Patient();

        System.out.print(ANSI_CYAN + "患者ID(留空自动生成): " + ANSI_RESET);
        String patientId = scanner.nextLine().trim();
        if (!patientId.isEmpty()) patient.setPatientId(patientId);

        System.out.print(ANSI_CYAN + "姓名: " + ANSI_RESET);
        patient.setName(scanner.nextLine());

        System.out.print(ANSI_CYAN + "性别(男/女): " + ANSI_RESET);
        patient.setGender(scanner.nextLine());

        System.out.print(ANSI_CYAN + "年龄: " + ANSI_RESET);
        try { patient.setAge(Integer.parseInt(scanner.nextLine())); } 
        catch (Exception e) { patient.setAge(0); }

        System.out.print(ANSI_CYAN + "手机号: " + ANSI_RESET);
        patient.setPhone(scanner.nextLine());

        System.out.print(ANSI_CYAN + "身份证号: " + ANSI_RESET);
        patient.setIdCard(scanner.nextLine());

        System.out.print(ANSI_CYAN + "病历信息: " + ANSI_RESET);
        patient.setMedicalHistory(scanner.nextLine());

        System.out.print(ANSI_CYAN + "密码: " + ANSI_RESET);
        patient.setPassword(scanner.nextLine());

        try {
            Patient savedPatient = patientService.create(patient);
            System.out.println("\n" + ANSI_GREEN + "✓ 患者注册成功！患者ID: " + savedPatient.getPatientId() + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 注册失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void registerDoctor(DoctorService doctorService, HospitalService hospitalService,
                                       DepartmentService departmentService, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   医生注册                                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        System.out.println(ANSI_YELLOW + "选择所属医院:" + ANSI_RESET);
        List<Hospital> hospitals = hospitalService.findAll();
        if (hospitals.isEmpty()) {
            System.out.println(ANSI_RED + "✗ 暂无医院" + ANSI_RESET);
            waitEnter();
            return;
        }

        Hospital selectedHospital = com.hospital.console.PaginationUtils.paginateWithSelection(hospitals, scanner, 10, new com.hospital.console.PaginationUtils.ItemRenderer<Hospital>() {
            @Override
            public void renderHeader() {
                System.out.println(ANSI_YELLOW + "选择所属医院:" + ANSI_RESET);
                System.out.println("┌──────┬────────────┬─────────────────────────────┐");
                System.out.println("│ 序号 │ 医院ID     │ 医院名称                    │");
                System.out.println("├──────┼────────────┼─────────────────────────────┤");
            }

            @Override
            public void renderItem(Hospital h, int index) {
                System.out.printf("│ %-4d│ %-10s│ %-25s│%n", index, h.getHospitalId(), h.getHospitalName());
            }

            @Override
            public void renderFooter() {
                System.out.println("└──────┴────────────┴─────────────────────────────┘");
            }
        });

        if (selectedHospital == null) {
            return;
        }
        String hospitalId = selectedHospital.getHospitalId();

        System.out.println("\n" + ANSI_YELLOW + "选择所属科室:" + ANSI_RESET);
        List<Department> depts = departmentService.findByHospitalId(hospitalId);
        for (int i = 0; i < depts.size(); i++) {
            Department d = depts.get(i);
            System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%s | %s%n", i + 1, d.getDeptId(), d.getDeptName());
        }
        int deptIdx = readInt(scanner, ANSI_BLUE + "请选择科室") - 1;
        if (deptIdx < 0 || deptIdx >= depts.size()) {
            System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
            waitEnter();
            return;
        }
        String deptId = depts.get(deptIdx).getDeptId();

        Doctor doctor = new Doctor();
        System.out.print(ANSI_CYAN + "\n医生ID(留空自动生成): " + ANSI_RESET);
        String doctorId = scanner.nextLine().trim();
        if (!doctorId.isEmpty()) doctor.setDoctorId(doctorId);

        System.out.print(ANSI_CYAN + "姓名: " + ANSI_RESET);
        doctor.setName(scanner.nextLine());
        System.out.print(ANSI_CYAN + "性别(男/女): " + ANSI_RESET);
        doctor.setGender(scanner.nextLine());
        System.out.print(ANSI_CYAN + "职称: " + ANSI_RESET);
        doctor.setTitle(scanner.nextLine());
        System.out.print(ANSI_CYAN + "专业擅长: " + ANSI_RESET);
        doctor.setExpertise(scanner.nextLine());
        System.out.print(ANSI_CYAN + "联系电话: " + ANSI_RESET);
        doctor.setPhone(scanner.nextLine());
        System.out.print(ANSI_CYAN + "挂号费: " + ANSI_RESET);
        try { doctor.setConsultationFee(Integer.parseInt(scanner.nextLine())); } 
        catch (Exception e) { doctor.setConsultationFee(0); }

        doctor.setHospitalId(hospitalId);
        doctor.setDeptId(deptId);
        doctor.setStatus(1);

        try {
            doctorService.create(doctor);
            System.out.println("\n" + ANSI_GREEN + "✓ 医生注册成功！医生ID: " + doctor.getDoctorId() + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 注册失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void manageHospitals(HospitalService service, Scanner scanner) {
        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                     医院管理                              ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  " + ANSI_GREEN + "[1] 查看医院列表" + ANSI_RESET + "      ║");
            System.out.println("║  " + ANSI_GREEN + "[2] 添加医院" + ANSI_RESET + "         ║");
            System.out.println("║  " + ANSI_GREEN + "[3] 修改医院信息" + ANSI_RESET + "    ║");
            System.out.println("║  " + ANSI_GREEN + "[4] 删除医院" + ANSI_RESET + "         ║");
            System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int choice = readInt(scanner, ANSI_BLUE + "请选择操作");
            switch (choice) {
                case 1 -> listHospitals(service);
                case 2 -> addHospital(service, scanner);
                case 3 -> updateHospital(service, scanner);
                case 4 -> deleteHospital(service, scanner);
                case 0 -> {
                    return;
                }
                default -> System.out.println("\n" + ANSI_RED + "✗ 无效选项" + ANSI_RESET);
            }
        }
    }

    private static void addHospital(HospitalService service, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_BLUE + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                     添加医院                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        try {
            Hospital hospital = new Hospital();
            hospital.setHospitalId(scanner.nextLine().trim());
            scanner.nextLine();
            System.out.print(ANSI_CYAN + "医院名称: " + ANSI_RESET);
            hospital.setHospitalName(scanner.nextLine().trim());
            System.out.print(ANSI_CYAN + "所在城市: " + ANSI_RESET);
            hospital.setCity(scanner.nextLine().trim());
            System.out.print(ANSI_CYAN + "医院等级: " + ANSI_RESET);
            hospital.setLevel(scanner.nextLine().trim());
            System.out.print(ANSI_CYAN + "医院地址: " + ANSI_RESET);
            hospital.setAddress(scanner.nextLine().trim());
            System.out.print(ANSI_CYAN + "联系电话: " + ANSI_RESET);
            hospital.setPhone(scanner.nextLine().trim());

            service.create(hospital);
            System.out.println("\n" + ANSI_GREEN + "✓ 医院添加成功！" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 添加失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void updateHospital(HospitalService service, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_BLUE + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                     修改医院信息                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        try {
            System.out.print(ANSI_CYAN + "请输入要修改的医院ID: " + ANSI_RESET);
            String hospitalId = scanner.nextLine().trim();

            Hospital hospital = service.findById(hospitalId);
            if (hospital == null) {
                System.out.println(ANSI_RED + "医院不存在" + ANSI_RESET);
                waitEnter();
                return;
            }

            System.out.println("\n当前信息：");
            System.out.println("医院名称: " + hospital.getHospitalName());
            System.out.println("所在城市: " + hospital.getCity());
            System.out.println("医院等级: " + hospital.getLevel());
            System.out.println("医院地址: " + hospital.getAddress());
            System.out.println("联系电话: " + hospital.getPhone());

            System.out.print("\n" + ANSI_CYAN + "新医院名称(回车跳过): " + ANSI_RESET);
            String name = scanner.nextLine().trim();
            if (!name.isEmpty()) hospital.setHospitalName(name);
            
            System.out.print(ANSI_CYAN + "新所在城市(回车跳过): " + ANSI_RESET);
            String city = scanner.nextLine().trim();
            if (!city.isEmpty()) hospital.setCity(city);
            
            System.out.print(ANSI_CYAN + "新医院等级(回车跳过): " + ANSI_RESET);
            String level = scanner.nextLine().trim();
            if (!level.isEmpty()) hospital.setLevel(level);
            
            System.out.print(ANSI_CYAN + "新医院地址(回车跳过): " + ANSI_RESET);
            String address = scanner.nextLine().trim();
            if (!address.isEmpty()) hospital.setAddress(address);
            
            System.out.print(ANSI_CYAN + "新联系电话(回车跳过): " + ANSI_RESET);
            String phone = scanner.nextLine().trim();
            if (!phone.isEmpty()) hospital.setPhone(phone);

            service.update(hospitalId, hospital);
            System.out.println("\n" + ANSI_GREEN + "✓ 医院信息修改成功！" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 修改失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void deleteHospital(HospitalService service, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_BLUE + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                     删除医院                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        try {
            System.out.print(ANSI_CYAN + "请输入要删除的医院ID: " + ANSI_RESET);
            String hospitalId = scanner.nextLine().trim();

            Hospital hospital = service.findById(hospitalId);
            if (hospital == null) {
                System.out.println(ANSI_RED + "医院不存在" + ANSI_RESET);
                waitEnter();
                return;
            }

            System.out.println("\n确认删除医院: " + hospital.getHospitalName());
            System.out.print(ANSI_CYAN + "确认删除? (y/n): " + ANSI_RESET);
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!"y".equals(confirm)) {
                System.out.println(ANSI_YELLOW + "已取消删除" + ANSI_RESET);
                waitEnter();
                return;
            }

            service.delete(hospitalId);
            System.out.println("\n" + ANSI_GREEN + "✓ 医院删除成功！" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 删除失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void listHospitals(HospitalService service) {
        List<Hospital> hospitals = service.findAll();
        if (hospitals.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   医院列表                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无医院" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) hospitals.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              医院列表 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, hospitals.size());

            System.out.println(ANSI_PURPLE + String.format("%-10s | %-30s | %-15s | %-8s", 
                    "医院ID", "医院名称", "城市", "等级") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────────────" + ANSI_RESET);

            for (int i = start; i < end; i++) {
                Hospital h = hospitals.get(i);
                System.out.printf("%-10s | %-30s | %-15s | %-8s%n",
                        h.getHospitalId(), truncate(h.getHospitalName(), 30), h.getCity(), h.getLevel());
            }
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + hospitals.size() + " 家医院" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void listDoctorsByHospital(DoctorService service, String hospitalId) {
        List<Doctor> doctors = service.findByHospitalId(hospitalId);
        if (doctors.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   本院医生列表                             ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无医生" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) doctors.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              本院医生列表 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, doctors.size());

            System.out.println(ANSI_PURPLE + String.format("%-12s | %-10s | %-12s | %-10s", 
                    "医生ID", "姓名", "职称", "科室ID") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────" + ANSI_RESET);

            for (int i = start; i < end; i++) {
                Doctor d = doctors.get(i);
                System.out.printf("%-12s | %-10s | %-12s | %-10s%n",
                        d.getDoctorId(), d.getName(), d.getTitle(), d.getDeptId());
            }
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + doctors.size() + " 位医生" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void listDoctors(DoctorService service) {
        List<Doctor> doctors = service.findAll();
        if (doctors.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   医生列表                                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无医生" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) doctors.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              医生列表 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, doctors.size());

            System.out.println(ANSI_PURPLE + String.format("%-12s | %-10s | %-12s | %-10s", 
                    "医生ID", "姓名", "职称", "科室ID") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────" + ANSI_RESET);

            for (int i = start; i < end; i++) {
                Doctor d = doctors.get(i);
                System.out.printf("%-12s | %-10s | %-12s | %-10s%n",
                        d.getDoctorId(), d.getName(), d.getTitle(), d.getDeptId());
            }
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + doctors.size() + " 位医生" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void listDepartmentsByHospital(DepartmentService service, String hospitalId) {
        List<Department> depts = service.findByHospitalId(hospitalId);
        if (depts.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   本科室列表                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无科室" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) depts.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              本科室列表 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, depts.size());

            System.out.println(ANSI_PURPLE + String.format("%-12s | %-20s | %-10s", 
                    "科室ID", "科室名称", "医院ID") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────" + ANSI_RESET);

            for (int i = start; i < end; i++) {
                Department d = depts.get(i);
                System.out.printf("%-12s | %-20s | %-10s%n", d.getDeptId(), d.getDeptName(), d.getHospitalId());
            }
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + depts.size() + " 个科室" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void listDepartments(DepartmentService service) {
        List<Department> depts = service.findAll();
        if (depts.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   科室列表                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无科室" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) depts.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              科室列表 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, depts.size());

            System.out.println(ANSI_PURPLE + String.format("%-12s | %-20s | %-10s", 
                    "科室ID", "科室名称", "医院ID") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────" + ANSI_RESET);

            for (int i = start; i < end; i++) {
                Department d = depts.get(i);
                System.out.printf("%-12s | %-20s | %-10s%n", d.getDeptId(), d.getDeptName(), d.getHospitalId());
            }
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + depts.size() + " 个科室" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void listPatients(PatientService service) {
        List<Patient> patients = service.findAll();
        if (patients.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   患者列表                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无患者" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) patients.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              患者列表 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, patients.size());

            System.out.println(ANSI_PURPLE + String.format("%-12s | %-8s | %-4s | %-4s | %-15s", 
                    "患者ID", "姓名", "性别", "年龄", "手机号") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────────────" + ANSI_RESET);

            for (int i = start; i < end; i++) {
                Patient p = patients.get(i);
                System.out.printf("%-12s | %-8s | %-4s | %-4d | %-15s%n",
                        p.getPatientId(), p.getName(), p.getGender(), p.getAge(), p.getPhone());
            }
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + patients.size() + " 位患者" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void manageSchedules(ScheduleService service, DoctorService doctorService, String hospitalId, Scanner scanner) {
        boolean isHospitalAdmin = hospitalId != null && !hospitalId.isEmpty();
        
        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   排班管理                                ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  " + ANSI_GREEN + "[1] 查看今日排班" + ANSI_RESET + "         ║");
            System.out.println("║  " + ANSI_GREEN + "[2] 按医生查询排班" + ANSI_RESET + "       ║");
            if (!isHospitalAdmin) {
                System.out.println("║  " + ANSI_GREEN + "[3] 批量生成保底排班" + ANSI_RESET + "   ║");
            }
            System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int choice = readInt(scanner, ANSI_BLUE + "请选择");
            switch (choice) {
                case 1 -> {
                    List<Schedule> schedules;
                    if (isHospitalAdmin) {
                        schedules = service.findByHospitalId(hospitalId);
                    } else {
                        schedules = service.findByDoctorAndDate("", LocalDate.now());
                    }
                    
                    if (schedules.isEmpty()) {
                        clearScreen();
                        System.out.println(ANSI_YELLOW + ANSI_BOLD + "暂无排班" + ANSI_RESET);
                        waitEnter();
                        break;
                    }

                    Map<String, List<Schedule>> doctorSchedules = schedules.stream()
                            .collect(Collectors.groupingBy(Schedule::getDoctorId));

                    List<String> doctorIds = new ArrayList<>(doctorSchedules.keySet());
                    int pageSize = 5;
                    int totalPages = (int) Math.ceil((double) doctorIds.size() / pageSize);
                    int currentPage = 1;

                    while (true) {
                        clearScreen();
                        System.out.println(ANSI_YELLOW + ANSI_BOLD);
                        if (isHospitalAdmin) {
                            System.out.println("本院排班 - 第 " + currentPage + " / " + totalPages + " 页");
                        } else {
                            System.out.println("今日排班 - 第 " + currentPage + " / " + totalPages + " 页");
                        }
                        System.out.println(ANSI_RESET);
                        System.out.println();

                        int start = (currentPage - 1) * pageSize;
                        int end = Math.min(start + pageSize, doctorIds.size());

                        for (int i = start; i < end; i++) {
                            String doctorId = doctorIds.get(i);
                            String doctorName = "未知医生";
                            try {
                                Doctor doctor = doctorService.findById(doctorId);
                                if (doctor != null) {
                                    doctorName = doctor.getName() + (doctor.getTitle() != null ? " " + doctor.getTitle() : "");
                                }
                            } catch (Exception e) {
                            }
                            
                            List<Schedule> doctorScheduleList = doctorSchedules.get(doctorId);
                            System.out.println(ANSI_GREEN + "◆ " + doctorName + " 的排班 (" + doctorScheduleList.size() + "个)" + ANSI_RESET);
                            System.out.println(ANSI_PURPLE + String.format("    %-12s | %-12s | %-8s", "日期", "时段", "号源") + ANSI_RESET);
                            System.out.println(ANSI_PURPLE + "    ────────────────────────────────" + ANSI_RESET);
                            
                            for (Schedule s : doctorScheduleList) {
                                System.out.printf("    %-12s | %-12s | %d/%d%n",
                                        s.getWorkDate().format(DATE_FORMAT), s.getTimeSlot(),
                                        s.getBookedCount(), s.getMaxPatients());
                            }
                            System.out.println();
                        }
                        System.out.println(ANSI_GREEN + "✓ 共 " + schedules.size() + " 个排班，" + doctorIds.size() + " 位医生" + ANSI_RESET);

                        if (totalPages > 1) {
                            System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                            System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                            String choice2 = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                            if ("P".equals(choice2) && currentPage > 1) {
                                currentPage--;
                            } else if ("N".equals(choice2) && currentPage < totalPages) {
                                currentPage++;
                            } else if ("0".equals(choice2)) {
                                break;
                            } else {
                                break;
                            }
                        } else {
                            waitEnter();
                            break;
                        }
                    }
                }
                case 2 -> {
                    System.out.print(ANSI_CYAN + "医生ID: " + ANSI_RESET);
                    String doctorId = scanner.nextLine();
                    List<Schedule> schedules = service.findByDoctorId(doctorId);
                    
                    if (schedules.isEmpty()) {
                        clearScreen();
                        System.out.println(ANSI_YELLOW + ANSI_BOLD + "该医生暂无排班" + ANSI_RESET);
                        waitEnter();
                        break;
                    }

                    String doctorName = "未知医生";
                    try {
                        Doctor doctor = doctorService.findById(doctorId);
                        if (doctor != null) {
                            doctorName = doctor.getName();
                        }
                    } catch (Exception e) {
                    }

                    clearScreen();
                    System.out.println(ANSI_YELLOW + ANSI_BOLD + doctorName + " 的排班" + ANSI_RESET);
                    System.out.println();
                    System.out.println(ANSI_PURPLE + String.format("%-12s | %-10s | %-8s", 
                            "日期", "时段", "号源") + ANSI_RESET);
                    System.out.println(ANSI_PURPLE + "────────────────────────────────" + ANSI_RESET);
                    for (Schedule s : schedules) {
                        System.out.printf("%-12s | %-10s | %d/%d%n",
                                s.getWorkDate().format(DATE_FORMAT), s.getTimeSlot(), 
                                s.getBookedCount(), s.getMaxPatients());
                    }
                    System.out.println(ANSI_GREEN + "\n✓ 共 " + schedules.size() + " 个排班" + ANSI_RESET);
                    waitEnter();
                }
                case 3 -> {
                    if (!isHospitalAdmin) {
                        System.out.print(ANSI_CYAN + "生成天数(默认7天): " + ANSI_RESET);
                        String daysStr = scanner.nextLine();
                        int days = daysStr.isEmpty() ? 7 : Integer.parseInt(daysStr);
                        System.out.println(ANSI_YELLOW + "正在生成保底排班...请稍候" + ANSI_RESET);
                        int count = service.generateBaseScheduleForAllDoctors(days);
                        System.out.println(ANSI_GREEN + "✓ 成功生成 " + count + " 个保底排班" + ANSI_RESET);
                        waitEnter();
                    } else {
                        System.out.println(ANSI_RED + "✗ 无效选项" + ANSI_RESET);
                        waitEnter();
                    }
                }
                case 0 -> { return; }
                default -> {
                    System.out.println(ANSI_RED + "✗ 无效选项" + ANSI_RESET);
                    waitEnter();
                }
            }
        }
    }

    private static void manageAdmins(AdminService service, Scanner scanner) {
        while (true) {
            clearScreen();
            System.out.println(ANSI_BLUE + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   管理员管理                               ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  " + ANSI_GREEN + "[1] 查看所有管理员" + ANSI_RESET + "     ║");
            System.out.println("║  " + ANSI_GREEN + "[2] 创建管理员" + ANSI_RESET + "        ║");
            System.out.println("║  " + ANSI_GREEN + "[3] 审核管理员" + ANSI_RESET + "        ║");
            System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int choice = readInt(scanner, ANSI_BLUE + "请选择");
            switch (choice) {
                case 1 -> {
                    List<Admin> admins = service.findAll();
                    clearScreen();
                    System.out.println(ANSI_YELLOW + ANSI_BOLD + "管理员列表" + ANSI_RESET);
                    System.out.println();
                    System.out.println(ANSI_PURPLE + String.format("%-12s | %-12s | %-12s | %-15s | %-10s", 
                            "管理员ID", "用户名", "姓名", "角色", "状态") + ANSI_RESET);
                    System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────────────────" + ANSI_RESET);
                    for (Admin a : admins) {
                        String status = switch (a.getAuditStatus()) {
                            case 0 -> "未审核";
                            case 1 -> ANSI_GREEN + "已通过" + ANSI_RESET;
                            case 2 -> ANSI_RED + "已拒绝" + ANSI_RESET;
                            default -> "未知";
                        };
                        System.out.printf("%-12s | %-12s | %-12s | %-15s | %-10s%n",
                                a.getAdminId(), a.getUsername(), a.getRealName(), getRoleDisplayName(a.getRole()), status);
                    }
                    System.out.println(ANSI_GREEN + "\n✓ 共 " + admins.size() + " 位管理员" + ANSI_RESET);
                    waitEnter();
                }
                case 2 -> {
                    Admin admin = new Admin();
                    clearScreen();
                    System.out.println(ANSI_YELLOW + ANSI_BOLD + "创建管理员" + ANSI_RESET);
                    System.out.println();
                    System.out.print(ANSI_CYAN + "用户名: " + ANSI_RESET);
                    admin.setUsername(scanner.nextLine());
                    System.out.print(ANSI_CYAN + "密码: " + ANSI_RESET);
                    admin.setPassword(scanner.nextLine());
                    System.out.print(ANSI_CYAN + "真实姓名: " + ANSI_RESET);
                    admin.setRealName(scanner.nextLine());
                    System.out.print(ANSI_CYAN + "角色(SUPER_ADMIN/HOSPITAL_ADMIN): " + ANSI_RESET);
                    admin.setRole(scanner.nextLine());
                    System.out.print(ANSI_CYAN + "医院ID(非超级管理员必填): " + ANSI_RESET);
                    admin.setHospitalId(scanner.nextLine());
                    try {
                        service.create(admin);
                        System.out.println("\n" + ANSI_GREEN + "✓ 创建成功" + ANSI_RESET);
                    } catch (Exception e) {
                        System.out.println("\n" + ANSI_RED + "✗ 创建失败: " + e.getMessage() + ANSI_RESET);
                    }
                    waitEnter();
                }
                case 3 -> {
                    System.out.print(ANSI_CYAN + "管理员ID: " + ANSI_RESET);
                    String adminId = scanner.nextLine();
                    System.out.print(ANSI_CYAN + "审核状态(0=未审核, 1=通过, 2=拒绝): " + ANSI_RESET);
                    int status = readInt(scanner, "");
                    try {
                        service.audit(adminId, status, "");
                        System.out.println("\n" + ANSI_GREEN + "✓ 审核成功" + ANSI_RESET);
                    } catch (Exception e) {
                        System.out.println("\n" + ANSI_RED + "✗ 审核失败: " + e.getMessage() + ANSI_RESET);
                    }
                    waitEnter();
                }
                case 0 -> { return; }
                default -> System.out.println(ANSI_RED + "✗ 无效选项" + ANSI_RESET);
            }
        }
    }

    private static String getRoleDisplayName(String role) {
        switch (role) {
            case "SUPER_ADMIN": return "超级管理员";
            case "HOSPITAL_ADMIN": return "医院管理员";
            case "EMERGENCY_ADMIN": return "应急管理员";
            default: return role;
        }
    }

    private static void viewDoctorSchedule(ScheduleService service, String doctorId) {
        List<Schedule> schedules = service.findAvailableByDoctorId(doctorId);
        if (schedules.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_CYAN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   我的排班                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无排班" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) schedules.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_CYAN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              我的排班 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, schedules.size());

            System.out.println(ANSI_PURPLE + String.format("%-12s | %-10s | %-8s | %-15s", 
                    "日期", "时段", "号源", "状态") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "─────────────────────────────────────" + ANSI_RESET);

            for (int i = start; i < end; i++) {
                Schedule s = schedules.get(i);
                String status = s.getBookedCount() >= s.getMaxPatients() ? ANSI_RED + "已满" + ANSI_RESET : ANSI_GREEN + "可预约" + ANSI_RESET;
                System.out.printf("%-12s | %-10s | %d/%d | %-15s%n", 
                        s.getWorkDate().format(DATE_FORMAT), s.getTimeSlot(), s.getBookedCount(), s.getMaxPatients(), status);
            }
            System.out.println(ANSI_PURPLE + "─────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + schedules.size() + " 个排班" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void manageCustomSchedule(ScheduleService scheduleService, DoctorService doctorService, String doctorId, Scanner scanner) {
        Doctor doctor = doctorService.findById(doctorId);
        if (doctor == null) {
            System.out.println(ANSI_RED + "✗ 医生信息不存在" + ANSI_RESET);
            waitEnter();
            return;
        }

        while (true) {
            clearScreen();
            System.out.println(ANSI_CYAN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   自定义排班管理                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  " + ANSI_GREEN + "[1] 添加排班" + ANSI_RESET + "           ║");
            System.out.println("║  " + ANSI_GREEN + "[2] 修改排班" + ANSI_RESET + "           ║");
            System.out.println("║  " + ANSI_GREEN + "[3] 删除排班" + ANSI_RESET + "           ║");
            System.out.println("║  " + ANSI_GREEN + "[4] 查看我的排班" + ANSI_RESET + "       ║");
            System.out.println("║  " + ANSI_YELLOW + "[0] 返回上一级" + ANSI_RESET + "       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "医生: " + doctor.getName() + " (" + (doctor.getTitle() != null ? doctor.getTitle() : "") + ")" + ANSI_RESET);

            int choice = readInt(scanner, ANSI_BLUE + "请选择操作");
            switch (choice) {
                case 1 -> addCustomSchedule(scheduleService, doctor, scanner);
                case 2 -> updateCustomSchedule(scheduleService, doctorId, scanner);
                case 3 -> deleteCustomSchedule(scheduleService, doctorId, scanner);
                case 4 -> viewDoctorSchedule(scheduleService, doctorId);
                case 0 -> {
                    return;
                }
                default -> {
                    System.out.println("\n" + ANSI_RED + "✗ 无效选项" + ANSI_RESET);
                    waitEnter();
                }
            }
        }
    }

    private static final String[] TIME_SLOTS = {
            "8:00-8:20", "8:20-8:40", "8:40-9:00",
            "9:00-9:20", "9:20-9:40", "9:40-10:00",
            "10:00-10:20", "10:20-10:40", "10:40-11:00",
            "11:00-11:20", "11:20-11:40", "11:40-12:00",
            "14:00-14:20", "14:20-14:40", "14:40-15:00",
            "15:00-15:20", "15:20-15:40", "15:40-16:00",
            "16:00-16:20", "16:20-16:40", "16:40-17:00",
            "17:00-17:20", "17:20-17:40", "17:40-18:00"
    };

    private static void addCustomSchedule(ScheduleService scheduleService, Doctor doctor, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   添加自定义排班                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_YELLOW + "医生: " + doctor.getName() + " | 科室: " + doctor.getDeptId() + ANSI_RESET);
        System.out.println();

        System.out.println(ANSI_YELLOW + "请选择排班日期(输入序号):" + ANSI_RESET);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            LocalDate date = today.plusDays(i);
            String tag = i == 0 ? ANSI_GREEN + " (今天)" + ANSI_RESET : (i == 1 ? ANSI_CYAN + " (明天)" + ANSI_RESET : "");
            System.out.printf(ANSI_GREEN + "[%2d] " + ANSI_RESET + "%s%s", i + 1, date.format(DATE_DISPLAY_FORMAT), tag);
            if ((i + 1) % 2 == 0) {
                System.out.println();
            } else {
                System.out.print("      ");
            }
        }
        System.out.println("\n");

        int dateIdx = readInt(scanner, ANSI_CYAN + "请输入日期序号") - 1;
        if (dateIdx < 0 || dateIdx >= 14) {
            System.out.println(ANSI_RED + "✗ 无效的日期选择" + ANSI_RESET);
            waitEnter();
            return;
        }
        LocalDate workDate = today.plusDays(dateIdx);

        System.out.println("\n" + ANSI_YELLOW + "请选择时段(输入序号):" + ANSI_RESET);
        for (int i = 0; i < TIME_SLOTS.length; i++) {
            System.out.printf(ANSI_GREEN + "[%2d] " + ANSI_RESET + "%s", i + 1, TIME_SLOTS[i]);
            if ((i + 1) % 4 == 0) {
                System.out.println();
            } else {
                System.out.print("  ");
            }
        }
        System.out.println("\n");

        int slotIdx = readInt(scanner, ANSI_CYAN + "请输入时段序号") - 1;
        if (slotIdx < 0 || slotIdx >= TIME_SLOTS.length) {
            System.out.println(ANSI_RED + "✗ 无效的时段选择" + ANSI_RESET);
            waitEnter();
            return;
        }
        String timeSlot = TIME_SLOTS[slotIdx];

        System.out.println("\n" + ANSI_YELLOW + "请选择最大预约人数(输入序号):" + ANSI_RESET);
        int[] maxOptions = {1, 2, 3, 4};
        for (int i = 0; i < maxOptions.length; i++) {
            String tag = maxOptions[i] == 2 ? ANSI_GREEN + " (默认)" + ANSI_RESET : "";
            System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%d人%s  ", i + 1, maxOptions[i], tag);
        }
        System.out.println("\n");

        int maxIdx = readInt(scanner, ANSI_CYAN + "请输入人数序号") - 1;
        int maxPatients;
        if (maxIdx < 0 || maxIdx >= maxOptions.length) {
            maxPatients = 2;
            System.out.println(ANSI_YELLOW + "使用默认人数: 2人" + ANSI_RESET);
        } else {
            maxPatients = maxOptions[maxIdx];
        }

        System.out.println("\n" + ANSI_YELLOW + "确认排班信息:" + ANSI_RESET);
        System.out.println("日期: " + workDate.format(DATE_DISPLAY_FORMAT));
        System.out.println("时段: " + timeSlot);
        System.out.println("号源: " + maxPatients + "人");
        System.out.print(ANSI_CYAN + "\n确认添加? (y/n): " + ANSI_RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm)) {
            System.out.println(ANSI_YELLOW + "已取消添加" + ANSI_RESET);
            waitEnter();
            return;
        }

        try {
            scheduleService.createSchedule(doctor.getHospitalId(), doctor.getDoctorId(), workDate, timeSlot, maxPatients);
            System.out.println("\n" + ANSI_GREEN + "✓ 排班添加成功！" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 添加失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void updateCustomSchedule(ScheduleService scheduleService, String doctorId, Scanner scanner) {
        List<Schedule> schedules = scheduleService.findByDoctorId(doctorId);
        if (schedules.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_YELLOW + "暂无排班记录" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) schedules.size() / pageSize);
        int currentPage = 1;
        Schedule selectedSchedule = null;

        while (selectedSchedule == null) {
            clearScreen();
            System.out.println(ANSI_CYAN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              修改排班 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, schedules.size());

            System.out.println(ANSI_PURPLE + String.format("%-4s | %-12s | %-10s | %-8s", "序号", "日期", "时段", "号源") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "──────────────────────────────────" + ANSI_RESET);

            int index = start + 1;
            for (int i = start; i < end; i++) {
                Schedule s = schedules.get(i);
                System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%-12s | %-10s | %d/%d%n", 
                        index++, s.getWorkDate().format(DATE_FORMAT), s.getTimeSlot(), s.getBookedCount(), s.getMaxPatients());
            }
            System.out.println(ANSI_PURPLE + "──────────────────────────────────" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println(ANSI_BLUE + "[P] 上一页  [N] 下一页" + ANSI_RESET);
            }
            System.out.print(ANSI_CYAN + "请选择要修改的排班序号(0返回): " + ANSI_RESET);
            String input = scanner.nextLine().trim();

            if ("0".equals(input)) {
                return;
            } else if ("P".equalsIgnoreCase(input) && currentPage > 1) {
                currentPage--;
                continue;
            } else if ("N".equalsIgnoreCase(input) && currentPage < totalPages) {
                currentPage++;
                continue;
            }

            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < schedules.size()) {
                    selectedSchedule = schedules.get(idx);
                } else {
                    System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
                    waitEnter();
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "✗ 无效输入" + ANSI_RESET);
                waitEnter();
            }
        }

        clearScreen();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   修改排班信息                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println("当前信息:");
        System.out.println("日期: " + selectedSchedule.getWorkDate().format(DATE_FORMAT));
        System.out.println("时段: " + selectedSchedule.getTimeSlot());
        System.out.println("当前号源: " + selectedSchedule.getMaxPatients() + "人");
        System.out.println("已预约: " + selectedSchedule.getBookedCount() + "人");
        System.out.println();

        System.out.print(ANSI_CYAN + "新日期(回车跳过，格式: yyyy-MM-dd): " + ANSI_RESET);
        String dateStr = scanner.nextLine().trim();
        LocalDate newDate = null;
        if (!dateStr.isEmpty()) {
            try {
                newDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "✗ 日期格式错误，将保留原日期" + ANSI_RESET);
            }
        }

        System.out.print(ANSI_CYAN + "新时段(回车跳过，输入序号): " + ANSI_RESET);
        String slotInput = scanner.nextLine().trim();
        String newTimeSlot = null;
        if (!slotInput.isEmpty()) {
            try {
                int slotIdx = Integer.parseInt(slotInput) - 1;
                if (slotIdx >= 0 && slotIdx < TIME_SLOTS.length) {
                    newTimeSlot = TIME_SLOTS[slotIdx];
                } else {
                    System.out.println(ANSI_RED + "✗ 无效时段，将保留原时段" + ANSI_RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "✗ 无效输入，将保留原时段" + ANSI_RESET);
            }
        }

        System.out.print(ANSI_CYAN + "新最大人数(回车跳过): " + ANSI_RESET);
        String maxStr = scanner.nextLine().trim();
        Integer newMaxPatients = null;
        if (!maxStr.isEmpty()) {
            try {
                newMaxPatients = Integer.parseInt(maxStr);
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "✗ 无效数字，将保留原人数" + ANSI_RESET);
            }
        }

        try {
            Schedule schedule = new Schedule();
            schedule.setWorkDate(newDate);
            schedule.setTimeSlot(newTimeSlot);
            schedule.setMaxPatients(newMaxPatients);
            scheduleService.update(selectedSchedule.getScheduleId(), schedule);
            System.out.println("\n" + ANSI_GREEN + "✓ 排班修改成功！" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 修改失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void deleteCustomSchedule(ScheduleService scheduleService, String doctorId, Scanner scanner) {
        List<Schedule> schedules = scheduleService.findByDoctorId(doctorId);
        if (schedules.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_YELLOW + "暂无排班记录" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) schedules.size() / pageSize);
        int currentPage = 1;
        Schedule selectedSchedule = null;

        while (selectedSchedule == null) {
            clearScreen();
            System.out.println(ANSI_CYAN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              删除排班 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, schedules.size());

            System.out.println(ANSI_PURPLE + String.format("%-4s | %-12s | %-10s | %-8s", "序号", "日期", "时段", "已预约") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "──────────────────────────────────" + ANSI_RESET);

            int index = start + 1;
            for (int i = start; i < end; i++) {
                Schedule s = schedules.get(i);
                String bookedStatus = s.getBookedCount() > 0 ? ANSI_RED + s.getBookedCount() + "人" + ANSI_RESET : ANSI_GREEN + "0人" + ANSI_RESET;
                System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%-12s | %-10s | %s%n", 
                        index++, s.getWorkDate().format(DATE_FORMAT), s.getTimeSlot(), bookedStatus);
            }
            System.out.println(ANSI_PURPLE + "──────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "注: 已有预约的排班无法删除" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println(ANSI_BLUE + "[P] 上一页  [N] 下一页" + ANSI_RESET);
            }
            System.out.print(ANSI_CYAN + "请选择要删除的排班序号(0返回): " + ANSI_RESET);
            String input = scanner.nextLine().trim();

            if ("0".equals(input)) {
                return;
            } else if ("P".equalsIgnoreCase(input) && currentPage > 1) {
                currentPage--;
                continue;
            } else if ("N".equalsIgnoreCase(input) && currentPage < totalPages) {
                currentPage++;
                continue;
            }

            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < schedules.size()) {
                    selectedSchedule = schedules.get(idx);
                } else {
                    System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
                    waitEnter();
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "✗ 无效输入" + ANSI_RESET);
                waitEnter();
            }
        }

        if (selectedSchedule.getBookedCount() > 0) {
            System.out.println(ANSI_RED + "✗ 该排班已有" + selectedSchedule.getBookedCount() + "人预约，无法删除" + ANSI_RESET);
            waitEnter();
            return;
        }

        System.out.print(ANSI_CYAN + "\n确认删除该排班? (y/n): " + ANSI_RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm)) {
            System.out.println(ANSI_YELLOW + "已取消删除" + ANSI_RESET);
            waitEnter();
            return;
        }

        try {
            scheduleService.delete(selectedSchedule.getScheduleId());
            System.out.println(ANSI_GREEN + "✓ 排班删除成功！" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "✗ 删除失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void viewTodaySchedule(ScheduleService service, String doctorId) {
        clearScreen();
        System.out.println(ANSI_CYAN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   今日排班概览                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        LocalDate today = LocalDate.now();
        List<Schedule> schedules = service.findByDoctorAndDate(doctorId, today);

        if (schedules.isEmpty()) {
            System.out.println(ANSI_YELLOW + "今日暂无排班" + ANSI_RESET);
            waitEnter();
            return;
        }

        int totalBooked = schedules.stream().mapToInt(Schedule::getBookedCount).sum();
        int totalMax = schedules.stream().mapToInt(Schedule::getMaxPatients).sum();

        System.out.println(ANSI_BLUE + "日期: " + ANSI_RESET + today.format(DATE_DISPLAY_FORMAT));
        System.out.println(ANSI_BLUE + "排班数: " + ANSI_RESET + schedules.size() + 
                " | " + ANSI_GREEN + "已预约: " + ANSI_RESET + totalBooked + 
                " | " + ANSI_YELLOW + "剩余: " + ANSI_RESET + (totalMax - totalBooked));
        System.out.println();

        System.out.println(ANSI_PURPLE + String.format("%-10s | %-8s | %-15s", "时段", "号源", "状态") + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "─────────────────────────────" + ANSI_RESET);

        for (Schedule s : schedules) {
            String status = s.getBookedCount() >= s.getMaxPatients() ? ANSI_RED + "已满" + ANSI_RESET : ANSI_GREEN + "可预约" + ANSI_RESET;
            System.out.printf("%-10s | %-8s | %-15s%n", s.getTimeSlot(), s.getBookedCount() + "/" + s.getMaxPatients(), status);
        }
        waitEnter();
    }

    private static void viewDoctorAppointments(AppointmentService service, PatientService patientService, String doctorId) {
        List<Appointment> appointments = service.findByDoctorId(doctorId);
        if (appointments.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_CYAN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   预约患者                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无预约患者" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) appointments.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_CYAN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              预约患者 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, appointments.size());

            System.out.println(ANSI_PURPLE + String.format("%-4s | %-12s | %-12s | %-10s | %-8s", 
                    "序号", "患者姓名", "日期", "时段", "状态") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────────────────" + ANSI_RESET);

            int index = start + 1;
            for (int i = start; i < end; i++) {
                Appointment a = appointments.get(i);
                String patientName = "未知患者";
                if (a.getPatientId() != null && !a.getPatientId().isEmpty()) {
                    try {
                        Patient patient = patientService.findById(a.getPatientId());
                        if (patient != null) {
                            patientName = patient.getName();
                            if (patientName.length() > 12) {
                                patientName = patientName.substring(0, 9) + "...";
                            }
                        }
                    } catch (Exception e) {
                        // 患者查询失败，使用默认值
                    }
                }
                String status = switch (a.getStatus()) {
                    case 0 -> ANSI_RED + "已取消" + ANSI_RESET;
                    case 1 -> ANSI_YELLOW + "待就诊" + ANSI_RESET;
                    case 2 -> ANSI_GREEN + "已完成" + ANSI_RESET;
                    default -> "未知";
                };
                String dateStr = a.getAppointmentDate() != null ? a.getAppointmentDate().format(DATE_FORMAT) : "";
                String timeSlot = a.getTimeSlot() != null ? a.getTimeSlot() : "";
                System.out.printf("%-4d | %-12s | %-12s | %-10s | %-8s%n", 
                        index++, patientName, dateStr, timeSlot, status);
            }
            System.out.println(ANSI_PURPLE + "──────────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + appointments.size() + " 位预约患者" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void searchHospitalAndBook(HospitalService hospitalService, DepartmentService departmentService,
                                               DoctorService doctorService, ScheduleService scheduleService,
                                               AppointmentService appointmentService, String patientId, Scanner scanner) {
        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                          选择医院                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        List<Hospital> hospitals = hospitalService.findAll();
        if (hospitals.isEmpty()) {
            System.out.println(ANSI_RED + "✗ 暂无医院" + ANSI_RESET);
            waitEnter();
            return;
        }

        Hospital selectedHospital = com.hospital.console.PaginationUtils.paginateWithSelection(hospitals, scanner, 10, new com.hospital.console.PaginationUtils.ItemRenderer<Hospital>() {
            @Override
            public void renderHeader() {
                System.out.println(ANSI_GREEN + ANSI_BOLD);
                System.out.println("╔══════════════════════════════════════════════════════════╗");
                System.out.println("║                          选择医院                          ║");
                System.out.println("╚══════════════════════════════════════════════════════════╝");
                System.out.println(ANSI_RESET);
                System.out.println("┌──────┬────────────┬─────────────────────────────┐");
                System.out.println("│ 序号 │ 医院ID     │ 医院名称                    │");
                System.out.println("├──────┼────────────┼─────────────────────────────┤");
            }

            @Override
            public void renderItem(Hospital h, int index) {
                System.out.printf("│ %-4d│ %-10s│ %-25s│%n", index, h.getHospitalId(), h.getHospitalName());
            }

            @Override
            public void renderFooter() {
                System.out.println("└──────┴────────────┴─────────────────────────────┘");
            }
        });

        if (selectedHospital == null) {
            return;
        }

        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   选择科室                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_YELLOW + "医院: " + selectedHospital.getHospitalName() + ANSI_RESET);

        List<Department> depts = departmentService.findByHospitalId(selectedHospital.getHospitalId());
        for (int i = 0; i < depts.size(); i++) {
            Department d = depts.get(i);
            System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%s | %s%n", i + 1, d.getDeptId(), d.getDeptName());
        }

        int deptIdx = readInt(scanner, ANSI_BLUE + "请选择科室") - 1;
        if (deptIdx < 0 || deptIdx >= depts.size()) {
            System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
            waitEnter();
            return;
        }
        Department selectedDept = depts.get(deptIdx);

        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   选择医生                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_YELLOW + "医院: " + selectedHospital.getHospitalName() + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "科室: " + selectedDept.getDeptName() + ANSI_RESET);

        List<Doctor> doctors = doctorService.findByDeptIdAndHospitalId(selectedDept.getDeptId(), selectedHospital.getHospitalId());
        for (int i = 0; i < doctors.size(); i++) {
            Doctor d = doctors.get(i);
            System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%s | %s | %s | 挂号费: %d%n", 
                    i + 1, d.getDoctorId(), d.getName(), d.getTitle(), d.getConsultationFee());
        }

        int doctorIdx = readInt(scanner, ANSI_BLUE + "请选择医生") - 1;
        if (doctorIdx < 0 || doctorIdx >= doctors.size()) {
            System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
            waitEnter();
            return;
        }
        Doctor selectedDoctor = doctors.get(doctorIdx);

        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   选择日期                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println(ANSI_YELLOW + "医生: " + selectedDoctor.getName() + " (" + selectedDoctor.getTitle() + ")" + ANSI_RESET);

        List<String> dates = scheduleService.findScheduleDatesByDoctor(selectedDoctor.getDoctorId());
        if (dates.isEmpty()) {
            System.out.println(ANSI_RED + "该医生目前没有排班" + ANSI_RESET);
            waitEnter();
            return;
        }

        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = LocalDate.parse(dates.get(i));
            String displayDate = date.format(DATE_DISPLAY_FORMAT);
            String tag = date.isEqual(LocalDate.now()) ? ANSI_GREEN + " (今天)" : "";
            System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%s%s%n", i + 1, displayDate, tag);
        }

        int dateIdx = readInt(scanner, ANSI_BLUE + "请选择日期") - 1;
        if (dateIdx < 0 || dateIdx >= dates.size()) {
            System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
            waitEnter();
            return;
        }
        LocalDate selectedDate = LocalDate.parse(dates.get(dateIdx));

        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   选择时段                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        List<Schedule> schedules = scheduleService.findAvailableSchedules(selectedDoctor.getDoctorId(), selectedDate);
        if (schedules.isEmpty()) {
            System.out.println(ANSI_RED + "该医生当日无可用号源" + ANSI_RESET);
            waitEnter();
            return;
        }

        System.out.println(ANSI_PURPLE + String.format("%-4s | %-10s | %-8s | %-15s", "序号", "时段", "号源", "状态") + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "─────────────────────────────────────" + ANSI_RESET);

        for (int i = 0; i < schedules.size(); i++) {
            Schedule s = schedules.get(i);
            String status = s.getBookedCount() >= s.getMaxPatients() ? ANSI_RED + "已满" + ANSI_RESET : ANSI_GREEN + "可预约" + ANSI_RESET;
            System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%-10s | %-8s | %-15s%n", i + 1, s.getTimeSlot(), s.getBookedCount() + "/" + s.getMaxPatients(), status);
        }

        int slotIdx = readInt(scanner, ANSI_BLUE + "请选择时段") - 1;
        if (slotIdx < 0 || slotIdx >= schedules.size()) {
            System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
            waitEnter();
            return;
        }
        Schedule selectedSchedule = schedules.get(slotIdx);

        clearScreen();
        System.out.println(ANSI_YELLOW + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   确认预约                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println();

        System.out.println(ANSI_BLUE + "医院: " + ANSI_RESET + selectedHospital.getHospitalName());
        System.out.println(ANSI_BLUE + "科室: " + ANSI_RESET + selectedDept.getDeptName());
        System.out.println(ANSI_BLUE + "医生: " + ANSI_RESET + selectedDoctor.getName() + " (" + selectedDoctor.getTitle() + ")");
        System.out.println(ANSI_BLUE + "日期: " + ANSI_RESET + selectedDate.format(DATE_DISPLAY_FORMAT));
        System.out.println(ANSI_BLUE + "时段: " + ANSI_RESET + selectedSchedule.getTimeSlot());
        System.out.println(ANSI_BLUE + "挂号费: " + ANSI_RESET + selectedDoctor.getConsultationFee());

        System.out.print(ANSI_CYAN + "\n确认预约? (y/n): " + ANSI_RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!"y".equals(confirm)) {
            System.out.println(ANSI_YELLOW + "已取消预约" + ANSI_RESET);
            waitEnter();
            return;
        }

        try {
            Appointment appointment = appointmentService.createAppointment(patientId, selectedSchedule.getScheduleId());
            System.out.println("\n" + ANSI_GREEN + "✓ 预约成功！预约ID: " + appointment.getAppointmentId() + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_RED + "✗ 预约失败: " + e.getMessage() + ANSI_RESET);
        }
        waitEnter();
    }

    private static void viewPatientAppointments(AppointmentService service, DoctorService doctorService, String patientId) {
        clearScreen();
        System.out.println(ANSI_GREEN + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   我的预约                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);

        List<Appointment> appointments = service.findByPatientId(patientId);
        if (appointments.isEmpty()) {
            System.out.println(ANSI_YELLOW + "暂无预约记录" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) appointments.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_GREEN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              我的预约 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, appointments.size());

            System.out.println(ANSI_PURPLE + String.format("%-6s | %-16s | %-12s | %-10s | %-8s", 
                    "序号", "医生姓名", "日期", "时段", "状态") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────────────" + ANSI_RESET);

            int index = start + 1;
            for (int i = start; i < end; i++) {
                Appointment a = appointments.get(i);
                String doctorName = "未知医生";
                if (a.getDoctorId() != null) {
                    Doctor doctor = doctorService.findById(a.getDoctorId());
                    if (doctor != null) {
                        String title = doctor.getTitle() != null ? doctor.getTitle() : "";
                        doctorName = doctor.getName() + (title.isEmpty() ? "" : " " + title);
                        if (doctorName.length() > 16) {
                            doctorName = doctorName.substring(0, 13) + "...";
                        }
                    }
                }
                String status = switch (a.getStatus()) {
                    case 0 -> ANSI_RED + "已取消" + ANSI_RESET;
                    case 1 -> ANSI_YELLOW + "待就诊" + ANSI_RESET;
                    case 2 -> ANSI_GREEN + "已完成" + ANSI_RESET;
                    case 3 -> ANSI_CYAN + "进行中" + ANSI_RESET;
                    default -> "未知";
                };
                String dateStr = a.getAppointmentDate() != null ? a.getAppointmentDate().format(DATE_FORMAT) : "";
                String timeSlot = a.getTimeSlot() != null ? a.getTimeSlot() : "";
                System.out.printf("%-6d | %-16s | %-12s | %-10s | %-8s%n", 
                        index++, doctorName, dateStr, timeSlot, status);
            }
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + appointments.size() + " 条预约" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
                System.out.print(ANSI_CYAN + "请输入操作: " + ANSI_RESET);
                String choice = new Scanner(System.in, "UTF-8").nextLine().trim().toUpperCase();
                if ("P".equals(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equals(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                waitEnter();
                break;
            }
        }
    }

    private static void cancelAppointment(AppointmentService service, DoctorService doctorService, String patientId, Scanner scanner) {
        List<Appointment> appointments = service.findByPatientId(patientId);
        if (appointments.isEmpty()) {
            clearScreen();
            System.out.println(ANSI_GREEN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║                   取消预约                                ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);
            System.out.println(ANSI_YELLOW + "暂无预约记录" + ANSI_RESET);
            waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) appointments.size() / pageSize);
        int currentPage = 1;

        while (true) {
            clearScreen();
            System.out.println(ANSI_GREEN + ANSI_BOLD);
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║              取消预约 - 第 " + currentPage + " / " + totalPages + " 页              ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println(ANSI_RESET);

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, appointments.size());

            System.out.println(ANSI_PURPLE + String.format("%-4s | %-16s | %-12s | %-10s | %-8s", "序号", "医生姓名", "日期", "时段", "状态") + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────────────" + ANSI_RESET);

            int index = start + 1;
            for (int i = start; i < end; i++) {
                Appointment a = appointments.get(i);
                String doctorName = "未知医生";
                if (a.getDoctorId() != null && !a.getDoctorId().isEmpty()) {
                    try {
                        Doctor doctor = doctorService.findById(a.getDoctorId());
                        if (doctor != null) {
                            String title = doctor.getTitle() != null ? doctor.getTitle() : "";
                            doctorName = doctor.getName() + (title.isEmpty() ? "" : " " + title);
                            if (doctorName.length() > 16) {
                                doctorName = doctorName.substring(0, 13) + "...";
                            }
                        }
                    } catch (Exception e) {
                        // 医生查询失败，使用默认值
                    }
                }
                String status = switch (a.getStatus()) {
                    case 0 -> ANSI_RED + "已取消" + ANSI_RESET;
                    case 1 -> ANSI_YELLOW + "待就诊" + ANSI_RESET;
                    case 2 -> ANSI_GREEN + "已完成" + ANSI_RESET;
                    default -> "未知";
                };
                String dateStr = a.getAppointmentDate() != null ? a.getAppointmentDate().format(DATE_FORMAT) : "";
                String timeSlot = a.getTimeSlot() != null ? a.getTimeSlot() : "";
                System.out.printf(ANSI_GREEN + "[%d] " + ANSI_RESET + "%-16s | %-12s | %-10s | %-8s%n", index++, doctorName, dateStr, timeSlot, status);
            }
            System.out.println(ANSI_PURPLE + "────────────────────────────────────────────────────────────" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "✓ 共 " + appointments.size() + " 条预约" + ANSI_RESET);

            if (totalPages > 1) {
                System.out.println("\n" + ANSI_BLUE + "[P] 上一页  [N] 下一页  [0] 返回" + ANSI_RESET);
            }

            System.out.print(ANSI_CYAN + "请选择要取消的预约序号（0返回）: " + ANSI_RESET);
            int choice = readInt(scanner, "");
            
            if (choice == 0) {
                break;
            }
            
            if (totalPages > 1) {
                if ("P".equalsIgnoreCase(String.valueOf((char)(choice + '0')))) {
                    if (currentPage > 1) currentPage--;
                    continue;
                } else if ("N".equalsIgnoreCase(String.valueOf((char)(choice + '0')))) {
                    if (currentPage < totalPages) currentPage++;
                    continue;
                }
            }

            int idx = choice - 1;
            if (idx < 0 || idx >= appointments.size()) {
                System.out.println(ANSI_RED + "✗ 无效选择" + ANSI_RESET);
                waitEnter();
                continue;
            }

            Appointment appointment = appointments.get(idx);
            if (appointment.getStatus() != 1) {
                System.out.println(ANSI_RED + "✗ 只能取消待就诊的预约" + ANSI_RESET);
                waitEnter();
                continue;
            }

            System.out.print(ANSI_CYAN + "确认取消预约? (y/n): " + ANSI_RESET);
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!"y".equals(confirm)) {
                System.out.println(ANSI_YELLOW + "已取消操作" + ANSI_RESET);
                waitEnter();
                continue;
            }

            try {
                service.cancelAppointmentSimple(appointment.getAppointmentId());
                System.out.println(ANSI_GREEN + "✓ 预约已取消" + ANSI_RESET);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "✗ 取消失败: " + e.getMessage() + ANSI_RESET);
            }
            waitEnter();
            break;
        }
    }

    private static void showSystemStats(HospitalService hospitalService, DoctorService doctorService,
                                        DepartmentService departmentService, ScheduleService scheduleService,
                                        AppointmentService appointmentService, PatientService patientService) {
        clearScreen();
        System.out.println(ANSI_PURPLE + ANSI_BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                   系统统计                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        System.out.println();

        List<Hospital> hospitals = hospitalService.findAll();
        int hospitalCount = hospitals.size();
        int doctorCount = doctorService.findAll().size();
        int deptCount = departmentService.findAll().size();
        int patientCount = patientService.findAll().size();
        
        int scheduleCount = 0;
        for (Hospital h : hospitals) {
            scheduleCount += scheduleService.findByHospitalId(h.getHospitalId()).size();
        }
        
        int appointmentCount = 0;
        List<Patient> patients = patientService.findAll();
        for (Patient p : patients) {
            appointmentCount += appointmentService.findByPatient(p.getPatientId()).size();
        }

        System.out.println(ANSI_BLUE + "【数据统计】");
        System.out.println("──────────────");
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 家%n", "医院: ", hospitalCount);
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 个%n", "科室: ", deptCount);
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 位%n", "医生: ", doctorCount);
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 位%n", "患者: ", patientCount);
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 个%n", "排班: ", scheduleCount);
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 条%n", "预约: ", appointmentCount);

        LocalDate today = LocalDate.now();
        int todayAppointments = 0;
        int pendingAppointments = 0;
        int completedAppointments = 0;
        
        for (Patient p : patients) {
            List<Appointment> apps = appointmentService.findByPatient(p.getPatientId());
            for (Appointment a : apps) {
                if (a.getAppointmentDate() != null && a.getAppointmentDate().equals(today.toString())) {
                    todayAppointments++;
                }
                if (a.getStatus() == 1) {
                    pendingAppointments++;
                } else if (a.getStatus() == 2) {
                    completedAppointments++;
                }
            }
        }

        System.out.println("\n" + ANSI_BLUE + "【今日统计】");
        System.out.println("──────────────");
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 条%n", "今日预约: ", todayAppointments);
        System.out.printf(ANSI_YELLOW + "%-12s" + ANSI_RESET + "%d 条%n", "待就诊: ", pendingAppointments);
        System.out.printf(ANSI_GREEN + "%-12s" + ANSI_RESET + "%d 条%n", "已完成: ", completedAppointments);

        System.out.println("\n" + ANSI_BLUE + "【系统状态】");
        System.out.println("──────────────");
        System.out.println(ANSI_GREEN + "✓ 数据库连接正常" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "✓ 服务运行正常" + ANSI_RESET);
        waitEnter();
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            try {
                if (!prompt.isEmpty()) {
                    System.out.print(prompt + ": ");
                }
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "✗ 请输入有效的数字" + ANSI_RESET);
            }
        }
    }

    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}

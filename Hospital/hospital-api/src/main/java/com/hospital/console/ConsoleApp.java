package com.hospital.console;

import com.hospital.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ConsoleApp implements CommandLineRunner {

    @Autowired
    private AdminService adminService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PatientService patientService;

    private Scanner scanner;
    private AdminConsole adminConsole;
    private DoctorConsole doctorConsole;
    private PatientConsole patientConsole;

    @Override
    public void run(String... args) throws Exception {
        if (System.console() == null) {
            return;
        }
        
        scanner = new Scanner(System.in);
        adminConsole = new AdminConsole(adminService, hospitalService, departmentService, scanner);
        doctorConsole = new DoctorConsole(doctorService, hospitalService, departmentService, scheduleService, appointmentService, scanner);
        patientConsole = new PatientConsole(patientService, hospitalService, departmentService, doctorService, scheduleService, appointmentService, scanner);
        
        System.out.println("\n\n");
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("请选择运行模式：");
        System.out.println("[1] Web服务模式（启动API）");
        System.out.println("[2] 控制台模式（命令行交互）");
        System.out.print("请输入选项：");
        
        try {
            int choice = scanner.nextInt();
            scanner.nextLine();
            
            if (choice == 2) {
                startConsole();
            }
        } catch (Exception e) {
            System.out.println("非交互式环境，跳过控制台模式");
        }
        // 如果选择1或其他，继续启动Web服务
    }

    public void startConsole() {
        while (true) {
            printMainMenu();
            int choice = ConsoleUtils.readInt(scanner, "请选择角色");

            switch (choice) {
                case 1:
                    adminConsole.login();
                    break;
                case 2:
                    doctorConsole.mainMenu();
                    break;
                case 3:
                    patientConsole.mainMenu();
                    break;
                case 0:
                    System.out.println("感谢使用医院预约系统，再见！");
                    System.exit(0);
                    break;
                default:
                    System.out.println("无效选项，请重新选择");
                    ConsoleUtils.waitEnter();
            }
        }
    }

    private void printMainMenu() {
        ConsoleUtils.clearScreen();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              医院预约系统 - 角色选择                  ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  [1] 管理员系统                                          ║");
        System.out.println("║  [2] 医生系统                                            ║");
        System.out.println("║  [3] 患者系统                                            ║");
        System.out.println("║  [0] 退出系统                                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.print("请输入选项：");
    }
}
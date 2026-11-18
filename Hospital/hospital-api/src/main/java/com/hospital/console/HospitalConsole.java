package com.hospital.console;

import com.hospital.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * 医院控制台主控制器
 */
@Component
public class HospitalConsole {

    private Scanner scanner;
    private AdminConsole adminConsole;
    private DoctorConsole doctorConsole;
    private PatientConsole patientConsole;

    public HospitalConsole(
            @Autowired AdminService adminService,
            @Autowired HospitalService hospitalService,
            @Autowired DoctorService doctorService,
            @Autowired DepartmentService departmentService,
            @Autowired ScheduleService scheduleService,
            @Autowired AppointmentService appointmentService,
            @Autowired PatientService patientService
    ) {
        this.scanner = new Scanner(System.in);
        this.adminConsole = new AdminConsole(adminService, hospitalService, departmentService, scanner);
        this.doctorConsole = new DoctorConsole(doctorService, hospitalService, departmentService, scheduleService, appointmentService, scanner);
        this.patientConsole = new PatientConsole(patientService, hospitalService, departmentService, doctorService, scheduleService, appointmentService, scanner);
    }

    public void start() {
        while (true) {
            printMainMenu();
            int choice = ConsoleUtils.readInt(scanner, "请输入选项");

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
                    System.out.println("\n感谢使用医院预约系统，再见！");
                    System.out.println();
                    return;
                default:
                    System.out.println("\n✗ 无效选项，请重新选择\n");
                    ConsoleUtils.waitEnter();
            }
        }
    }

    private void printMainMenu() {
        ConsoleUtils.clearScreen();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║               🏥 医院预约系统 - 控制台                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║                                                           ║");
        System.out.println("║     [1]  管理员系统                                        ║");
        System.out.println("║                                                           ║");
        System.out.println("║     [2]  医生系统                                          ║");
        System.out.println("║                                                           ║");
        System.out.println("║     [3]  患者系统                                          ║");
        System.out.println("║                                                           ║");
        System.out.println("║     [0]  退出系统                                          ║");
        System.out.println("║                                                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  提示：请输入选项数字，然后按回车键                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
package com.hospital.console;

import com.hospital.entity.*;
import com.hospital.service.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class PatientConsole {

    private PatientService patientService;
    private HospitalService hospitalService;
    private DepartmentService departmentService;
    private DoctorService doctorService;
    private ScheduleService scheduleService;
    private AppointmentService appointmentService;
    private Scanner scanner;
    private Patient currentPatient;

    public PatientConsole(PatientService patientService, HospitalService hospitalService,
                         DepartmentService departmentService, DoctorService doctorService,
                         ScheduleService scheduleService, AppointmentService appointmentService,
                         Scanner scanner) {
        this.patientService = patientService;
        this.hospitalService = hospitalService;
        this.departmentService = departmentService;
        this.doctorService = doctorService;
        this.scheduleService = scheduleService;
        this.appointmentService = appointmentService;
        this.scanner = scanner;
    }

    public void mainMenu() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("患者系统");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 患者登录                                            ║");
            System.out.println("║  [2] 患者注册                                            ║");
            System.out.println("║  [0] 返回主菜单                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1:
                    login();
                    break;
                case 2:
                    register();
                    break;
                case 0:
                    return;
                default:
                    ConsoleUtils.printError("无效选项");
                    ConsoleUtils.waitEnter();
            }
        }
    }

    private void login() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("患者登录");

        String phone = ConsoleUtils.readString(scanner, "请输入手机号");
        String password = ConsoleUtils.readString(scanner, "请输入密码");

        try {
            Patient patient = patientService.login(phone, password);
            if (patient != null) {
                currentPatient = patient;
                ConsoleUtils.printSuccess("登录成功！欢迎, " + patient.getName());
                ConsoleUtils.waitEnter();
                showPatientMenu();
            } else {
                ConsoleUtils.printError("手机号或密码错误");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("登录失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void register() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("患者注册");

        try {
            Patient patient = new Patient();
            patient.setPatientId(java.util.UUID.randomUUID().toString().replace("-", ""));
            patient.setName(ConsoleUtils.readString(scanner, "请输入姓名"));
            patient.setPhone(ConsoleUtils.readString(scanner, "请输入手机号"));
            patient.setPassword(ConsoleUtils.readString(scanner, "请输入密码"));
            patient.setIdCard(ConsoleUtils.readString(scanner, "请输入身份证号"));
            patient.setGender(ConsoleUtils.readString(scanner, "请输入性别"));
            patient.setAge(Integer.parseInt(ConsoleUtils.readString(scanner, "请输入年龄")));

            Patient saved = patientService.create(patient);
            ConsoleUtils.printSuccess("注册成功！您的患者ID: " + saved.getPatientId());
        } catch (Exception e) {
            ConsoleUtils.printError("注册失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void showPatientMenu() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("患者系统 - " + currentPatient.getName());
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 预约挂号（支持按地区筛选医院）                      ║");
            System.out.println("║  [2] 查看我的预约记录                                    ║");
            System.out.println("║  [3] 取消预约                                            ║");
            System.out.println("║  [0] 返回主菜单                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1:
                    browseHospitals();
                    break;
                case 2:
                    viewMyAppointments();
                    break;
                case 3:
                    cancelAppointment();
                    break;
                case 0:
                    currentPatient = null;
                    return;
                default:
                    ConsoleUtils.printError("无效选项");
                    ConsoleUtils.waitEnter();
            }
        }
    }

    private void browseHospitals() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("医院列表");

        String province = ConsoleUtils.readString(scanner, "请输入省份筛选（回车跳过）");
        String city = ConsoleUtils.readString(scanner, "请输入城市筛选（回车跳过）");

        List<Hospital> hospitals = hospitalService.findActive();

        List<Hospital> filteredHospitals = new java.util.ArrayList<>();
        for (Hospital h : hospitals) {
            boolean match = true;
            if (!province.isEmpty() && !h.getAddress().contains(province)) match = false;
            if (!city.isEmpty() && !h.getAddress().contains(city)) match = false;
            if (match) {
                filteredHospitals.add(h);
            }
        }

        if (filteredHospitals.isEmpty()) {
            System.out.println("\n没有找到符合条件的医院\n");
            ConsoleUtils.waitEnter();
            return;
        }

        Hospital selectedHospital = PaginationUtils.paginateWithSelection(filteredHospitals, scanner, 10, new PaginationUtils.ItemRenderer<Hospital>() {
            @Override
            public void renderHeader() {
                ConsoleUtils.printTitle("医院列表");
                System.out.println("┌──────┬─────────────────────────────┬─────────────────────┐");
                System.out.println("│ 序号 │ 医院名称                    │ 地址                │");
                System.out.println("├──────┼─────────────────────────────┼─────────────────────┤");
            }

            @Override
            public void renderItem(Hospital h, int index) {
                System.out.printf("│ %-4d│ %-25s│ %-19s│\n",
                        index, h.getHospitalName(), truncate(h.getAddress(), 19));
            }

            @Override
            public void renderFooter() {
                System.out.println("└──────┴─────────────────────────────┴─────────────────────┘");
            }
        });

        if (selectedHospital != null) {
            selectDepartment(selectedHospital);
        }
    }

    private void selectDepartment(Hospital hospital) {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle(hospital.getHospitalName() + " - 科室列表");

        List<Department> departments = departmentService.findByHospitalId(hospital.getHospitalId());

        if (departments.isEmpty()) {
            System.out.println("该医院暂无科室信息");
            ConsoleUtils.waitEnter();
            return;
        }

        Department selectedDept = PaginationUtils.paginateWithSelection(departments, scanner, 10, new PaginationUtils.ItemRenderer<Department>() {
            @Override
            public void renderHeader() {
                ConsoleUtils.printTitle(hospital.getHospitalName() + " - 科室列表");
                System.out.println("┌──────┬─────────────────────────────┬─────────────────────┐");
                System.out.println("│ 序号 │ 科室名称                    │ 服务时间            │");
                System.out.println("├──────┼─────────────────────────────┼─────────────────────┤");
            }

            @Override
            public void renderItem(Department d, int index) {
                System.out.printf("│ %-4d│ %-25s│ %-19s│\n",
                        index, d.getDeptName(), d.getServiceTime());
            }

            @Override
            public void renderFooter() {
                System.out.println("└──────┴─────────────────────────────┴─────────────────────┘");
            }
        });

        if (selectedDept != null) {
            selectDoctor(hospital, selectedDept);
        }
    }

    private void selectDoctor(Hospital hospital, Department department) {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle(department.getDeptName() + " - 医生列表");

        List<Doctor> doctors = doctorService.findByDeptIdAndHospitalId(department.getDeptId(), hospital.getHospitalId());

        if (doctors.isEmpty()) {
            System.out.println("该科室暂无医生信息");
            ConsoleUtils.waitEnter();
            return;
        }

        Doctor selectedDoctor = PaginationUtils.paginateWithSelection(doctors, scanner, 10, new PaginationUtils.ItemRenderer<Doctor>() {
            @Override
            public void renderHeader() {
                ConsoleUtils.printTitle(department.getDeptName() + " - 医生列表");
                System.out.println("┌──────┬─────────┬────────────┬─────────────────────┐");
                System.out.println("│ 序号  │ 姓名    │ 职称       │ 专业特长            │");
                System.out.println("├──────┼─────────┼────────────┼─────────────────────┤");
            }

            @Override
            public void renderItem(Doctor d, int index) {
                System.out.printf("│ %-4d│ %-7s│ %-10s│ %-19s│\n",
                        index, d.getName(), d.getTitle(), truncate(d.getExpertise(), 19));
            }

            @Override
            public void renderFooter() {
                System.out.println("└──────┴─────────┴────────────┴─────────────────────┘");
            }
        });

        if (selectedDoctor != null) {
            selectSchedule(hospital, department, selectedDoctor);
        }
    }

    private void selectSchedule(Hospital hospital, Department department, Doctor doctor) {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle(doctor.getName() + "医生 - 可预约时间段");

        List<Schedule> schedules = scheduleService.findAvailableByDoctorId(doctor.getDoctorId());

        if (schedules.isEmpty()) {
            System.out.println("该医生暂无可预约时间段");
            ConsoleUtils.waitEnter();
            return;
        }

        System.out.println("┌──────┬────────────┬──────────────┬─────────────┐");
        System.out.println("│ 序号 │ 日期       │ 时间段       │ 剩余号源    │");
        System.out.println("├──────┼────────────┼──────────────┼─────────────┤");

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int index = 1;
        for (Schedule s : schedules) {
            int remaining = s.getMaxPatients() - s.getBookedCount();
            String formattedSlot = TimeSlotUtils.formatSlot(s.getTimeSlot());
            System.out.printf("│ %-4d│ %-10s│ %-12s│ %-11d│\n",
                    index++, s.getWorkDate().format(df), formattedSlot, remaining);
        }
        System.out.println("└──────┴────────────┴──────────────┴─────────────┘");

        int choice = ConsoleUtils.readInt(scanner, "请输入要预约的时间段序号（0返回）");
        if (choice > 0 && choice <= schedules.size()) {
            confirmAppointment(hospital, department, doctor, schedules.get(choice - 1));
        }
    }

    private void confirmAppointment(Hospital hospital, Department department, Doctor doctor, Schedule schedule) {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("确认预约");

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int remaining = schedule.getMaxPatients() - schedule.getBookedCount();

        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("医院: " + hospital.getHospitalName());
        System.out.println("科室: " + department.getDeptName());
        System.out.println("医生: " + doctor.getName() + " (" + doctor.getTitle() + ")");
        System.out.println("日期: " + schedule.getWorkDate().format(df));
        System.out.println("时段: " + TimeSlotUtils.formatSlot(schedule.getTimeSlot()));
        System.out.println("剩余号源: " + remaining + " / " + schedule.getMaxPatients());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        String confirm = ConsoleUtils.readString(scanner, "确认预约？(y/n)");
        if ("y".equalsIgnoreCase(confirm)) {
            try {
                Appointment appointment = appointmentService.createAppointment(currentPatient.getPatientId(), schedule.getScheduleId());
                ConsoleUtils.printSuccess("预约成功！预约ID: " + appointment.getAppointmentId());
            } catch (Exception e) {
                ConsoleUtils.printError("预约失败：" + e.getMessage());
            }
        } else {
            ConsoleUtils.printInfo("已取消预约");
        }
        ConsoleUtils.waitEnter();
    }

    private void viewMyAppointments() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("我的预约记录");

        List<Appointment> appointments = appointmentService.findByPatientId(currentPatient.getPatientId());

        if (appointments.isEmpty()) {
            System.out.println("暂无预约记录");
            ConsoleUtils.waitEnter();
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) appointments.size() / pageSize);
        int currentPage = 1;

        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("我的预约记录 - 第 " + currentPage + " / " + totalPages + " 页");

            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, appointments.size());

            System.out.println("┌──────┬────────────────┬────────────┬──────────────┬────────────┐");
            System.out.println("│ 序号 │ 医生姓名       │ 日期       │ 时间段       │ 状态       │");
            System.out.println("├──────┼────────────────┼────────────┼──────────────┼────────────┤");

            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            int index = start + 1;
            for (int i = start; i < end; i++) {
                Appointment a = appointments.get(i);
                String doctorName = "未知医生";
                if (a.getDoctorId() != null) {
                    Doctor doctor = doctorService.findById(a.getDoctorId());
                    if (doctor != null) {
                        doctorName = truncate(doctor.getName() + " " + (doctor.getTitle() != null ? doctor.getTitle() : ""), 14);
                    }
                }
                String statusStr = a.getStatus() == 0 ? "已取消" : (a.getStatus() == 1 ? "待就诊" : (a.getStatus() == 2 ? "已完成" : "进行中"));
                System.out.printf("│ %-4d│ %-14s│ %-10s│ %-12s│ %-10s│\n",
                        index++, doctorName,
                        a.getAppointmentDate() != null ? a.getAppointmentDate().format(df) : "",
                        a.getTimeSlot() != null ? truncate(a.getTimeSlot(), 12) : "",
                        statusStr);
            }
            System.out.println("└──────┴────────────────┴────────────┴──────────────┴────────────┘");

            if (totalPages > 1) {
                System.out.println("\n[P] 上一页  [N] 下一页  [0] 返回");
                String choice = ConsoleUtils.readString(scanner, "请输入操作");
                if ("P".equalsIgnoreCase(choice) && currentPage > 1) {
                    currentPage--;
                } else if ("N".equalsIgnoreCase(choice) && currentPage < totalPages) {
                    currentPage++;
                } else if ("0".equals(choice)) {
                    break;
                } else {
                    break;
                }
            } else {
                ConsoleUtils.waitEnter();
                break;
            }
        }
    }

    private void cancelAppointment() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("取消预约");

        List<Appointment> appointments = appointmentService.findByPatientId(currentPatient.getPatientId());

        if (appointments.isEmpty()) {
            System.out.println("暂无预约记录");
            ConsoleUtils.waitEnter();
            return;
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        System.out.println("┌──────┬────────────────┬────────────┬──────────────┬────────────┐");
        System.out.println("│ 序号 │ 医生姓名       │ 日期       │ 时间段       │ 状态       │");
        System.out.println("├──────┼────────────────┼────────────┼──────────────┼────────────┤");

        int index = 1;
        for (Appointment a : appointments) {
            String doctorName = "未知医生";
            if (a.getDoctorId() != null) {
                Doctor doctor = doctorService.findById(a.getDoctorId());
                if (doctor != null) {
                    doctorName = truncate(doctor.getName() + " " + (doctor.getTitle() != null ? doctor.getTitle() : ""), 14);
                }
            }
            String statusStr = a.getStatus() == 0 ? "已取消" : (a.getStatus() == 1 ? "待就诊" : (a.getStatus() == 2 ? "已完成" : "进行中"));
            System.out.printf("│ %-4d│ %-14s│ %-10s│ %-12s│ %-10s│\n",
                    index++, doctorName,
                    a.getAppointmentDate() != null ? a.getAppointmentDate().format(df) : "",
                    a.getTimeSlot() != null ? truncate(a.getTimeSlot(), 12) : "",
                    statusStr);
        }
        System.out.println("└──────┴────────────────┬────────────┴──────────────┴────────────┘");

        int choice = ConsoleUtils.readInt(scanner, "请输入要取消的预约序号（0返回）");
        if (choice > 0 && choice <= appointments.size()) {
            Appointment appointment = appointments.get(choice - 1);
            String confirm = ConsoleUtils.readString(scanner, "确认取消该预约？(y/n)");
            if ("y".equalsIgnoreCase(confirm)) {
                try {
                    appointmentService.cancelAppointment(appointment.getAppointmentId(), currentPatient.getPatientId());
                    ConsoleUtils.printSuccess("取消成功");
                } catch (Exception e) {
                    ConsoleUtils.printError("取消失败：" + e.getMessage());
                }
            }
        }
        ConsoleUtils.waitEnter();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}

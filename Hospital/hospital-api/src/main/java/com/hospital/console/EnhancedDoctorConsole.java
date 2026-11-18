package com.hospital.console;

import com.hospital.entity.*;
import com.hospital.service.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EnhancedDoctorConsole {

    private DoctorService doctorService;
    private DoctorDetailService doctorDetailService;
    private HospitalService hospitalService;
    private DepartmentService departmentService;
    private ScheduleService scheduleService;
    private AppointmentService appointmentService;
    private Scanner scanner;
    private Doctor currentDoctor;
    private DoctorDetail currentDoctorDetail;

    private static final String[] TITLES = {"主任医师", "副主任医师", "主治医师", "住院医师", "实习医师"};

    public EnhancedDoctorConsole(DoctorService doctorService, DoctorDetailService doctorDetailService,
                                HospitalService hospitalService, DepartmentService departmentService,
                                ScheduleService scheduleService, AppointmentService appointmentService,
                                Scanner scanner) {
        this.doctorService = doctorService;
        this.doctorDetailService = doctorDetailService;
        this.hospitalService = hospitalService;
        this.departmentService = departmentService;
        this.scheduleService = scheduleService;
        this.appointmentService = appointmentService;
        this.scanner = scanner;
    }

    public void mainMenu() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("医生系统");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 医生登录                                            ║");
            System.out.println("║  [2] 医生注册                                            ║");
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
        ConsoleUtils.printTitle("医生登录");

        try {
            String username = ConsoleUtils.readString(scanner, "请输入用户名");
            String password = ConsoleUtils.readString(scanner, "请输入密码");

            List<Doctor> doctors = doctorService.findAll();
            Doctor doctor = doctors.stream()
                    .filter(d -> username.equals(d.getUsername()) && password.equals(d.getPassword()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

            if (doctor.getStatus() != 1) {
                throw new RuntimeException("账号已被禁用");
            }

            currentDoctor = doctor;
            currentDoctorDetail = doctorDetailService.findByDoctorId(doctor.getDoctorId());
            ConsoleUtils.printSuccess("登录成功！欢迎，" + doctor.getName() + "医生");
            ConsoleUtils.waitEnter();
            showDoctorMenu();
        } catch (Exception e) {
            ConsoleUtils.printError("登录失败：" + e.getMessage());
            ConsoleUtils.waitEnter();
        }
    }

    private void register() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("医生注册");

        try {
            Doctor doctor = new Doctor();
            DoctorDetail detail = new DoctorDetail();

            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("                    基本信息");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            doctor.setUsername(ConsoleUtils.readString(scanner, "请输入手机号/用户名"));
            doctor.setPassword(ConsoleUtils.readString(scanner, "请输入密码"));
            doctor.setName(ConsoleUtils.readString(scanner, "请输入姓名"));

            System.out.println("\n请选择性别：");
            System.out.println("1. 男");
            System.out.println("2. 女");
            int genderChoice = ConsoleUtils.readInt(scanner, "请输入性别序号");
            doctor.setGender(genderChoice == 1 ? "男" : "女");

            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("                    所属医院");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            List<Hospital> hospitals = hospitalService.findActive();
            if (hospitals.isEmpty()) {
                ConsoleUtils.printError("暂无医院，请联系管理员添加");
                ConsoleUtils.waitEnter();
                return;
            }

            Hospital selectedHospital = PaginationUtils.paginateWithSelection(hospitals, scanner, 10, new PaginationUtils.ItemRenderer<Hospital>() {
                @Override
                public void renderHeader() {
                    System.out.println("\n请选择所属医院：");
                    System.out.println("┌──────┬─────────────────────────────┐");
                    System.out.println("│ 序号 │ 医院名称                    │");
                    System.out.println("├──────┼─────────────────────────────┤");
                }

                @Override
                public void renderItem(Hospital h, int index) {
                    System.out.printf("│ %-4d│ %-25s│\n", index, h.getHospitalName());
                }

                @Override
                public void renderFooter() {
                    System.out.println("└──────┴─────────────────────────────┘");
                }
            });
            if (selectedHospital == null) {
                ConsoleUtils.printError("未选择医院，操作取消");
                ConsoleUtils.waitEnter();
                return;
            }
            doctor.setHospitalId(selectedHospital.getHospitalId());

            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("                    所属科室");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            List<Department> departments = departmentService.findByHospitalId(doctor.getHospitalId());
            System.out.println("\n请选择所属科室：");
            for (int i = 0; i < departments.size(); i++) {
                System.out.printf("%d. %s\n", i + 1, departments.get(i).getDeptName());
            }
            System.out.printf("%d. 其他（输入科室名称）\n", departments.size() + 1);
            int deptIndex = ConsoleUtils.readInt(scanner, "请输入科室序号") - 1;
            if (deptIndex < departments.size()) {
                doctor.setDeptId(departments.get(deptIndex).getDeptId());
            } else {
                String deptName = ConsoleUtils.readString(scanner, "请输入科室名称");
                Department newDept = new Department();
                newDept.setDeptId("DEPT" + System.currentTimeMillis());
                newDept.setHospitalId(doctor.getHospitalId());
                newDept.setDeptName(deptName);
                departmentService.create(newDept);
                doctor.setDeptId(newDept.getDeptId());
            }

            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("                    职称与专长");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("\n请选择职称：");
            for (int i = 0; i < TITLES.length; i++) {
                System.out.printf("%d. %s\n", i + 1, TITLES[i]);
            }
            System.out.printf("%d. 其他（输入职称）\n", TITLES.length + 1);
            int titleChoice = ConsoleUtils.readInt(scanner, "请输入职称序号") - 1;
            if (titleChoice < TITLES.length) {
                doctor.setTitle(TITLES[titleChoice]);
            } else {
                doctor.setTitle(ConsoleUtils.readString(scanner, "请输入职称"));
            }

            detail.setExpertiseDesc(ConsoleUtils.readString(scanner, "请输入专长描述"));
            detail.setConsultationFee(ConsoleUtils.readInt(scanner, "请输入挂号费（元）"));
            detail.setQualification(ConsoleUtils.readString(scanner, "请输入相关材料（如执业证书编号）"));

            doctor.setStatus(1);
            doctor.setPhone(doctor.getUsername());

            Doctor savedDoctor = doctorService.create(doctor);

            detail.setDoctorId(savedDoctor.getDoctorId());
            doctorDetailService.create(detail);

            ConsoleUtils.printSuccess("注册成功！您的医生ID：" + savedDoctor.getDoctorId());
        } catch (Exception e) {
            ConsoleUtils.printError("注册失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void showDoctorMenu() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("医生系统 - " + currentDoctor.getName());
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 今日排班                                            ║");
            System.out.println("║  [2] 我的时间管理                                        ║");
            System.out.println("║  [3] 查看我的预约                                        ║");
            System.out.println("║  [4] 修改个人信息                                        ║");
            System.out.println("║  [5] 注销/停用账号                                       ║");
            System.out.println("║  [0] 返回主菜单                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1:
                    todaySchedule();
                    break;
                case 2:
                    timeManagement();
                    break;
                case 3:
                    viewAppointments();
                    break;
                case 4:
                    updateProfile();
                    break;
                case 5:
                    deactivateAccount();
                    return;
                case 0:
                    currentDoctor = null;
                    currentDoctorDetail = null;
                    return;
                default:
                    ConsoleUtils.printError("无效选项");
                    ConsoleUtils.waitEnter();
            }
        }
    }

    private void todaySchedule() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("今日排班");
            
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            
            System.out.println("\n当前日期：" + today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            System.out.println("\n请选择日期：");
            System.out.println("1. 今天");
            System.out.println("2. 明天");
            System.out.println("0. 返回上一级");

            int dateChoice = ConsoleUtils.readInt(scanner, "请输入选项");
            LocalDate targetDate;
            if (dateChoice == 1) {
                targetDate = today;
            } else if (dateChoice == 2) {
                targetDate = tomorrow;
            } else if (dateChoice == 0) {
                return;
            } else {
                ConsoleUtils.printError("无效选项");
                ConsoleUtils.waitEnter();
                continue;
            }

            List<Schedule> todaySchedules = scheduleService.findByDoctorAndDate(currentDoctor.getDoctorId(), targetDate);
            
            System.out.println("\n" + targetDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) + " 已有排班：");
            if (todaySchedules.isEmpty()) {
                System.out.println("  暂无排班");
            } else {
                System.out.println("┌──────┬──────────────┬──────────┬───────┐");
                System.out.println("│ 序号 │ 时间段       │ 状态     │ 操作  │");
                System.out.println("├──────┼──────────────┼──────────┼───────┤");
                int index = 1;
                for (Schedule s : todaySchedules) {
                    String status = s.getStatus() == 1 ? "可预约" : "已关闭";
                    String formattedSlot = TimeSlotUtils.formatSlot(s.getTimeSlot());
                    System.out.printf("│ %-4d│ %-12s│ %-8s│ [%d]   │\n",
                            index, formattedSlot, status, index);
                    index++;
                }
                System.out.println("└──────┴──────────────┴──────────┴───────┘");
            }

            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("1. 添加时间段");
            System.out.println("2. 删除时间段");
            System.out.println("0. 返回");

            int actionChoice = ConsoleUtils.readInt(scanner, "请输入操作选项");
            switch (actionChoice) {
                case 1:
                    addScheduleForDate(targetDate);
                    break;
                case 2:
                    deleteScheduleForDate(targetDate, todaySchedules);
                    break;
                case 0:
                    break;
                default:
                    ConsoleUtils.printError("无效选项");
                    ConsoleUtils.waitEnter();
            }
        }
    }

    private void addScheduleForDate(LocalDate date) {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("添加排班时间段 - " + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        System.out.println("提示：使用自然时间格式，例如：8am, 9:30pm, 2pm");
        System.out.println("系统会自动将您的大时间段拆分成20分钟一段，每段最多2人预约\n");

        try {
            String startTime = ConsoleUtils.readString(scanner, "请输入开始时间（例如：8am, 9:30pm）");
            if (!TimeSlotUtils.isValidTime(startTime)) {
                ConsoleUtils.printError("开始时间格式错误，请使用自然格式，例如：8am, 9:30pm");
                ConsoleUtils.waitEnter();
                return;
            }
            
            String endTime = ConsoleUtils.readString(scanner, "请输入结束时间（例如：10am, 2pm）");
            if (!TimeSlotUtils.isValidTime(endTime)) {
                ConsoleUtils.printError("结束时间格式错误，请使用自然格式，例如：8am, 9:30pm");
                ConsoleUtils.waitEnter();
                return;
            }
            
            if (!TimeSlotUtils.isEndAfterStart(startTime, endTime)) {
                ConsoleUtils.printError("结束时间必须晚于开始时间");
                ConsoleUtils.waitEnter();
                return;
            }
            
            List<String> slots = TimeSlotUtils.splitTo20MinuteSlots(startTime, endTime);
            
            if (slots.isEmpty()) {
                ConsoleUtils.printError("时间段太短，至少需要20分钟");
                ConsoleUtils.waitEnter();
                return;
            }
            
            System.out.println("\n将为您创建以下时间段（每段最多2人）：");
            for (int i = 0; i < slots.size(); i++) {
                System.out.printf("  %d. %s\n", i + 1, TimeSlotUtils.formatSlot(slots.get(i)));
            }
            
            System.out.println();
            String confirm = ConsoleUtils.readString(scanner, "确认创建？(y/n)");
            if (!confirm.toLowerCase().startsWith("y")) {
                System.out.println("已取消");
                ConsoleUtils.waitEnter();
                return;
            }
            
            int successCount = 0;
            for (String slot : slots) {
                Schedule schedule = new Schedule();
                schedule.setDoctorId(currentDoctor.getDoctorId());
                schedule.setHospitalId(currentDoctor.getHospitalId());
                schedule.setWorkDate(date);
                schedule.setTimeSlot(slot);
                schedule.setMaxPatients(2);
                schedule.setBookedCount(0);
                schedule.setStatus(1);
                
                try {
                    scheduleService.create(schedule);
                    successCount++;
                } catch (Exception e) {
                    // 时间段已存在，跳过
                }
            }
            
            ConsoleUtils.printSuccess("成功创建 " + successCount + " 个时间段！");
        } catch (Exception e) {
            ConsoleUtils.printError("添加失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void deleteScheduleForDate(LocalDate date, List<Schedule> schedules) {
        if (schedules.isEmpty()) {
            ConsoleUtils.printError("该日期暂无排班");
            ConsoleUtils.waitEnter();
            return;
        }

        int index = ConsoleUtils.readInt(scanner, "请输入要删除的序号（0取消）");
        if (index == 0) {
            return;
        }
        if (index < 1 || index > schedules.size()) {
            ConsoleUtils.printError("无效序号");
            ConsoleUtils.waitEnter();
            return;
        }

        Schedule toDelete = schedules.get(index - 1);
        
        List<Appointment> appointments = appointmentService.findByDoctorAndDate(
                currentDoctor.getDoctorId(), date);
        
        boolean hasAppointment = appointments.stream()
                .anyMatch(a -> a.getTimeSlot().equals(toDelete.getTimeSlot()) && a.getStatus() == 1);
        
        if (hasAppointment) {
            ConsoleUtils.printError("该时间段已被预约，请先与患者沟通取消预约后再删除");
        } else {
            scheduleService.delete(toDelete.getScheduleId());
            ConsoleUtils.printSuccess("删除成功！");
        }
        ConsoleUtils.waitEnter();
    }

    private void timeManagement() {
        while (true) {
            ConsoleUtils.clearScreen();
            ConsoleUtils.printTitle("我的时间管理");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  [1] 添加可预约时间段（任意日期）                        ║");
            System.out.println("║  [2] 查看/删除已设置的时间段                            ║");
            System.out.println("║  [3] 标记某天不接诊                                     ║");
            System.out.println("║  [0] 返回上一级                                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");

            int choice = ConsoleUtils.readInt(scanner, "请输入选项");
            switch (choice) {
                case 1:
                    addSchedule();
                    break;
                case 2:
                    viewAndManageSchedules();
                    break;
                case 3:
                    markUnavailable();
                    break;
                case 0:
                    return;
                default:
                    ConsoleUtils.printError("无效选项");
                    ConsoleUtils.waitEnter();
            }
        }
    }

    private void addSchedule() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("添加可预约时间段");
        
        System.out.println("提示：使用自然时间格式，例如：8am, 9:30pm, 2pm");
        System.out.println("系统会自动将您的大时间段拆分成20分钟一段，每段最多2人预约\n");

        try {
            String dateStr = ConsoleUtils.readString(scanner, "请输入日期（格式：yyyy-MM-dd，例如：2026-05-20）");
            
            if (!dateStr.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                ConsoleUtils.printError("日期格式错误，请使用 yyyy-MM-dd 格式");
                ConsoleUtils.waitEnter();
                return;
            }
            
            LocalDate workDate = LocalDate.parse(dateStr);
            
            String startTime = ConsoleUtils.readString(scanner, "请输入开始时间（例如：8am, 9:30pm）");
            if (!TimeSlotUtils.isValidTime(startTime)) {
                ConsoleUtils.printError("开始时间格式错误，请使用自然格式，例如：8am, 9:30pm");
                ConsoleUtils.waitEnter();
                return;
            }
            
            String endTime = ConsoleUtils.readString(scanner, "请输入结束时间（例如：10am, 2pm）");
            if (!TimeSlotUtils.isValidTime(endTime)) {
                ConsoleUtils.printError("结束时间格式错误，请使用自然格式，例如：8am, 9:30pm");
                ConsoleUtils.waitEnter();
                return;
            }
            
            if (!TimeSlotUtils.isEndAfterStart(startTime, endTime)) {
                ConsoleUtils.printError("结束时间必须晚于开始时间");
                ConsoleUtils.waitEnter();
                return;
            }
            
            List<String> slots = TimeSlotUtils.splitTo20MinuteSlots(startTime, endTime);
            
            if (slots.isEmpty()) {
                ConsoleUtils.printError("时间段太短，至少需要20分钟");
                ConsoleUtils.waitEnter();
                return;
            }
            
            System.out.println("\n将为您创建以下时间段（每段最多2人）：");
            for (int i = 0; i < slots.size(); i++) {
                System.out.printf("  %d. %s\n", i + 1, TimeSlotUtils.formatSlot(slots.get(i)));
            }
            
            System.out.println();
            String confirm = ConsoleUtils.readString(scanner, "确认创建？(y/n)");
            if (!confirm.toLowerCase().startsWith("y")) {
                System.out.println("已取消");
                ConsoleUtils.waitEnter();
                return;
            }
            
            int successCount = 0;
            for (String slot : slots) {
                Schedule schedule = new Schedule();
                schedule.setDoctorId(currentDoctor.getDoctorId());
                schedule.setHospitalId(currentDoctor.getHospitalId());
                schedule.setWorkDate(workDate);
                schedule.setTimeSlot(slot);
                schedule.setMaxPatients(2);
                schedule.setBookedCount(0);
                schedule.setStatus(1);
                
                try {
                    scheduleService.create(schedule);
                    successCount++;
                } catch (Exception e) {
                    // 时间段已存在，跳过
                }
            }
            
            ConsoleUtils.printSuccess("成功创建 " + successCount + " 个时间段！");
        } catch (Exception e) {
            ConsoleUtils.printError("添加失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void viewAndManageSchedules() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("我的排班列表");

        List<Schedule> schedules = scheduleService.findByDoctorId(currentDoctor.getDoctorId());

        if (schedules.isEmpty()) {
            System.out.println("暂无排班");
            ConsoleUtils.waitEnter();
            return;
        }

        schedules.sort((a, b) -> {
            int dateCompare = a.getWorkDate().compareTo(b.getWorkDate());
            if (dateCompare != 0) return dateCompare;
            return a.getTimeSlot().compareTo(b.getTimeSlot());
        });

        System.out.println("┌──────┬────────────┬──────────────┬──────────┬─────────┬───────┐");
        System.out.println("│ 序号 │ 日期       │ 时间段       │ 状态     │ 已预约数│ 操作  │");
        System.out.println("├──────┼────────────┼──────────────┼──────────┼─────────┼───────┤");

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int index = 1;
        for (Schedule s : schedules) {
            String status = s.getStatus() == 1 ? "可预约" : "已关闭";
            String formattedSlot = TimeSlotUtils.formatSlot(s.getTimeSlot());
            System.out.printf("│ %-4d│ %-10s│ %-12s│ %-8s│ %-7d│ [%d]   │\n",
                    index, s.getWorkDate().format(df), formattedSlot, status, s.getBookedCount(), index);
            index++;
        }
        System.out.println("└──────┴────────────┴──────────────┴──────────┴─────────┴───────┘");

        int choice = ConsoleUtils.readInt(scanner, "请输入要删除的序号（0返回）");
        if (choice > 0 && choice <= schedules.size()) {
            Schedule toDelete = schedules.get(choice - 1);
            
            List<Appointment> appointments = appointmentService.findByDoctorAndDate(
                    currentDoctor.getDoctorId(), toDelete.getWorkDate());
            
            boolean hasActiveAppointment = appointments.stream()
                    .anyMatch(a -> a.getTimeSlot().equals(toDelete.getTimeSlot()) && a.getStatus() == 1);
            
            if (hasActiveAppointment) {
                ConsoleUtils.printError("该时间段已被预约，请先与患者沟通取消预约后再删除");
            } else {
                scheduleService.delete(toDelete.getScheduleId());
                ConsoleUtils.printSuccess("删除成功！");
            }
        }
        ConsoleUtils.waitEnter();
    }

    private void markUnavailable() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("标记不接诊日期");

        try {
            String dateStr = ConsoleUtils.readString(scanner, "请输入不接诊日期（格式：yyyy-MM-dd）");
            LocalDate date = LocalDate.parse(dateStr);

            List<Schedule> schedules = scheduleService.findByDoctorId(currentDoctor.getDoctorId());
            boolean found = false;
            int count = 0;
            for (Schedule s : schedules) {
                if (s.getWorkDate().equals(date)) {
                    s.setStatus(0);
                    scheduleService.update(s.getScheduleId(), s);
                    found = true;
                    count++;
                }
            }

            if (found) {
                ConsoleUtils.printSuccess("已标记 " + count + " 个时间段为不接诊");
            } else {
                ConsoleUtils.printSuccess("该日期没有排班，无需标记");
            }
        } catch (Exception e) {
            ConsoleUtils.printError("操作失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void viewAppointments() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("我的预约列表");

        List<Appointment> appointments = appointmentService.findByDoctorId(currentDoctor.getDoctorId());

        if (appointments.isEmpty()) {
            System.out.println("暂无患者预约");
            ConsoleUtils.waitEnter();
            return;
        }

        appointments.sort((a, b) -> a.getAppointmentDate().compareTo(b.getAppointmentDate()));

        System.out.println("┌──────┬────────────┬──────────────┬────────┬──────────┐");
        System.out.println("│ 序号 │ 日期       │ 时间段       │ 患者ID │ 状态     │");
        System.out.println("├──────┼────────────┼──────────────┼────────┼──────────┤");

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int index = 1;
        for (Appointment a : appointments) {
            String status = switch (a.getStatus()) {
                case 1 -> "待就诊";
                case 2 -> "已完成";
                case 3 -> "就诊中";
                default -> "已取消";
            };
            String formattedSlot = TimeSlotUtils.formatSlot(a.getTimeSlot());
            System.out.printf("│ %-4d│ %-10s│ %-12s│ %-8s│ %-8s│\n",
                    index++, a.getAppointmentDate().format(df), formattedSlot, a.getPatientId(), status);
        }
        System.out.println("└──────┴────────────┴──────────────┴────────┴──────────┘");
        ConsoleUtils.waitEnter();
    }

    private void updateProfile() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("修改个人信息");

        try {
            System.out.println("当前信息：");
            System.out.println("姓名：" + currentDoctor.getName());
            System.out.println("性别：" + currentDoctor.getGender());
            System.out.println("电话：" + currentDoctor.getPhone());
            System.out.println("职称：" + currentDoctor.getTitle());
            System.out.println("专业特长：" + currentDoctor.getExpertise());
            
            if (currentDoctorDetail != null) {
                System.out.println("专长描述：" + currentDoctorDetail.getExpertiseDesc());
                System.out.println("挂号费：" + currentDoctorDetail.getConsultationFee() + "元");
                System.out.println("执业证书：" + currentDoctorDetail.getQualification());
            }

            Doctor update = new Doctor();
            update.setName(ConsoleUtils.readString(scanner, "请输入新姓名（回车跳过）", currentDoctor.getName()));
            update.setGender(ConsoleUtils.readString(scanner, "请输入新性别（回车跳过）", currentDoctor.getGender()));
            update.setPhone(ConsoleUtils.readString(scanner, "请输入新电话（回车跳过）", currentDoctor.getPhone()));
            update.setTitle(ConsoleUtils.readString(scanner, "请输入新职称（回车跳过）", currentDoctor.getTitle()));
            update.setExpertise(ConsoleUtils.readString(scanner, "请输入新专业特长（回车跳过）", currentDoctor.getExpertise()));
            update.setStatus(currentDoctor.getStatus());
            update.setConsultationFee(currentDoctor.getConsultationFee());
            update.setHospitalId(currentDoctor.getHospitalId());
            update.setDeptId(currentDoctor.getDeptId());

            doctorService.update(currentDoctor.getDoctorId(), update);
            currentDoctor = doctorService.findById(currentDoctor.getDoctorId());

            if (currentDoctorDetail != null) {
                DoctorDetail detailUpdate = new DoctorDetail();
                String newDesc = ConsoleUtils.readString(scanner, "请输入新专长描述（回车跳过）", currentDoctorDetail.getExpertiseDesc());
                if (newDesc != null && !newDesc.isEmpty()) {
                    detailUpdate.setExpertiseDesc(newDesc);
                }
                
                String feeStr = ConsoleUtils.readString(scanner, "请输入新挂号费（回车跳过）", 
                        currentDoctorDetail.getConsultationFee() != null ? 
                                currentDoctorDetail.getConsultationFee().toString() : "");
                if (!feeStr.isEmpty()) {
                    detailUpdate.setConsultationFee(Integer.parseInt(feeStr));
                }
                
                String qual = ConsoleUtils.readString(scanner, "请输入新执业证书（回车跳过）", currentDoctorDetail.getQualification());
                if (qual != null && !qual.isEmpty()) {
                    detailUpdate.setQualification(qual);
                }
                
                doctorDetailService.update(currentDoctor.getDoctorId(), detailUpdate);
                currentDoctorDetail = doctorDetailService.findByDoctorId(currentDoctor.getDoctorId());
            }

            ConsoleUtils.printSuccess("信息修改成功！");
        } catch (Exception e) {
            ConsoleUtils.printError("修改失败：" + e.getMessage());
        }
        ConsoleUtils.waitEnter();
    }

    private void deactivateAccount() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printTitle("注销账号");

        if (ConsoleUtils.confirm(scanner, "确定要注销您的账号吗？此操作不可恢复")) {
            try {
                currentDoctor.setStatus(0);
                doctorService.update(currentDoctor.getDoctorId(), currentDoctor);
                ConsoleUtils.printSuccess("账号已注销");
                currentDoctor = null;
                currentDoctorDetail = null;
            } catch (Exception e) {
                ConsoleUtils.printError("注销失败：" + e.getMessage());
            }
        } else {
            ConsoleUtils.printSuccess("取消注销");
        }
        ConsoleUtils.waitEnter();
    }
}
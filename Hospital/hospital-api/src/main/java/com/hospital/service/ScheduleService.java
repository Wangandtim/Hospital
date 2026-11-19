package com.hospital.service;

import com.hospital.entity.Department;
import com.hospital.entity.Doctor;
import com.hospital.entity.Hospital;
import com.hospital.entity.Schedule;
import com.hospital.repository.DepartmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.HospitalRepository;
import com.hospital.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private com.hospital.config.ScheduleRules scheduleRules;

    public List<Schedule> findByHospitalId(String hospitalId) {
        return scheduleRepository.findByHospitalId(hospitalId);
    }

    public List<Schedule> findByHospitalAndDate(String hospitalId, LocalDate workDate) {
        return scheduleRepository.findByHospitalIdAndWorkDate(hospitalId, workDate);
    }

    public List<Schedule> findByDoctorId(String doctorId) {
        return scheduleRepository.findByDoctorId(doctorId);
    }

    public List<Schedule> findByDoctorAndDate(String doctorId, LocalDate workDate) {
        return scheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
    }

    public List<com.hospital.dto.ScheduleDTO> findByDoctorAndDateDTO(String doctorId, LocalDate workDate) {
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
        return schedules.stream()
                .map(s -> new com.hospital.dto.ScheduleDTO(
                        s.getScheduleId(),
                        s.getHospitalId(),
                        s.getDoctorId(),
                        s.getWorkDate(),
                        s.getTimeSlot(),
                        s.getMaxPatients(),
                        s.getBookedCount(),
                        s.getStatus()
                ))
                .toList();
    }

    public List<Schedule> findByHospitalAndDoctorAndDate(String hospitalId, String doctorId, LocalDate workDate) {
        return scheduleRepository.findByHospitalIdAndDoctorIdAndWorkDate(hospitalId, doctorId, workDate);
    }

    /** 某医院某天的排班明细：医生姓名/职称、科室、时段、剩余号一次返回。 */
    public List<com.hospital.dto.ScheduleDetailDTO> findByHospitalAndDateDetail(String hospitalId, LocalDate workDate) {
        return toDetailDTOs(scheduleRepository.findByHospitalIdAndWorkDate(hospitalId, workDate));
    }

    /** 某医院指定区间内有排班的日期（供日历标记，不加载明细）。 */
    public List<LocalDate> findScheduleDates(String hospitalId, LocalDate from, LocalDate to) {
        return scheduleRepository.findWorkDatesByHospitalId(hospitalId, from, to);
    }

    /** 医生指定日期区间的排班明细（医生详情页未来 7 天）。 */
    public List<com.hospital.dto.ScheduleDetailDTO> findByDoctorAndDateRangeDetail(String doctorId,
                                                                                   LocalDate from, LocalDate to) {
        return toDetailDTOs(scheduleRepository.findByDoctorIdAndWorkDateBetween(doctorId, from, to));
    }

    /** 某医院某医生某天的排班明细。 */
    public List<com.hospital.dto.ScheduleDetailDTO> findByHospitalAndDoctorAndDateDetail(
            String hospitalId, String doctorId, LocalDate workDate) {
        return toDetailDTOs(scheduleRepository.findByHospitalIdAndDoctorIdAndWorkDate(hospitalId, doctorId, workDate));
    }

    /** 某医生某天的排班明细（小程序按医生查时段使用）。 */
    public List<com.hospital.dto.ScheduleDetailDTO> findByDoctorAndDateDetail(String doctorId, LocalDate workDate) {
        return toDetailDTOs(scheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate));
    }

    /**
     * 批量补全医生/科室/医院信息后转换为 DTO。
     * 医生、科室、医院各只查一次，避免按行查询造成 N+1（888 行排班只产生 3 次附加查询）。
     */
    private List<com.hospital.dto.ScheduleDetailDTO> toDetailDTOs(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> doctorIds = new HashSet<>();
        Set<String> hospitalIds = new HashSet<>();
        for (Schedule schedule : schedules) {
            if (schedule.getDoctorId() != null) {
                doctorIds.add(schedule.getDoctorId());
            }
            if (schedule.getHospitalId() != null) {
                hospitalIds.add(schedule.getHospitalId());
            }
        }

        Map<String, Doctor> doctorMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            for (Doctor doctor : doctorRepository.findAllById(doctorIds)) {
                doctorMap.put(doctor.getDoctorId(), doctor);
            }
        }

        Set<String> deptIds = new HashSet<>();
        for (Doctor doctor : doctorMap.values()) {
            if (doctor.getDeptId() != null && !doctor.getDeptId().isEmpty()) {
                deptIds.add(doctor.getDeptId());
            }
        }
        Map<String, Department> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (Department department : departmentRepository.findAllById(deptIds)) {
                deptMap.put(department.getDeptId(), department);
            }
        }

        Map<String, Hospital> hospitalMap = new HashMap<>();
        if (!hospitalIds.isEmpty()) {
            for (Hospital hospital : hospitalRepository.findAllById(hospitalIds)) {
                hospitalMap.put(hospital.getHospitalId(), hospital);
            }
        }

        List<com.hospital.dto.ScheduleDetailDTO> result = new ArrayList<>(schedules.size());
        for (Schedule schedule : schedules) {
            com.hospital.dto.ScheduleDetailDTO dto = new com.hospital.dto.ScheduleDetailDTO();
            dto.setScheduleId(schedule.getScheduleId());
            dto.setHospitalId(schedule.getHospitalId());
            dto.setDoctorId(schedule.getDoctorId());
            dto.setWorkDate(schedule.getWorkDate());
            dto.setTimeSlot(schedule.getTimeSlot());
            dto.setMaxPatients(schedule.getMaxPatients());
            dto.setBookedCount(schedule.getBookedCount());
            dto.setStatus(schedule.getStatus());

            int max = schedule.getMaxPatients() == null ? 0 : schedule.getMaxPatients();
            int booked = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
            dto.setRemainingSlots(Math.max(max - booked, 0));
            dto.setAvailable(schedule.getStatus() != null && schedule.getStatus() == 1 && booked < max);

            Hospital hospital = schedule.getHospitalId() == null ? null : hospitalMap.get(schedule.getHospitalId());
            dto.setHospitalName(hospital == null ? null : hospital.getHospitalName());

            Doctor doctor = schedule.getDoctorId() == null ? null : doctorMap.get(schedule.getDoctorId());
            if (doctor != null) {
                dto.setDoctorName(doctor.getName());
                dto.setDoctorTitle(doctor.getTitle());
                dto.setDeptId(doctor.getDeptId());
                Department department = doctor.getDeptId() == null ? null : deptMap.get(doctor.getDeptId());
                dto.setDeptName(department == null ? null : department.getName());
            }

            // 急诊科不参与预约：号源列表不返回急诊科排班
            if (scheduleRules.isEmergencyDeptName(dto.getDeptName())) {
                continue;
            }

            result.add(dto);
        }

        result.sort(Comparator
                .comparing(com.hospital.dto.ScheduleDetailDTO::getTimeSlot,
                        Comparator.nullsLast(String::compareTo))
                .thenComparing(com.hospital.dto.ScheduleDetailDTO::getDoctorId,
                        Comparator.nullsLast(String::compareTo)));
        return result;
    }

    public Schedule findById(String scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("排班记录不存在"));
    }

    @Transactional
    public Schedule createSchedule(String hospitalId, String doctorId, LocalDate workDate,
                                   String timeSlot, Integer maxPatients) {
        return createSchedule(hospitalId, doctorId, workDate, timeSlot, maxPatients, null);
    }

    @Transactional
    public Schedule createSchedule(String hospitalId, String doctorId, LocalDate workDate,
                                   String timeSlot, Integer maxPatients, Integer status) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("医生不存在"));

        var existing = scheduleRepository.findByHospitalIdAndDoctorIdAndWorkDateAndTimeSlot(
                hospitalId, doctorId, workDate, timeSlot);
        if (existing.isPresent()) {
            throw new RuntimeException("该时段排班已存在");
        }

        // 同一医生同一天内的时段不允许重叠（如 09:00-10:00 与 09:30-10:30）
        List<Schedule> sameDay = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
        for (Schedule schedule : sameDay) {
            if (schedule.getHospitalId() != null && !schedule.getHospitalId().equals(hospitalId)) {
                continue;
            }
            if (timeSlotsOverlap(timeSlot, schedule.getTimeSlot())) {
                throw new RuntimeException("该时段与已有排班重叠");
            }
        }

        Schedule schedule = new Schedule();
        schedule.setScheduleId(UUID.randomUUID().toString().replace("-", ""));
        schedule.setHospitalId(hospitalId);
        schedule.setDoctorId(doctorId);
        schedule.setWorkDate(workDate);
        schedule.setTimeSlot(timeSlot);
        schedule.setMaxPatients(maxPatients != null ? maxPatients : 2);
        schedule.setBookedCount(0);
        schedule.setStatus(status != null ? status : 1);

        return scheduleRepository.save(schedule);
    }

    /** 判断两个 "HH:mm-HH:mm" 时段是否有交集。 */
    private boolean timeSlotsOverlap(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        try {
            String[] firstParts = first.split("-");
            String[] secondParts = second.split("-");
            if (firstParts.length != 2 || secondParts.length != 2) {
                return false;
            }
            LocalTime firstStart = LocalTime.parse(firstParts[0].trim());
            LocalTime firstEnd = LocalTime.parse(firstParts[1].trim());
            LocalTime secondStart = LocalTime.parse(secondParts[0].trim());
            LocalTime secondEnd = LocalTime.parse(secondParts[1].trim());
            return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Schedule updateSchedule(String scheduleId, LocalDate workDate, String timeSlot, Integer maxPatients, Integer status) {
        Schedule schedule = findById(scheduleId);

        if (workDate != null) {
            schedule.setWorkDate(workDate);
        }
        if (timeSlot != null) {
            schedule.setTimeSlot(timeSlot);
        }
        if (maxPatients != null) {
            schedule.setMaxPatients(maxPatients);
        }
        if (status != null) {
            schedule.setStatus(status);
        }

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(String scheduleId) {
        Schedule schedule = findById(scheduleId);
        if (schedule.getBookedCount() > 0) {
            throw new RuntimeException("该排班已有预约，无法删除");
        }
        schedule.setIsDeleted(1);
        schedule.setDeleteTime(java.time.LocalDateTime.now());
        schedule.setDeletedBy("ADMIN");
        scheduleRepository.save(schedule);
    }

    public List<Schedule> findDeletedByHospitalId(String hospitalId) {
        return scheduleRepository.findDeletedByHospitalId(hospitalId);
    }

    public Schedule findByIdIncludingDeleted(String scheduleId) {
        return scheduleRepository.findIncludingDeleted(scheduleId)
                .orElseThrow(() -> new RuntimeException("排班不存在"));
    }

    public void restoreSchedule(String scheduleId) {
        if (scheduleRepository.restoreDeleted(scheduleId) == 0) {
            throw new RuntimeException("排班不存在或未被删除");
        }
    }

    @Transactional
    public void generateWeeklySchedule(String hospitalId, String doctorId, LocalDate startDate,
                                       List<String> timeSlots, Integer maxPatients) {
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            for (String slot : timeSlots) {
                try {
                    createSchedule(hospitalId, doctorId, date, slot, maxPatients);
                } catch (RuntimeException e) {
                    // 已存在的跳过
                }
            }
        }
    }

    public List<Schedule> findAvailableSchedules(String doctorId, LocalDate date) {
        // 急诊科不参与预约，不返回可预约号源
        if (isEmergencyDoctor(doctorId)) {
            return new ArrayList<>();
        }
        List<Schedule> list = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, date);
        return list.stream()
                .filter(Schedule::hasAvailableSlot)
                .toList();
    }

    public boolean checkAppointmentConflict(String doctorId, LocalDate date, String timeSlot, String excludeScheduleId) {
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, date);
        for (Schedule s : schedules) {
            if (s.getTimeSlot().equals(timeSlot) && !s.getScheduleId().equals(excludeScheduleId)) {
                if (!s.hasAvailableSlot()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查询医生的可用排班（控制台使用）- 仅返回20分钟标准时段
     */
    public List<Schedule> findAvailableByDoctorId(String doctorId) {
        List<Schedule> all = scheduleRepository.findByDoctorId(doctorId);
        return all.stream()
                .filter(s -> s.getStatus() == 1 && s.getWorkDate().isAfter(LocalDate.now().minusDays(1))
                        && isStandardTimeSlot(s.getTimeSlot()))
                .sorted(Comparator.comparing(Schedule::getWorkDate)
                        .thenComparing(Schedule::getTimeSlot))
                .collect(Collectors.toList());
    }

    /**
     * 创建排班（控制台使用）
     */
    public Schedule create(Schedule schedule) {
        if (schedule.getScheduleId() == null) {
            schedule.setScheduleId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (schedule.getBookedCount() == null) {
            schedule.setBookedCount(0);
        }
        if (schedule.getStatus() == null) {
            schedule.setStatus(1);
        }
        return scheduleRepository.save(schedule);
    }

    /**
     * 更新排班（控制台使用）
     */
    public Schedule update(String scheduleId, Schedule schedule) {
        Schedule existing = findById(scheduleId);
        if (schedule.getWorkDate() != null) {
            existing.setWorkDate(schedule.getWorkDate());
        }
        if (schedule.getTimeSlot() != null) {
            existing.setTimeSlot(schedule.getTimeSlot());
        }
        if (schedule.getMaxPatients() != null) {
            existing.setMaxPatients(schedule.getMaxPatients());
        }
        if (schedule.getStatus() != null) {
            existing.setStatus(schedule.getStatus());
        }
        return scheduleRepository.save(existing);
    }

    /**
     * 删除排班（控制台使用）：软删除，可在回收站恢复
     */
    @Transactional
    public void delete(String scheduleId) {
        Schedule schedule = findById(scheduleId);
        schedule.setIsDeleted(1);
        schedule.setDeleteTime(java.time.LocalDateTime.now());
        schedule.setDeletedBy("ADMIN");
        scheduleRepository.save(schedule);
    }

    /**
     * 查询医生有排班的日期列表
     */
    public List<String> findScheduleDatesByDoctor(String doctorId) {
        List<Schedule> schedules = scheduleRepository.findByDoctorId(doctorId);
        return schedules.stream()
                .filter(s -> s.getWorkDate().isAfter(LocalDate.now().minusDays(1)))
                .map(s -> s.getWorkDate().toString())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * 将今天的排班自动复制到明天。
     * 只续排正常工作时段内的号源，且跳过急诊科医生（急诊不参与预约）。
     */
    @Transactional
    public int copyScheduleToNextDay() {
        LocalDate today = LocalDate.now();
        return copySchedules(scheduleRepository.findByWorkDate(today), today.plusDays(1));
    }

    /**
     * 手动触发复制排班到指定日期（API调用）。
     * 与自动续排使用同一套工作时间与急诊科过滤规则。
     */
    @Transactional
    public int copyScheduleToDate(LocalDate sourceDate, LocalDate targetDate) {
        return copySchedules(scheduleRepository.findByWorkDate(sourceDate), targetDate);
    }

    private int copySchedules(List<Schedule> sourceSchedules, LocalDate targetDate) {
        if (sourceSchedules == null || sourceSchedules.isEmpty()) {
            return 0;
        }
        Set<String> emergencyDoctors = emergencyDoctorIds(sourceSchedules);
        Set<String> existingKeys = new HashSet<>();
        for (Schedule existing : scheduleRepository.findByWorkDate(targetDate)) {
            existingKeys.add(scheduleKey(existing.getHospitalId(), existing.getDoctorId(), existing.getTimeSlot()));
        }

        List<Schedule> toSave = new ArrayList<>();
        for (Schedule schedule : sourceSchedules) {
            if (!scheduleRules.isWithinWorkHours(schedule.getTimeSlot())) {
                continue;
            }
            if (emergencyDoctors.contains(schedule.getDoctorId())) {
                continue;
            }
            String key = scheduleKey(schedule.getHospitalId(), schedule.getDoctorId(), schedule.getTimeSlot());
            if (existingKeys.contains(key)) {
                continue;
            }
            Schedule newSchedule = new Schedule();
            newSchedule.setScheduleId(UUID.randomUUID().toString().replace("-", ""));
            newSchedule.setHospitalId(schedule.getHospitalId());
            newSchedule.setDoctorId(schedule.getDoctorId());
            newSchedule.setWorkDate(targetDate);
            newSchedule.setTimeSlot(schedule.getTimeSlot());
            newSchedule.setMaxPatients(schedule.getMaxPatients());
            newSchedule.setBookedCount(0);
            newSchedule.setStatus(1);
            toSave.add(newSchedule);
            existingKeys.add(key);
        }
        scheduleRepository.saveAll(toSave);
        return toSave.size();
    }

    private String scheduleKey(String hospitalId, String doctorId, String timeSlot) {
        return (hospitalId == null ? "" : hospitalId) + "|"
                + (doctorId == null ? "" : doctorId) + "|"
                + (timeSlot == null ? "" : timeSlot);
    }

    /** 一次批量查出候选排班中属于急诊科的医生，避免逐条查科室。 */
    private Set<String> emergencyDoctorIds(List<Schedule> schedules) {
        Set<String> doctorIds = new HashSet<>();
        for (Schedule schedule : schedules) {
            if (schedule.getDoctorId() != null) {
                doctorIds.add(schedule.getDoctorId());
            }
        }
        if (doctorIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> deptIds = new HashSet<>();
        Map<String, Doctor> doctors = new HashMap<>();
        for (Doctor doctor : doctorRepository.findAllById(doctorIds)) {
            doctors.put(doctor.getDoctorId(), doctor);
            if (doctor.getDeptId() != null) {
                deptIds.add(doctor.getDeptId());
            }
        }
        Map<String, Department> depts = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (Department department : departmentRepository.findAllById(deptIds)) {
                depts.put(department.getDeptId(), department);
            }
        }
        Set<String> emergencyDoctors = new HashSet<>();
        for (Doctor doctor : doctors.values()) {
            Department department = doctor.getDeptId() == null ? null : depts.get(doctor.getDeptId());
            if (department != null && scheduleRules.isEmergencyDeptName(department.getName())) {
                emergencyDoctors.add(doctor.getDoctorId());
            }
        }
        return emergencyDoctors;
    }

    /** 所有科室 id → 名称，供批量判断急诊科使用。 */
    private Map<String, String> departmentNames() {
        Map<String, String> names = new HashMap<>();
        for (Department department : departmentRepository.findAll()) {
            names.put(department.getDeptId(), department.getName());
        }
        return names;
    }

    private String deptName(String deptId) {
        if (deptId == null) {
            return null;
        }
        return departmentRepository.findById(deptId)
                .map(Department::getName)
                .orElse(null);
    }

    /**
     * 为所有医生生成保底排班
     * 每个医生每天根据职称安排合理的排班时段：
     * - 主任医师：每天6-8个时段
     * - 副主任医师：每天8-10个时段
     * - 主治医师：每天10-12个时段
     * - 住院医师：每天12-14个时段
     * @param days 生成未来几天的排班
     * @return 生成的排班总数
     */
    @Transactional
    public int generateBaseScheduleForAllDoctors(int days) {
        List<Doctor> allDoctors = doctorRepository.findAll();
        int totalGenerated = 0;

        LocalDate today = LocalDate.now();

        // 只生成正常工作时段内的号源（默认 08:00-12:00、14:00-17:30）
        List<String> allTimeSlots = scheduleRules.timeSlots();
        Map<String, String> deptNames = departmentNames();

        Random random = new Random();

        for (Doctor doctor : allDoctors) {
            // 急诊科不参与预约，不生成可预约号源
            if (scheduleRules.isEmergencyDeptName(deptNames.get(doctor.getDeptId()))) {
                continue;
            }
            // 根据职称确定排班时段数量
            int slotCount = getSlotCountByTitle(doctor.getTitle());
            
            for (int i = 0; i < days; i++) {
                LocalDate workDate = today.plusDays(i);
                
                // 随机选择指定数量的时段
                List<String> selectedSlots = selectRandomSlots(allTimeSlots, slotCount, random);
                
                for (String timeSlot : selectedSlots) {
                    var existing = scheduleRepository.findByHospitalIdAndDoctorIdAndWorkDateAndTimeSlot(
                            doctor.getHospitalId(), doctor.getDoctorId(), workDate, timeSlot);
                    
                    if (existing.isEmpty()) {
                        Schedule schedule = new Schedule();
                        schedule.setScheduleId(UUID.randomUUID().toString().replace("-", ""));
                        schedule.setHospitalId(doctor.getHospitalId());
                        schedule.setDoctorId(doctor.getDoctorId());
                        schedule.setWorkDate(workDate);
                        schedule.setTimeSlot(timeSlot);
                        schedule.setMaxPatients(scheduleRules.getMaxPatientsPerSlot());
                        schedule.setBookedCount(0);
                        schedule.setStatus(1);
                        
                        scheduleRepository.save(schedule);
                        totalGenerated++;
                    }
                }
            }
        }
        
        return totalGenerated;
    }
    
    /**
     * 根据职称确定排班时段数量
     */
    private int getSlotCountByTitle(String title) {
        if (title == null) return 10;
        
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("主任")) {
            return 7; // 主任医师：每天7个时段
        } else if (lowerTitle.contains("副主任")) {
            return 9; // 副主任医师：每天9个时段
        } else if (lowerTitle.contains("主治")) {
            return 11; // 主治医师：每天11个时段
        } else {
            return 13; // 住院医师等：每天13个时段
        }
    }
    
    /**
     * 随机选择指定数量的时段，确保时段分布均匀
     */
    private List<String> selectRandomSlots(List<String> allSlots, int count, Random random) {
        List<String> slots = new ArrayList<>(allSlots);
        List<String> selected = new ArrayList<>();
        
        // 上午/下午以中午 12:00 为界，按实际时段字符串切分，不写死段数
        int morningBoundary = 0;
        while (morningBoundary < slots.size()
                && slotStart(slots.get(morningBoundary)).isBefore(LocalTime.NOON)) {
            morningBoundary++;
        }
        List<String> morningSlots = new ArrayList<>(slots.subList(0, morningBoundary));
        List<String> afternoonSlots = new ArrayList<>(slots.subList(morningBoundary, slots.size()));
        
        int morningCount = count / 2;
        int afternoonCount = count - morningCount;
        
        // 随机选择上午时段
        for (int i = 0; i < morningCount && !morningSlots.isEmpty(); i++) {
            int index = random.nextInt(morningSlots.size());
            selected.add(morningSlots.remove(index));
        }
        
        // 随机选择下午时段
        for (int i = 0; i < afternoonCount && !afternoonSlots.isEmpty(); i++) {
            int index = random.nextInt(afternoonSlots.size());
            selected.add(afternoonSlots.remove(index));
        }
        
        // 如果还有剩余名额，从全天中随机选择
        while (selected.size() < count && !slots.isEmpty()) {
            int index = random.nextInt(slots.size());
            String slot = slots.remove(index);
            if (!selected.contains(slot)) {
                selected.add(slot);
            }
        }
        
        Collections.sort(selected);
        return selected;
    }

    /** 解析 "HH:mm-HH:mm" 的开始时间，解析失败时排到最后。 */
    private LocalTime slotStart(String timeSlot) {
        try {
            return LocalTime.parse(timeSlot.split("-")[0].trim());
        } catch (Exception e) {
            return LocalTime.MAX;
        }
    }

    /**
     * 为指定医生生成保底排班
     * @param doctorId 医生ID
     * @param days 生成未来几天的排班
     * @return 生成的排班数
     */
    @Transactional
    public int generateBaseScheduleForDoctor(String doctorId, int days) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("医生不存在"));
        if (scheduleRules.isEmergencyDeptName(deptName(doctor.getDeptId()))) {
            return 0;
        }

        int generated = 0;
        LocalDate today = LocalDate.now();
        List<String> baseTimeSlots = scheduleRules.timeSlots();
        
        for (int i = 0; i < days; i++) {
            LocalDate workDate = today.plusDays(i);
            
            for (String timeSlot : baseTimeSlots) {
                var existing = scheduleRepository.findByHospitalIdAndDoctorIdAndWorkDateAndTimeSlot(
                        doctor.getHospitalId(), doctorId, workDate, timeSlot);
                
                if (existing.isEmpty()) {
                    Schedule schedule = new Schedule();
                    schedule.setScheduleId(UUID.randomUUID().toString().replace("-", ""));
                    schedule.setHospitalId(doctor.getHospitalId());
                    schedule.setDoctorId(doctorId);
                    schedule.setWorkDate(workDate);
                    schedule.setTimeSlot(timeSlot);
                    schedule.setMaxPatients(scheduleRules.getMaxPatientsPerSlot());
                    schedule.setBookedCount(0);
                    schedule.setStatus(1);
                    
                    scheduleRepository.save(schedule);
                    generated++;
                }
            }
        }
        
        return generated;
    }

    /**
     * 分页查询医生的排班（按时间排序）
     * @param doctorId 医生ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public Map<String, Object> findByDoctorIdWithPage(String doctorId, int page, int size) {
        List<Schedule> all = scheduleRepository.findByDoctorIdOrderByWorkDateAscTimeSlotAsc(doctorId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", all.size());
        result.put("totalPages", (int) Math.ceil((double) all.size() / size));
        result.put("currentPage", page);
        
        int start = page * size;
        int end = Math.min(start + size, all.size());
        
        if (start < all.size()) {
            result.put("data", all.subList(start, end));
        } else {
            result.put("data", new ArrayList<>());
        }
        
        return result;
    }

    /**
     * 过滤出标准20分钟时段的排班
     * 20分钟时段格式示例: 08:00-08:20, 08:20-08:40, 09:00-09:20 等
     */
    private boolean isStandardTimeSlot(String timeSlot) {
        if (timeSlot == null || !timeSlot.contains("-")) {
            return false;
        }
        
        try {
            String[] parts = timeSlot.split("-");
            if (parts.length != 2) {
                return false;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            LocalTime start = LocalTime.parse(parts[0].trim(), formatter);
            LocalTime end = LocalTime.parse(parts[1].trim(), formatter);
            
            // 检查是否为20分钟间隔，且必须落在正常工作时段（排除历史遗留的全天时段）
            long minutesBetween = java.time.Duration.between(start, end).toMinutes();
            return minutesBetween == 20 && scheduleRules.isWithinWorkHours(timeSlot);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 扣减号源（带事务）
     * @param scheduleId 排班ID
     * @return 更新的记录数，0表示扣减失败
     */
    @Transactional
    public int decrementAvailableSlot(String scheduleId) {
        return scheduleRepository.incrementBookedCount(scheduleId);
    }

    /**
     * 查询医生的可用排班（仅返回20分钟标准时段）
     */
    public List<Schedule> findAvailableSchedulesStandard(String doctorId, LocalDate date) {
        // 急诊科不参与预约，不返回可预约号源
        if (isEmergencyDoctor(doctorId)) {
            return new ArrayList<>();
        }
        List<Schedule> list = scheduleRepository.findByDoctorIdAndWorkDate(doctorId, date);
        return list.stream()
                .filter(s -> s.hasAvailableSlot() && isStandardTimeSlot(s.getTimeSlot()))
                .sorted(Comparator.comparing(Schedule::getTimeSlot))
                .collect(Collectors.toList());
    }

    /** 医生是否属于急诊科（此类医生不参与预约）。 */
    private boolean isEmergencyDoctor(String doctorId) {
        if (doctorId == null) {
            return false;
        }
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        return doctor != null && scheduleRules.isEmergencyDeptName(deptName(doctor.getDeptId()));
    }

    /**
     * 分页查询医生的排班（仅返回20分钟标准时段，按时间排序）
     */
    public Map<String, Object> findByDoctorIdStandardWithPage(String doctorId, int page, int size) {
        List<Schedule> all = scheduleRepository.findByDoctorId(doctorId).stream()
                .filter(s -> isStandardTimeSlot(s.getTimeSlot()))
                .sorted(Comparator.comparing(Schedule::getWorkDate)
                        .thenComparing(Schedule::getTimeSlot))
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", all.size());
        result.put("totalPages", (int) Math.ceil((double) all.size() / size));
        result.put("currentPage", page);
        
        int start = page * size;
        int end = Math.min(start + size, all.size());
        
        if (start < all.size()) {
            result.put("data", all.subList(start, end));
        } else {
            result.put("data", new ArrayList<>());
        }
        
        return result;
    }
}
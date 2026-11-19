package com.hospital.repository;

import com.hospital.entity.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {

    List<Schedule> findByHospitalId(String hospitalId);
    List<Schedule> findByDoctorId(String doctorId);
    List<Schedule> findByWorkDate(LocalDate workDate);
    List<Schedule> findByDoctorIdAndWorkDate(String doctorId, LocalDate workDate);
    List<Schedule> findByHospitalIdAndWorkDate(String hospitalId, LocalDate workDate);

    /** 某医院指定日期区间内有排班的日期（去重，供日历标记）。 */
    @Query("SELECT DISTINCT s.workDate FROM Schedule s "
            + "WHERE s.hospitalId = :hospitalId AND s.workDate BETWEEN :from AND :to "
            + "ORDER BY s.workDate")
    List<LocalDate> findWorkDatesByHospitalId(@Param("hospitalId") String hospitalId,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    /** 某天各医院仍有号的排班数（一次查询，供医院列表标记今日/明日可约）。 */
    @Query("SELECT s.hospitalId, COUNT(s) FROM Schedule s "
            + "WHERE s.workDate = :date AND s.status = 1 AND s.bookedCount < s.maxPatients "
            + "GROUP BY s.hospitalId")
    List<Object[]> countAvailableByHospital(@Param("date") LocalDate date);

    /** 单院某天是否还有可约号源（走 hospital+date 索引，供分页列表逐院判断）。 */
    @Query("SELECT COUNT(s) FROM Schedule s "
            + "WHERE s.hospitalId = :hospitalId AND s.workDate = :date "
            + "AND s.status = 1 AND s.bookedCount < s.maxPatients")
    long countAvailable(@Param("hospitalId") String hospitalId, @Param("date") LocalDate date);
    List<Schedule> findByHospitalIdAndDoctorIdAndWorkDate(String hospitalId, String doctorId, LocalDate workDate);
    Optional<Schedule> findByDoctorIdAndWorkDateAndTimeSlot(String doctorId, LocalDate workDate, String timeSlot);
    
    List<Schedule> findByDoctorIdOrderByWorkDateAscTimeSlotAsc(String doctorId);
    List<Schedule> findByDoctorIdAndWorkDateBetween(String doctorId, LocalDate from, LocalDate to);
    Optional<Schedule> findByHospitalIdAndDoctorIdAndWorkDateAndTimeSlot(String hospitalId, String doctorId, LocalDate workDate, String timeSlot);

    // 增强版乐观锁扣减（带版本号校验更严格）
    @Modifying
    @Query("UPDATE Schedule s SET s.bookedCount = s.bookedCount + 1, " +
            "s.status = CASE WHEN s.bookedCount + 1 >= s.maxPatients THEN 2 ELSE s.status END, " +
            "s.version = s.version + 1 " +
            "WHERE s.scheduleId = :scheduleId " +
            "AND s.bookedCount < s.maxPatients " +
            "AND s.status = 1")
    int incrementBookedCount(@Param("scheduleId") String scheduleId);

    // 取消恢复号源
    @Modifying
    @Query("UPDATE Schedule s SET s.bookedCount = s.bookedCount - 1, " +
            "s.status = CASE WHEN s.status = 2 THEN 1 ELSE s.status END, " +
            "s.version = s.version + 1 " +
            "WHERE s.scheduleId = :scheduleId AND s.bookedCount > 0")
    int decrementBookedCount(@Param("scheduleId") String scheduleId);

    // 带悲观锁查询（极端并发场景备用）
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.scheduleId = :id")
    Optional<Schedule> findByIdWithPessimisticLock(@Param("id") String id);

    @Query(value = "SELECT * FROM schedule WHERE hospital_id = :hospitalId AND is_deleted = 1 ORDER BY delete_time DESC LIMIT 100",
            nativeQuery = true)
    List<Schedule> findDeletedByHospitalId(@Param("hospitalId") String hospitalId);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query(value = "UPDATE schedule SET is_deleted = 0, delete_time = NULL, deleted_by = NULL WHERE schedule_id = :scheduleId",
            nativeQuery = true)
    int restoreDeleted(@Param("scheduleId") String scheduleId);

    @Query(value = "SELECT * FROM schedule WHERE schedule_id = :scheduleId", nativeQuery = true)
    Optional<Schedule> findIncludingDeleted(@Param("scheduleId") String scheduleId);

    /**
     * 分诊用：按科室名（+可选医院）查询 7 天内仍有号且未停用的排班。
     * department 与 doctor 均需未删除/启用，避免路由到无效号源。
     */
    @Query(value = "SELECT s.* FROM schedule s "
            + "JOIN doctor d ON s.doctor_id = d.doctor_id "
            + "JOIN department dp ON d.dept_id = dp.dept_id "
            + "WHERE s.is_deleted = 0 AND s.status = 1 AND s.booked_count < s.max_patients "
            + "AND s.work_date >= :fromDate AND s.work_date <= :toDate "
            + "AND d.status = 1 AND dp.is_deleted = 0 AND dp.dept_name = :deptName "
            + "AND (:hospitalId IS NULL OR s.hospital_id = :hospitalId) "
            + "ORDER BY s.work_date ASC, s.time_slot ASC", nativeQuery = true)
    List<Schedule> findAvailableForDept(@Param("deptName") String deptName,
                                        @Param("hospitalId") String hospitalId,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate);
}
package com.hospital.repository;

import com.hospital.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    
    List<Department> findByHospitalId(String hospitalId);
    
    @Query("SELECT d FROM Department d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Department> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT d FROM Department d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Department> searchByName(@Param("keyword") String keyword);
    
    @Query("SELECT d FROM Department d WHERE d.hospitalId = :hospitalId AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Department> searchByKeywordAndHospital(@Param("keyword") String keyword, @Param("hospitalId") String hospitalId);

    @Query(value = "SELECT * FROM department WHERE hospital_id = :hospitalId AND is_deleted = 1 ORDER BY delete_time DESC",
            nativeQuery = true)
    List<Department> findDeletedByHospitalId(@Param("hospitalId") String hospitalId);

    @Query(value = "SELECT * FROM department WHERE dept_id = :deptId", nativeQuery = true)
    Optional<Department> findIncludingDeleted(@Param("deptId") String deptId);

    Optional<Department> findByHospitalIdAndName(String hospitalId, String name);

    @Modifying
    @Transactional
    @Query(value = "UPDATE department SET is_deleted = 0, delete_time = NULL, deleted_by = NULL WHERE dept_id = :deptId",
            nativeQuery = true)
    int restoreDeleted(@Param("deptId") String deptId);
}

package com.hospital.repository;

import com.hospital.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, String>, JpaSpecificationExecutor<Hospital> {
    List<Hospital> findByStatus(Integer status);
    
    // 按地区查询医院
    List<Hospital> findByProvince(String province);
    List<Hospital> findByProvinceAndCity(String province, String city);
    List<Hospital> findByProvinceAndCityAndDistrict(String province, String city, String district);
    List<Hospital> findByCity(String city);

    // 分页查询
    Page<Hospital> findByStatus(Integer status, Pageable pageable);
    Page<Hospital> findByProvince(String province, Pageable pageable);
    Page<Hospital> findByProvinceAndCity(String province, String city, Pageable pageable);
    Page<Hospital> findByProvinceAndCityAndDistrict(String province, String city, String district, Pageable pageable);

    // 查询所有省份
    @Query("SELECT DISTINCT h.province FROM Hospital h WHERE h.province IS NOT NULL AND h.status = 1 ORDER BY h.province")
    List<String> findDistinctProvinces();

    // 根据省份查询城市
    @Query("SELECT DISTINCT h.city FROM Hospital h WHERE h.province = :province AND h.city IS NOT NULL AND h.status = 1 ORDER BY h.city")
    List<String> findCitiesByProvince(@Param("province") String province);

    // 根据省份和城市查询区县
    @Query("SELECT DISTINCT h.district FROM Hospital h WHERE h.province = :province AND h.city = :city AND h.district IS NOT NULL AND h.status = 1 ORDER BY h.district")
    List<String> findDistrictsByProvinceAndCity(@Param("province") String province, @Param("city") String city);

    // 模糊搜索医院（按名称、地址、等级）
    @Query(value = "SELECT * FROM hospital h WHERE h.status = 1 AND (h.hospital_name LIKE ?1 OR h.address LIKE ?1 OR h.level LIKE ?1) ORDER BY h.hospital_id ASC", nativeQuery = true)
    Page<Hospital> searchByKeyword(String keyword, Pageable pageable);

    // 模糊搜索医院（按关键词和省份）
    @Query(value = "SELECT * FROM hospital h WHERE h.status = 1 AND h.province = ?2 AND (h.hospital_name LIKE ?1 OR h.address LIKE ?1 OR h.level LIKE ?1) ORDER BY h.hospital_id ASC", nativeQuery = true)
    Page<Hospital> searchByKeywordAndProvince(String keyword, String province, Pageable pageable);

    // 模糊搜索医院（按关键词和省市）
    @Query(value = "SELECT * FROM hospital h WHERE h.status = 1 AND h.province = ?2 AND h.city = ?3 AND (h.hospital_name LIKE ?1 OR h.address LIKE ?1 OR h.level LIKE ?1) ORDER BY h.hospital_id ASC", nativeQuery = true)
    Page<Hospital> searchByKeywordAndCity(String keyword, String province, String city, Pageable pageable);

    // 模糊搜索医院（按关键词和省市区）
    @Query(value = "SELECT * FROM hospital h WHERE h.status = 1 AND h.province = ?2 AND h.city = ?3 AND h.district = ?4 AND (h.hospital_name LIKE ?1 OR h.address LIKE ?1 OR h.level LIKE ?1) ORDER BY h.hospital_id ASC", nativeQuery = true)
    Page<Hospital> searchByKeywordAndLocation(String keyword, String province, String city, String district, Pageable pageable);

    // 统一分页：省/市/区/关键词均可为空，按医院名称的拼音顺序 A-Z 排列
    @Query(value = "SELECT * FROM hospital h WHERE h.status = 1 "
            + "AND (?2 IS NULL OR h.province = ?2) "
            + "AND (?3 IS NULL OR h.city = ?3) "
            + "AND (?4 IS NULL OR h.district = ?4) "
            + "AND (?1 IS NULL OR h.hospital_name LIKE ?1 OR h.address LIKE ?1 OR h.level LIKE ?1) "
            + "ORDER BY h.hospital_id ASC",
            countQuery = "SELECT COUNT(*) FROM hospital h WHERE h.status = 1 "
                    + "AND (?2 IS NULL OR h.province = ?2) "
                    + "AND (?3 IS NULL OR h.city = ?3) "
                    + "AND (?4 IS NULL OR h.district = ?4) "
                    + "AND (?1 IS NULL OR h.hospital_name LIKE ?1 OR h.address LIKE ?1 OR h.level LIKE ?1)",
            nativeQuery = true)
    Page<Hospital> searchPage(String keyword, String province, String city, String district, Pageable pageable);
}

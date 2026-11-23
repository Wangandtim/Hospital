package com.hospital.repository;

import com.hospital.entity.HospitalArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalArticleRepository extends JpaRepository<HospitalArticle, String> {

    List<HospitalArticle> findByHospitalIdAndStatusOrderByCreateTimeDesc(String hospitalId, Integer status);

    List<HospitalArticle> findByHospitalIdOrderByCreateTimeDesc(String hospitalId);

    List<HospitalArticle> findByTitleContainingOrderByCreateTimeDesc(String keyword);

    /** 患者端已发布公告：按标题关键词在数据库侧过滤。 */
    @Query("SELECT a FROM HospitalArticle a WHERE a.hospitalId = :hospitalId AND a.status = 1 "
            + "AND LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "ORDER BY a.createTime DESC, a.articleId ASC")
    List<HospitalArticle> searchPublishedByTitle(@Param("hospitalId") String hospitalId,
                                                 @Param("keyword") String keyword);
}

package com.hospital.service;

import com.hospital.entity.Hospital;
import com.hospital.entity.HospitalArticle;
import com.hospital.exception.NotFoundException;
import com.hospital.repository.HospitalArticleRepository;
import com.hospital.repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class HospitalArticleService {

    @Autowired
    private HospitalArticleRepository repository;

    @Autowired
    private HospitalRepository hospitalRepository;

    /** 医院级公告开关：医院不存在或未开启一律视为关闭。 */
    public boolean isNoticeEnabled(String hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .map(hospital -> Boolean.TRUE.equals(hospital.getNoticeEnabled()))
                .orElse(false);
    }

    public void setNoticeEnabled(String hospitalId, boolean enabled) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new NotFoundException("医院不存在"));
        hospital.setNoticeEnabled(enabled);
        hospitalRepository.save(hospital);
    }

    /** 患者端：开关关闭时返回空列表；开启后只返回已发布内容。 */
    public List<HospitalArticle> listPublished(String hospitalId) {
        if (!isNoticeEnabled(hospitalId)) {
            return Collections.emptyList();
        }
        return repository.findByHospitalIdAndStatusOrderByCreateTimeDesc(hospitalId, 1);
    }

    /** 患者端详情：开关关闭或内容未发布时一律 404，不暴露存在性。 */
    public HospitalArticle findPublishedById(String articleId) {
        HospitalArticle article = repository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("内容不存在"));
        if (article.getStatus() == null || article.getStatus() != 1
                || !isNoticeEnabled(article.getHospitalId())) {
            throw new NotFoundException("内容不存在");
        }
        return article;
    }

    /** 管理端：本院全部内容 */
    public List<HospitalArticle> listAll(String hospitalId) {
        return repository.findByHospitalIdOrderByCreateTimeDesc(hospitalId);
    }

    public HospitalArticle findById(String articleId) {
        return repository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("内容不存在"));
    }

    public HospitalArticle create(HospitalArticle article) {
        article.setArticleId("A" + System.currentTimeMillis());
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        if (article.getStatus() == null) {
            article.setStatus(1);
        }
        return repository.save(article);
    }

    public HospitalArticle update(String articleId, HospitalArticle article) {
        HospitalArticle existing = findById(articleId);
        if (article.getTitle() != null) existing.setTitle(article.getTitle());
        if (article.getSummary() != null) existing.setSummary(article.getSummary());
        if (article.getContent() != null) existing.setContent(article.getContent());
        if (article.getCoverUrl() != null) existing.setCoverUrl(article.getCoverUrl());
        if (article.getCategory() != null) existing.setCategory(article.getCategory());
        if (article.getStatus() != null) existing.setStatus(article.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        return repository.save(existing);
    }

    public void delete(String articleId) {
        repository.deleteById(articleId);
    }
}

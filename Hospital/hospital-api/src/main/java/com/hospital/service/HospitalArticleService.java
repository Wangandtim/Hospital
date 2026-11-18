package com.hospital.service;

import com.hospital.entity.HospitalArticle;
import com.hospital.repository.HospitalArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HospitalArticleService {

    @Autowired
    private HospitalArticleRepository repository;

    /** 患者端：只看已发布内容 */
    public List<HospitalArticle> listPublished(String hospitalId) {
        return repository.findByHospitalIdAndStatusOrderByCreateTimeDesc(hospitalId, 1);
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

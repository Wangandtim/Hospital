package com.hospital.repository;

import com.hospital.entity.WechatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WechatUserRepository extends JpaRepository<WechatUser, String> {
}

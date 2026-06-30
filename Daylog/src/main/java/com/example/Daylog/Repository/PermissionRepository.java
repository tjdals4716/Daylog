package com.example.Daylog.Repository;

import com.example.Daylog.Entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// [B] edit by smsong - 사용자 권한 저장소
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByUid(String uid);
    List<PermissionEntity> findAllByOrderByAccessAllowedDescUpdatedAtDesc();
}
// [E] edit by smsong

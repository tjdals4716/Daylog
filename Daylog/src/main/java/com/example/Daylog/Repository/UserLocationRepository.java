package com.example.Daylog.Repository;

import com.example.Daylog.Entity.UserLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// [B] edit by smsong - 사용자 실시간 위치 기록 저장소
public interface UserLocationRepository extends JpaRepository<UserLocationEntity, Long> {

    // 특정 사용자의 위치 이력 (최신순)
    List<UserLocationEntity> findByUidOrderByCapturedAtDesc(String uid);

    // 특정 사용자의 가장 최근 위치 1건
    Optional<UserLocationEntity> findTop1ByUidOrderByCapturedAtDesc(String uid);

    // 특정 사용자의 마지막 적재 시각 이후 중복 적재 방지용
    Optional<UserLocationEntity> findTop1ByUidOrderByCreatedAtDesc(String uid);

    // 기간 내 위치 이력
    List<UserLocationEntity> findByUidAndCapturedAtBetweenOrderByCapturedAtAsc(String uid, LocalDateTime from, LocalDateTime to);
}
// [E] edit by smsong

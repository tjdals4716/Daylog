package com.example.Daylog.Repository;

import com.example.Daylog.Entity.ChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Long> {

    // 특정 유저(owner)가 작성한 가볼곳 목록
    List<ChecklistEntity> findByOwnerUid(String uid);

    // 휴지통에 없는(정상) 가볼곳만 조회 — 지도/목록 노출용
    List<ChecklistEntity> findByDeletedFalse();

    // 내가 휴지통으로 보낸 가볼곳 목록
    List<ChecklistEntity> findByOwnerUidAndDeletedTrue(String uid);

    // [B] edit by smsong - 휴지통 30일 자동 삭제: 이동(trashedAt)된 지 기준시각 이전인 가볼곳
    List<ChecklistEntity> findByDeletedTrueAndTrashedAtBefore(LocalDateTime cutoff);
    // [E] edit by smsong
}

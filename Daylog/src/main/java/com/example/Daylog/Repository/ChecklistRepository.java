package com.example.Daylog.Repository;

import com.example.Daylog.Entity.ChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Long> {

    // 특정 유저(owner)가 작성한 가볼곳 목록
    List<ChecklistEntity> findByOwnerUid(String uid);

    // 휴지통에 없는(정상) 가볼곳만 조회 — 지도/목록 노출용
    List<ChecklistEntity> findByDeletedFalse();

    // 내가 휴지통으로 보낸 가볼곳 목록
    List<ChecklistEntity> findByOwnerUidAndDeletedTrue(String uid);
}

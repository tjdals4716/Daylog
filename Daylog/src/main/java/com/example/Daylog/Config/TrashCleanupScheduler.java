package com.example.Daylog.Config;

import com.example.Daylog.Service.ChecklistService;
import com.example.Daylog.Service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// [B] edit by smsong - 휴지통 30일 자동 삭제 스케줄러
//  ※ 동작하려면 메인 애플리케이션 클래스(@SpringBootApplication)에 @EnableScheduling 추가 필요.
//    스케줄러를 켜지 않아도 휴지통 조회(getTrash) 시 만료 항목이 자동 정리되므로 사용자 화면 기준 30일 자동삭제는 보장됨.
@Component
@RequiredArgsConstructor
public class TrashCleanupScheduler {

    private final MemoryService memoryService;
    private final ChecklistService checklistService;

    // 매일 새벽 4시 — 보관 30일 경과한 추억/가볼곳 영구 삭제
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeExpiredTrash() {
        try { memoryService.purgeExpiredTrash(); } catch (Exception ignored) {}
        try { checklistService.purgeExpiredTrash(); } catch (Exception ignored) {}
    }
}
// [E] edit by smsong

package com.example.Daylog.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// [B] edit by smsong - 사용자 실시간 위치 기록 테이블 (10분 단위 적재)
@Entity(name = "user_locations")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserLocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 사용자의 위치인지 (UserEntity.uid)
    @Column(nullable = false)
    private String uid;

    // 사용자 이름(스냅샷 · 조회 편의용, 선택)
    private String name;

    // 위도/경도 (필수)
    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    // 도로명까지 매우 상세한 주소 (역지오코딩 결과)
    @Column(length = 500)
    private String address;

    // 지번/구 주소 (선택)
    @Column(length = 500)
    private String roadAddress;

    // 큰 지역명(시/도 + 시군구 등, 선택)
    private String placeName;

    // 위치 정확도(m), 고도(m), 속도(m/s), 방향(0~360)
    private Double accuracy;
    private Double altitude;
    private Double speed;
    private Double heading;

    // 위치 수집 출처 (foreground / background / manual 등)
    private String source;

    // 클라이언트가 위치를 측정한 시각
    private LocalDateTime capturedAt;

    // 서버에 적재된 시각
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.capturedAt == null) this.capturedAt = now;
    }
}
// [E] edit by smsong

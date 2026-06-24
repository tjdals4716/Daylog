package com.example.Daylog.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity(name = "memories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MemoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String content;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    // 검색/선택한 장소 이름 (예: "노들섬") — 선택 사항
    private String placeName;

    // 역지오코딩된 상세 주소 (도로명/지번) — 선택 사항
    @Column(length = 500)
    private String address;

    private String mediaURL;

    // 휴지통(소프트 삭제) 플래그 — true 면 휴지통으로 이동된 상태
    @Column(nullable = false)
    private boolean deleted;

    // 작성자 연관관계 (BuildingEntity 패턴 참고)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private UserEntity owner;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        // createdAt 이 지정되지 않았을 때(예: 메타데이터 없음)만 현재 시각으로 채움.
        // 사진 촬영일(메타데이터) 등으로 값이 들어온 경우엔 그 값을 그대로 사용.
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
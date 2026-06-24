package com.example.Daylog.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "checklists")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChecklistEntity {
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

    // 큰 영역(시/도 + 시·군·구) — MemoryEntity 와 동일하게 placeName 에 보관
    private String placeName;

    // 상세 주소
    @Column(length = 500)
    private String address;

    // 이미지(선택) — 지도 마커에는 표시하지 않지만 속성으로 보관
    private String mediaURL;

    // 타입 (CAFE / FOOD / SPOT / SHOPPING / ETC 등)
    private String type;

    // 휴지통(소프트 삭제) 플래그 — true 면 휴지통으로 이동된 상태
    @Column(nullable = false)
    private boolean deleted;

    // 다녀왔는지 여부
    @Column(nullable = false)
    private boolean visited;

    // 다녀온 날짜 (visited=true 일 때만 의미 있음)
    private LocalDate visitedDate;

    // 작성자(= 체크리스트를 만든 사람) 연관관계 (MemoryEntity 패턴 동일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private UserEntity owner;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

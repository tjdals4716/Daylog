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

    private String mediaURL;

    // 작성자 연관관계 (BuildingEntity 패턴 참고)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private UserEntity owner;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
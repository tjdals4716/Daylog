package com.example.Daylog.DTO;

import com.example.Daylog.Entity.ChecklistEntity;
import com.example.Daylog.Entity.UserEntity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChecklistDTO {
    private Long id;
    private String title;
    private String content;
    private Double lat;   // 필수 위치 데이터
    private Double lng;
    private String placeName; // 큰 영역 (주소)
    private String address;   // 상세 주소
    private String mediaURL;  // 이미지 (선택)
    private String type;      // 타입 (카페/식당/장소 등)
    private boolean deleted;  // 휴지통 여부
    private boolean visited;  // 다녀왔는지 여부
    private LocalDate visitedDate; // 다녀온 날짜 (선택)
    private String ownerUid;
    private LocalDateTime createdAt;

    // Entity -> DTO
    public static ChecklistDTO entityToDto(ChecklistEntity e) {
        String ownerUid = (e.getOwner() != null) ? e.getOwner().getUid() : null;
        return new ChecklistDTO(
                e.getId(),
                e.getTitle(),
                e.getContent(),
                e.getLat(),
                e.getLng(),
                e.getPlaceName(),
                e.getAddress(),
                e.getMediaURL(),
                e.getType(),
                e.isDeleted(),
                e.isVisited(),
                e.getVisitedDate(),
                ownerUid,
                e.getCreatedAt()
        );
    }

    // DTO -> Entity
    public ChecklistEntity dtoToEntity(UserEntity owner) {
        return ChecklistEntity.builder()
                .id(id)
                .title(title)
                .content(content)
                .lat(lat)
                .lng(lng)
                .placeName(placeName)
                .address(address)
                .mediaURL(mediaURL)
                .type(type)
                .deleted(deleted)
                .visited(visited)
                .visitedDate(visitedDate)
                .owner(owner)
                .createdAt(createdAt)
                .build();
    }
}

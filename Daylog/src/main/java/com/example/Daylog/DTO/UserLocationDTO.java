package com.example.Daylog.DTO;

import com.example.Daylog.Entity.UserLocationEntity;
import lombok.*;

import java.time.LocalDateTime;

// [B] edit by smsong - 사용자 실시간 위치 DTO
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserLocationDTO {
    private Long id;
    private String uid;
    private String name;
    private Double lat;
    private Double lng;
    private String address;
    private String roadAddress;
    private String placeName;
    private Double accuracy;
    private Double altitude;
    private Double speed;
    private Double heading;
    private String source;
    private LocalDateTime capturedAt;
    private LocalDateTime createdAt;

    public static UserLocationDTO entityToDto(UserLocationEntity e) {
        return UserLocationDTO.builder()
                .id(e.getId())
                .uid(e.getUid())
                .name(e.getName())
                .lat(e.getLat())
                .lng(e.getLng())
                .address(e.getAddress())
                .roadAddress(e.getRoadAddress())
                .placeName(e.getPlaceName())
                .accuracy(e.getAccuracy())
                .altitude(e.getAltitude())
                .speed(e.getSpeed())
                .heading(e.getHeading())
                .source(e.getSource())
                .capturedAt(e.getCapturedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public UserLocationEntity dtoToEntity() {
        return UserLocationEntity.builder()
                .uid(uid)
                .name(name)
                .lat(lat)
                .lng(lng)
                .address(address)
                .roadAddress(roadAddress)
                .placeName(placeName)
                .accuracy(accuracy)
                .altitude(altitude)
                .speed(speed)
                .heading(heading)
                .source(source)
                .capturedAt(capturedAt)
                .build();
    }
}
// [E] edit by smsong

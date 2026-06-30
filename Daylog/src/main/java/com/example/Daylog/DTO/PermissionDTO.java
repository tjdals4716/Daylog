package com.example.Daylog.DTO;

import com.example.Daylog.Entity.PermissionEntity;
import lombok.*;

import java.time.LocalDateTime;

// [B] edit by smsong - 사용자 권한 DTO
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PermissionDTO {
    private Long id;
    private String uid;
    private String name;
    private String nickname;
    private String email;
    private String provider;
    private String profileURL;
    private boolean accessAllowed;
    private boolean canEdit;
    private boolean canTrash;
    private boolean canDelete;
    private boolean admin;           // 관리자(name '송성민') 여부 — 표시용
    private String requestStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;

    public static PermissionDTO entityToDto(PermissionEntity e, boolean isAdmin) {
        return PermissionDTO.builder()
                .id(e.getId())
                .uid(e.getUid())
                .name(e.getName())
                .nickname(e.getNickname())
                .email(e.getEmail())
                .provider(e.getProvider())
                .profileURL(e.getProfileURL())
                .accessAllowed(e.isAccessAllowed())
                .canEdit(e.isCanEdit())
                .canTrash(e.isCanTrash())
                .canDelete(e.isCanDelete())
                .admin(isAdmin)
                .requestStatus(e.getRequestStatus())
                .requestedAt(e.getRequestedAt())
                .decidedAt(e.getDecidedAt())
                .build();
    }
}
// [E] edit by smsong

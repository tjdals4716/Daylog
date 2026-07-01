package com.example.Daylog.Service;

import com.example.Daylog.DTO.PermissionDTO;
import com.example.Daylog.Entity.PermissionEntity;
import com.example.Daylog.Entity.UserEntity;
import com.example.Daylog.Repository.PermissionRepository;
import com.example.Daylog.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// [B] edit by smsong - 사용자 권한 관리 서비스 (uid 기반)
//  관리자 = uid '3635939452' (무조건 모든 권한 + 접근). 이 uid만 상시 접근 허용(부트스트랩).
//  그 외 사용자는 관리자가 승인/부여해야 접근·기능 가능. (이름 하드코딩 전부 제거)
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public static final String ADMIN_UID = "3635939452";

    // ===== 판별 (uid 기준) =====
    private boolean isAdminUid(String uid) { return uid != null && ADMIN_UID.equals(uid.trim()); }
    private boolean privileged(String uid) { return isAdminUid(uid); } // 상시 허용 = 관리자 uid만

    public boolean isAdmin(UserDetails ud) { return ud != null && isAdminUid(ud.getUsername()); }

    private Optional<PermissionEntity> rowOf(UserDetails ud) {
        if (ud == null) return Optional.empty();
        return permissionRepository.findByUid(ud.getUsername());
    }

    // ===== 실효 권한 (Memory/Checklist 서비스에서 사용) =====
    public boolean hasAccess(UserDetails ud) {
        if (ud == null) return false;
        if (privileged(ud.getUsername())) return true;
        return rowOf(ud).map(PermissionEntity::isAdminApproved).orElse(false);
    }
    public boolean canCreate(UserDetails ud) {
        if (ud == null) return false;
        if (privileged(ud.getUsername())) return true;
        return rowOf(ud).map(PermissionEntity::isCanCreate).orElse(false);
    }
    public boolean canEdit(UserDetails ud) {
        if (ud == null) return false;
        if (privileged(ud.getUsername())) return true;
        return rowOf(ud).map(PermissionEntity::isCanEdit).orElse(false);
    }
    public boolean canTrash(UserDetails ud) {
        if (ud == null) return false;
        if (privileged(ud.getUsername())) return true;
        return rowOf(ud).map(PermissionEntity::isCanTrash).orElse(false);
    }
    public boolean canDelete(UserDetails ud) {
        if (ud == null) return false;
        if (privileged(ud.getUsername())) return true;
        return rowOf(ud).map(PermissionEntity::isCanDelete).orElse(false);
    }

    private void syncSnapshot(PermissionEntity e, UserEntity user) {
        if (user == null) return;
        e.setUser(user);
        e.setName(user.getName());
        e.setNickname(user.getNickname());
        e.setEmail(user.getEmail());
        e.setProvider(user.getProvider());
        e.setProfileURL(user.getProfileURL());
    }

    // ===== 등록(upsert): 로그인 시 권한 목록 등록 + 본인 실효권한 반환 + 접근 자가치유 =====
    @Transactional
    public PermissionDTO registerAndGetMine(UserDetails ud) {
        if (ud == null) throw new RuntimeException("권한이 없습니다");
        String uid = ud.getUsername();
        UserEntity user = userRepository.findByUid(uid).orElse(null);
        PermissionEntity e = permissionRepository.findByUid(uid)
                .orElseGet(() -> PermissionEntity.builder().uid(uid).requestStatus("NONE").build());
        syncSnapshot(e, user);

        boolean admin = isAdminUid(uid);
        boolean priv = privileged(uid);
        boolean access = priv || e.isAdminApproved();
        e.setAccessAllowed(access);
        if (access && !"APPROVED".equals(e.getRequestStatus())) e.setRequestStatus("APPROVED");
        if (!access && "APPROVED".equals(e.getRequestStatus())) e.setRequestStatus("NONE");

        e = permissionRepository.save(e);
        return PermissionDTO.effective(e, admin, priv);
    }

    @Transactional(readOnly = true)
    public PermissionDTO getMine(UserDetails ud) {
        if (ud == null) throw new RuntimeException("권한이 없습니다");
        String uid = ud.getUsername();
        boolean admin = isAdminUid(uid);
        boolean priv = privileged(uid);
        PermissionEntity e = permissionRepository.findByUid(uid)
                .orElseGet(() -> PermissionEntity.builder().uid(uid).requestStatus("NONE").build());
        return PermissionDTO.effective(e, admin, priv);
    }

    // ===== 접근 요청 (차단된 사용자도 호출 가능) =====
    @Transactional
    public PermissionDTO requestAccess(UserDetails ud) {
        if (ud == null) throw new RuntimeException("권한이 없습니다");
        String uid = ud.getUsername();
        UserEntity user = userRepository.findByUid(uid).orElse(null);
        PermissionEntity e = permissionRepository.findByUid(uid)
                .orElseGet(() -> PermissionEntity.builder().uid(uid).build());
        syncSnapshot(e, user);
        boolean priv = privileged(uid);
        if (!priv && !e.isAdminApproved()) {
            e.setRequestStatus("PENDING");
            e.setRequestedAt(LocalDateTime.now());
        }
        e = permissionRepository.save(e);
        return PermissionDTO.effective(e, isAdminUid(uid), priv);
    }

    // ===== 관리자: 전체 사용자 목록 =====
    @Transactional(readOnly = true)
    public List<PermissionDTO> listAll(UserDetails ud) {
        requireAdmin(ud);
        return permissionRepository.findAllByOrderByAccessAllowedDescUpdatedAtDesc().stream()
                .map(e -> {
                    boolean adm = isAdminUid(e.getUid());
                    return PermissionDTO.raw(e, adm, adm);
                })
                .collect(Collectors.toList());
    }

    // ===== 관리자: 권한 변경 (생성/수정/휴지통/삭제 + 접근) =====
    @Transactional
    public PermissionDTO updatePermission(String targetUid, PermissionDTO patch, UserDetails ud) {
        requireAdmin(ud);
        PermissionEntity e = permissionRepository.findByUid(targetUid)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));
        // null 이 들어와도 안전하게 처리 (Boolean.TRUE.equals)
        e.setCanCreate(Boolean.TRUE.equals(patch.getCanCreate()));
        e.setCanEdit(Boolean.TRUE.equals(patch.getCanEdit()));
        e.setCanTrash(Boolean.TRUE.equals(patch.getCanTrash()));
        e.setCanDelete(Boolean.TRUE.equals(patch.getCanDelete()));
        boolean approve = Boolean.TRUE.equals(patch.getAccessAllowed());
        e.setAdminApproved(approve);
        e.setAccessAllowed(approve);
        e.setRequestStatus(approve ? "APPROVED" : "REJECTED");
        e.setDecidedAt(LocalDateTime.now());
        e = permissionRepository.save(e);
        boolean adm = isAdminUid(e.getUid());
        return PermissionDTO.raw(e, adm, adm);
    }

    // ===== 관리자: 접근 요청 승인/거절 =====
    @Transactional
    public PermissionDTO decideAccess(String targetUid, boolean approve, UserDetails ud) {
        requireAdmin(ud);
        PermissionEntity e = permissionRepository.findByUid(targetUid)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));
        e.setAdminApproved(approve);
        e.setAccessAllowed(approve);
        e.setRequestStatus(approve ? "APPROVED" : "REJECTED");
        e.setDecidedAt(LocalDateTime.now());
        if (!approve) {
            e.setCanCreate(false); e.setCanEdit(false); e.setCanTrash(false); e.setCanDelete(false);
        }
        e = permissionRepository.save(e);
        boolean adm = isAdminUid(e.getUid());
        return PermissionDTO.raw(e, adm, adm);
    }

    private void requireAdmin(UserDetails ud) {
        if (!isAdmin(ud)) throw new RuntimeException("관리자만 접근할 수 있습니다.");
    }
}
// [E] edit by smsong

package com.example.Daylog.Controller;

import com.example.Daylog.DTO.PermissionDTO;
import com.example.Daylog.Service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// [B] edit by smsong - 사용자 권한 관리 API
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // 로그인 사용자 등록(upsert) + 본인 권한 반환 (앱 진입 시 호출)
    @PostMapping("/register")
    public ResponseEntity<PermissionDTO> register(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(permissionService.registerAndGetMine(ud));
    }

    // 본인 권한 조회
    @GetMapping("/me")
    public ResponseEntity<PermissionDTO> me(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(permissionService.getMine(ud));
    }

    // 접근 권한 요청 (차단 화면에서 호출)
    @PostMapping("/request")
    public ResponseEntity<PermissionDTO> request(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(permissionService.requestAccess(ud));
    }

    // 관리자: 전체 사용자 목록
    @GetMapping("/users")
    public ResponseEntity<List<PermissionDTO>> users(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(permissionService.listAll(ud));
    }

    // 관리자: 권한 변경
    @PutMapping("/{uid}")
    public ResponseEntity<PermissionDTO> update(@PathVariable("uid") String uid,
                                                @RequestBody PermissionDTO patch,
                                                @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(permissionService.updatePermission(uid, patch, ud));
    }

    // 관리자: 접근 요청 승인/거절
    @PostMapping("/{uid}/decide")
    public ResponseEntity<PermissionDTO> decide(@PathVariable("uid") String uid,
                                                @RequestParam("approve") boolean approve,
                                                @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(permissionService.decideAccess(uid, approve, ud));
    }
}
// [E] edit by smsong

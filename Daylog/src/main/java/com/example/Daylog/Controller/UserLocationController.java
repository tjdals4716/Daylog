package com.example.Daylog.Controller;

import com.example.Daylog.DTO.UserLocationDTO;
import com.example.Daylog.Service.UserLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// [B] edit by smsong - 사용자 실시간 위치 API
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class UserLocationController {

    private final UserLocationService locationService;

    // 내 위치 1건 적재 (클라이언트가 10분 주기로 호출)
    @PostMapping
    public ResponseEntity<UserLocationDTO> save(@RequestBody UserLocationDTO dto,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(locationService.saveLocation(dto, userDetails));
    }

    // 특정 사용자 위치 이력
    @GetMapping("/{uid}")
    public ResponseEntity<List<UserLocationDTO>> history(@PathVariable("uid") String uid,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(locationService.getHistory(uid, userDetails));
    }

    // 전체 사용자 최신 위치
    @GetMapping("/latest")
    public ResponseEntity<List<UserLocationDTO>> latestAll(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(locationService.getLatestOfAllUsers(userDetails));
    }
}
// [E] edit by smsong

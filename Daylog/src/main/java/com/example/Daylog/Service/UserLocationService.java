package com.example.Daylog.Service;

import com.example.Daylog.DTO.UserLocationDTO;
import com.example.Daylog.Entity.UserEntity;
import com.example.Daylog.Entity.UserLocationEntity;
import com.example.Daylog.Repository.UserLocationRepository;
import com.example.Daylog.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// [B] edit by smsong - 사용자 실시간 위치 적재/조회 서비스
@Service
@RequiredArgsConstructor
public class UserLocationService {

    private final UserLocationRepository locationRepository;
    private final UserRepository userRepository;

    // 같은 사용자가 너무 잦게 적재하는 것을 방지 (최소 적재 간격, 분)
    private static final long MIN_INTERVAL_MINUTES = 9;

    // 내 위치 1건 적재 (인증된 본인만)
    @Transactional
    public UserLocationDTO saveLocation(UserLocationDTO dto, UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("권한이 없습니다");
        String uid = userDetails.getUsername();
        if (dto.getLat() == null || dto.getLng() == null) {
            throw new IllegalArgumentException("위치 정보가 필수입니다.");
        }

        // 직전 적재로부터 9분 이내면 중복으로 보고 건너뜀(10분 주기 안정화)
        LocalDateTime now = LocalDateTime.now();
        var last = locationRepository.findTop1ByUidOrderByCreatedAtDesc(uid);
        if (last.isPresent() && last.get().getCreatedAt() != null
                && last.get().getCreatedAt().isAfter(now.minusMinutes(MIN_INTERVAL_MINUTES))) {
            return UserLocationDTO.entityToDto(last.get());
        }

        UserEntity user = userRepository.findByUid(uid).orElse(null);
        UserLocationEntity entity = dto.dtoToEntity();
        entity.setUid(uid); // 인증 사용자로 강제
        if (user != null && (entity.getName() == null || entity.getName().isBlank())) {
            entity.setName(user.getName());
        }
        return UserLocationDTO.entityToDto(locationRepository.save(entity));
    }

    // 특정 사용자 위치 이력 (본인 또는 모든 인증 사용자가 조회 — 커플 공유)
    @Transactional(readOnly = true)
    public List<UserLocationDTO> getHistory(String uid, UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("권한이 없습니다");
        return locationRepository.findByUidOrderByCapturedAtDesc(uid).stream()
                .map(UserLocationDTO::entityToDto)
                .collect(Collectors.toList());
    }

    // 가입된 모든 사용자의 '가장 최근 위치' (실시간 현황판)
    @Transactional(readOnly = true)
    public List<UserLocationDTO> getLatestOfAllUsers(UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("권한이 없습니다");
        return userRepository.findAll().stream()
                .map(u -> locationRepository.findTop1ByUidOrderByCapturedAtDesc(u.getUid()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(UserLocationDTO::entityToDto)
                .collect(Collectors.toList());
    }
}
// [E] edit by smsong

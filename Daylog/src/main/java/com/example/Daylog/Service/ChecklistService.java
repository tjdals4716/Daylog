package com.example.Daylog.Service;

import com.example.Daylog.DTO.ChecklistDTO;
import com.example.Daylog.Entity.ChecklistEntity;
import com.example.Daylog.Entity.UserEntity;
import com.example.Daylog.Repository.ChecklistRepository;
import com.example.Daylog.Repository.UserRepository;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final UserRepository userRepository;
    private final Storage storage;

    @Value("${google.cloud.credentials.header}")
    private String googleCloudHeader;
    @Value("${google.cloud.storage.bucket}")
    private String bucket;

    // 토큰의 사용자와 요청 uid 가 일치하는지 확인 (MemoryService 동일)
    private UserEntity getAuthorizedUser(String uid, UserDetails userDetails) {
        if (userDetails == null || !userDetails.getUsername().equals(uid)) {
            throw new RuntimeException("권한이 없습니다");
        }
        return userRepository.findByUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }

    // 소유자 검증 후 체크리스트 반환
    private ChecklistEntity getOwnedChecklist(Long id, UserDetails userDetails) {
        ChecklistEntity c = checklistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("가볼곳을 찾을 수 없습니다"));
        String ownerUid = (c.getOwner() != null) ? c.getOwner().getUid() : null;
        if (userDetails == null || ownerUid == null || !ownerUid.equals(userDetails.getUsername())) {
            throw new RuntimeException("권한이 없습니다");
        }
        return c;
    }

    // GCS 업로드 (선택 — 이미지 없으면 null)
    private String uploadMedia(MultipartFile mediaFile) {
        if (mediaFile == null || mediaFile.isEmpty()) return null;
        try {
            UUID uuid = UUID.randomUUID();
            String original = mediaFile.getOriginalFilename();
            String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : "";
            String fileName = uuid.toString() + ext;

            BlobId blobId = BlobId.of(bucket, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("image/jpeg")
                    .build();
            storage.create(blobInfo, mediaFile.getBytes());
            return googleCloudHeader + fileName;
        } catch (IOException e) {
            throw new RuntimeException("업로드 실패", e);
        }
    }

    @Transactional
    public ChecklistDTO createChecklist(String uid, ChecklistDTO dto, MultipartFile mediaFile, UserDetails userDetails) {
        UserEntity owner = getAuthorizedUser(uid, userDetails);

        if (dto.getLat() == null || dto.getLng() == null) {
            throw new IllegalArgumentException("위치 정보가 필수입니다.");
        }
        // 다녀오지 않았으면 다녀온 날짜는 무시
        if (!dto.isVisited()) {
            dto.setVisitedDate(null);
        }

        ChecklistEntity entity = dto.dtoToEntity(owner);
        String mediaURL = uploadMedia(mediaFile);
        if (mediaURL != null) entity.setMediaURL(mediaURL);

        ChecklistEntity saved = checklistRepository.save(entity);
        return ChecklistDTO.entityToDto(saved);
    }

    // 지도/목록 노출용 — 휴지통에 없는 가볼곳 조회 (커플 공유)
    @Transactional(readOnly = true)
    public List<ChecklistDTO> getAllChecklists(String uid, UserDetails userDetails) {
        return checklistRepository.findByDeletedFalse().stream()
                .map(ChecklistDTO::entityToDto)
                .collect(Collectors.toList());
    }

    // 본인 소유 체크리스트 수정 (위치 제외 — 제목/내용/타입/방문여부/방문일/이미지)
    @Transactional
    public ChecklistDTO updateChecklist(Long id, ChecklistDTO dto, MultipartFile mediaFile, UserDetails userDetails) {
        ChecklistEntity c = getOwnedChecklist(id, userDetails);

        if (dto.getTitle() != null)   c.setTitle(dto.getTitle());
        if (dto.getContent() != null) c.setContent(dto.getContent());
        if (dto.getType() != null)    c.setType(dto.getType());
        c.setVisited(dto.isVisited());
        c.setVisitedDate(dto.isVisited() ? dto.getVisitedDate() : null);

        // 새 이미지가 있으면 교체 (없으면 기존 이미지 유지)
        String newUrl = uploadMedia(mediaFile);
        if (newUrl != null) c.setMediaURL(newUrl);

        return ChecklistDTO.entityToDto(checklistRepository.save(c));
    }

    // 휴지통으로 이동 (소프트 삭제 · 소유자만)
    @Transactional
    public void moveToTrash(Long id, UserDetails userDetails) {
        ChecklistEntity c = getOwnedChecklist(id, userDetails);
        c.setDeleted(true);
        checklistRepository.save(c);
    }

    // 휴지통에서 복원 (소유자만)
    @Transactional
    public ChecklistDTO restoreChecklist(Long id, UserDetails userDetails) {
        ChecklistEntity c = getOwnedChecklist(id, userDetails);
        c.setDeleted(false);
        return ChecklistDTO.entityToDto(checklistRepository.save(c));
    }

    // 영구 삭제 (소유자만)
    @Transactional
    public void permanentDelete(Long id, UserDetails userDetails) {
        ChecklistEntity c = getOwnedChecklist(id, userDetails);
        checklistRepository.delete(c);
    }

    // 내가 휴지통으로 보낸 가볼곳 목록
    @Transactional(readOnly = true)
    public List<ChecklistDTO> getTrash(String uid, UserDetails userDetails) {
        UserEntity user = getAuthorizedUser(uid, userDetails);
        return checklistRepository.findByOwnerUidAndDeletedTrue(user.getUid()).stream()
                .map(ChecklistDTO::entityToDto)
                .collect(Collectors.toList());
    }
}

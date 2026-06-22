package com.example.Daylog.Service;

import com.example.Daylog.DTO.MemoryDTO;
import com.example.Daylog.Entity.MemoryEntity;
import com.example.Daylog.Entity.UserEntity;
import com.example.Daylog.Repository.MemoryRepository;
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
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final UserRepository userRepository;
    private final Storage storage;

    @Value("${google.cloud.credentials.header}")
    private String googleCloudHeader;
    @Value("${google.cloud.storage.bucket}")
    private String bucket;

    private UserEntity getAuthorizedUser(String uid, UserDetails userDetails) {
        if (userDetails == null || !userDetails.getUsername().equals(uid)) {
            throw new RuntimeException("권한이 없습니다");
        }
        return userRepository.findByUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    }

    // GCS 업로드 로직 (BuildingService와 동일)
    private String uploadMedia(MultipartFile mediaFile) {
        if (mediaFile == null || mediaFile.isEmpty()) return null;
        try {
            UUID uuid = UUID.randomUUID();
            String original = mediaFile.getOriginalFilename();
            String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf(".")) : "";
            String fileName = uuid.toString() + ext;
            String contentType = "image/jpeg"; // 간략화

            BlobId blobId = BlobId.of(bucket, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();
            storage.create(blobInfo, mediaFile.getBytes());
            return googleCloudHeader + fileName;
        } catch (IOException e) {
            throw new RuntimeException("업로드 실패", e);
        }
    }

    @Transactional
    public MemoryDTO createMemory(String uid, MemoryDTO memoryDTO, MultipartFile mediaFile, UserDetails userDetails) {
        UserEntity owner = getAuthorizedUser(uid, userDetails);

        // 위치 데이터가 넘어오지 않은 경우 예외 처리
        if(memoryDTO.getLat() == null || memoryDTO.getLng() == null) {
            throw new IllegalArgumentException("위치 정보가 필수입니다.");
        }

        MemoryEntity memoryEntity = memoryDTO.dtoToEntity(owner);
        String mediaURL = uploadMedia(mediaFile);
        if (mediaURL != null) memoryEntity.setMediaURL(mediaURL);

        MemoryEntity saved = memoryRepository.save(memoryEntity);
        return MemoryDTO.entityToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MemoryDTO> getAllMemories(String uid, UserDetails userDetails) {
        return memoryRepository.findAll().stream()
                .map(MemoryDTO::entityToDto)
                .collect(Collectors.toList());
    }
}
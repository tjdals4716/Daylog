package com.example.Daylog.Controller;

import com.example.Daylog.DTO.PlaceDTO;
import com.example.Daylog.Service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // 상호명/장소 키워드 검색 (네이버 지역검색 프록시)
    @GetMapping("/place")
    public ResponseEntity<List<PlaceDTO>> searchPlace(@RequestParam("query") String query,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(searchService.searchPlace(query));
    }
}

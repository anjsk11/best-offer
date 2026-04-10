package me.anjsk.bestoffer.controller;

import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.annotation.RequireLogin;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.dto.AuctionDetailResponse;
import me.anjsk.bestoffer.dto.AuctionListResponse;
import me.anjsk.bestoffer.service.AuctionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    @RequireLogin
    public ResponseEntity<String> createAuction(@RequestBody AuctionCreateRequest request, HttpSession session) {
        // 인터셉터를 통과했다는 것은 무조건 세션에 LOGIN_USER가 있다는 뜻, 안전
        Long sellerId = (Long) session.getAttribute("LOGIN_USER");

        auctionService.createAuction(request, sellerId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("경매가 성공적으로 등록되었습니다.");
    }

    // 아무나 경매를 조회할 수 있게 로그인이 필요없음을 참고
    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionDetailResponse> getAuction(@PathVariable Long auctionId) {
        AuctionDetailResponse response = auctionService.getAuction(auctionId);
        return ResponseEntity
                .ok(response);
    }

    // 목록 조회 (예: GET /api/v1/auctions?page=0&size=10)
    @GetMapping
    public ResponseEntity<Page<AuctionListResponse>> getAuctions(
            // 기본값: 한 페이지에 10개씩, ID 기준 내림차순(최신순) 정렬
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuctionListResponse> response = auctionService.getAuctions(pageable);
        return ResponseEntity
                .ok(response);
    }
}

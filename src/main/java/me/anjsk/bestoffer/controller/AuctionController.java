package me.anjsk.bestoffer.controller;

import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.annotation.RequireLogin;
import me.anjsk.bestoffer.dto.AuctionCreateRequest;
import me.anjsk.bestoffer.service.AuctionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

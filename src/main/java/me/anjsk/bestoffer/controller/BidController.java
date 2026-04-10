package me.anjsk.bestoffer.controller;

import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.annotation.RequireLogin;
import me.anjsk.bestoffer.dto.BidRequest;
import me.anjsk.bestoffer.service.BidService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) { this.bidService = bidService; }

    @PostMapping
    @RequireLogin
    public ResponseEntity<String> placeBid(
            @PathVariable Long auctionId,
            @RequestBody BidRequest request,
            HttpSession session) {

        Long bidderId = (Long) session.getAttribute("LOGIN_USER");
        // 요청이 컨트롤러에 도달한 즉시 시간을 기록
        LocalDateTime arrivalTime = LocalDateTime.now();

        bidService.placeBid(auctionId, request.getBidPrice(), bidderId, arrivalTime);

        return ResponseEntity.ok("입찰이 완료되었습니다.");
    }
}

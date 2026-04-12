package me.anjsk.bestoffer.controller;

import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.annotation.RequireLogin;
import me.anjsk.bestoffer.dto.BidRequest;
import me.anjsk.bestoffer.facade.BidLockFacade;
import me.anjsk.bestoffer.service.BidService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
public class BidController {

    // private final BidService bidService;
    private final BidLockFacade bidLockFacade;

    public BidController( BidLockFacade bidLockFacade ) {
        this.bidLockFacade = bidLockFacade;
    }

    @PostMapping
    // @RequireLogin       // TEST MODE
    public ResponseEntity<String> placeBid(
            @PathVariable Long auctionId,
            @RequestBody BidRequest request,
            HttpSession session) {

        // Long bidderId = (Long) session.getAttribute("LOGIN_USER");       // TEST MODE

        Long bidderId = request.getBidderId();      // 테스트용

        // 요청이 컨트롤러에 도달한 즉시 시간을 기록
        LocalDateTime arrivalTime = LocalDateTime.now();

         // bidService.placeBid(auctionId, request.getBidPrice(), bidderId, arrivalTime);    // 분산 락 적용을 위해 대체
        bidLockFacade.placeBidWithLock(auctionId, request.getBidPrice(), bidderId, arrivalTime);

        return ResponseEntity.ok("입찰이 완료되었습니다.");
    }
}

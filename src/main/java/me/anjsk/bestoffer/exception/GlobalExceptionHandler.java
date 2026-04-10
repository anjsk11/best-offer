package me.anjsk.bestoffer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    // 회원가입 시 이미 등록된 이메일일 때 (409 Conflict)
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<String> handleDuplicateEmailException(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용 중인 이메일입니다. 다른 이메일을 입력해주세요.");
    }

    // 아이디/비번이 틀려 로그인 실패일 때 (401 Unauthorized)
    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<String> handleLoginFailedException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 잘못되었습니다.");
    }

    // 로그인이 필요할 때 (401 Unauthorized)
    @ExceptionHandler(LoginRequiredException.class)
    public ResponseEntity<String> handleLoginRequiredException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요한 서비스입니다.");
    }

    // 관리자 권한이 필요할 때 (403 Forbidden)
    @ExceptionHandler(AdminRequiredException.class)
    public ResponseEntity<String> handleAdminRequiredException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자 외에는 접근할 수 없습니다.");
    }

    // 유저를 찾을 수 없을 때 (404 Not Found)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 사용자입니다.");
    }

    // 마감 시간이 잘못되었을 때 (400 Bad Request)
    @ExceptionHandler(InvalidEndTimeException.class)
    public ResponseEntity<String> handleInvalidEndTimeException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("경매 마감 시간은 최소 1시간 이후로 설정해야 합니다.");
    }

    // 가격이 잘못되었을 때 (400 Bad Request)
    @ExceptionHandler(InvalidPriceException.class)
    public ResponseEntity<String> handleInvalidPriceException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("가격은 0원 이상이어야 합니다.");
    }

    // 경매를 찾을 수 없을 때 (404 Not Found)
    @ExceptionHandler(AuctionNotFoundException.class)
    public ResponseEntity<String> handleAuctionNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 경매입니다.");
    }

    // 권한이 없는 행동을 하였을 때 (403 Forbidden)
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> handleUnauthorizedAccessException() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("해당 행동에 대한 권한이 없습니다.");
    }

    // 본인 경매에 입찰 시 (400 Bad Request)
    @ExceptionHandler(SelfBidException.class)
    public ResponseEntity<String> handleSelfBidException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("본인의 경매에는 입찰할 수 없습니다.");
    }

    // 입찰가가 현재가보다 낮거나 같을 때 (400 Bad Request)
    @ExceptionHandler(LowBidPriceException.class)
    public ResponseEntity<String> handleLowBidPriceException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("현재 최고가보다 높은 금액만 입찰 가능합니다.");
    }

    // 이미 종료된 경매일 때 (400 Bad Request)
    @ExceptionHandler(AuctionClosedException.class)
    public ResponseEntity<String> handleAuctionClosedException() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 종료된 경매입니다.");
    }


    //===========================
    // 그 외 예상치 못한 모든 예외 처리
    //===========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllUncaughtException(Exception e) {
        e.printStackTrace(); // 로컬 콘솔에서 에러 추적을 위해 남겨둠
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부에서 예상치 못한 문제가 발생했습니다.");
    }
}
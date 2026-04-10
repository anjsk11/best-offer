package me.anjsk.bestoffer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<String> handleDuplicateEmailException(DuplicateEmailException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("이미 사용 중인 이메일입니다. 다른 이메일을 입력해주세요.");
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<String> handleLoginFailedException() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                .body("아이디 또는 비밀번호가 잘못되었습니다.");
    }

    // 로그인이 필요할 때 (401 Unauthorized)
    @ExceptionHandler(LoginRequiredException.class)
    public ResponseEntity<String> handleLoginRequiredException() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("로그인이 필요한 서비스입니다.");
    }

    // 관리자 권한이 필요할 때 (403 Forbidden)
    @ExceptionHandler(AdminRequiredException.class)
    public ResponseEntity<String> handleAdminRequiredException() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("접근 권한이 없습니다.");
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


    //===========================
    // 그 외 예상치 못한 모든 예외 처리
    //===========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllException(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부 오류가 발생했습니다.");
    }
}
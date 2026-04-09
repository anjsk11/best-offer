package me.anjsk.bestoffer.controller;

import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.dto.LoginRequest;
import me.anjsk.bestoffer.dto.SignupRequest;
import me.anjsk.bestoffer.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity
                .ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request, HttpSession session) {
        userService.login(request, session);
        return ResponseEntity
                .ok("로그인이 성공적으로 완료되었습니다.");
    }
}
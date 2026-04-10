package me.anjsk.bestoffer.service;

import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.dto.LoginRequest;
import me.anjsk.bestoffer.dto.SignupRequest;
import me.anjsk.bestoffer.exception.DuplicateEmailException;
import me.anjsk.bestoffer.exception.LoginFailedException;
import me.anjsk.bestoffer.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Long signup(SignupRequest request) {
        // 이메일 중복 체크
        validateDuplicateEmail(request.getEmail());

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                encodedPassword,
                request.getNickname(),
                UserRole.ROLE_USER
        );

        // 저장 및 ID 반환
        return userRepository.save(user).getId();
    }

    private void validateDuplicateEmail(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new DuplicateEmailException();
                });
    }

    @Transactional(readOnly = true)
    public void login(LoginRequest request, HttpSession session) {
        // 이메일로 유저 찾기
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(LoginFailedException::new); // 이메일 없으면 예외

        // 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new LoginFailedException(); // 비밀번호 틀려도 같은 예외
        }

        // 로그인 성공 시 세션에 정보 저장
        session.setAttribute("LOGIN_USER", user.getId());
        session.setAttribute("USER_ROLE", user.getRole());
    }
}
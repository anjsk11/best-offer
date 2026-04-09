package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.dto.SignupRequest;
import me.anjsk.bestoffer.exception.DuplicateEmailException;
import me.anjsk.bestoffer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks // Mock들을 UserService에 주입
    private UserService userService;

    // 테스트 데이터 준비
    private SignupRequest signupRequest;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest("test@test.com", "password123", "테스터");
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // 중복된 이메일이 없다고 가정 (Empty Optional 반환)
        given(userRepository.findByEmail(signupRequest.getEmail()))
                .willReturn(Optional.empty());

        // 비밀번호 암호화 결과 설정
        given(passwordEncoder.encode(signupRequest.getPassword()))
                .willReturn("encoded_password");

        User savedUser = new User(
                signupRequest.getEmail(),
                "encoded_password",
                signupRequest.getNickname(),
                UserRole.ROLE_USER
        );
        // Reflection 등을 쓰지 않고 ID를 주입할 수 없으니, Mock을 더 활용
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // 테스트 메서드 실행
        userService.signup(signupRequest);

        // 검증
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void signup_Fail_DuplicateEmail() {
        // Given
        // 이미 해당 이메일을 가진 유저가 있다고 가정
        User existingUser = new User("test@test.com", "...", "...", UserRole.ROLE_USER);
        given(userRepository.findByEmail(signupRequest.getEmail()))
                .willReturn(Optional.of(existingUser));

        // DuplicateEmailException이 터지는지 검증
        assertThrows(DuplicateEmailException.class, () -> {
            userService.signup(signupRequest);
        });

        // 저장이 절대 호출되지 않았는지 확인
        verify(userRepository, never()).save(any(User.class));
    }
}

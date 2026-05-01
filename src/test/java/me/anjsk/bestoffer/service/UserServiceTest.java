package me.anjsk.bestoffer.service;

import jakarta.servlet.http.HttpSession;
import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.dto.LoginRequest;
import me.anjsk.bestoffer.dto.SignupRequest;
import me.anjsk.bestoffer.exception.DuplicateEmailException;
import me.anjsk.bestoffer.exception.LoginFailedException;
import me.anjsk.bestoffer.repository.UserRepository;
import me.anjsk.bestoffer.support.TestFixtures;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "test@test.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded_password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        SignupRequest request = new SignupRequest(EMAIL, RAW_PASSWORD, "tester");
        User savedUser = TestFixtures.user(1L, EMAIL, "tester");

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        userService.signup(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void signup_Fail_DuplicateEmail() {
        SignupRequest request = new SignupRequest(EMAIL, RAW_PASSWORD, "tester");
        User existingUser = TestFixtures.user(1L, EMAIL, "tester");

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(existingUser));

        assertThrows(DuplicateEmailException.class, () -> userService.signup(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
        User user = TestFixtures.user(1L, EMAIL, "tester");

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, user.getPassword())).willReturn(true);

        userService.login(request, session);

        verify(session).setAttribute("LOGIN_USER", user.getId());
        verify(session).setAttribute("USER_ROLE", UserRole.ROLE_USER);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_EmailNotFound() {
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        assertThrows(LoginFailedException.class, () -> userService.login(request, session));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_WrongPassword() {
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
        User user = TestFixtures.user(1L, EMAIL, "tester");

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PASSWORD, user.getPassword())).willReturn(false);

        assertThrows(LoginFailedException.class, () -> userService.login(request, session));
    }
}

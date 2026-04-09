package me.anjsk.bestoffer.service;

import me.anjsk.bestoffer.domain.User;
import me.anjsk.bestoffer.domain.enums.UserRole;
import me.anjsk.bestoffer.dto.SignupRequest;
import me.anjsk.bestoffer.exception.DuplicateEmailException;
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
}